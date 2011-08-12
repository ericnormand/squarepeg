;; Copyright (c) Eric Normand. All rights reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 [1] which can be found in the file
;; epl-v10.html at the root of this distribution.  By using this
;; software in any fashion, you are agreeing to be bound by the terms
;; of this license.
;;
;; [1]: http://opensource.org/licenses/eclipse-1.0.php
;;
;; You must not remove this notice, or any other, from this software.
(ns clj-peg)

;; A rule is a function [input bindings context memo]
;; -> {:i seq :b {symbol vec} :r value :s seqvalue :m memo}
;; OR {:fail str :m memo} to signal failure
;; input is a seq of the rest of the input
;; bindings is a map from keyword to value
;; context is a map from keyword to value (immutable)
;; memo is the memoization hash
;; :fail is a failure message for the user

;; define success and failure
(defn fail
  "Fail a rule call with the given msg"
  [msg memo]
  {:fail msg :m memo})
(defn failure?
  "Is x a failure?"
  [x]
  (:fail x))

(defn succeed
  "Succeed a rule call."
  [return sreturn input bindings memo]
  {:i input :b bindings :r return :s sreturn :m memo})
(defn success?
  "Is x a success?"
  [x]
  (not (failure? x)))

(defn- coerce [v t]
  (if (and (= :string t) (sequential? v) (every? char? v))
    (apply str v)
    v))

(defn mkfn
  "Create a function useful for calling a rule at the REPL."
  [f]
  (fn ff
    ([input]
       (let [context (if (string? input)
                       {:expected-type :string}
                       {})]
         (ff input context)))
    ([input context]
       (let [r (f input {} context {})]
         (if (success? r)
           (coerce (:r r) (:expected-type context))
           (throw (RuntimeException. (:fail r))))))
    ([input bindings context memo]
       (f input bindings context memo))))

(defn mknot
  "Create a rule that fails if the next input matches rule and
succeeds otherwise."
  [rule]
  (fn not-fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (failure? r)
        (succeed nil [] input bindings (:m r))
        (fail (str "NOT failed") (:m r))))))

(defn mkbind
  "Create a rule that binds the return value of rule to var."
  [rule var]
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) (assoc (:b r) var (coerce (:r r) (:expected-type context))) (:m r))
        r))))

(defn mkpr
  "Create a rule that consumes one item from the input. If pr applied
to that item returns true, the rule succeeds. Otherwise, fail."
  [pr]
  (fn [input bindings context memo]
    (if (nil? (seq input))
      (fail "End of input" memo)
      (let [i (first input)]
        (if (pr i)
          (succeed i [i] (rest input) bindings memo)
          (fail (str i " does not match predicate.") memo))))))

(defn mkret
  "Create a rule that returns a value. The value is computed by ret,
which is a function of a bindings map. The rule also binds the return
value of rule to :ret."
  [rule ret]
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (success? r)
        (let [b (assoc (:b r) :ret (coerce (:r r) (:expected-type context)))
              v (ret b context)]
          (succeed v [v] (:i r) b (:m r)))
        r))))

(defn mknothing
  "Create a rule that succeeds, fails, and consumes just like rule but
returns nothing."
  [rule]
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (success? r)
        (succeed nil [] (:i r) (:b r) (:m r))
        r))))

;; helper function to concatenate vecs
(defn- vec-cat [a b]
  (reduce conj a b))

(defn- noreturn? [r]
  (and (nil? (:r r)) (nil? (seq (:s r)))))

(defn- catreturns [r1 r2 context]
  (cond
   (noreturn? r1)
   [(coerce (:r r2) (:expected-type context)) (:s r2)]

   (noreturn? r2)
   [(coerce (:r r1) (:expected-type context)) (:s r1)]

   :otherwise
   (let [val (vec-cat (:s r1) (:s r2))]
     [(coerce val (:expected-type context)) val])))

(defn mkseq
  "Create a rule that matches all of rules in order. Returns a vec of
the return values of each."
  ([]
     #(succeed nil [] %1 %2 %4))
  ([rule]
     rule)
  ([rule1 rule2]
     (fn [input bindings context memo]
       (let [r1 (rule1 input bindings context memo)]
         (if (failure? r1)
           r1
           (let [r2 (rule2 (:i r1) (:b r1) context (:m r1))]
             (if (failure? r2)
               r2
               (let [[r s] (catreturns r1 r2 context)]
                 (succeed r s (:i r2) (:b r2) (:m r2)))))))))
  ([rule1 rule2 & rules]
     (reduce mkseq  (cons rule1 (cons rule2 rules)))))

(defn mkalt
  "Create a rule which succeeds if one of rules succeeds."
  ([]
     #(fail "no rules to match" %4))
  ([rule]
     rule)
  ([rule1 rule2]
     (fn [input bindings context memo]
       (let [r1 (rule1 input bindings context memo)]
         (if (failure? r1)
           (rule2 input bindings context (:m r1))
           r1))))
  ([rule1 rule2 & rules]
     (reduce mkalt (cons rule1 (cons rule2 rules)))))

(defn mkpred
  "Create a rule which never returns a value or consumes input. It
succeeds if f returns non-nil and fails otherwise. f is a function of
bindings and context."
  [f]
  (fn [input bindings context memo]
    (if (f bindings context)
      (succeed nil [] input bindings memo)
      (fail "Failed to match predicate" memo))))

(defn mkzom
  "Create a rule which matches rule consecutively as many times as
possible. The rule never fails. Returns a seq of all matched values."
  [rule]
  (fn [input bindings context memo]
    (loop [val [] input input bindings bindings memo memo]
      (let [r (rule input bindings context memo)]
        (if (success? r)
          (recur (vec-cat val (:s r)) (:i r) (:b r) (:m r))
          (succeed val val input bindings memo))))))

(defn mkscope
  "Create a rule which contains the scope of the given rule. Bindings
made in rule do not escape this rule's scope."
  [rule]
  (fn [input bindings context memo]
    (let [r (rule input {} context memo)]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) bindings (:m r))
        r))))

(defn mksub
  "Create a rule which applies a rule to a nested seq within the
input."
  [rule]
  (fn [input bindings context memo]
    (if (and (seq input) (sequential? (first input)))
      (let [r (rule (first input) bindings context memo)]
        (if (success? r)
          (succeed (:r r) (:s r) (rest input) (:b r) (:m r))
          r))
      (fail "Input not a seq." memo))))

(defn mk1om
  "Create a rule which matches rule as many times as possible but at
least once."
  [rule]
  (mkseq rule (mkzom rule)))

(defn mkopt
  "Create a rule which matches rule or not, but never fails."
  [rule]
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (failure? r)
        (succeed [] [] input bindings (:m r))
        r))))

;; literal matcher
(defn mklit
  "Create a rule which consumes one item of input. If it is equal to
l, succeed."
  [l]
  (mkpr #(= l %)))

(defn mkstr
  "Create a rule which matches all of the characters of a String s in
sequence."
  [s]
  (apply mkseq (map mklit s)))

(defn mkmemo
  "Create a rule that memoizes the given rule."
  [rule]
  (let [memoid (gensym)]
    (fn [input bindings context memo]
      (if memo
        (let [key [memoid input bindings]
              mv (memo key)]
          (when (:print-hits context)
            (println "hits: "(:hit memo) " miss: " (:miss memo)))
          (if mv
            (assoc mv :m (assoc memo :hit (inc (memo :hit 0))))
            (let [r (rule input bindings context memo)]
              (assoc r :m (assoc (:m r)
                            key  (dissoc r :m)
                            :miss (inc ((:m r) :miss 0)))))))
        (rule input bindings context memo)))))

(defn- unhead [l tl]
  (cond
   (nil? (seq l))
   nil
   (= l tl)
   nil
   :otherwise
   (lazy-seq (cons (first l) (unhead (rest l) tl)))))

(defn mkmatch
  "Create a rule that binds the input sequence that is matched
  to :match."
  [rule]
  (fn [input bindings context memo]
    (let [before input
          r (rule input bindings context memo)
          after (:i r)]
      (if (failure? r)
        r
        (succeed (:r r)
                 (:s r)
                 (:i r)
                 (assoc (:b r)
                   :match (conj (:match (:b r))
                                (coerce (unhead before after) (:expected-type context))))
                 (:m r))))))

;; utilities

(defn- transbody
  ([]
     `(mkalt))
  ([a]
     (cond
      (and (list? a)
           (= '=> (first a)))
      `(mksub ~(apply transbody (rest a)))
      (and (list? a)
           (= 'or (first a)))
      (apply transbody (rest a))
      (and (list? a)
           (= '? (first a)))
      `(mkpred (fn ~@(rest a)))
      (and (map? a)
           (keyword? (first (keys a))))
      `(mkbind ~(transbody (first (vals a))) ~(first (keys a)))
      (and (map? a)
           (= '! (first (keys a))))
      `(mknot ~(transbody (first (vals a))))
      (and (map? a)
           (= '% (first (keys a))))
      `(mknothing ~(transbody (first (vals a))))
      (and (map? a)
           (= '? (first (keys a))))
      `(mkpr ~(first (vals a)))
      (and (map? a)
           (= '* (first (vals a))))
      `(mkzom ~(transbody (first (keys a))))
      (and (map? a)
           (= '+ (first (vals a))))
      `(mk1om ~(transbody (first (keys a))))      
      (and (map? a)
           (= '? (first (vals a))))
      `(mkopt ~(transbody (first (keys a))))      
      (set? a)
      (throw (RuntimeException. "Set (return function) can't be first."))
      (symbol? a)
      `(fn [i# b# c# m#]
         (~a i# b# c# m#))
      (string? a)
      `(mkstr ~a)
      (or (char? a)
          (number? a))
      `(mklit ~a)
      (vector? a)
      `(mkseq ~@(map transbody a))
      :otherwise
      a))
  ([a b & cs]
     (cond
      (and (set? b)
           (keyword? (first b)))
      `(mkalt (mkret ~(transbody a)
                     (fn [b# c#]
                       (if (contains? b# ~(first b))
                         (~(first b) b#)
                         (throw (RuntimeException. (str "Binding does not exist: " ~(first b)))))))
              ~(apply transbody cs))
      (and (set? b)
           (some #(% (first b)) [number? char? string? vector? set? map? #(and (seq? %) (= 'quote (first %)))]))
      `(mkalt (mkret ~(transbody a) (constantly ~(first b)))
              ~(apply transbody cs))
      (set? b)
      `(mkalt (mkret ~(transbody a) ~(first b))
              ~(apply transbody cs))
      :otherwise
      `(mkalt ~(transbody a) ~(apply transbody (cons b cs))))))

(defmacro defrule
  ([name & body]
     (list 'def
           (with-meta name
             {:arglists ''([input] [input context] [input bindings context memo])
              :doc (str name " is a clj-peg parser rule. Call with a seq of input or
        use it as a rule (4 arguments).")})
           `(mkfn (mkscope (mkmemo ~(apply transbody body)))))))

(def always (mkpred (constantly true)))
(def never  (mkpred (constantly false)))
(def anything (mkpr (constantly true)))

(def whitespace (mkpr #(Character/isSpace %)))

(def digit (mkpr #(Character/isDigit %)))
(def alpha (mkpr #(Character/isLetter %)))
(def alphanum (mkpr #(Character/isLetterOrDigit %)))

(def end (mknot anything))

(def match-char    (mkpr char?    ))
(def match-float   (mkpr float?   ))
(def match-hash    (mkpr map?     ))
(def match-integer (mkpr integer? ))
(def match-keyword (mkpr keyword? ))
(def match-list    (mkpr list?    ))
(def match-number  (mkpr number?  ))
(def match-string  (mkpr string?  ))
(def match-symbol  (mkpr symbol?  ))
(def match-vector  (mkpr vector?  ))

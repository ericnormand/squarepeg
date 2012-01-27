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
(ns squarepeg.core)

;; A rule is a function [input bindings context memo]
;; -> {:i seq :b {symbol vec} :r value :s seqvalue :m memo}
;; OR {:fail str :m memo} to signal failure
;; input is a seq of the rest of the input
;; bindings is a map from keyword to value
;; context is a map from keyword to value (immutable)
;; memo is the memoization hash
;; :fail is a failure message for the user

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
succeeds otherwise.

`mknot` inverts a rule. Given a rule, `mknot` returns a new
rule that fails when the given rule succeeds, and vice versa. It never
returns a value and never consumes input. It is most useful for making
a rule that says \"and is not followed by . . .\".

Example:

    (def not-followed-by-whitespace (mknot whitespace))
    (not-followed-by-whitespace \"abc\" {} {} {}) 
        => {:r nil :s [] :i (\\a \\b \\c) :b {} :m {})
    (not-followed-by-whitespace \" abc\" {}) => {:fail \"NOT failed\"}"
  [rule]
  (fn not-fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (failure? r)
        (succeed nil [] input bindings (:m r))
        (fail (str "NOT failed") (:m r))))))

(defn mkbind
  "Create a rule that binds the return value of rule to var.

`mkbind` creates a rule that will bind the return value of its first
argument (a rule) to its second argument (typically a
keyword). Binding is useful if you want to refer to the return value
of a rule later.

Example:

    ;; bind the matched digits to :digits
    (def digits (mkbind 
        (mk1om (mkpr #(Character/isDigit %))) :digits))
    (digits \"123\" {} {} {}) 
        => {:r [\\1 \\2 \\3] :s [\\1 \\2 \\3] :i nil :b {:digits [\\1 \\2 \\3]}
            :m {}}"
  [rule var]
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) (assoc (:b r) var (coerce (:r r) (:expected-type context))) (:m r))
        r))))

(defn mkpr
  "Create a rule that consumes one item from the input. If pr applied
to that item returns true, the rule succeeds. Otherwise, fail.

`mkpr` creates a rule that consumes one item from the input. It then
calls the given predicate on it. If the predicate returns nil, the
rule fails. Otherwise, the rule passes. The return value is the item
consumed. (It does not consume input if the predicate fails.)

Example:

    ;; match only even numbers
    (def even (mkpr evenp))"
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
value of rule to :ret.

<code>mkret</code> changes the current return value. It takes a
function which takes the bindings map.

Example:

    ;; parse digit characters as an int
    (def integer (mkret (mkbind (mk1om (mkpr #(Character/isDigit %))) 
                                :digits) 
                        #(Integer/parseInt (:digits %))))"
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
returns nothing.

<code>mknothing</code> makes a rule return nothing.

Example:

    ;; ignore whitespace
    (def ignorewhitespace (mknothing (mk1om #(Character/isSpace %))))"
  [rule]
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (success? r)
        (succeed nil [] (:i r) (:b r) (:m r))
        r))))

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
the return values of each.

<code>mkseq</code> takes any number of rules and creates a rule that must
match all of them in sequence.

Example:

    ;; match three even numbers then two odds
    (def even3odd2 (mkseq even even even odd odd))"
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
     (reduce mkseq (mkseq rule1 rule2) rules)))

(defn mkalt
  "Create a rule which succeeds if one of rules succeeds.

<code>mkalt</code> takes any number of rules and tries each one in
order. The first rule that matches is returned.

Example:

    ;; match 'a' or 'b' or 'c' (in that order)
    (def aorborc (mkalt (mklit \\a) (mklit \\b) (mklit \\c)))"
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
     (reduce mkalt (mkalt rule1 rule2) rules)))

(defn mkpred
  "Create a rule which never returns a value or consumes input. It
succeeds if f returns non-nil and fails otherwise. f is a function of
bindings and context.

<code>mkpred</code> takes a predicate and returns a rule that succeeds
when the predicate applied to the first input element succeeds. It
never returns a value and never consumes input. The predicate operates
on the bindings map.

Example:

    ;; match two numbers if their sum > 100
    (def sum>100 (mkseq (mkbind integer :a) (mkbind integer :b)
                        (mkpred #(> (+ (:a %) (:b %)) 100))))"
  [f]
  (fn [input bindings context memo]
    (if (f bindings context)
      (succeed nil [] input bindings memo)
      (fail "Failed to match predicate" memo))))

(defn mkzom
  "Create a rule which matches rule consecutively as many times as
possible. The rule never fails. Returns a seq of all matched values.

<code>mkzom</code> creates a rule which matches zero or more times. It
will match the most possible times. It never fails.

Example:

    ;; match zero or more whitespace
    (def w* (mkzom (mkpr #(Character/isSpace %))))"
  [rule]
  (fn [input bindings context memo]
    (loop [val [] input input bindings bindings memo memo]
      (let [r (rule input bindings context memo)]
        (if (success? r)
          (recur (vec-cat val (:s r)) (:i r) (:b r) (:m r))
          (succeed val val input bindings memo))))))

(defn mkscope
  "Create a rule which contains the scope of the given rule. Bindings
made in rule do not escape this rule's scope.

<code>mkscope</code> creates a scope protector around a rule so that
bindings that the given rule creates do not leak into the current
scope. This function should be used around your own rules.

Example:

    ;; a rule that binds but does not protect
    (def as (mkbind (mk1om (mklit \\a)) :as))
    ;; a rule that calls as
    (def xab (mkseq (mkbind (mk1om (mklit \\x)) :as) ;; bind to :as
                    (mkscope as)                    ;; make sure as
                                                    ;; does not bind
                    (mk1om (mklit \\b))))"
  [rule]
  (fn [input bindings context memo]
    (let [r (rule input {} context memo)]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) bindings (:m r))
        r))))

(defn mksub
  "Create a rule which applies a rule to a nested seq within the
input.

<code>mksub</code> creates a rule that matches a nested seq within the
seq.

Example:

    ;; match a seq of ones
    (def ones (mk0om (mklit 1)))
    ;; match a seq of ones followed by 2s
    (def onesthentwos (mkseq (mksub ones) (mk0om (mklit 2))))
    (onesthentwos [[1 1 1] 2 2] {}) => SUCCESS
    (onesthentwos [1 1 1 2 2] {}) => FAILURE"
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
least once.

<code>mk1om</code> creates a rule that matches the given rule at least
once. Returns all matches.

Example:

    ;; match one or more digits
    (def digits (mk1om digit))
    (digits \"1234\" {} {} {}) 
        => {:r [\\1 \\2 \\3 \\4] :s [\\1 \\2 \\3 \\4] :i nil :b {} :m {}}
    (digits \"123 4\" {} {} {}) 
        => {:r [\\1 \\2 \\3] :s [\\1 \\2 \\3] :i (\\4) :b {} :m {}}"
  [rule]
  (mkseq rule (mkzom rule)))

(defn mkopt
  "Create a rule which matches rule or not, but never fails.

<code>mkopt</code> creates a rule that always succeeds. If the rule it
is given matches, it returns its value. Otherwise, it succeeds with no
return.

Example:

    ;; optionally match 'xyz'
    (def xyz? (mkopt (mkstr \"xyz\")))"
  [rule]
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (failure? r)
        (succeed [] [] input bindings (:m r))
        r))))

(defn mklit
  "Create a rule which consumes one item of input. If it is equal to
l, succeed.

<code>mklit</code> creates a rule that matches a value if it is equal.

Example:

    ;; match the number 12
    (def twelve (mklit 12))
    (twelve [12] {} {} {}) => {:r 12 :s [12] :i nil :b {} :m {}}"
  [l]
  (mkpr #(= l %)))

(defn mkstr
  "Create a rule which matches all of the characters of a String s in
sequence.

<code>mkstr</code> create a sequential rule that matches all of the
characters of a string.

Example:

    ;; match the string \"hello\" followed by whitespace
    (def hellow+ (mkseq [(mkstr \"hello\") (mk1om whitespace)]))"
  [s]
  (apply mkseq (map mklit s)))

(defn mkmemo
  "Create a rule that memoizes the given rule.

<code>mkmemo</code> creates a new rule which memoizes the given
rule. The best way to use this is directly inside of a
<code>mkscope</code> when defining a top-level rule for most efficient
results. Memoizing is done to trade space efficiency for time
efficiency. Effectively using mkmemo will make a parse use linear
space and linear time with respect to input size."
  [rule]
  (let [memoid (gensym)]
    (fn [input bindings context memo]
      (if memo
        (let [key [memoid input bindings]
              mv (memo key)]
          (when (:print-hits context)
            (println "hits: " (:hit memo) " miss: " (:miss memo)))
          (if mv
            (assoc mv :m (assoc memo :hit (inc (memo :hit 0))))
            (let [r (rule input bindings context memo)]
              (assoc r :m (assoc (:m r)
                            key  (dissoc r :m)
                            :miss (inc ((:m r) :miss 0)))))))
        (rule input bindings context memo)))))

(defn- unhead
  "Given a list and a tail of that list, return the head."
  [l tl]
  (cond
   (nil? (seq l))
   nil
   (= l tl)
   nil
   :otherwise
   (lazy-seq (cons (first l) (unhead (rest l) tl)))))

(defn mkmatch
  "Create a rule that binds the input sequence that is matched
  to :match.

<code>mkmatch</code> create a new rule which returns the matched
portion of the input. It binds that portion of the input matched by
the given rule to :match. It also coerces Strings if possible.

Example:

    ;; match a \"SELECT\" statement in a contrived query language.
    ;; perform a \"lookup\" of everything after the SELECT followed
    ;; by whitespace
    (def selectstmt (mkscope (mkmemo (mkret (mkseq [(mkstr \"SELECT\") w+
       (mkmatch (mk1om anything))]) (fn [b c] (lookup (:match b))))))"
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
                   :match (coerce (unhead before after) (:expected-type context)))
                 (:m r))))))

(defn mklower
  "Make a rule that transforms the input to lowercase. This is helpful
  for creating a rule that is case insensitive. Rules executed after
  this rule will see the original input."
  [rule]
  (fn [input bindings context memo]
    (let [ci (map #(if (char? %)
                     (Character/toLowerCase %)
                     %) input)
          r (rule ci bindings context memo)
          after (:i r)]
      (if (failure? r)
        r
        (let [ri (unhead ci after)
              newinput (drop (count ri) input)]
         (succeed (:r r)
                  (:s r)
                  newinput
                  (:b r)
                  (:m r)))))))

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
           (= '$ (first (keys a))))
      `(mkmatch ~(transbody (first (vals a))))
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
              :doc (str name " is a squarepeg parser rule. Call with a
        seq of input or use it as a rule (4 arguments).")})
           `(mkfn (mkscope (mkmemo ~(apply transbody body)))))))

(defrule always   (mkpred (constantly true)))
(defrule never    (mkpred (constantly false)))
(defrule anything (mkpr   (constantly true)))

(defrule whitespace (mkpr #(Character/isSpace %)))

(defrule digit    (mkpr #(Character/isDigit         %)))
(defrule alpha    (mkpr #(Character/isLetter        %)))
(defrule alphanum (mkpr #(Character/isLetterOrDigit %)))

(defrule end (mknot anything))

(defrule match-char    (mkpr char?    ))
(defrule match-float   (mkpr float?   ))
(defrule match-hash    (mkpr map?     ))
(defrule match-integer (mkpr integer? ))
(defrule match-keyword (mkpr keyword? ))
(defrule match-list    (mkpr list?    ))
(defrule match-number  (mkpr number?  ))
(defrule match-string  (mkpr string?  ))
(defrule match-symbol  (mkpr symbol?  ))
(defrule match-vector  (mkpr vector?  ))

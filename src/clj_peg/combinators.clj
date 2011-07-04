(ns clj-peg.combinators)

;; A rule is a function [input bindings context memo]
;; -> {:i seq :b {symbol vec} :r vec :m memo}
;; OR {:fail str :m memo} to signal failure
;; input is a seq of the rest of the input
;; bindings is a map from keyword to value
;; context is a map from keyword to value (immutable)
;; memo is the memoization hash
;; :fail is a failure message for the user

;; define success and failure
(defn fail [msg memo]
  "Fail a rule call with the given msg"
  {:fail msg :m memo})
(defn failure? [x]
  "Is x a failure?"
  (:fail x))

(defn succeed [return sreturn input bindings memo]
  "Succeed a rule call."
  {:i input :b bindings :r return :s sreturn :m memo})
(defn success? [x]
  "Is x a success?"
  (not (failure? x)))

(defn mkfn [f]
  "Create a function useful for calling a rule at the REPL."
  (fn ff
    ([input]
       (ff input {}))
    ([input context]
       (let [r (f input {} context {})]
         (if (success? r)
           (:r r)
           (throw (RuntimeException. (:fail r))))))
    ([input bindings context memo]
       (f input bindings context memo))))

(defn mknot [rule]
  "Create a rule that fails if the next input matches rule and
succeeds otherwise."
  (fn not-fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (failure? r)
        (succeed nil [] input bindings (:m r))
        (fail (str "NOT failed") (:m r))))))

(defn mkbind [rule var]
  "Create a rule that binds the return value of rule to var."
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) (assoc (:b r) var (:r r)) (:m r))
        r))))

(defn mkpr [pr]
  "Create a rule that consumes one item from the input. If pr applied
to that item returns true, the rule succeeds. Otherwise, fail."
  (fn [input bindings context memo]
    (if (nil? (seq input))
      (fail "End of input" memo)
      (let [i (first input)]
        (if (pr i)
          (succeed i [i] (rest input) bindings memo)
          (fail (str i " does not match predicate.") memo))))))

(defn mkret [rule ret]
  "Create a rule that returns a value. The value is computed by ret,
which is a function of a bindings map. The rule also binds the return
value of rule to :ret."
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (success? r)
        (let [b (assoc (:b r) :ret (:r r))
              v (ret b context)]
          (succeed v [v] (:i r) b (:m r)))
        r))))

(defn mknothing [rule]
  "Create a rule that succeeds, fails, and consumes just like rule but
returns nothing."
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

(defn- catreturns [r1 r2]
  (cond
   (noreturn? r1)
   [(:r r2) (:s r2)]

   (noreturn? r2)
   [(:r r1) (:s r1)]

   :otherwise
   (let [val (vec-cat (:s r1) (:s r2))]
     [val val])))

(defn mkcat [rule1 rule2]
  "Create a rule that matches rule1 followed by rule2. Returns a vec
of all return values."
  (fn [input bindings context memo]
    (let [r1 (rule1 input bindings context memo)]
      (if (failure? r1)
        r1
        (let [r2 (rule2 (:i r1) (:b r1) context (:m r1))]
          (if (failure? r2)
            r2
            (let [[r s] (catreturns r1 r2)]
              (succeed r s (:i r2) (:b r2) (:m r2)))))))))

(defn mkseq [rules]
  "Create a rule that matches all of rules in order. Returns a vec of
the return values of each."
  (reduce mkcat #(succeed nil [] %1 %2 %4) rules))

(defn mkeither [rule1 rule2]
  "Create a rule that tries to match rule1. If it succeeds, the rule
succeeds. Otherwise, it calls rule2."
  (fn [input bindings context memo]
    (let [r1 (rule1 input bindings context memo)]
      (if (failure? r1)
        (rule2 input bindings context (:m r1))
        r1))))

(defn mkalt [rules]
  "Create a rule which succeeds if one of rules succeeds."
  (cond
   (nil? (seq rules))
   #(fail "no rules to match" %4)

   (nil? (next rules))
   (first rules)

   :otherwise
   (reduce mkeither rules)))

(defn mkpred [f]
  "Create a rule which never returns a value or consumes input. It
succeeds if f returns non-nil and fails otherwise. f is a function of
input bindings and context."
  (fn [input bindings context memo]
    (if (f bindings context)
      (succeed nil [] input bindings memo)
      (fail "Failed to match predicate" memo))))

(defn mkzom [rule]
  "Create a rule which matches rule consecutively as many times as
possible. The rule never fails. Returns a seq of all matched values."
  (fn [input bindings context memo]
    (loop [val [] input input bindings bindings memo memo]
      (let [r (rule input bindings context memo)]
        (if (success? r)
          (recur (vec-cat val (:s r)) (:i r) (:b r) (:m r))
          (succeed val val input bindings memo))))))

(defn mkscope [rule]
  "Create a rule which contains the scope of the given rule. Bindings
made in rule do not escape this rule's scope."
  (fn [input bindings context memo]
    (let [r (rule input {} context memo)]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) bindings (:m r))
        r))))

(defn mksub [rule]
  "Create a rule which applies a rule to a nested seq within the
input."
  (fn [input bindings context memo]
    (if (and (seq input) (sequential? (first input)))
      (let [r (rule (first input) bindings context memo)]
        (if (success? r)
          (succeed (:r r) (:s r) (rest input) (:b r) (:m r))
          r))
      (fail "Input not a seq." memo))))

(defn mk1om [rule]
  "Create a rule which matches rule as many times as possible but at
least once."
  (mkseq [rule (mkzom rule)]))

(defn mkopt [rule]
  "Create a rule which matches rule or not, but never fails."
  (fn [input bindings context memo]
    (let [r (rule input bindings context memo)]
      (if (failure? r)
        (succeed [] [] input bindings (:m r))
        r))))

;; literal matcher
(defn mklit [l]
  "Create a rule which consumes one item of input. If it is equal to
l, succeed."
  (mkpr #(= l %)))

(defn mkstr [s]
  "Create a rule which matches all of the characters of a String s in
sequence."
  (mkseq (map mklit s)))

(defn mkmemo [rule]
  "Create a rule that memoizes the given rule."
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

;; utilities

(def always (mkpred (constantly true)))
(def never  (mkpred (constantly false)))
(def anything (mkpr (constantly true)))

(def whitespace (mkpr #(Character/isSpace %)))

(def digit (mkpr #(Character/isDigit %)))


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



(ns clj-peg.combinators)

;; A rule is a function [input bindings]
;; -> {:i seq :b {:keyword value} :r value :s seqvalue }
;; OR {:fail str} to signal failure
;; input is a seq of the rest of the input
;; bindings is a map from keywords to values of output
;; 
;; :fail is a failure message for the user

;; define success and failure
(defn fail [msg]
  "Fail a rule call with the given msg"
  {:fail msg})
(defn failure? [x]
  "Is x a failure?"
  (:fail x))

(defn succeed [return sreturn input bindings]
  "Succeed a rule call."
  {:i input :b bindings :r return :s sreturn})
(defn success? [x]
  "Is x a success?"
  (not (failure? x)))

(defn mkfn [f]
  "Create a function useful for calling a rule at the REPL."
  (fn
    ([input]
       (let [r (f input {})]
         (if (success? r)
           (:r r)
           (throw (RuntimeException. (:fail r))))))
    ([input bindings]
       (f input bindings))))

(defn mknot [rule]
  "Create a rule that fails if the next input matches rule and
succeeds otherwise."
  (fn not-fn [input bindings]
    (let [r (rule input bindings)]
      (if (failure? r)
        (succeed nil [] input bindings)
        (fail (str "NOT failed"))))))

(defn mkbind [rule var]
  "Create a rule that binds the return value of rule to var."
  (fn [input bindings]
    (let [r (rule input bindings)]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) (assoc (:b r) var (:r r)))
        r))))

(defn mkpr [pr]
  "Create a rule that consumes one item from the input. If pr applied
to that item returns true, the rule succeeds. Otherwise, fail."
  (fn [input bindings]
    (if (nil? (seq input))
      (fail "End of input")
      (let [i (first input)]
        (if (pr i)
          (succeed i [i] (rest input) bindings)
          (fail (str i " does not match predicate.")))))))

(defn mkret [rule ret]
  "Create a rule that returns a value. The value is computed by ret,
which is a function of a bindings map. The rule also binds the return
value of rule to :ret."
  (fn [input bindings]
    (let [r (rule input bindings)]
      (if (success? r)
        (let [b (assoc (:b r) :ret (:r r))
              v (ret b)]
          (succeed v [v] (:i r) b))
        r))))

(defn mknothing [rule]
  "Create a rule that succeeds, fails, and consumes just like rule but
returns nothing."
  (fn [input bindings]
    (let [r (rule input bindings)]
      (if (success? r)
        (succeed nil [] (:i r) (:b r))
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
  (fn [input bindings]
    (let [r1 (rule1 input bindings)]
      (if (failure? r1)
        r1
        (let [r2 (rule2 (:i r1) (:b r1))]
          (if (failure? r2)
            r2
            (let [[r s] (catreturns r1 r2)]
              (succeed r s (:i r2) (:b r2)))))))))

(defn mkseq [rules]
  "Create a rule that matches all of rules in order. Returns a vec of
the return values of each."
  (reduce mkcat #(succeed nil [] %1 %2) rules))

(defn mkeither [rule1 rule2]
  "Create a rule that tries to match rule1. If it succeeds, the rule
succeeds. Otherwise, it calls rule2."
  (fn [input bindings]
    (let [r1 (rule1 input bindings)]
      (if (failure? r1)
        (rule2 input bindings)
        r1))))

(defn mkalt [rules]
  "Create a rule which succeeds if one of rules succeeds."
  (cond
   (nil? (seq rules))
   (constantly (fail "no rules to match"))

   (nil? (next rules))
   (first rules)

   :otherwise
   (reduce mkeither rules)))

(defn mkpred [f]
  "Create a rule which never returns a value or consumes input. It
succeeds if f returns non-nil and fails otherwise. f is a function of
input bindings."
  (fn [input bindings]
    (if (f bindings)
      (succeed nil [] input bindings)
      (fail "Failed to match predicate"))))

(defn mkzom [rule]
  "Create a rule which matches rule consecutively as many times as
possible. The rule never fails. Returns a seq of all matched values."
  (fn [input bindings]
    (loop [val [] input input bindings bindings]
      (let [r (rule input bindings)]
        (if (success? r)
          (recur (vec-cat val (:s r)) (:i r) (:b r))
          (succeed val val input bindings))))))

(defn mkscope [rule]
  "Create a rule which contains the scope of the given rule. Bindings
made in rule do not escape this rule's scope."
  (fn [input bindings]
    (let [r (rule input {})]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) bindings)
        r))))

(defn mksub [rule]
  "Create a rule which applies a rule to a nested seq within the
input."
  (fn [input bindings]
    (if (and (seq input) (sequential? (first input)))
      (let [r (rule (first input) bindings)]
        (if (success? r)
          (succeed (:r r) (:s r) (rest input) (:b r))
          r))
      (fail "Input not a seq."))))

(defn mk1om [rule]
  "Create a rule which matches rule as many times as possible but at
least once."
  (mkseq [rule (mkzom rule)]))

(defn mkopt [rule]
  "Create a rule which matches rule or not, but never fails."
  (fn [input bindings]
    (let [r (rule input bindings)]
      (if (failure? r)
        (succeed [] [] input bindings)
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



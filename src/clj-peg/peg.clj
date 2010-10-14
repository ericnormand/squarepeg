(ns peg.peg)

;; A rule is a function [input bindings] -> {:i seq :b {symbol vec} :r vec} OR nil to signal failure
;; input is a seq of the rest of the input
;; bindings is a map from symbol to vecs of output

;; we define functions outside of the fn so that they will be dynamically rebound

;; not makes failure success and vice versa and never consumes input
;; Fails when rule succeeds.
;; Succeeds when rule fails.
(defn not-fn [rule input bindings]
  (when (not (rule input bindings))
    {:i input :b bindings :r []}))
(defn make-not [rule]
  (fn [input bindings]
    (not-fn rule input bindings)))

;; make a rule bind its return to a var
;; Fails when rule fails.
;; Succeeds when rule succeeds.
(defn bind-fn [rule var input bindings]
  (when-let [r (rule input bindings)]
    (assoc r :b (assoc (:b r) var (:r r)))))
(defn make-bind [rule var]
  (fn [input bindings]
    (bind-fn rule var input bindings)))

;; make a rule return something else.
;; ret is a fn that takes the map of bindings
;; also binds the var -match- to the return value
;; Fails when rule fails.
;; Succeeds when rule succeeds.
(defn return-fn [rule ret input bindings]
  (when-let [r (rule input bindings)]
    (let [b (assoc (:b r) '-match- (:r r))]
      (assoc r :b b :r (ret b)))))
(defn make-return [rule ret]
  (fn [input bindings]
    (return-fn rule ret input bindings)))

;; helper function to concatenate vecs
(defn vec-cat [a b]
  (reduce conj a b))

;; A sequence matcher matches all rules in order against the input.
;; Fails when any rule fails.
;; Succeeds when all rules succeed.
(defn sequence-fn [rules val input bindings]
  (if (nil? (seq rules))
    {:i input :b bindings :r val}
    (when-let [r ((first rules) input bindings)]
      (recur (rest rules) (vec-cat val (:r r)) (:i r) (:b r)))))
(defn make-sequence-matcher [rules]
  (fn [input bindings]
    (sequence-fn rules [] input bindings)))

;; An alternative matcher tries to match one rule, in the order given, to the input or fails if none match
;; Fails if all rules fail.
;; Succeeds if one rule succeeds.
(defn alt-fn [rules input bindings]
  (when (seq rules)
    (or ((first rules) input bindings)
	(recur (rest rules) input bindings))))
(defn make-alt-matcher [rules]
  (fn [input bindings]
    (alt-fn rules input bindings)))

;; A predicate matcher allows us to fail conditionally based on the return value of a predicate.
;; The predicate fn f is a function of the bindings active when it is called.
;; The predicate matcher consumes no input.
;; Fails when (f bindings) returns false
;; Succeeds when (f bindings) returns true.
(defn pred-fn [f input bindings]
  (when (f bindings)
    {:i input :b bindings :r []}))
(defn make-pred-matcher [f]
  (fn [input bindings]
    (pred-fn f input bindings)))

;; zero or more matcher matches a rule as many times as possible
;; Fails never
;; Succeeds always
(defn zom-fn [rule val input bindings]
  (if-let [r (rule input bindings)]
    (recur rule (vec-cat val (:r r)) (:i r) (:b r))
    {:i input :b bindings :r val}))
(defn make-zero-or-more-matcher [rule]
  (fn [input bindings]
    (zom-fn rule [] input bindings)))
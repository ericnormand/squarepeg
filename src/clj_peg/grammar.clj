(ns clj-peg.grammar)

;; we can define a new type of rule that looks up another rule in the grammar
;; but we don't want to give it any bindings or let it alter the bindings
;; we want this to lookup the rule at run-time, not before
(defn rule-fn [v input bindings]
  (when-let [r (v input {})]
    (assoc r :b bindings)))

;; rule matcher looks up a rule in the lexically scoped grammar
(defn make-rule-matcher [v]
  (fn [input bindings]
    (rule-fn v input bindings)))

;; helper functions

(defmacro run [rule & d]
  `(~rule '~d {}))

(defmacro srun [rule s]
  `(~rule ~s {}))
(ns clj-peg.grammar)

;; we can define a new type of rule that looks up another rule in the grammar
;; but we don't want to give it any bindings or let it alter the bindings
;; we want this to lookup the rule at run-time, not before
(defn rule-fn [ns rule-name input bindings]
  (if-let [rule (ns-resolve ns rule-name)]
    (when-let [r (@rule input {})]
      (assoc r :b bindings))
    (throw (RuntimeException. (str "Rule not defined: " ns "/" rule-name)))))
(defn make-rule-matcher [rule-name]
  (let [ns *ns*]
   (fn [input bindings]
     (rule-fn ns rule-name input bindings))))

;; helper functions

(defmacro run [rule & d]
  `(~rule '~d {}))

(defmacro srun [rule s]
  `(~rule ~s {}))
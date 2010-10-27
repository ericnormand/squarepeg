(ns clj-peg.grammar
  (:use clj-peg.peg))

;; we can define a new type of rule that looks up another rule in the grammar
;; but we don't want to give it any bindings or let it alter the bindings
;; we want this to lookup the rule at run-time, not before
(defn mkrule [rule-name]
  (let [ns *ns*]
    (mkfn
     (fn [input bindings]
       (if-let [rule (ns-resolve ns rule-name)]
	 (let [r (@rule input {})]
	   (if (success? r)
	     (succeed (:r r) (:i r) bindings)
	     r))
	 (throw (RuntimeException. (str "Rule not defined: " ns "/" rule-name))))))))

(defmacro defrule [name rule]
  `(def
    ~name
    (let [rule# ~rule]
      (fn
	([input#]
	   (let [r# (~name input# {})]
	     (cond
	      (failure? r#)
	      (throw (RuntimeException. (:fail r#)))
	      (nil? (seq (:r r#)))
	      nil
	      (nil? (seq (rest (:r r#))))
	      (first (:r r#))
	      :otherwise
	      (:r r#))))
	([input# bindings#]
	   (rule# input# bindings#))))))
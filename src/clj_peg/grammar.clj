(ns clj-peg.grammar
  (:use clj-peg.peg))

;; we can define a new type of rule that looks up another rule in the grammar
;; but we don't want to give it any bindings or let it alter the bindings
;; we want this to lookup the rule at run-time, not before
(defn mkrule [rule]
  (fn [input bindings]
    (let [r (rule input {})]
      (if (success? r)
	(succeed (:r r) (:i r) bindings)
	r))))




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
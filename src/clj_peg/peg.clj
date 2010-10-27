(ns clj-peg.peg)

;; A rule is a function [input bindings] -> {:i seq :b {symbol vec} :r vec}
;;    OR {:fail str} to signal failure
;; input is a seq of the rest of the input
;; bindings is a map from symbol to vecs of output
;; :fail is a failure message for the user

;; define success and failure
(defn fail [msg] {:fail msg})
(defn failure? [x] (:fail x))

(defn success? [x] (not (failure? x)))
(defn succeed [return input bindings]
  {:i input :b bindings :r return})

(defn mkfn [f]
  (fn
    ([input]
       (let [r (f input {})]
	 (if (success? r)
	   (cond
	    (nil? (seq (:r r)))
	    nil
	    (nil? (seq (rest (:r r))))
	    (first (:r r))
	    :otherwise
	    (:r r))
	   (throw (RuntimeException. (:fail r))))))
    ([input bindings]
       (f input bindings))))

;; not makes failure success and vice versa and never consumes input
(defn mknot [rule]
  (fn not-fn [input bindings]
    (let [r (rule input bindings)]
      (if (failure? r)
	(succeed [] input bindings)
	(fail (str "NOT failed"))))))

;; make a rule bind its return to a var
(defn mkbind [rule var]
  (fn [input bindings]
    (let [r (rule input bindings)]
      (if (success? r)
	(succeed (:r r) (:i r) (assoc (:b r) var (:r r)))
	r))))

;; changes the current return value
;; ret is a fn that takes the map of bindings
(defn mkret [rule ret]
  (fn [input bindings]
    (let [r (rule input bindings)]
      (if (success? r)
	(let [b (assoc (:b r) :-match- (:r r))]
	  (succeed (ret b) (:i r) b))
       r))))

;; helper function to concatenate vecs
(defn vec-cat [a b]
  (reduce conj a b))

;; A sequence matcher matches all rules in order against the input.
(defn mkseq [rules]
  (let [f (reduce (fn [f rule]
		    (fn [val input bindings]
		      (let [r (rule input bindings)]
			(if (success? r)
			  (f (vec-cat val (:r r)) (:i r) (:b r))
			  r))))
		  succeed
		  (reverse rules))]
    (fn [input bindings] (f []  input bindings))))

;; An alternative matcher tries to match one rule, in the order given, to the input or fails if none match
(defn mkalt [rules]
  (reduce (fn [f rule]
	    (fn [input bindings]
	      (let [r (rule input bindings)]
		(if (failure? r)
		  (f input bindings)
		  r))))
	  (reverse rules)))

;; A predicate matcher allows us to fail conditionally based on the return value of a predicate.
;; The predicate fn f is a function of the bindings active when it is called.
;; The predicate matcher consumes no input.
(defn mkpred [f]
  (fn [input bindings]
    (if (f bindings)
      (succeed [] input bindings)
      (fail "Failed to match predicate"))))

;; zero or more matcher matches a rule as many times as possible
(defn mkzom [rule]
  (partial (fn [val input bindings]
	     (let [r (rule input bindings)]
	       (if (success? r)
		 (recur (vec-cat val (:r r)) (:i r) (:b r))
		 (succeed val input bindings))))
	   []))
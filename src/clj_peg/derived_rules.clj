(ns clj-peg.derived-rules
  (:use clj-peg.peg)
  (:use clj-peg.grammar))

(defrule always (mkpred (constantly true)))
(defrule never  (mkpred (constantly false)))

; optional matcher
(defn mkopt [rule]
  (mkalt [rule (mkrule 'always)]))

; one or more
(defn mk1om [rule]
  (mkseq [rule (mkzom rule)]))

; anything matches any single element and fails on end of input
(defrule anything
  (fn [input bindings]
    (if (seq input)
      (succeed [(first input)] (rest input) bindings)
      (fail "end of input"))))

; literal matcher
(defn mklit [l]
  (mkseq
   [(mkbind (mkrule 'anything) '-match-)
    (mkpred (fn [b]
	      (let [i (first (b '-match-))]
		(= i l))))]))

; whitespace
(defrule whitespace
  (mkalt
   (doall (map mklit [\newline \space \tab]))))

; string matcher
(defn mkstr [s]
  (mkseq (doall (map mklit s))))

(defrule end
  (mknot (mkrule 'anything)))

(defmacro defpredrule [name f]
  `(def ~name
	(mkseq
	 [(mkbind (mkrule 'anything) '-match-)
	  (mkpred
	   (fn [b#]
	     (let [x# (first (b# '-match-))]
	       (~f x#))))])))

(defpredrule match-string string?)
(defpredrule match-number number?)
(defpredrule match-integer integer?)
(defpredrule match-float float?)
(defpredrule match-symbol symbol?)
(defpredrule match-keyword keyword?)
(defpredrule match-char char?)
(defpredrule match-vec vector?)
(defpredrule match-list list?)
(defpredrule match-hash map?)
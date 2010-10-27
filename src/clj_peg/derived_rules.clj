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
   [(mkbind anything :-match-)
    (mkpred (fn [b]
	      (let [i (first (b :-match-))]
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

(defn mksub [rule]
  (fn [input bindings]
    (if (seq input)
     (let [r (rule (first input) bindings)]
       (if (success? r)
	 (succeed [(:r r)] (rest input) (:b r))
	 r)))))

(defn mkpr [f]
  (mkseq
   [(mkbind anything :-match-)
    (mkpred
     (fn [b]
       (-> b :-match- first f)))]))

(def match-string (mkpr string?))
(def match-number (mkpr number?))
(def match-integer (mkpr integer?))
(def match-float (mkpr float?))
(def match-symbol (mkpr symbol?))
(def match-keyword (mkpr keyword?))
(def match-char (mkpr char?))
(def match-vec (mkpr vector?))
(def match-list (mkpr list?))
(def match-hash (mkpr map?))
(ns clj-peg.peg_in_peg_test
  (:use [clj-peg.peg])
  (:use [clj-peg.grammar])
  (:use [clj-peg.derived-rules])
  (:use [clj-peg.peg-in-peg] :reload-all)
  (:use [clojure.test]))

(deftest test-keyword-literals
  (is (literal= '(=) {}))
  (is (literal-> '(->) {}))
  (is (literal* '(*) {}))
  (is (literal+ '(+) {}))
  (is (literal? '(?) {}))
  (is (literal=> '(=>) {}))
  (is (literal-slash '(/) {}))
  (is (literal-- '(--) {})))

(deftest test-keyword-rule
   (dorun
    (map #(is (keyword-match [%] {}))
	 keyword-list))
   (is (not (keyword-match ['abc] {}))))

 (deftest test-variable
   (is (= ['hh] (:r (variable ['hh] {}))))
   (is (not (variable [] {})))
   (is (not (variable [0] {}))))

 (deftest test-string
   (is (= ["abc"] (:r (string ["abc"] {}))))
   (is (not (string [5] {}))))

 (deftest test-symbol
   (is (= ['hh] (:r (symbol-match ['hh] {}))))
   (is (not (symbol-match [] {})))
   (is (not (symbol-match [0] {}))))

 (deftest test-string-match
   (is (not (string-match [1] {})))
   (is (ifn? (first (:r (string-match ["abc"] {})))))
   (is (= (vec "abc") (:r ((first (:r (string-match ["abc"] {}))) "abc" {}))))
   (is (not ((first (:r (string-match ["abc"] {}))) "acb" {}))))

 (deftest test-rule-match
   (is (ifn? (first (:r (rule-match ['clj-peg.derived-rules/anything] {})))))
   (is (not (rule-match [0 3] {})))
   (is (= [1] (:r ((first (:r (rule-match ['clj-peg.derived-rules/anything] {}))) [1] {}))))
   (is (not ((first (:r (rule-match ['clj-peg.derived-rules/end] {}))) [1] {}))))

 (deftest test-list
   (is (not (list-match [1] {})))
   (is (= ['(1 2 3)] (:r (list-match ['(1 2 3)] {})))))

 (deftest test-vec
   (is (not (vec-match [1] {})))
   (is (= [[1 2 3]] (:r (vec-match [[1 2 3]] {})))))

 (deftest test-parenthesis
   (is (not (parenthesis [1] {})))
   (is (= [1 2 3] (:r ((first (:r (parenthesis [['clj-peg.derived-rules/anything 'clj-peg.derived-rules/anything 'clj-peg.derived-rules/anything]] {}))) [1 2 3] {}))))
   (is (= [3] ((:b ((first (:r (parenthesis '([clj-peg.derived-rules/anything clj-peg.derived-rules/anything clj-peg.derived-rules/anything => b]) {}))) [1 2 3] {})) 'b))))

 (deftest test-predicate
   (is (not (predicate [1] {})))
   (is (ifn? (first (:r (predicate ['(even? p)] {})))))
   (is ((first (:r (predicate ['(= 1 1)] {}))) [] {}))
   (is (not ((first (:r (predicate ['(= 1 2)] {}))) [] {}))))

 (deftest test-atom
   (is (atom-match [1] {}))
   (is (atom-match "a" {})))

 (deftest test-rule-element-atomic
   (dorun
    (map #(is (rule-element-atomic [%] {}))
	 '((even? a b)
	   [1 2 3]
	   anything
	   "abc"
	   1
	   \a
	   )))
   (is (rule-element-atomic '(-- anything) {})))

 (deftest test-1-o-m
   (is (one-or-more '(clj-peg.derived-rules/anything +) {}))
   (is (not (one-or-more '(anything) {})))
   (is (not (one-or-more '(anything anything +) {})))
   (is (= [1 2 3] (:r ((first (:r (one-or-more '(clj-peg.derived-rules/anything +) {}))) [1 2 3] {})))))

 (deftest test-not
   (let [nr (not-rule '(-- clj-peg.derived-rules/anything) {})]
    (is nr)
    (is (not (not-rule '(anything) {})))
    (is ((first (:r nr)) [] {}))
    (is (not ((first (:r nr)) [1] {})))))

 (deftest test-0-o-m
   (let [z (zero-or-more '(clj-peg.derived-rules/anything *) {})]
    (is z)
    (is ((first (:r z)) [] {}))
    (is ((first (:r z)) [1] {}))
    (is ((first (:r z)) [1 2] {})))
   )

 
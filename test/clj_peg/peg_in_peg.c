(ns clj-peg.peg_in_peg_test
  (:use [clj-peg.peg])
  (:use [clj-peg.grammar])
  (:use [clj-peg.derived-rules])
  (:use [clj-peg.peg-in-peg] :reload-all)
  (:use [clojure.test]))

(deftest test-keyword-literals
  (dorun
   (map #(is ((lookup *peg* %) [%] {}))
	keyword-list)))

(deftest test-keyword-rule
  (dorun
   (map #(is ((lookup *peg* 'keyword) [%] {}))
	keyword-list)))

(deftest test-variable
  (is (= ['hh] (:r ((lookup *peg* 'variable) ['hh] {}))))
  (is (not ((lookup *peg* 'variable) [] {})))
  (is (not ((lookup *peg* 'variable) [0] {}))))

(deftest test-string
  (is (= ["abc"] (:r ((lookup *peg* 'string) ["abc"] {}))))
  (is (not ((lookup *peg* 'string) [5] {}))))

(deftest test-symbol
  (is (= ['hh] (:r ((lookup *peg* 'symbol) ['hh] {}))))
  (is (not ((lookup *peg* 'symbol) [] {})))
  (is (not ((lookup *peg* 'symbol) [0] {}))))

(deftest test-string-match
  (is (not ((lookup *peg* 'string-match) [1] {})))
  (is (ifn? (first (:r ((lookup *peg* 'string-match) ["abc"] {})))))
  (is (= (vec "abc") (:r ((first (:r ((lookup *peg* 'string-match) ["abc"] {}))) "abc" {}))))
  (is (not ((first (:r ((lookup *peg* 'string-match) ["abc"] {}))) "acb" {}))))

(deftest test-rule-match
  (is (ifn? (first (:r ((lookup *peg* 'rule-match) ['anything] {})))))
  (is (not ((lookup *peg* 'rule-match) [0 3] {})))
  (is (= [1] (:r ((first (:r ((lookup *peg* 'rule-match) ['anything] {}))) [1] {}))))
  (is (not ((first (:r ((lookup *peg* 'rule-match) ['end] {}))) [1] {}))))

(deftest test-list
  (is (not ((lookup *peg* 'list) [1] {})))
  (is (= ['(1 2 3)] (:r ((lookup *peg* 'list) ['(1 2 3)] {})))))

(deftest test-vec
  (is (not ((lookup *peg* 'vec) [1] {})))
  (is (= [[1 2 3]] (:r ((lookup *peg* 'vec) [[1 2 3]] {})))))

(deftest test-parenthesis
  (is (not ((lookup *peg* 'parenthesis) [1] {})))
  (is (= [1 2 3] (:r ((first (:r ((lookup *peg* 'parenthesis) [['anything 'anything 'anything]] {}))) [1 2 3] {}))))
  (is (= [3] ((:b ((first (:r ((lookup *peg* 'parenthesis) '([anything anything anything => b]) {}))) [1 2 3] {})) 'b))))

(deftest test-predicate
  (is (not ((lookup *peg* 'predicate) [1] {})))
  (is (ifn? (first (:r ((lookup *peg* 'predicate) ['(even? p)] {})))))
  (is ((first (:r ((lookup *peg* 'predicate) ['(= 1 1)] {}))) [] {}))
  (is (not ((first (:r ((lookup *peg* 'predicate) ['(= 1 2)] {}))) [] {}))))

(deftest test-atom
  (is ((lookup *peg* 'atom) [1] {}))
  (is ((lookup *peg* 'atom) "a" {})))

(deftest test-rule-element-atomic
  (dorun
   (map #(is ((lookup *peg* 'rule-element-atomic) [%] {}))
	'((even? a b)
	  [1 2 3]
	    anything
	    "abc"
	    1
	    \a
	    )))
  (is ((lookup *peg* 'rule-element-atomic) '(-- anything) {})))

(deftest test-1-o-m
  (is ((lookup *peg* 'one-or-more) '(anything +) {}))
  (is (not ((lookup *peg* 'one-or-more) '(anything) {})))
  (is (not ((lookup *peg* 'one-or-more) '(anything anything +) {})))
  (is (= [1 2 3] (:r ((first (:r ((lookup *peg* 'one-or-more) '(anything +) {}))) [1 2 3] {})))))

(deftest test-not
  (is ((lookup *peg* 'not) '(-- anything) {}))
  (is (not ((lookup *peg* 'not) '(anything) {})))
  (is ((first (:r ((lookup *peg* 'not) '(-- anything) {}))) [] {}))
  (is (not ((first (:r ((lookup *peg* 'not) '(-- anything) {}))) [1] {}))))

(deftest test-0-o-m
  (is ((lookup *peg* 'zero-or-more) '(anything *) {}))
  (is ((first (:r ((lookup *peg* 'zero-or-more) '(anything *) {}))) [] {}))
  (is ((first (:r ((lookup *peg* 'zero-or-more) '(anything *) {}))) [1] {}))
  (is ((first (:r ((lookup *peg* 'zero-or-more) '(anything *) {}))) [1 2] {})))

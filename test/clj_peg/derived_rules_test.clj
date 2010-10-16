(ns clj-peg.derived-rules-test
  (:use [clj-peg.peg])
  (:use [clj-peg.grammar])
  (:use [clj-peg.derived-rules] :reload-all)
  (:use [clojure.test]))

(deftest test-string
   (is (= ["abc"] (:r (match-string ["abc"] {}))))
   (is (not (match-string [5] {}))))

 (deftest test-symbol
   (is (= ['hh] (:r (match-symbol ['hh] {}))))
   (is (not (match-symbol [] {})))
   (is (not (match-symbol [0] {}))))

(deftest test-list
   (is (not (match-list [1] {})))
   (is (= ['(1 2 3)] (:r (match-list ['(1 2 3)] {})))))

 (deftest test-vec
   (is (not (match-vec [1] {})))
   (is (= [[1 2 3]] (:r (match-vec [[1 2 3]] {})))))



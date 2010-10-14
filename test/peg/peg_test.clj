(ns peg.peg_test
  (:use [peg.peg] :reload-all)
  (:use [peg.grammar])
  (:use [peg.derived-rules])
  (:use [clojure.test]))

(def always (make-pred-matcher (constantly true)))
(def never (make-pred-matcher (constantly false)))

;; test predicate matcher
(deftest pred-test
  ;; true case
  (is (= [] (:r (always [] {}))))
  ;; false case
  (is (not (never [] {}))))

;; test alternative matcher
(deftest alt-test
  ;; no alternatives always fails
  (is (not ((make-alt-matcher []) [] {})))
  ;; single alternative
  (is (not ((make-alt-matcher [never]) [] {})))
  (is (= [] (:r ((make-alt-matcher [always]) [] {}))))
  ;; two alternatives
  (is (= [] (:r ((make-alt-matcher [always always]) [] {}))))
  (is (= [] (:r ((make-alt-matcher [never always]) [] {}))))
  (is (= [] (:r  ((make-alt-matcher [always never]) [] {}))))
  (is (not ((make-alt-matcher [never never]) [] {}))))

;; test zero-or-more matcher
(deftest zom-test
  ;; zero should be true
  (is (= [] (:r ((make-zero-or-more-matcher anything) [] {}))))
  (is (= [] (:r ((make-zero-or-more-matcher never) [] {}))))
  (is (= [] (:r ((make-zero-or-more-matcher never) [1] {}))))
  ;; one should be true
  (is (= [1] (:r ((make-zero-or-more-matcher anything) [1] {}))))
  ;; two should be true
  (is (= [1 2] (:r ((make-zero-or-more-matcher anything) [1 2] {})))))

;; test sequence matcher
(deftest seq-test
  ;; zero in sequence passes
  (is (= [] (:r ((make-sequence-matcher []) [] {}))))
  ;; one in sequence passes if matches
  (is (= [1] (:r ((make-sequence-matcher [anything]) [1] {}))))
  (is (not ((make-sequence-matcher [never]) [1] {})))
  ;; two in sequence pass if they match
  (is (= [1 2] (:r ((make-sequence-matcher [anything anything]) [1 2] {}))))
  (is (not ((make-sequence-matcher [anything never]) [1 2] {})))
  (is (not ((make-sequence-matcher [never anything]) [1 2] {})))
  (is (not ((make-sequence-matcher [never never]) [1 2] {})))
  ;; fail if matchers run out of input
  (is (not ((make-sequence-matcher [anything]) [] {})))
  )

(deftest anything-test
  (is (not (anything [] {})))
  (is (= [1] (:r (anything [1] {}))))
  (is (= [1] (:r (anything [1 2] {}))))
  (is (= [2] (vec (:i (anything [1 2] {}))))))

(deftest return-test
  ;; if rule fails, return should fail
  (is (not ((make-return never (fn [b] [])) [] {})))
  ;; if rule passes, return should pass and return a different value
  (is (= ['val] (:r ((make-return anything (fn [b] ['val])) [1] {}))))
  ;; if rule passes, -match- should be bound
  (is (= [1] ('-match- (:b ((make-return anything (fn [b] ['val])) [1] {}))))))

(deftest bind-test
  ;; if rule fails, bind should fail
  (is (not ((make-bind never 'v) [] {})))
  ;; if rule passes, ret should be the same
  (is (= (:r ((make-bind anything 'v) [1] {}))
	 (:r (anything [1] {}))))
  ;; bind should not consume input
  (is (= (:i ((make-bind anything 'v) [1] {}))
	 (:i (anything [1] {}))))
  ;; if rule passes, var should be bound
  (is (= (:r ((make-bind anything 'v) [1] {}))
	 ((:b ((make-bind anything 'v) [1] {})) 'v)))
  )

(deftest not-test
  ;; if rule fails, not should pass
  (is ((make-not never) [] {}))
  ;; if rule passes, not should fail
  (is (not ((make-not always) [] {})))
  )
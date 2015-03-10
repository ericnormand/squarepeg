;; Copyright (c) Eric Normand. All rights reserved.
;;
;; The use and distribution terms for this software are covered by the
;; Eclipse Public License 1.0 [1] which can be found in the file
;; epl-v10.html at the root of this distribution.  By using this
;; software in any fashion, you are agreeing to be bound by the terms
;; of this license.
;;
;; [1]: http://opensource.org/licenses/eclipse-1.0.php
;;
;; You must not remove this notice, or any other, from this software.
(ns squarepeg.core-test
  (:require [clojure.test :refer :all])
  (:require [squarepeg.core :refer :all]))

(deftest test-mknot
  (is (failure? ((mknot always) []  {} {} {})) "mknot always always fails")
  (is (success? ((mknot never)  []  {} {} {})) "mknot never always succeeds"))

(deftest test-mkbind
  ;; if it succeeds, the value should be bound
  (is (= \a (:a (:b ((mkbind (fn [i b c m] (succeed \a [\a] [] b m)) :a) [] {} {} {})))))
  ;; a failure should still fail
  (is (failure? ((mkbind (fn [i b c m] (fail "" m)) :a) [] {} {} {})))
  ;; testing coerced strings
  (is (= "aa" (:a (:b ((mkbind (fn [i b c m] (succeed [\a \a] [\a \a] [] b m)) :a) [] {} {:expected-type :string} {}))))))

(deftest test-mkret
  ;; if it succeeds, the value should be returned
  (is (= \A (:r ((mkret (fn [i b c m] (succeed \a [\a] [] b m))
                        (fn [b c] (Character/toUpperCase (:ret b))))
                 [] {} {} {}))))
  ;; a failure should still fail
  (is (failure? ((mkret (fn [i b c m] (fail "" m))
                        (fn [b c] (Character/toUpperCase (:ret b)))) [] {} {} {})))
  ;; testing coerced strings
  (is (= 2 (:r ((mkret (fn [i b c m] (succeed [\a \a] [\a \a] [] b m))
                       (fn [b c] (.length (:ret b)))) [] {} {:expected-type :string} {})))))

(deftest test-mknothing
  ;; mknothing succeeds when underlying rule succeeds
  (is (success? ((mknothing (fn [i b c m] (succeed 1 [1] []  b m))) [] {} {} {})))
  ;; mknothing fails when underlying rule fails
  (is (failure? ((mknothing (fn [i b c m] (fail "" m))) [] {} {} {})))
  ;; mknothing does not return a value on success
  (let [r ((mknothing (fn [i b c m] (succeed 1 [1] []  b m))) [] {} {} {})]
    (is (= (:r r) nil))
    (is (= (:s r) []))))

(deftest test-mkpr
  ;; mkpr fails on end of input
  (is (failure? ((mkpr (constantly true)) [] {} {} {})))
  ;; mkpr fails when predicate fails
  (is (failure? ((mkpr identity) [false] {} {} {})))
  ;; mkpr succeeds when predicate succeeds
  (is (success? ((mkpr identity) [true] {} {} {}))))


(deftest test-mkseq
  ;; empty rules always succeeds
  (is (success? ((mkseq) [] {} {} {})))
  ;; if any fail, mkseq fails
  (is (failure? ((mkseq always always never) [] {} {} {})))
  ;; if none fail, mkseq succeeds
  (is (success? ((mkseq always always always) [] {} {} {}))))

(deftest test-mkalt
  ;; empty rules always fails
  (is (failure? ((mkalt) [] {} {} {})))
  ;; if none succeed, it fails
  (is (failure? ((mkalt never never never) [] {} {} {})))
  ;; if one succeeds, it succeeds
  (is (success? ((mkalt never always never) [] {} {} {}))))

(deftest test-mkpred
  ;; mkpred fails when the predicate fails
  (is (failure? ((mkpred (constantly false)) [] {} {} {})))
  ;; mkpred succeeds when the predicate succeeds
  (is (success? ((mkpred (constantly true)) [] {} {} {})))
  ;; mkpred never returns a value
  (let [r ((mkpred (constantly true)) [] {} {} {})]
    (is (nil? (:r r)))
    (is (= [] (:s r)))))

(deftest test-mkzom
  ;; mkzom never fails
  (is (success? ((mkzom anything) [] {} {} {})))
  ;; mkzom will match greedily
  (let [r ((mkzom anything) "abcdfdfsdfjdfksdjlkf" {} {} {})]
    (is (empty? (:i r)))))

(deftest test-mkscope
  ;; mkscope fails when rule fails
  (is (failure? ((mkscope never) [] {} {} {})))
  ;; mkscope succeeds when rule succeeds
  (is (success? ((mkscope always) [] {} {} {})))
  ;; mkscope never modifies bindings
  (let [r ((mkscope (fn [i b c m] (succeed nil [] [] {:crazy :bindings} {}))) [] {} {} {})]
    (is (= {} (:b r))))
  ;; mkscope hides bindings
  (is (success? ((mkscope (fn [i b c m]
                            (if (= b {})
                              (succeed [] [] [] {} {})
                              (fail "" {}))))
                 [] {:some :binding} {} {}))))

(deftest test-mksub
  ;; mksub fails with no input left
  (is (failure? ((mksub anything) [] {} {} {})))
  ;; mksub fails when next item is not a sequential
  (is (failure? ((mksub anything) [1] {} {} {})))
  ;; mksub succeeds sometimes
  (is (success? ((mksub anything) [[1]] {} {} {})))
  ;; mksub fails when rule fails
  (is (failure? ((mksub never) [[1]] {} {} {}))))

(deftest test-mk1om
  ;; mk1om fails when it doesn't match at least once
  (is (failure? ((mk1om anything) [] {} {} {})))
  ;; mk1om succeeds when it matches once
  (is (success? ((mk1om anything) [1] {} {} {})))
  ;; mk1om succeeds when it matches twice or more
  (is (success? ((mk1om anything) [1 2] {} {} {})))
  ;; mk1om will match greedily
  (let [r ((mk1om anything) "abcdfdfsdfjdfksdjlkf" {} {} {})]
    (is (empty? (:i r)))))

(deftest test-mkopt
  ;; mkopt never fails
  (is (success? ((mkopt never) [] {} {} {})))
  (is (success? ((mkopt always) [] {} {} {})))
  ;; mkopt returns on success
  (let [r ((mkopt anything) [1] {} {} {})]
    (is (= 1 (:r r))))
  ;; mkopt returns empty vec on failure (this means it doesn't return
  ;; nothing)
  ;; a mkseq will reify return to seq return
  (let [r ((mkopt anything) [] {} {} {})]
    (is (= [] (:r r)))
    (is (= [] (:s r)))))

(deftest test-mklit
  ;; mklit fails with no input
  (is (failure? ((mklit 1) [] {} {} {})))
  ;; mklit fails when no match
  (is (failure? ((mklit 1) [2] {} {} {})))
  ;; mklit succeeds when match
  (let [r ((mklit 1) [1] {} {} {})]
    (is (success? r))
    (is (= 1 (:r r)))))

(deftest test-mkstr
  ;; mkstr fails when string doesn't match
  (is (failure? ((mkstr "abc") "hfh" {} {} {})))
  (is (failure? ((mkstr "abc") "ab" {} {} {})))
  ;; mkstr succeeds when string matches
  (let [r ((mkstr "abc") "abc" {} {} {})]
    (is (success? r))
    (is (= (seq "abc") (:r r)))
    (is (empty? (:i r))))
  ;; mkstr succeeds when there's more input
  (let [r ((mkstr "abc") "abc123" {} {} {})]
    (is (success? r))
    (is (= (seq "abc") (:r r)))
    (is (= [\1 \2 \3] (:i r)))))

(deftest test-mkmemo
  ;; mkmemo should fail when rule fails
  (is (failure? ((mkmemo never) [] {} {} {})))
  ;; mkmemo should succeed when rule succeeds
  (is (success? ((mkmemo always) [] {} {} {}))))

(deftest test-mkmatch
  ;; mkmatch should fail when rule fails
  (is (failure? ((mkmatch never) [] {} {} {})))
  ;; mkmatch should succeed when rule succeeds
  (is (success? ((mkmatch always) [] {} {} {})))
  ;; mkmatch should bind the matched sequence
  (let [r ((mkmatch anything) [1 2] {} {} {})]
    (is (= [1] (:match (:b r)))))
  ;; mkmatch should coerce strings
  (let [r ((mkmatch anything) "abc" {} {:expected-type :string} {})]
    (is (= "a" (:match (:b r)))))
  )

(deftest test-mklower
  (is (= (seq "ABC") (:i ((mklower (mkstr "xyz")) "XYZABC" {} {} {}))))
  (is (= (seq "ABC") (:i ((mklower (mkstr "xyz")) "xyzABC" {} {} {}))))
  (is (failure? ((mklower (mkstr "xyz1")) "XYZABC" {} {} {})))
  (is (failure? ((mklower (mkstr "xyzA")) "XYZABC" {} {} {})))
  (is (= (seq "ABC") (:i ((mkseq (mkstr "123") (mklower (mkstr "xyz"))) "123XYZABC" {} {} {})))))

(deftest test-defrule
  (defrule b (mkpr even?))
  (is (:r (b [2] {} {} {})))
  (is (:fail (b [1] {} {} {})))
  (is (= 2 (b [2])))
  (is (:doc (meta #'b)))
  (is (:arglists (meta #'b))))

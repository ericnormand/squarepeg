(ns clj-peg.test.dsl
  (:use clj-peg.dsl)
  (:use clojure.test)
  (:use clj-peg.combinators))

(comment
 (deftest test-pegdef
   (is (thrown? RuntimeException (pegdef "")))
   (is (= "abc" (:r ((pegdef "'abc'") "abc" {} {:expected-type :string} {}))))
   (is (= "xyz" (:r ((pegdef "'abc'|'xyz'") "xyz" {} {:expected-type :string} {}))))
   (is (= "xyz" (:r ((pegdef "'abc' | 'xyz'") "xyz" {} {:expected-type :string} {}))))  
   (defrule UUU (mkstr "uuu"))
   (defrule us (pegdef "UUU"))
   (is (= "uuu" (us "uuu")))

   (defrule uss (pegdef "UUU UUU"))
   (is (= "uuuuuu" (uss "uuuuuu")))

   (defrule n (pegdef "digit => 1;"))
   (is (= 1 (n "9")))

   (defrule a* (pegdef "'a' * "))
   (is (= "aaa" (a* "aaa")))

   (defrule a+ (pegdef "'a'+"))
   (is (= "aaaa" (a+ "aaaa")))

   (defrule abs (pegdef "'a' 'b' +"))
   (is (thrown? RuntimeException (abs "aabb")))

   (defrule ap (pegdef "('a')"))
   (is (= "a" (ap "a")))

   (defrule asbs (pegdef "('a' | 'b') +"))
   (is (= "abaabb" (asbs "abaabb")))
   ))

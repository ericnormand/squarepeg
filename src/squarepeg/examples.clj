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
(ns squarepeg.examples
  (:require [squarepeg.core :refer :all]))

;; match 0 or more whitespace
(defrule w** (mkzom whitespace))

;; match 0 or more whitespace but return nothing
;; (effectively ignoring whitespace)
(defrule w* (mknothing (mkzom whitespace)))

;; match a newline
(defrule nl (mklit \newline))

;; match anything that is not a newline
(defrule non-newline (mkseq (mknot nl) anything))

;; match clojure style single-line comments
(defrule clj-comment (mkseq w* (mklit \;) (mkzom non-newline)
                            (mkalt nl end)))

;; a calculator (can do +, *, and parens
(defrule calc
  (mkseq w* sum end))
(defrule sum
  (mkalt
   (mkret
    (mkseq
     (mkbind #'product :p)
     w*
     (mklit \+)
     w*
     (mkbind #'sum :s))
    (fn [b _] (+ (:p b) (:s b))))
   #'product))
(defrule product
  (mkalt
   (mkret
    (mkseq
     (mkbind #'term :t)
     w*
     (mklit \*)
     w*
     (mkbind #'product :p))
    (fn [b _] (* (:t b) (:p b))))
   #'term))
(defrule term
  (mkalt
   (mkret
    (mkseq
     (mklit \()
     w*
     (mkbind #'sum :s)
     w*
     (mklit \)))
    :s)
   #'integer))
(defrule integer (mkret (mk1om digit) (fn [b _] (Integer/parseInt (:ret b)))))

;; give it a shot:

;; (calc "50 + 20") => 70
;; (calc "2 * (2 + 2 * 2)") => 12

;; whitespace is a little awkward in `calc`
;; we can make it easier by doing whitespace "at the bottom"

;; let's make a more complex parser that can handle +,*,- (including -
;; as negation),/, and decimal numbers

;; I'll use caps to distinguish the rules from the grammar above

(defrule CALC
  (mkseq w* #'SUM end))

;; no need to mention whitespace in SUM
(defrule SUM
  (mkalt
   (mkret
    (mkseq
     (mkbind #'PRODUCT :p)
     #'PLUS
     (mkbind #'SUM :s))
    (fn [b _]
      (+ (:p b) (:s b))))
   (mkret
    (mkseq
     (mkbind #'PRODUCT :p)
     #'MINUS
     (mkbind #'SUM :s))
    (fn [b _]
      (- (:p b) (:s b))))
   #'PRODUCT))

(defrule PRODUCT
  (mkalt
   (mkret
    (mkseq
     (mkbind #'TERM :t)
     #'SLASH
     (mkbind #'PRODUCT :p))
    (fn [b _] (/ (:t b) (:p b))))
   (mkret
    (mkseq
     (mkbind #'PREFIX :t)
     #'STAR
     (mkbind #'PRODUCT :p))
    (fn [b _] (* (:t b) (:p b))))
   #'PREFIX))
(defrule PREFIX
  (mkalt
   (mkret
    (mkseq #'PLUS (mkbind #'TERM :i))
    (fn [b _] (:i b)))
   (mkret
    (mkseq #'MINUS (mkbind #'TERM :i))
    (fn [b _] (- (:i b))))
   #'TERM))
(defrule TERM
  (mkalt
   (mkseq
    (mknothing #'OPEN)
    #'SUM
    (mknothing #'CLOSE))
   #'NUMBER))

;; we test for decimal first to ensure proper matching
(defrule NUMBER
  (mkalt #'DECIMAL #'INTEGER))

;; our "tokens" know about whitespace
(defrule INTEGER
  (mkret
   (mkseq (mk1om digit) w*)
   (fn [b _]
     (Long/parseLong (:ret b)))))
(defrule DECIMAL
  (mkret
   (mkseq
    (mkalt
     (mkseq (mk1om digit) #'POINT (mk1om digit))
     (mkseq               #'POINT (mk1om digit))
     (mkseq (mk1om digit) #'POINT))
    w*)
   (fn [b _]
     (Double/parseDouble (:ret b)))))

(defrule OPEN  (mkseq (mklit \() w*))
(defrule CLOSE (mkseq (mklit \)) w*))
(defrule PLUS  (mkseq (mklit \+) w*))
(defrule MINUS (mkseq (mklit \-) w*))
(defrule STAR  (mkseq (mklit \*) w*))
(defrule SLASH (mkseq (mklit \/) w*))

(defrule POINT (mklit \.))

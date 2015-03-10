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
(ns squarepeg.core
  (:use [squarepeg combinators])
  (:require [squarepeg.dsl :as dsl]))

(defmacro defrule
  ([name & body]
     `(defrule* ~name ~(dsl/parserule body))))


(defrule calc sum)
(defrule sum
  p:product whitespace* \+ whitespace* s:sum => (+ p s))
(defrule product
  t:term whitespace* \* whitespace* p:product => (* t p))
(defrule term
  integer => ret | integer)
(defrule integer
  digit+ => (Integer/parseInt ret))

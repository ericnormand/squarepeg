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
  (:use squarepeg.core))

;; match 0 or more whitespace

(defrule  w**  {whitespace *})

;; match 0 or more whitespace but return nothing
;; (effectively ignoring whitespace)
(defrule w* (mknothing (mkzom whitespace)))

;; match a newline
(defrule newline \newline)

;; match anything that is not a newline
(defrule non-newline [{! newline} anything])

;; match clojure style single-line comments
(defrule clj-comment [\; {non-newline *} (or newline
                                             end)])

;; match an integer and parse it
;; :ret is bound to the entire match's return value
;; this will only work if context contains :expected-type :string
;; which happens when you start with a string
(defrule integer {digit +} #{(fn [b c] (Integer/parseInt (:ret b)))})

;; match a + character
(defrule plus \+)

;; match a * character
(defrule times \*)

;; match an open paren
(defrule open \()

;; match a close paren
(defrule close \))

;; using the above rules, we can define a simple calculator

;; we must declare our variables here because the rules are mutually recursive
(declare sum product term)

(defrule product
  [{:a term} w* times w* {:b product}] #{(fn [b c] (* (:a b) (:b b)))}
  term)

(defrule sum
  [{:a product} w* plus w* {:b sum}] #{(fn [b c] (+ (:a b) (:b b)))}
  product)

(defrule term
  [open w* {:value sum} w* close] #{(fn [b c] (:value b))}
  integer)

;; create a convenient function to call the rule with
(defrule calc sum)

;; try (calc "(2 +4) * 9")
;; try (calc "2 + 4 * 9")

;; besides operating on strings, we can also operate on seqs
;; we can define a math-expression optimizer

(declare addition multiplication expr fncall args)

;;  call it like this:
;; (optimize '[(+ 1 (* 2 3) a)]) => '(+ 7 a)
(defrule optimize expr)

;; an expression is a (+) expr, a (*) expr, another fncall, a var, or
;; a number
;; => defines a subrule (matching on a seq within a seq)
(defrule expr
  (=> addition)
  (=> multiplication)
  (=> fncall)
  match-symbol
  match-number)

(defn separate [pr coll]
  [(filter pr coll)
   (filter (complement pr) coll)])

;; an addition expression is a + followed by arguments
;; args will recursively optimize the arguments
(defrule addition [(mklit '+) {:args args}]
                   ;;find the literal numbers
  #{(fn [b c]
      (let [[nums other] (separate number? (:args b))
            sum (apply + nums)]
        (cond
         ;; if there are only numbers, we
         ;; don't need to add at runtime
         (nil? (seq other))
         sum
         ;; if the sum is zero and only one
         ;; other term, we don't need to add
         (and (zero? sum) (nil? (rest other)))
         (first other)
         ;; if the sum is zero, just add the
         ;; other terms
         (zero? sum)
         `(+ ~@other)
         ;; otherwise, return the addition
         :otherwise 
         `(+ ~sum ~@other))))})

(defrule multiplication  [(mklit '*) {:args args}]
  ;;find the literal numbers
  #{(fn [b c]
      (let [[nums other] (separate number? (:args b))
            prod (apply * nums)]
        (cond
         ;; if there are only numbers, we
         ;; don't need to multiply at runtime
         (nil? (seq other))
         prod
         ;; if the prod is zero, we return 0
         (zero? prod)
         0
         ;; if the prod is 1, we don't need
         ;; it
         (and (= 1 prod) (nil? (rest other)))
         (first other)
         (= 1 prod)
         `(* ~@other)
         ;; otherwise, return the addition
         :otherwise 
         `(* ~prod ~@other))))})

(defrule fncall
  [{:sym match-symbol} {:args args}] #{(fn [b c]
                                         `(~(:sym b) ~@(:args b)))})

;; match all args up to the end
(defrule args [{expr *} end])

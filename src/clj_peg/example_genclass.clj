(ns clj-peg.example-genclass
  (:use clj-peg)
  (:gen-class
   :name parsezero))

(defrule eatme \0)

(defn -eatme [this input]
  (eatme input))

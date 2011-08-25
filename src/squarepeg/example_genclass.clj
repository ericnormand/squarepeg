(ns squarepeg.example-genclass
  (:use squarepeg.core)
  (:gen-class
   :name parsezero))

(defrule eatme \0)

(defn -eatme [this input]
  (eatme input))

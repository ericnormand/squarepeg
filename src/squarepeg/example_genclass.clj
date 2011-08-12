(ns squarepeg.example-genclass
  (:use squarepeg)
  (:gen-class
   :name parsezero))

(defrule eatme \0)

(defn -eatme [this input]
  (eatme input))

(ns squarepeg.example-genclass
  (:require [squarepeg.core :refer :all])
  (:gen-class
   :name parsezero))

(defrule eatme (mklit 0))

(defn -eatme [this input]
  (eatme input))

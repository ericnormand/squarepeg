(ns clj-peg.examples
  (:use clj-peg.dsl)
  (:use clj-peg.peg))

;; a number parser

(def digits (range 10))

(def simple-number-names
  [
   ["eleven"      , 11],
   ["twelve"      , 12],
   ["thirteen"    , 13],
   ["fourteen"    , 14],
   ["fifteen"     , 15],
   ["sixteen"     , 16],
   ["seventeen"   , 17],
   ["eighteen"    , 18],
   ["nineteen"    , 19],
   ["ninteen"     , 19], ;; Common mis-spelling
   ["zero"        , 0],
   ["one"         , 1],
   ["two"         , 2],
   ["three"       , 3],
   ["four"        , 4],  ;; The weird regex is so that it matches four but not fourty
   ["five"        , 5],
   ["six"         , 6],
   ["seven"       , 7],
   ["eight"       , 8],
   ["nine"        , 9],
   ["ten"         , 10],
;;   ["\\ba[\\b^$]" , "1"] ;; doesn"t make sense for an "a" at the end to be a 1
   ]
)

(def ordinals
  [
   ["first"   , 1 "st"],
   ["third"   , 3 "rd"],
   ["fourth"  , 4 "th"],
   ["fifth"   , 5 "th"],
   ["sixth"   , 6 "th"],
   ["seventh" , 7 "th"],
   ["eighth"  , 8 "th"],
   ["ninth"   , 9 "th"],
   ["tenth"   , 10 "th"]
   ])

(def ten-prefixes
  [
   ["twenty"  , 20],
   ["thirty"  , 30],
   ["forty"   , 40],
   ["fourty"  , 40], ;; Common mis-spelling
   ["fifty"   , 50],
   ["sixty"   , 60],
   ["seventy" , 70],
   ["eighty"  , 80],
   ["ninety"  , 90]
   ])

(def powers-of-ten
  [
   ["hundred"  , 100],
   ["thousand" , 1000],
   ["million"  , 1000000],
   ["billion"  , 1000000000],
   ["trillion" , 1000000000000],
   ])

(def match-digit (apply OR (map str digits)))

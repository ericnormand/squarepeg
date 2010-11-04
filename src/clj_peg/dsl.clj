(ns clj-peg.dsl
  (:use clj-peg.peg))

(defprotocol ParserNotation
  (psr [x]))

(extend-protocol ParserNotation
  clojure.lang.IPersistentVector
  (psr [x]
	  ;; a vector is either a sequence or an alternative
	  (cond
	   ;; alternative
	   (and (= 1 (count x)) (vector? (first x)))
	   (mkalt (map psr (first x)))

	   :otherwise ;; just a sequence
	   (mkseq (map psr x))))
  clojure.lang.IPersistentMap
  (psr [m]
	  ;; a map is a binding or a return
	  (let [[r v] (first m)]
	    (cond
	     ;; if value is a keyword, make a binding
	     (keyword? v)
	     (mkbind (psr r) v)
	     ;; it's a function that generates a new return value
	     (fn? v)
	     (mkret (psr r) v)
	     :otherwise ;; it's just a return value
	     (mkret (psr r) (constantly v)))
	    ))
  clojure.lang.IPersistentSet
  (psr [s]
	  ;; a set is an operator
	  (cond
	   (s :*)
	   (-> s (disj :*) first psr mkzom)
	   (s :+)
	   (-> s (disj :+) first psr mk1om)
	   (s :?)
	   (-> s (disj :?) first psr mkopt)
	   (s :-)
	   (-> s (disj :-) first psr mknot)
	   (= 1 (count s)) ;; a single element is treated as a predicate
	   (-> s first mkpr)
	   ))
  String
  (psr [s]
	  (mkstr s))
  clojure.lang.Fn
  (psr [f]
       ;; want to wrap a rule so it returns the bindings
       (mkrule f))
  clojure.lang.Keyword
  (psr [f]
       (mklit f))
  Number
  (psr [f]
       (mklit f))
  clojure.lang.Var
  (psr [f]
       (mkrule f))
  Character
  (psr [c]
	  (mklit c)))

(defn ? [p v]
  (mkpred #(-> % v p)))

(defn =- [rule] (mksub (psr rule)))

(defn => [f & vars]
  #(apply f (map % vars)))

(defn parser [x]
  (-> x psr mkfn))

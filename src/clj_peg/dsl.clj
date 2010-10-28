(ns clj-peg.dsl
  (:use clj-peg.peg)
  (:use clj-peg.derived-rules)
  (:use clj-peg.grammar))

(defprotocol ParserNotation
  (psr [x]))

(extend-protocol ParserNotation
  clojure.lang.IPersistentVector
  (psr [x]
	  ;; a vector is either a sequence or an alternative or a special form (none yet)
	  
	  (cond
	   (and (= 1 (count x)) (vector? (first x)))
	   (mkalt (map psr (first x)))

	   :otherwise ;; just a sequence
	   (mkseq (map psr x))))
  clojure.lang.IPersistentMap
  (psr [m]
	  ;; a map is a binding or a return
	  (let [[r v] (first m)]
	    (cond
	     (keyword? v)
	     (mkbind (psr r) v)
	     (fn? v)
	     (mkret (psr r) v)
	     :otherwise ;; it's just a return value
	     (mkret (psr r) (constantly [v])))
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
	   (= 1 (count s)) ;; a single element is treated as a predicate
	   (-> s first mkpr)
	   ))
  String
  (psr [s]
	  (mkstr s))
  clojure.lang.Fn
  (psr [f]
       f)
  clojure.lang.Keyword
  (psr [f]
	  (mklit f))
  Character
  (psr [c]
	  (mklit c)))

(defn $< [x]
  (cond
   (nil? (seq x))
   nil
   (nil? (next x))
   (first x)
   :otherwise
   x))

(defn $> [x]
  (if (vector? x)
    x
    [x]))

(defn ? [p v]
  (mkpred #(-> % v first p)))

(defn =- [rule] (mksub (psr rule)))

(defn => [f & vars]
  #($> (apply f (map $< (map % vars)))))

(defn parser [x]
  (-> x psr mkfn))

(def day-word (parser [[{:today 100}
			{:yesterday 0}
			{:tomorrow 200}]]))

(def amount-exp {[{match-number :x} :days] (=> #(* % 1000 60 60 24) :x)})

(def rel-exp {[{amount-exp :x} :before] (=> #(fn [x] (- x %)) :x)})

(def date-expr (parser {[{rel-exp :op} {day-word :time}]
			(=> #(%1 %2) :op :time)}))

(def path-sep "/")
(def path-root "/")

(def path-seg [[".."
		[{anything :a} (? #(not (= \/ %)) :a)]]])

(def path-match [[[path-root path-match] 
		  [path-seg #{[path-sep path-seg] :*}]
		  {path-root "root"}
		  ]])

(def infinite-as (parser {#{"a" :*} (=> count :-match-)}))

(defn g []
  'hello)

(def handle-request (parser
		     [[[(=- "/") :get]
		       ]]))
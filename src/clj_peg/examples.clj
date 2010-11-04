(ns clj-peg.examples
  (:use clj-peg.dsl)
  (:use clj-peg.peg))

(defn now []
  (System/currentTimeMillis))

(def time-unit [[{:days    (* 1000 60 60 24)}
		 {:day     (* 1000 60 60 24)}
		 {:hour    (* 1000 60 60)}
		 {:hours   (* 1000 60 60)}
		 {:minute  (* 1000 60)}
		 {:minutes (* 1000 60)}
		 {:second  1000}
		 {:seconds 1000}]])

(def time-interval {[{#{number?} :n} {time-unit :u}] (=> * :n :u)})

(def absolute-time [[{:now         (=> now)}
		     {:today       (=> now)}
		     {:yesterday   (=> #(- (now) (* 1000 60 60 24)))}
		     {:tomorrow    (=> #(+ (now) (* 1000 60 60 24)))}
		     ]])

(def relative-time (parser [[
			     {[{time-interval :i} :ago] (=> #(- (now) %) :i)}
			     {[{time-interval :i} :before {absolute-time :t}] (=> - :t :i)}
			     {[{time-interval :i} [[:after :from]] {absolute-time :t}] (=> + :t :i)}
			     ]]))


(declare calc)
(declare sum)
(declare product)

(def term (parser [[#{number?}
		    {#{vector?} (=> #'calc :match)}]]))

(def product (parser [[
		       {[{#'term :a} :* {#'sum :b}] (=> * :a :b)}
		       #'term
		       ]]))

(def sum (parser [[
		   {[{#'product :a} :+ {#'sum :b}] (=> + :a :b)}
		   #'product
		   ]]))

(def calc (parser {[sum end] (=> first :match)}))

(comment

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
			 ]])))
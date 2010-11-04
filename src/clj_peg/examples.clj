(ns clj-peg.examples
  (:use clj-peg.dsl))



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
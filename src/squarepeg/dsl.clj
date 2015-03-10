(ns squarepeg.dsl
  (:use [squarepeg.combinators])
  (:require [clojure.set :as set]))

(defrule* rule (mkseq #'ws* #'alternatives #'ws* #'end))

(defrule* alternatives
  (mkret
   (mkseq (mkbind #'sequen :s1)
          (mkbind (mkzom (mkret (mkseq #'BAR (mkbind #'sequen :s)) :s)) :ss))
   (fn [{:keys [s1 ss]} _]
     (prn ss)
     {:bindings (reduce set/union (:bindings s1) (map :bindings ss))
      :expression `(mkalt ~(:expression s1) ~@(map :expression ss))})))

(defrule* sequen
  (mkalt
   (mkret (mkseq (mkbind (mk1om #'expression) :exps)
                 #'ARROW
                 (mkbind #'clojure-expression :rt))
          (fn [{:keys [exps rt]} _]
            (prn exps)
            {:bindings (reduce set/union #{} (map :bindings exps))
             :expression `(mkret (mkseq ~@(map :expression exps))
                                 (fn [{:keys [~@(cons 'ret (mapcat :bindings exps))]
                                       :as ~(symbol "*bindings*")}
                                      ~(symbol "*context*")]
                                   ~(:expression rt)))}))
   (mkret (mkbind (mk1om #'expression) :exps)
          (fn [{:keys [exps]} _]
            (prn exps)
            {:bindings (reduce set/union #{} (map :bindings exps))
             :expression `(mkseq ~@(map :expression exps))}))))

(defrule* expression
  (mkseq
   (mkalt
    #'named-expression
    #'operated-expression
    #'naked-expression)
   #'ws*))

(defrule* named-expression
  (mkalt
   (mkret (mkseq (mkbind #'sname :n) #'COLON (mkbind #'operated-expression :e))
          (fn [{:keys [n e]} _]
            {:bindings (conj (:bindings e) (:expression n))
             :expression `(mkbind ~(:expression e) ~(keyword (:expression n)))}))
   (mkret (mkseq (mkbind #'sname :n) #'COLON (mkbind #'naked-expression :e))
          (fn [{:keys [n e]} _]
            {:bindings (conj (:bindings e) (:expression n))
             :expression `(mkbind ~(:expression e) ~(keyword (:expression n)))}))))

(defrule* operated-expression
  (mkalt
   (mkret (mkseq (mkbind #'naked-expression :e) #'STAR)
          (fn [{:keys [e]} _]
            {:bindings (:bindings e)
             :expression `(mkzom ~(:expression e))}))
   (mkret (mkseq (mkbind #'naked-expression :e) #'QUEST)
          (fn [{:keys [e]} _]
            {:bindings (:bindings e)
             :expression `(mkopt ~(:expression e))}))
   (mkret (mkseq (mkbind #'naked-expression :e) #'PLUS)
          (fn [{:keys [e]} _]
            {:bindings (:bindings e)
             :expression `(mk1om ~(:expression e))}))))

(defrule* naked-expression
  (mkalt (mkret (mkbind #'sname :n)
                (fn [{{exp :expression} :n} _]
                  {:bindings #{}
                   :expression `#'~exp}))
         (mkret (mkseq (mkstr "(") (mkbind #'alternatives :e) (mkstr ")") #'ws*) :e)
         (mkret (mkbind #'literal-string :s)
                (fn [{:keys [s]} _]
                  {:bindings #{}
                   :expression `(mkstr ~(:expression s))}))
         (mkret (mkbind #'literal-char :c)
                (fn [{:keys [c]} _]
                  {:bindings #{}
                   :expression `(mklit ~(:expression c))}))
         ))

(def invalid-chars (set "{}[]()"))

(defrule* clojure-expression
  (mkret
   (mkmatch
    (mkalt
     (mkseq (mkstr "(") (mkzom #'clojure-expression) (mkstr ")") #'ws*)
     (mkseq (mkstr "[") (mkzom #'clojure-expression) (mkstr "]") #'ws*)
     (mkseq (mkstr "{") (mkzom #'clojure-expression) (mkstr "}") #'ws*)
     (mkseq (mkstr "#{") (mkzom #'clojure-expression) (mkstr "}") #'ws*)
     (mkseq (mkstr "#(") (mkzom #'clojure-expression) (mkstr ")") #'ws*)
     (mkseq (mklit \") (mkzom (mkseq
                               (mknot (mkpred #(= \" %)))
                               (mkalt (mkstr "\\\"")
                                      match-char))) (mklit \") #'ws*)
     (mkseq (mk1om (mkalt (mkstr "\\)")
                          (mkstr "\\}")
                          (mkstr "\\]")
                          (mkpr (complement invalid-chars)))) #'ws*)))
   (fn [{:keys [match]} _]
     {:bindings #{}
      :expression (read-string match)})))

(defrule* sname
  (mkret (mkseq (mkmatch (mkseq alpha (mkzom alphanum))) #'ws*)
         (fn [{:keys [match]} _]
           {:bindings #{}
            :expression (symbol match)})))

(defrule* literal-string
  (mkret (mkmatch (mkseq (mklit \")
                         (mkzom (mkseq
                                 (mknot (mkpr #(= \" %)))
                                 (mkalt (mkstr "\\\"")
                                        match-char)))
                         (mklit \") #'ws*))
         (fn [{:keys [match]} _]
           {:bindings #{}
            :expression (read-string match)})))

(defrule* literal-char
  (mkret (mkseq (mklit \\)
                (mkbind match-char :c)
                #'ws*)
         (fn [{:keys [c]} _]
           {:bindings #{}
            :expression c})))

(defrule* ARROW (mkseq (mkstr "=>") #'ws*))
(defrule* COLON (mkseq (mklit \:) #'ws*))

(defrule* BAR (mkseq (mklit \|) #'ws*))

(defrule* STAR (mkseq (mklit \*) #'ws*))
(defrule* QUEST (mkseq (mklit \?) #'ws*))
(defrule* PLUS (mkseq (mklit \+) #'ws*))

(defrule* ws* (mkzom (mkalt whitespace (mklit \,))))

(defn parserule [body]
  (let [r (pr-str body)
        r (.substring r 1 (dec (count r)))
        [{:keys [expression]}] (rule r)]
    expression))




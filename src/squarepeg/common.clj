(ns squarepeg.common
  (:require [clojure.set :as set]))

(comment
 (defn- externs [exp]
   (cond
    (symbol? exp)
    (if (or (contains? (ns-refers *ns*) exp)
            (contains? (ns-map *ns*) exp)
            (= \# (last (name exp)))
            (namespace exp))
      #{}
      #{exp})
    (seq? exp)
    (reduce set/union #{} (map externs exp))
    (list? exp)
    (reduce set/union #{} (map externs exp))
    (map? exp)
    (reduce set/union #{} (mapcat #(map externs %) exp))
    (set? exp)
    (reduce set/union #{} (map externs exp))
    (vector? exp)
    (reduce set/union #{} (map externs exp))
    :else
    #{})))

(comment
 (defmacro defrule*
   ([name & body]
      (list 'do
            (cons 'declare (externs body))
            (list 'def
                  (with-meta name
                    {:arglists ''([input] [input context] [input bindings context memo])
                     :doc (str name " is a squarepeg parser rule. Call with a
        seq of input or use it as a rule (4 arguments).")})
                  `(mkfn (comb/mkmsg (comb/mkscope (comb/mkmemo ~body))
                                     (str "Failed trying to match '" ~(str name) "'"))))))))

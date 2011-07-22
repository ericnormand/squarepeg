(ns clj-peg.macros
  (:use clj-peg.combinators))

(defmacro defrule
  ([name body]
     (list 'def
           (with-meta name
             {:arglists ''([input] [input context] [input bindings context memo])
              :doc (str name " is a clj-peg parser rule. Call with a seq of input or
        use it as a rule (4 arguments).")})
           `(mkfn ~body))))

(ns clj-peg.examples
  (:use [clj-peg.combinators :as peg]))

;; match 0 or more whitespace
(def w** (mkscope (mkzom whitespace)))

;; match 0 or more whitespace but return nothing
;; (effectively ignoring whitespace)
(def w* (mkscope (mknothing (mkzom whitespace))))

;; match a newline
(def newline (mkscope (mklit \newline)))

;; match anything that is not a newline
(def non-newline (mkscope (mkseq [(mknot newline) anything])))

;; match clojure style single-line comments
(def clj-comment (mkscope (mkmemo (mkseq [(mklit \;) (mkzom non-newline) (mkalt [newline end])]))))

;; match an integer and parse it
;; the (apply str ) is necessary at the moment to convert the seq of
;; chars back to a string
;; mkscope is important to preven the binding of :ret (from mkret) to
;; leak out to the calling rule
(def integer (mkscope (mkmemo (mkret (mk1om digit) (fn [b c] (Integer/parseInt (apply str (:ret b))))))))

;; match a + character
(def plus (mkscope (mklit \+)))

;; match a * character
(def times (mkscope (mklit \*)))

;; match an open paren
(def open (mkscope (mklit \()))

;; match a close paren
(def close (mkscope (mklit \))))

;; using the above rules, we can define a simple calculator

;; we must declare our variables here because the rules are mutually recursive
(declare sum product term)

;; also, it is often necessary to use the var (with #') when using
;; mutually or self-recursive rules
;; note that because we use #' and we have predeclared, the rules can
;; appear in any order
(def term (mkscope (mkmemo (mkalt [(mkret (mkseq [open w* (mkbind #'sum :value) w* close]) :value)
                                   integer]))))

(def product (mkscope (mkmemo (mkalt [(mkret (mkseq [(mkbind term :a) w* times w* (mkbind #'product :b)])
                                             (fn [b c] (* (:a b) (:b b))))
                                      term
                                      ]))))

(def sum (mkscope (mkmemo (mkalt [(mkret (mkseq [(mkbind #'product :a) w* plus w* (mkbind #'sum :b)])
                                         (fn [b c] (+ (:a b) (:b b))))
                                  #'product
                                  ]))))

;; create a convenient function to call the rule with
(def calc (mkfn #'sum))

;; a future release will contain a dsl for defining the rules more conveniently

;; besides operating on strings, we can also operate on seqs
;; we can define a math-expression optimizer

(declare addition multiplication expr fncall args)

(def optimize- (mkfn #'expr))
(defn optimize [e]
  (optimize- [e] {:print-hits true}))

;; an expression is a (+) expr, a (*) expr, another fncall, a var, or
;; a number
(def expr (mkscope (mkmemo (mkalt [(mksub #'addition)
                                   (mksub #'multiplication)
                                   (mksub #'fncall)
                                   match-symbol
                                   match-number]))))

(defn separate [pr coll]
  [(filter pr coll)
   (filter (complement pr) coll)])

;; an addition expression is a + followed by arguments
;; args will recursively optimize the arguments
(def addition (mkscope
               (mkmemo
                (mkret (mkseq [(mklit '+) (mkbind #'args :args)])
                       ;;find the literal numbers
                       (fn [b c]
                         (let [[nums other] (separate number? (:args b))
                               sum (apply + nums)]
                           (cond
                            ;; if there are only numbers, we
                            ;; don't need to add at runtime
                            (nil? (seq other))
                            sum
                            ;; if the sum is zero and only one
                            ;; other term, we don't need to add
                            (and (zero? sum) (nil? (rest other)))
                            (first other)
                            ;; if the sum is zero, just add the
                            ;; other terms
                            (zero? sum)
                            `(+ ~@other)
                            ;; otherwise, return the addition
                            :otherwise 
                            `(+ ~sum ~@other))))))))

(def multiplication (mkscope
                     (mkmemo
                      (mkret (mkseq [(mklit '*) (mkbind #'args :args)])
                             ;;find the literal numbers
                             (fn [b c]
                               (let [[nums other] (separate number? (:args b))
                                     prod (apply * nums)]
                                 (cond
                                  ;; if there are only numbers, we
                                  ;; don't need to multiply at runtime
                                  (nil? (seq other))
                                  prod
                                  ;; if the prod is zero, we return 0
                                  (zero? prod)
                                  0
                                  ;; if the prod is 1, we don't need
                                  ;; it
                                  (and (= 1 prod) (nil? (rest other)))
                                  (first other)
                                  (= 1 prod)
                                  `(* ~@other)
                                  ;; otherwise, return the addition
                                  :otherwise 
                                  `(* ~prod ~@other))))))))

(def fncall (mkscope
             (mkmemo
              (mkret (mkseq [(mkbind match-symbol :sym) (mkbind #'args :args)])
                     (fn [b c]
                       `(~(:sym b) ~@(:args b)))))))

;; match all args up to the end
(def args (mkscope (mkmemo (mkseq [(mkzom #'expr) end]))))

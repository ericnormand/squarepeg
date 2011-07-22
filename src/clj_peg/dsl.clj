(ns clj-peg.dsl
  (:use clj-peg.combinators))

(declare OR)

(defrule SP (mkzom whitespace))
(defrule QUOTE (mklit \'))
(defrule CHAR  (mkalt [(mkseq [(mklit \\) (map mklit "abefnrtv'\"[]\\")])
                       (mkseq [(mknot (mklit \\)) anything])]))
(defrule BAR (mkseq [(mklit \|) SP]))
(defrule SEMI (mkseq [(mklit \;) SP]))
(defrule STAR (mkseq [(mklit \*) SP]))
(defrule PLUS (mkseq [(mklit \+) SP]))

(defrule OPEN (mkseq [(mklit \() SP]))
(defrule CLOSE (mkseq [(mklit \)) SP]))
;; Trying to match legal clojure symbols, but this is good enough
(defrule IDENT (mkret (mkseq [(mkmatch (mkseq [(mkalt (cons alpha    (map mklit "*+!-_?")))
                                               (mkzom (mkalt (cons alphanum (map mklit "*+!-_?/."))))]))
                              SP])
                      (fn [b c]
                        (eval `(var ~(symbol (:match b)))))))

(defrule STRING (mkret (mkseq [QUOTE (mkmatch (mkzom (mkseq [(mknot QUOTE) CHAR]))) QUOTE SP])
                       (fn [b c] (mkstr (:match b)))))

(defrule ACTION (mkret (mkseq [(mkstr "=>")
                               (mkmatch (mk1om (mkseq [(mknot SEMI) anything])))
                               SEMI])
                       (fn [b c]
                         (read-string (:match b)))))



(defrule PRIMARY (mkalt [STRING
                         IDENT
                         (mkret (mkseq [OPEN (mkbind #'OR :meat) CLOSE])
                                (fn [b c]
                                  (:meat b)))]))

(defrule SUFFIX (mkalt [(mkret (mkseq [(mkbind PRIMARY :p) STAR])
                               (fn [b c]
                                 (mkzom (:p b))))
                        (mkret (mkseq [(mkbind PRIMARY :p) PLUS])
                               (fn [b c]
                                 (mk1om (:p b))))
                        PRIMARY])
 )

(defrule SEQ (mkalt [ (mkret (mkseq [(mkbind (mk1om SUFFIX) :seq) (mkbind ACTION :action)])
                                      (fn [b c]
                                        (mkret (mkseq (:seq b))
                                               (fn [x y]
                                                  (eval (:action b))))))
                      (mkret (mk1om SUFFIX)
                             (fn [b c]
                               (mkseq (:ret b))))]))


(defrule OR (mkret (mkalt [(mkseq [(mkbind SEQ :a) (mkzom (mkseq [BAR (mkbind OR :b)]))])])
                   (fn [b c] (if (:b b)
                              (mkalt [(:a b) (:b b)])
                              (:a b)))))

(defrule pegdef OR)

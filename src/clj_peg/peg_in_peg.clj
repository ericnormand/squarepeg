(ns clj-peg.peg-in-peg
  (:use clj-peg.peg)
  (:use clj-peg.grammar)
  (:use clj-peg.derived-rules))

;; I don't really like this because it uses eval, but I don't know how to
;; dynamically let something any other way
(defn build-let-fn [p]
  (fn [b]
    (eval
     `(let [~@(apply concat b)]
	~p))))

(defrule rule-def
  (mkret
   (mkseq
    [(mkbind (mkrule 'rule-name) 'rule-name)
     (mklit '=)
     (mkbind (mkrule 'alternatives) 'rule-body)
     (mkrule 'end)])
   (fn [b]
     [{:rule-name (first (b 'rule-name))
       :rule-body (first (b 'rule-body))}])))

; a symbol but not a keyword
(defrule rule-name
  (mkseq
   [(mknot (mkrule 'peg-keyword))
    (mkrule 'match-symbol)]))

(defrule sequence-match
     (mkret
      (mkseq
       [(mkseq
	 (mk1om (mkrule 'rule-element))
	 'rules)
	(make-opt-matcher
	 (make-sequence-matcher
	  [(make-rule-matcher 'clj-peg.peg-in-peg/literal->)
	   (make-bind
	    (make-rule-matcher 'clj-peg.peg-in-peg/return-expression)
	    'r-exp)]))])
      (fn [b]
	(let [rules (b 'rules)
	      exp (first (b 'r-exp))]
	  [(if exp
	     (make-return
	      (make-sequence-matcher rules)
	      exp)
	     (make-sequence-matcher rules))]))))

(def return-expression
     (make-return
      (make-rule-matcher 'clj-peg.derived-rules/anything)
      (fn [b]
	(let [exp (first (b '-match-))]
	  [(build-let-fn exp)]))))

(def alternatives
     (make-return
      (make-sequence-matcher
       [(make-zero-or-more-matcher
	 (make-sequence-matcher
	  [(make-rule-matcher 'clj-peg.peg-in-peg/sequence-match)
	   (make-rule-matcher 'clj-peg.peg-in-peg/literal-slash)]))
	(make-rule-matcher 'clj-peg.peg-in-peg/sequence-match)])
      (fn [b]
	(let [alts (b '-match-)]
	  [(make-alt-matcher
	    (filter #(not (= '/ %)) alts))]))))

(def rule-element
     (make-alt-matcher
      [(make-rule-matcher 'clj-peg.peg-in-peg/binding-match)
       (make-rule-matcher 'clj-peg.peg-in-peg/optional)
       (make-rule-matcher 'clj-peg.peg-in-peg/zero-or-more)
       (make-rule-matcher 'clj-peg.peg-in-peg/one-or-more)
       (make-rule-matcher 'clj-peg.peg-in-peg/rule-element-atomic)]))

(def binding-match
     (make-return
      (make-sequence-matcher
       [(make-bind (make-rule-matcher 'clj-peg.peg-in-peg/rule-element-atomic) 'rule)
	(make-rule-matcher 'clj-peg.peg-in-peg/literal=>)
	(make-bind (make-rule-matcher 'clj-peg.peg-in-peg/variable) 'var)])
      (fn [b]
	(let [var (first (b 'var))
	      rule (first (b 'rule))]
	  [(make-bind rule var)]))))

(def optional
     (make-return
      (make-sequence-matcher
       [(make-bind (make-rule-matcher  'clj-peg.peg-in-peg/rule-element-atomic) 'rule)
	(make-rule-matcher 'clj-peg.peg-in-peg/literal?)])
      (fn [b]
	(let [rule (first (b 'rule))]
	  [(make-opt-matcher rule)]))))

(def zero-or-more
     (make-return
      (make-sequence-matcher
       [(make-bind (make-rule-matcher 'clj-peg.peg-in-peg/rule-element-atomic) 'rule)
	(make-rule-matcher 'clj-peg.peg-in-peg/literal*)])
      (fn [b]
	(let [rule (first (b 'rule))]
	  [(make-zero-or-more-matcher rule)]))))

(def not-rule
     (make-return
      (make-sequence-matcher
       [(make-rule-matcher  'clj-peg.peg-in-peg/literal--)
	(make-bind (make-rule-matcher  'clj-peg.peg-in-peg/rule-element-atomic) 'rule)])
      (fn [b]
	(let [rule (first (b 'rule))]
	  [(make-not rule)]))))

(def one-or-more
     (make-return
      (make-sequence-matcher
       [(make-bind (make-rule-matcher 'clj-peg.peg-in-peg/rule-element-atomic) 'rule)
	(make-rule-matcher  'clj-peg.peg-in-peg/literal+)])
      (fn [b]
	(let [rule (first (b 'rule))]
	  [(make-one-or-more-matcher rule)]))))

(def rule-element-atomic
     (make-alt-matcher
      [(make-rule-matcher 'clj-peg.peg-in-peg/predicate)
       (make-rule-matcher 'clj-peg.peg-in-peg/parenthesis)
       (make-rule-matcher 'clj-peg.peg-in-peg/rule-match)
       (make-rule-matcher 'clj-peg.peg-in-peg/string-matcher)
       (make-rule-matcher 'clj-peg.peg-in-peg/atom-match)
       (make-rule-matcher 'clj-peg.peg-in-peg/not-rule)]))

(def atom-match
     (make-return
      (make-alt-matcher
       [(make-rule-matcher 'clj-peg.derived-rules/match-number)
	(make-rule-matcher 'clj-peg.derived-rules/match-char)])
      (fn [b]
	(let [a (first (b 'a))]
	  [(make-literal-matcher a)]))))

(def predicate
     (make-return
      (make-rule-matcher 'clj-peg.derived-rules/match-list)
      (fn [b]
	(let [p (first (b '-match-))]
	  [(make-pred-matcher (build-let-fn p))]))))

(def parenthesis
     (make-return
      (make-rule-matcher 'clj-peg.derived-rules/match-vec)
      (fn [b]
	(let [v (first (b '-match-))
	      r (alternatives v b)]
	  (:r r)))))

(def rule-match
     (make-return
      (make-sequence-matcher
       [(make-not (make-rule-matcher 'clj-peg.peg-in-peg/peg-keyword))
	(make-rule-matcher 'clj-peg.derived-rules/match-symbol)])
      (fn [b]
	(let [s (first (b '-match-))]
	  [(make-rule-matcher  s)]))))

(def string-matcher
     (make-return
      (make-rule-matcher 'clj-peg.derived-rules/match-string)
      (fn [b]
	(let [s (first (b '-match-))]
	  [(make-string-matcher s)]))))

(def variable
     (make-sequence-matcher
      [(make-not (make-rule-matcher 'clj-peg.peg-in-peg/peg-keyword))
       (make-rule-matcher 'clj-peg.derived-rules/match-symbol)]))

(def peg-keyword
     (make-alt-matcher
      [(make-rule-matcher 'clj-peg.peg-in-peg/literal=)
       (make-rule-matcher 'clj-peg.peg-in-peg/literal->)
       (make-rule-matcher 'clj-peg.peg-in-peg/literal?)
       (make-rule-matcher 'clj-peg.peg-in-peg/literal*)
       (make-rule-matcher 'clj-peg.peg-in-peg/literal+)
       (make-rule-matcher 'clj-peg.peg-in-peg/literal=>)
       (make-rule-matcher 'clj-peg.peg-in-peg/literal--)
       (make-rule-matcher 'clj-peg.peg-in-peg/literal-slash)]))

(def literal= (make-literal-matcher '=))
(def literal-> (make-literal-matcher '->))
(def literal? (make-literal-matcher '?))
(def literal* (make-literal-matcher '*))
(def literal+ (make-literal-matcher '+))
(def literal=> (make-literal-matcher '=>))
(def literal-- (make-literal-matcher '--))
(def literal-slash (make-literal-matcher '/))

;; a macro to create a new rule
  
(defmacro dr [& d]
  (let [r  (rule-def d {})]
    (if (nil? r)
      (throw (RuntimeException. "not a valid grammar"))
      (let [rule (first (:r r))]
       `(def ~(:rule-name rule) ~(:rule-body rule))))))
(ns clj-peg.peg-in-peg
  (:use clj-peg.peg)
  (:use clj-peg.grammar)
  (:use clj-peg.derived-rules))

(def keyword-list '(= -> ? * + => -- /))

;; I don't really like this because it uses eval, but I don't know how to
;; dynamically let something any other way
(defn build-let-fn [p]
  (fn [b]
    (eval
     `(let [~@(apply concat b)]
	~p))))

(def rule-def
     (make-return 
      (make-sequence-matcher
       [(make-bind (make-rule-matcher 'rule-name) 'rule-name)
	(make-rule-matcher 'literal=)
	(make-bind (make-rule-matcher 'alternatives) 'rule-body)
	(make-rule-matcher 'end)])
      (fn [b]
	[{:rule-name (first (b 'rule-name))
	  :rule-body (first (b 'rule-body))}])))

(def rule-name
     (make-sequence-matcher
      [(make-not (make-rule-matcher 'keyword-match))
       (make-rule-matcher 'symbol)]))

(def sequence-match
     (make-return
      (make-sequence-matcher
       [(make-bind
	 (make-one-or-more-matcher (make-rule-matcher 'rule-element))
	 'rules)
	(make-opt-matcher
	 (make-sequence-matcher
	  [(make-rule-matcher 'literal->)
	   (make-bind
	    (make-rule-matcher 'return-expression)
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
      (make-rule-matcher 'anything)
      (fn [b]
	(let [exp (first (b '-match-))]
	  [(build-let-fn exp)]))))

(def alternatives
     (make-return
      (make-sequence-matcher
       [(make-zero-or-more-matcher
	 (make-sequence-matcher [(make-rule-matcher 'sequence-match)
				 (make-rule-matcher 'literal-slash)]))
	(make-rule-matcher  'sequence)])
      (fn [b]
	(let [alts (b '-match-)]
	  [(make-alt-matcher
	    (filter #(not (= '/ %)) alts))]))))

(def rule-element
     (make-alt-matcher
      (map #(make-rule-matcher  %)
	   '(binding-match optional zero-or-more one-or-more rule-element-atomic))))

(def binding-match
     (make-return
      (make-sequence-matcher
       [(make-bind (make-rule-matcher 'rule-element-atomic) 'rule)
	(make-rule-matcher  '=>)
	(make-bind (make-rule-matcher  'variable) 'var)])
      (fn [b]
	(let [var (first (b 'var))
	      rule (first (b 'rule))]
	  [(make-bind rule var)]))))

(def optional
     (make-return
      (make-sequence-matcher
       [(make-bind (make-rule-matcher  'rule-element-atomic) 'rule)
	(make-rule-matcher 'literal?)])
      (fn [b]
	(let [rule (first (b 'rule))]
	  [(make-opt-matcher rule)]))))

(def zero-or-more
     (make-return
      (make-sequence-matcher
       [(make-bind (make-rule-matcher  'rule-element-atomic) 'rule)
	(make-rule-matcher 'literal*)])
      (fn [b]
	(let [rule (first (b 'rule))]
	  [(make-zero-or-more-matcher rule)]))))

(def not-rule
     (make-return
      (make-sequence-matcher
       [(make-rule-matcher  '--)
	(make-bind (make-rule-matcher  'rule-element-atomic) 'rule)])
      (fn [b]
	(let [rule (first (b 'rule))]
	  [(make-not rule)]))))

(def one-or-more
     (make-return
      (make-sequence-matcher
       [(make-bind (make-rule-matcher  'rule-element-atomic) 'rule)
	(make-rule-matcher  'literal+)])
      (fn [b]
	(let [rule (first (b 'rule))]
	  [(make-one-or-more-matcher rule)]))))

(def rule-element-atomic
     (make-alt-matcher
      (map #(make-rule-matcher  %)
	   '(predicate parenthesis rule-match string-match atom-match not-rule))))

(def atom-match
     (make-return
      (make-sequence-matcher
       [(make-bind (make-rule-matcher  'anything) 'a)
	(make-pred-matcher
	 (fn [b]
	   (let [a (first (b 'a))]
	     (or
	      (number? a)
	      (char? a)))))])
      (fn [b]
	(let [a (first (b 'a))]
	  [(make-literal-matcher a)]))))

(def predicate
     (make-return
      (make-rule-matcher  'list-match)
      (fn [b]
	(let [p (first (b '-match-))]
	  [(make-pred-matcher (build-let-fn p))]))))

(def parenthesis
     (make-return
      (make-rule-matcher  'vec-match)
      (fn [b]
	(let [v (first (b '-match-))
	      r (alternatives v b)]
	  (:r r)))))

(def vec-match
     (make-sequence-matcher
      [(make-bind (make-rule-matcher 'anything) 'v)
       (make-pred-matcher
	(fn [b]
	  (let [v (first (b 'v))]
	    (vector? v))))]))

(def list-match
     (make-sequence-matcher
      [(make-bind (make-rule-matcher 'anything) 'l)
       (make-pred-matcher
	(fn [b]
	  (let [l (first (b 'l))]
	    (list? l))))]))

(def rule-match
     (make-return
      (make-sequence-matcher
       [(make-not (make-rule-matcher  'keyword-match))
	(make-rule-matcher  'symbol-match)])
      (fn [b]
	(let [s (first (b '-match-))]
	  [(make-rule-matcher  s)]))))

(def string-match
     (make-return
      (make-rule-matcher  'string)
      (fn [b]
	(let [s (first (b '-match-))]
	  [(make-string-matcher s)]))))

(def symbol-match
     (make-sequence-matcher
      [(make-bind (make-rule-matcher  'anything) 's)
       (make-pred-matcher (fn [b]
			    (let [s (first (b 's))]
			      (symbol? s))))]))

(def string
     (make-sequence-matcher
      [(make-bind (make-rule-matcher  'anything) 's)
       (make-pred-matcher (fn [b]
			    (let [s (first (b 's))]
			      (string? s))))]))

(def variable
     (make-sequence-matcher
      [(make-not (make-rule-matcher  'keyword-match))
       (make-rule-matcher  'symbol)]))

(def keyword-match
     (make-alt-matcher
      [(make-rule-matcher 'literal=)
       (make-rule-matcher 'literal->)
       (make-rule-matcher 'literal?)
       (make-rule-matcher 'literal*)
       (make-rule-matcher 'literal+)
       (make-rule-matcher 'literal=>)
       (make-rule-matcher 'literal--)
       (make-rule-matcher 'literal-slash)]))

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
      `(def ~(:rule-name r) ~(:rule-body r)))))
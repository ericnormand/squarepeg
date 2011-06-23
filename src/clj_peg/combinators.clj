(ns clj-peg.combinators)

;; A rule is a function [input bindings] -> {:i seq :b {symbol vec} :r vec}
;;                                       OR {:fail str} to signal failure
;; input is a seq of the rest of the input
;; bindings is a map from symbol to vecs of output
;; :fail is a failure message for the user

;; define success and failure
(defn fail [msg] {:fail msg})
(defn failure? [x] (:fail x))

(defn success? [x] (not (failure? x)))
(defn succeed [return sreturn input bindings]
  {:i input :b bindings :r return :s sreturn})

(defn mkfn [f]
  (fn
    ([input]
       (let [r (f input {})]
         (if (success? r)
           (:r r)
           (throw (RuntimeException. (:fail r))))))
    ([input bindings]
       (f input bindings))))

;; not makes failure success and vice versa and never consumes input
(defn mknot [rule]
  (fn not-fn [input bindings]
    (let [r (rule input bindings)]
      (if (failure? r)
        (succeed nil [] input bindings)
        (fail (str "NOT failed"))))))

;; make a rule bind its return to a var
(defn mkbind [rule var]
  (fn [input bindings]
    (let [r (rule input bindings)]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) (assoc (:b r) var (:r r)))
        r))))

(defn mkpr [pr]
  (fn [input bindings]
    (if (nil? (seq input))
      (fail "End of input")
      (let [i (first input)]
        (if (pr i)
          (succeed i [i] (rest input) bindings)
          (fail (str i " does not match predicate.")))))))

;; changes the current return value
;; ret is a fn that takes the map of bindings
(defn mkret [rule ret]
  (fn [input bindings]
    (let [r (rule input bindings)]
      (if (success? r)
        (let [b (assoc (:b r) :match (:r r))
              v (ret b)]
          (succeed v [v] (:i r) b))
        r))))

;; helper function to concatenate vecs
(defn vec-cat [a b]
  (reduce conj a b))

(defn mkcat [rule1 rule2]
  (fn [input bindings]
    (let [r1 (rule1 input bindings)]
      (if (failure? r1)
        r1
        (let [r2 (rule2 (:i r1) (:b r1))]
          (if (failure? r2)
            r2
            (let [val (vec-cat (:s r1) (:s r2))]
              (succeed val val (:i r2) (:b r2)))))))))

;; A sequence matcher matches all rules in order against the input and returns a vec of the outputs
(defn mkseq [rules]
  (reduce mkcat #(succeed nil [] %1 %2) rules))

(defn mkeither [rule1 rule2]
  (fn [input bindings]
    (let [r1 (rule1 input bindings)]
      (if (failure? r1)
        (rule2 input bindings)
        r1))))

;; An alternative matcher tries to match one rule, in the order given, to the input or fails if none match
(defn mkalt [rules]
  (cond
   (nil? (seq rules))
   (constantly (fail "no rules to match"))

   (nil? (next rules))
   (first rules)

   :otherwise
   (reduce mkeither rules)))

;; A predicate matcher allows us to fail conditionally based on the return value of a predicate.
;; The predicate fn f is a function of the bindings active when it is called.
;; The predicate matcher consumes no input.
(defn mkpred [f]
  (fn [input bindings]
    (if (f bindings)
      (succeed true [] input bindings)
      (fail "Failed to match predicate"))))

;; zero or more matcher matches a rule as many times as possible
(defn mkzom [rule]
  (fn [input bindings]
    (loop [val [] input input bindings bindings]
      (let [r (rule input bindings)]
        (if (success? r)
          (recur (vec-cat val (:s r)) (:i r) (:b r))
          (succeed val val input bindings))))))

(def always (mkpred (constantly true)))
(def never  (mkpred (constantly false)))

(def anything (mkpr (constantly true)))

;; optional matcher
(defn mkopt [rule]
  (mkalt [rule always]))

;; one or more
(defn mk1om [rule]
  (mkseq [rule (mkzom rule)]))

;; literal matcher
(defn mklit [l]
  (mkpr #(= l %)))

;; whitespace
(def whitespace (mkpr #{\newline \space \tab}))

;; string matcher
(defn mkstr [s]
  (mkseq (doall (map mklit s))))

(def end (mknot anything))

(defn mksub [rule]
  (fn [input bindings]
    (if (seq input)
      (let [r (rule (first input) bindings)]
        (if (success? r)
          (succeed (:s r) [(:s r)] (rest input) (:b r))
          r)))))

(def match-string (mkpr string?))
(def match-number (mkpr number?))
(def match-integer (mkpr integer?))
(def match-float (mkpr float?))
(def match-symbol (mkpr symbol?))
(def match-keyword (mkpr keyword?))
(def match-char (mkpr char?))
(def match-vec (mkpr vector?))
(def match-list (mkpr list?))
(def match-hash (mkpr map?))

(defn mkrule [rule]
  (fn [input bindings]
    (let [r (rule input {})]
      (if (success? r)
        (succeed (:r r) (:s r) (:i r) bindings)
        r))))

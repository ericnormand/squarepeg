#clj-peg

##Introduction

clj-peg is a library for defining PEGs. PEG stands for Parsing
Expression Grammar.

The library defines a set of parser combinator creating rules.

Parsers created with clj-peg are data-structure agnostic. They can
operate on any seq and on any data type in the seq. The parsers can be
used to define DSLs using Clojure data structures. clj-peg can also be
used to define traditional text parsers.

I originally intended to write a DSL for defining clj-peg
parsers. This turned out to be more difficult than I at first
imagined. With release 0.1.0, the code attempting to do that has been
deleted. You can find it in the git history, if you wish to revert.

http://github.com/ericnormand/clj-peg

You may want to jump right in to the examples in
src/clj_peg/examples.clj

##How it works

clj-peg is defined in terms of combinators. Each combinator is a
function which generates an atomic unit of a parser (called a
_rule_). By combining these parts (with combinators! ;-), you can
generate complex parsers that can handle a superset of Context Free
Grammars.

First, we'll go over some basic concepts.

###Rules

A rule is a function of two arguments, input and bindings.

input is a seq of inputs (a text string, a vector, a lazy list), and
bindings is a map of current bindings.

A rule should return (success . . .) or (fail msg) with a failure
message.

*NOTE*: In an upcoming future release, rules will probably take more
 arguments. <code>mkfn</code> should still work as advertised and is a
 convenient way for writing future-compatible code.

###Success

Success is defined as a map of four values, :i, :b, :r, and :s

:i is the rest of the input after the rule has consumed all it wants.

:b is the bindings available to the rest of the rules. Typically, a
rule will assoc new bindings onto the bindings parameter.

*NOTE*: Returning {} or nil will clear all bindings from subsequent
 rule calls.

:r is the return value of the rule.

:s is the sequence-context return value, which means it's always a
vec.

The function <code>success?</code> determines if an rule was
successful.

####A little more detail on return values

Most rules return a single value, let's call it a. This means you set
:r to a and :s to [a].

If you want to return a sequence of values [a b c], set :r to [a b c]
and :s to [a b c].

If you want to return _no value_, you should set :r to nil and :s to
[].

Finally, there are times when you want to return a seq of values, but
have the parser consider it to be a single value, you would set :r to
[a b c] and :s to [[a b c]].

:s is primarily used by the mkseq combinator to collect return values
of its subrules.

*NOTE*: This may or may not change in a future version. The current
 functionality is somewhat confusing and does not always yield
 expected results.

###Failure

Failure is a map with a single value, :fail, which is mapped to the
failure message.

Use <code>failure?</code> to determine if a rule failed.

*NOTE*: The failure object will change in future versions. It will
 have more than just the failure message. <code>failure?</code> will most
 likely remain unchanged.

###Combinators

Combinators are functions that generate rules.

####Built-in combinators

Combinators are defined in src/clj_peg/combinators.clj

<code>mknot</code> inverts a rule. Given a rule, mknot returns a new
rule that fails when the given rule succeeds, and vice versa. It never
returns a value and never consumes input. It is most useful for making
a rule that says "and is not followed by . . .".

Example:

    (def not-followed-by-whitespace (mknot whitespace))
    (not-followed-by-whitespace "abc" {}) 
        => {:r nil :s [] :i (\a \b \c) :b {})
    (not-followed-by-whitespace " abc" {}) => {:fail "NOT failed"}

<code>mkbind</code> creates a rule that will bind the return value of
its first argument (a rule) to its second argument (typically a
keyword). Binding is useful if you want to refer to a rule later.

Example:

    ;; bind the matched digits to :digits
    (def digits (mkbind 
        (mk1om (mkpr #(Character/isDigit %))) :digits))
    (digits "123" {}) 
        => {:r [\1 \2 \3] :s [\1 \2 \3] :i nil :b {:digits [\1 \2 \3]}}

<code>mkpr</code> creates a rule that consumes one item from the
input. It then calls the given predicate on it. If the predicate
returns nil, the rule fails. Otherwise, the rule passes. The return
value is the item consumed. (It does not consume input if the
predicate fails.)

Example:

    ;; match only even numbers
    (def even (mkpr evenp))

<code>mkret</code> changes the current return value. It takes a
function which takes the bindings map.

Example:

    ;; parse digit characters as an int
    (def integer (mkret (mkbind (mk1om (mkpr #(Character/isDigit %))) 
                                :digits) #(Integer/parseInt 
                                             (:digits %))))

<code>mknothing</code> makes a rule return nothing.

Example:

    ;; ignore whitespace
    (def ignorewhitespace (mknothing (mk1om #(Character/isSpace %))))

<code>mkcat</code> takes two rules and returns a rule that will
require the first rule to match and then required the second rule to
match.

Example:

    ;; match an even number followed by an odd number
    (def evenodd (mkcat (mkpr even?) (mkpr odd?)))

<code>mkseq</code> takes a seq of rules and creates a rule that must
match all of them in sequence.

Example:

    ;; match three even numbers then two odds
    (def even3odd2 (mkseq [even even even odd odd]))

<code>mkeither</code> takes two rules and returns a new rule wherein
if the first rules fails, the second one is tried.

Example:

    ;; match either 'a' or 'b' (in that order)
    (def aorb (mkeither (mklit \a) (mklit \b)))

<code>mkalt</code> takes a seq of rules and tries each one in
order. The first rule that matches is returned.

Example:

    ;; match 'a' or 'b' or 'c' (in that order)
    (def aorborc (mkalt [(mklit \a) (mklit \b) (mklit \c)]))

<code>mkpred</code> takes a predicate and returns a rule that succeeds
when the predicate applied to the first input element succeeds. It
never returns a value and never consumes input. The predicate operates
on the bindings map.

Example:

    ;; match two numbers if their sum > 100
    (def sum>100 (mkseq [(mkbind integer :a) (mkbind integer :b)
                         (mkpred #(> (+ (:a %) (:b %)) 100))]))

<code>mkzom</code> creates a rule which matches zero or more times. It
will match the most possible times. It never fails.

Example:

    ;; match zero or more whitespace
    (def w* (mkzom (mkpr #(Character/isSpace %))))

<code>mkscope</code> creates a scope protector around a rule so that
bindings that the given rule creates do not leak into the current
scope. This function should be used around your own rules.

Example:

    ;; a rule that binds but does not protect
    (def as (mkbind (mk1om (mklit \a)) :as))
    ;; a rule that calls as
    (def xab (mkseq [(mkbind (mk1om (mklit \x)) :as) ;; bind to :as
                     (mkscope as)                    ;; make sure as
                                                     ;; does not bind
                     (mk1om (mklit \b))]))

<code>mksub</code> creates a rule that matches a nested seq within the
seq.

Example:

    ;; match a seq of ones
    (def ones (mk0om (mklit 1)))
    ;; match a seq of ones followed by 2s
    (def anesthetics (mkseq [(mksub ones) (mk0om (mklit 2))]))
    (onesthentwos [[1 1 1] 2 2] {}) => SUCCESS
    (onesthentwos [1 1 1 2 2] {}) => FAILURE

<code>mk1om</code> creates a rule that matches the given rule at least
once. Returns all matches.

Example:

    ;; match one or more digits
    (def digits (mk1om digit))
    (digits "1234" {}) => {:r [\1 \2 \3 \4] :s [\1 \2 \3\ 4] :i nil :b {}}
    (digits "123 4" {}) => {:r [\1 \2 \3] :s [\1 \2 \3] :i (\4) :b {}}
             
<code>mkopt</code> creates a rule that always succeeds. If the rule it
is given matches, it returns its value. Otherwise, it succeeds with no
return.

Example:
    ;; optionally match 'xyz'
    (def xyz? (mkopt (mkstr "xyz")))

<code>mklit</code> creates a rule that matches a value if it is equal.

Example:
    ;; match the number 12
    (def twelve (mklit 12))
    (twelve [12] {}) => {:r 12 :s [12] :i nil :b {}}

<code>mkstr</code> create a sequential rule that matches all of the
characters of a string.

Example:
    ;; match the string "hello" followed by whitespace
    (def hellow+ (mkseq [(mkstr "hello") (mk1om whitespace)]))

###Predefined rules

Here are the predefined rules that may come in handy.

<code>always</code> a utility rule (not a combinator) which always
matches.

<code>never</code> a utility rule (not a combinator) which never
matches.

<code>anything</code> a utitily rule that consumes one item of input
and returns it.

<code>whitespace</code> matches a single character that is a space.

<code>digit</code> matches a single character that is a digit.

###Utility

<code>mkfn</code> creates a function that is a little bit more
friendly to call yourself but is still valid as a rule.

When called with a seq of inputs, it calls the given rule with empty
bindings and throws a RuntimeException if the parse fails and the :r
value if it succeeds.

When called with input and bindings, it acts as a normal rule.

##Other documents

See RELEASENOTES.md for a history of the project.

See FUTUREPLANS.md to know where the project is headed.

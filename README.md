# squarepeg

## Credits and Acknowledgements

This software was written by Eric Normand and is released under the
[Eclipse Public License]. You can [find it on github][github].

[github]: http://github.com/ericnormand/squarepeg

[Eclipse Public License]: http://opensource.org/licenses/eclipse-1.0.php

Special thanks also goes to [Christophe Grand][cgrand] for his help.

[cgrand]: http://clj-me.cgrand.net/

## Quickstart

### To use in your project.

Add <code>[squarepeg "0.6.1"]</code> to your project's dependencies then
run.

    lein deps

In the relevant code, add the following to you <code>ns</code>
declaration:

    (:use squarepeg.core)

### To hack the code

    git clone git@github.com:ericnormand/squarepeg.git
    cd squarepeg
    lein deps
    lein test

## Introduction

squarepeg is a library for defining PEGs. PEG stands for Parsing
Expression Grammar.

The library defines a set of parser combinators for creating grammar
rules.

Parsers created with squarepeg are data-structure agnostic. They can of
course input Java Strings. But they can also input any seq. This means
you can apply a grammar to a sequence of Integers, for example.

You may want to jump right in to the examples in
src/squarepeg/examples.clj

There is also a small example of how to use squarepeg to compile a
class file for use with Java.

NOTE: squarepeg used to be called clj-peg! But then I realized the
name was taken. Now I have a name without a hyphen.

## How it works

squarepeg is defined in terms of combinators. Each combinator is a
function which generates an atomic unit of a parser (called a
_rule_). By combining these parts (with combinators! ;-), you can
generate complex parsers that can handle a superset of Context Free
Grammars.

Note that the combinators are not monadic! I tried monadic combinators
but they didn't seem like a good fit for Clojure, which does not have
built-in support for Monads, like Haskell does. If you want to find a
monadic combinator library, there are plenty around.

First, we'll go over some basic concepts.

### Rules

A rule is a function of four arguments, input, bindings, context and
memo.

input is a seq of inputs (a text string, a vector, a lazy list), and
bindings is a map of current bindings.

Context is a map of bindings that are not mutable. Think of it as a
global scope for the duration of the parse.

Memo is a map used for memoization.

A rule should return (success . . .) or (fail msg) with a failure
message.

### Success

Success is defined as a map of five keys, :i, :b, :r, :s, and :m.

:i is the rest of the input after the rule has consumed all it wants.

:b is the bindings available to the rest of the rules. Typically, a
rule will assoc new bindings onto the bindings parameter.

*NOTE*: Returning {} or nil will clear all bindings from subsequent
 rule calls.

:r is the return value of the rule.

:s is the sequence-context return value, which means it's always a
vec.

:m is the memoization map. Parser rules that wish to memoize should
assoc their results to this map. Please see <code>mkmemo</code> for an
example.

The function <code>success?</code> determines if a rule was
successful.

#### A little more detail on return values

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

The rule is simple: if the :r element is equal to the :s element, we
are dealing with a seq of values, not a single value.

### Failure

Failure is a map with two values, :fail and :m, which are mapped to
the failure message and the memo object, respectively.

Use <code>failure?</code> to determine if a rule failed.

### Combinators

Combinators are functions that generate rules.

#### Built-in combinators

Combinators are defined in src/squarepeg/core.clj

<code>mknot</code> inverts a rule. Given a rule, mknot returns a new
rule that fails when the given rule succeeds, and vice versa. It never
returns a value and never consumes input. It is most useful for making
a rule that says "and is not followed by . . .".

Example:

    (def not-followed-by-whitespace (mknot whitespace))
    (not-followed-by-whitespace "abc" {} {} {}) 
        => {:r nil :s [] :i (\a \b \c) :b {} :m {})
    (not-followed-by-whitespace " abc" {}) => {:fail "NOT failed"}

<code>mkbind</code> creates a rule that will bind the return value of
its first argument (a rule) to its second argument (typically a
keyword). Binding is useful if you want to refer to a rule later.

Example:

    ;; bind the matched digits to :digits
    (def digits (mkbind 
        (mk1om (mkpr #(Character/isDigit %))) :digits))
    (digits "123" {} {} {}) 
        => {:r [\1 \2 \3] :s [\1 \2 \3] :i nil :b {:digits [\1 \2 \3]}
            :m {}}

<code>mkpr</code> creates a rule that consumes one item from the
input. It then calls the given predicate on it. If the predicate
returns nil, the rule fails. Otherwise, the rule passes. The return
value is the item consumed. (It does not consume input if the
predicate fails.)

Example:

    ;; match only even numbers
    (def even (mkpr even?))

<code>mkret</code> changes the current return value. It takes a
function which takes the bindings map and a context.

Example:

    ;; parse digit characters as an int
    (def integer (mkret (mkbind (mk1om (mkpr #(Character/isDigit %))) 
                                :digits) 
                        (fn [b c] (Integer/parseInt (:digits b)))))

<code>mknothing</code> makes a rule return nothing.

Example:

    ;; ignore whitespace
    (def ignorewhitespace (mknothing (mk1om #(Character/isSpace %))))

<code>mkseq</code> takes any number of rules and creates a rule that must
match all of them in sequence.

Example:

    ;; match three even numbers then two odds
    (def even3odd2 (mkseq even even even odd odd))

<code>mkalt</code> takes any number of rules and tries each one in
order. The first rule that matches is returned.

Example:

    ;; match 'a' or 'b' or 'c' (in that order)
    (def aorborc (mkalt (mklit \a) (mklit \b) (mklit \c)))

<code>mkpred</code> takes a predicate and returns a rule that succeeds
when the predicate applied to the first input element succeeds. It
never returns a value and never consumes input. The predicate operates
on the bindings map.

Example:

    ;; match two numbers if their sum > 100
    (def sum>100 (mkseq (mkbind integer :a) (mkbind integer :b)
                        (mkpred #(> (+ (:a %) (:b %)) 100))))

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
    (def xab (mkseq (mkbind (mk1om (mklit \x)) :as) ;; bind to :as
                    (mkscope as)                    ;; make sure as
                                                    ;; does not bind
                    (mk1om (mklit \b))))

<code>mksub</code> creates a rule that matches a nested seq within the
seq.

Example:

    ;; match a seq of ones
    (def ones (mkzom (mklit 1)))
    ;; match a seq of ones followed by 2s
    (def onesthentwos (mkseq (mksub ones) (mkzom (mklit 2))))
    (onesthentwos [[1 1 1] 2 2] {}) => SUCCESS
    (onesthentwos [1 1 1 2 2] {}) => FAILURE

<code>mk1om</code> creates a rule that matches the given rule at least
once. Returns all matches.

Example:

    ;; match one or more digits
    (def digits (mk1om digit))
    (digits "1234" {} {} {}) 
        => {:r [\1 \2 \3 \4] :s [\1 \2 \3\ 4] :i nil :b {} :m {}}
    (digits "123 4" {} {} {}) 
        => {:r [\1 \2 \3] :s [\1 \2 \3] :i (\4) :b {} :m {}}
             
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
    (twelve [12] {} {} {}) => {:r 12 :s [12] :i nil :b {} :m {}}

<code>mkstr</code> create a sequential rule that matches all of the
characters of a string.

Example:

    ;; match the string "hello" followed by whitespace
    (def hellow+ (mkseq [(mkstr "hello") (mk1om whitespace)]))

<code>mkmemo</code> creates a new rule which memoizes the given
rule. The best way to use this is directly inside of a
<code>mkscope</code> when defining a top-level rule for most efficient
results. Memoizing is done to trade space efficiency for time
efficiency. Effectively using mkmemo will make a parse use linear
space and linear time with respect to input size.

<code>mkmatch</code> create a new rule which returns the matched
portion of the input. It binds that portion of the input matched by
the given rule to :match. It also coerces Strings if possible.

Example:

    ;; match a "SELECT" statement in a contrived query language.
    ;; perform a "lookup" of everything after the SELECT followed
    ;; by whitespace
    (def selectstmt (mkscope (mkmemo (mkret (mkseq [(mkstr "SELECT") w+
       (mkmatch (mk1om anything))]) (fn [b c] (lookup (:match b))))))

### Predefined rules

Here are the predefined rules that may come in handy.

<code>always</code> a utility rule (not a combinator) which always
matches.

<code>never</code> a utility rule (not a combinator) which never
matches.

<code>anything</code> a utitily rule that consumes one item of input
and returns it.

<code>whitespace</code> matches a single character that is a space.

<code>digit</code> matches a single character that is a digit.

### Utility

<code>mkfn</code> creates a function that is a little bit more
friendly to call yourself but is still valid as a rule.

When called with a seq of inputs, it calls the given rule with empty
bindings and throws a RuntimeException if the parse fails and the :r
value if it succeeds.

<code>mkfn</code> functions can also be called with input + context.

When called with input, bindings, context, and memo, it acts as a
normal rule.

## Other documents

See RELEASENOTES.md for a history of the project.

See FUTUREPLANS.md to know where the project is headed.

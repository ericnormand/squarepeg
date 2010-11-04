#clj-peg

##Introduction

clj-peg is a library for defining PEGs. PEG stands for Parsing
Expression Grammar.

The library defines a set of parser combinators and a dsl for creating
rules.

Parsers created with clj-peg are data-structure agnostic. They can
operate on any seq and on any data type in the seq. The parsers can be
used to define DSLs using Clojure data structures. clj-peg can also be
used to define traditional text parsers.

##How it works

###Rules

A rule is a function of two arguments, input and bindings.

input is a seq of inputs, and bindings is a map of current bindings.

A rule should return (success . . .) or (fail msg) with a failure message.

###Success

Success is defined as a map of four values, :i, :b, :r, and :s

:i is the rest of the input after the rule has consumed all it wants.

:b is the bindings available to the rest of the rules. Typically, a
rule will assoc new bindings onto the bindings parameter.

:r is the return value of the rule.

:s is the sequence-context return value, which means it's always a
vec.

###Failure

Failure is a map with a single value, :fail, which is mapped to the
failure message.

###Combinators

Combinators are functions that generate rules.

####Built-in combinators

The code is pretty readable. Read src/clj_peg/peg.clj to see all of
the combinators defined. I'll go over some of the basic ones.

mknot inverts a rule. mknot returns a rule that fails when the given
rule succeeds, and vice versa.

mkcat takes two rules and returns a rule that will match the second
after the first.

mkpred takes a predicate and returns a rule that succeeds when the
predicate applied to the first input element succeeds.

Like I said, the code is readable. I've worked hard on cleaning it up.

##DSL

I've written a DSL based on the fine work of Christophe Grand. It uses
Clojure's built-in data structures, some helper functions, and no
macros.

###Interpretation

To compile a DSL, use the **parser** function. Pass it something and
it will make a rule for you.

If you pass it a number, it will create a rule that matches that number.

  (def one (parser 1))

If you pass it a String, it will create a rule that matches that string.

  (def abc (parser "abc"))

There's some tricks, though.

If you pass it a vec, it will make a sequence matcher.

  (def one-then-two-then-three (parser [1 2 3]))

If you pass it a vec with only one vec in it (and nothing else) it
makes an alternative matcher.

  (def one-or-two-or-three (parser [[1 2 3]]))

Order is important in a PEG, so define it well!

If you pass it a map of a rule and a :keyword, you create a binding:

  (def bind-a (parser {anything :a}))

If you pass it a map of a rule and a fn, you make a return. You => is a
helper that binds :keywords to parameters:

  (def ret-vec (parser {match-integer (=> inc :match))))

:match is always bound to the return value of the rule in the map.

If you pass it a map of a rule and anything else, it returns that anything.

  (def always-0 (parser {anything 0}))

But you don't need to run parser all the time. You can nest these things.

  (def nested [ret-vec bind-a one-or-two-or-three])

  (def top [nested])

Look at the examples to see more. The code is readable.

Let me know if you have any questions.
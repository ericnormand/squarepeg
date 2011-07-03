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

I originally intended to write a DSL for defining clj-peg
parsers. This turned out to be more difficult than I at first
imagined. With release 0.1.0, the code attempting to do that has been
deleted. You can find it in the git history, if you wish to revert.

http://github.com/ericnormand/clj-peg

##How it works

clj-peg is defined in terms of combinators. Each combinator is a
function which generates an atomic unit of a parser. By combining
these parts (with combinators! ;-), you can generate complex parsers
that can handle a superset of Context Free Grammars.

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

*NOTE*: This may or may not go away in a future version. I have not
 found it useful to keep a seq of the return values.

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

<code>mkbind</code> creates a rule that will bind the return value of
its first argument (a rule) to its second argument (typically a
keyword). Binding is useful if you want to refer to a rule later.

Example:

    ;; bind the matched digits to :digits
    (def digits (mkbind (mk1om (mkpr #(Character/isDigit %))) :digits))

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

###Utility

<code>mkfn</code> creates a function that is a little bit more
friendly to call yourself but is still valid as a rule.

When called with a seq of inputs, it calls the given rule with empty
bindings and throws a RuntimeException if the parse fails and the :r
value if it succeeds.

When called with input and bindings, it acts as a normal rule.

## History

### Prior to 0.1.0
No history was tracked. There was much confusion.

### 0.1.0

This version is mainly a documentation release. Documentation,
examples, etc, were poor and I want it to be easier to use.

Also, as noted before, I attempted to create an internal DSL that
would use Clojure's data structures to easily create readable grammars
by avoiding the combinators. This turned out to be difficult. Way over
my head. I now don't think it's possible to provide all of the
functionality of a useful PEG grammar in terms of vecs, hashes, and
Strings. If you have ideas on this, let me know. Otherwise, see future
planning for 0.3.0 for an idea.

Some things that I hope to add in this release:

* docstrings for all functions.
* metadata to make the defined rules look more like regular defn'd functions
* examples to learn from
* making utility functions not exported

## Future History (planning)

### 0.2.0

This release will add memoizing, fix an issue with bindings, and add
support for immutable data passed in to a rule as context that is not
suitable for the mutable bindings argument.

Memoizing trades space efficiency for computational
efficiency. Typically, PEG parsers use memoizing to provide infinite
lookahead in O(n) time. Currently, clj-peg does not memoize the
results of rules. For long inputs, it could perform very poorly
time-wise. Memoizing should fix that.

There is an issue with bindings in which a subrule can overwrite an
existing binding. This behavior violates the existing mental model of
a rule as an atomic unit that can be reused and composed freely. The
current workaround is to always uniquely name your bindings. This is
an unacceptable solution.

The solution I propose is to create a <code>mkscope</code> combinator
which will protect the bindings of the rule it is passed from
modifying other rules. It should be used whenever a top-level rule is
defined (for instance, a rule bound to a var) or whenever you would
like to use a rule from code which you do not control and do not
trust. For instance, if you would like to import a rule defined in
another module, you could first wrap it in an <code>mkscope</code>,
which would protect it from modifying your bindings.

The third issue, which is somewhat related, is the idea of passing in
an immutable context to a rule when it is called. Bindings are not
suitable for this purpose since they are mutable.

Scoping and context make the rules generated with the combinators safe
to share with others. I expect memoizing will make it more efficient.

The argument signature of rules will have to change. This will be the
last breaking change before 1.0.0. Any existing combinators will have
to be reworked. This is why I want to do all of these changes at once;
it will finalize the calling signature of a rule.

### 0.3.0

In this release I will remove the idea of notion of a sequence return
in favor of a single return value, :r. I have not found :s to be
valuable.

However, I will add a combinator that should increase the usefulness
of the parser. It will bind the matched input seq to :match, which
will then be available to subsequent rules.

This will require the scoping combinator <code>mkscope</code> to work
safely.

### 0.4.0

This release will create a test suite for the parser. Success and
failure cases will test every possible exit point of the individual
parser combinators. There is a small number of them and each is
simple, so this should not be difficult.

### 0.5.0

This release will be the last foreseeable release before 1.0.0. It
will mainly be work to make parsers simpler to define and share, as
well as enabling use by other languages on the JVM.

Although it is not entirely planned at the moment, here are some
thoughts that may make it into the final release:

* An external DSL to define rules. The rules would be made available
  in the current namespace (via <code> (def . . .) </code>). The DSL
  could be defined in a String or in an external file.
* A macro for generating a parser class suitable for use by Java.
* Macros to compile the parser at compile time instead of at runtime.
* A parser-generator class suitable for use by Java.

### A series of bugfixes, implementations of feature requests, and optimizations

### 1.0.0

The PEG parser in all its glory.

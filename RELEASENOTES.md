# History

### 0.5.0

This release will mainly be work to make parsers simpler to define and
share.

Mainly, this involved reorganizing the namespace, paring down the
combinators, and creating a macro for helping to define new rules.

The namespace is now flatter. You <code>(use squarepeg)</code> instead
of <code>(use 'squarepeg.combinators)</code> like it used to be. There
is still <code>squarepeg.examples</code> but you should rarely have to
import that.

The binary sequence and alternation combinators (<code>mkcat</code>
and <code>mkeither</code>) have been removed. <code>mkseq</code> now
has definitions for different numbers of parameters. It also no longer
takes a seq of rules. Instead, it takes a variable number of rules as
arguments. What was removed was truly unnecessary. In the spirit of
Clojure's minimalism with indenting and nesting, I removed a level of
nesting.

I've made a dsl in the form of a macro. I know everyone is all about
internal dsls using the built-in data structures. But I tried that
many times in different ways in vain. There are just too many
combinators to be able to create something that can be recombined
without complex semantics.

But don't worry! The <code>defrule</code> macro has some very simple
semantics. It started out simply as a wrapper for <code>def</code> and
evolved organically from that. For example, it's a good idea to always
wrap your rules in a <code>mkscope</code> and a <code>mkmemo</code>,
so <code>defrule</code> automatically does that. Then I added
<code>mkseq</code> automatically when you use a vec. It grew from
there.

The dsl is now recursively complete, meaning you can nest things
arbitrarily. And it still works for the more classical combinator
usage.

One last thing: I've started using [Marginalia] to generate
documentation. I'm a little disappointed by how it looks, but I've got
a release in the FUTUREPLANS.md dedicated to sprucing it up.

marginalia: https://github.com/fogus/marginalia

I've also managed to keep up the test-driven development, so every
line of code in the library (apart from examples) are tested.

### 0.4.0

This release will create/complete a test suite for the parser. Success
and failure cases will test every possible exit point of the
individual parser combinators. There is a small number of them and
each is simple, so this should not be difficult.

In addition, practical combinations of the combinators will be tested
to ensure that the parser works as one would intuit.

### 0.3.0

I have added a combinator that should increase the usefulness of the
parser. It will bind the matched input seq to :match, which will then
be available to subsequent rules.

This requires the scoping combinator <code>mkscope</code> to work
safely.

I've also added logic to maintain the String type for the match when
the input is a String. Getting a vec of chars is not the expected
behavior.

### 0.2.0

This release will add memoizing and add support for immutable data
passed in to a rule as context that is not suitable for the mutable
bindings argument.

Memoizing trades space efficiency for computational
efficiency. Typically, PEG parsers use memoizing to provide infinite
lookahead in O(n) time. Currently, squarepeg does not memoize the
results of rules. For long inputs, it could perform very poorly
time-wise. Memoizing should fix that.

The second issue, which is somewhat related, is the idea of passing in
an immutable context to a rule when it is called. Bindings are not
suitable for this purpose since they are mutable. A context will allow
the preservation of types. For instance, currently when you pass a
String as an input to a parser, it converts it to a seq of
characters. You have to convert it back manually. Passing in the type
as part of the context will eliminate this awkwardness.

Scoping (with <code>mkscope</code>) and context make the rules
generated with the combinators safe to share with others. I expect
memoizing will make it more efficient.

The argument signature of rules will have to change. This will be the
last breaking change before 1.0.0. Any existing combinators will have
to be reworked. This is why I want to do all of these changes at once;
it will finalize the calling signature of a rule.

## 0.1.0

This version is mainly a documentation release. Documentation,
examples, etc, were poor and I want it to be easier to use.

Also, as noted before, I attempted to create an internal DSL that
would use Clojure's data structures to easily create readable grammars
by avoiding the combinators. This turned out to be difficult. Way over
my head. I now don't think it's possible to provide all of the
functionality of a useful PEG grammar in terms of vecs, hashes, and
Strings. If you have ideas on this, let me know. Otherwise, see future
planning for 0.3.0 for an idea.

Some things in this release:

* docstrings for all functions.
* examples to learn from (src/squarepeg/examples.clj)
* making utility functions not exported

## Prior to 0.1.0
No history was tracked. There was much confusion.

# History

### 0.2.0

This release will add memoizing and add support for immutable data
passed in to a rule as context that is not suitable for the mutable
bindings argument.

Memoizing trades space efficiency for computational
efficiency. Typically, PEG parsers use memoizing to provide infinite
lookahead in O(n) time. Currently, clj-peg does not memoize the
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
* examples to learn from (src/clj_peg/examples.clj)
* making utility functions not exported

## Prior to 0.1.0
No history was tracked. There was much confusion.

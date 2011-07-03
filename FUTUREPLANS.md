## Future History (planning)

### 0.2.0

This release will add memoizing and add support for immutable data
passed in to a rule as context that is not suitable for the mutable
bindings argument.

Memoizing trades space efficiency for computational
efficiency. Typically, PEG parsers use memoizing to provide infinite
lookahead in O(n) time. Currently, clj-peg does not memoize the
results of rules. For long inputs, it could perform very poorly
time-wise. Memoizing should fix that.

The third issue, which is somewhat related, is the idea of passing in
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

### 0.3.0

I will add a combinator that should increase the usefulness of the
parser. It will bind the matched input seq to :match, which will then
be available to subsequent rules.

This will require the scoping combinator <code>mkscope</code> to work
safely.

### 0.4.0

This release will create/complete a test suite for the parser. Success
and failure cases will test every possible exit point of the
individual parser combinators. There is a small number of them and
each is simple, so this should not be difficult.

In addition, practical combinations of the combinators will be tested
to ensure that the parser works as one would intuit.

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
* A mechanism for augmenting the failure message.

### A series of bugfixes, implementations of feature requests, and optimizations

### 1.0.0

The PEG parser in all its glory.

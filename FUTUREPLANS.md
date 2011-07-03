## Future History (planning)

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

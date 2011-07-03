# History

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

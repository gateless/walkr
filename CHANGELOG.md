This is a history of changes to gateless/walkr

# 0.2.0
* Added support for walking multiple collections in tandem: `walk-reduce`, `postwalk-reduce`, and `prewalk-reduce` now accept optional trailing secondary collections that are walked alongside the primary form (paired by key for maps/records, by value/membership for sets, by index for vectors/lists/seqs) and passed as extra read-only arguments to the walk function.

# 0.1.0
* The initial release.

(ns walkr.core
  (:import [clojure.lang IMapEntry MapEntry IRecord]))

(defn- pad-secondary
  "Returns sec as an infinite sequence for zipping against the primary: a
  sequential sec is followed by endless nils past its own length; a
  non-sequential (or nil) sec resolves to nil at every position."
  [sec]
  (if (sequential? sec)
    (concat sec (repeat nil))
    (repeat nil)))

(defn- positional-children
  "Builds [primary secondary...] child tuples for a positionally-ordered
  form (list, seq, vector), pairing form's items with each secondary in
  more-forms by position. Walks form and every secondary together in
  lock-step via map (each secondary padded to be infinite), rather than
  seeking to each index, so a list/seq secondary costs O(n) overall
  instead of O(n^2)."
  [form more-forms]
  (apply map
         (fn [item & secs] (into [item] secs))
         form
         (for [sec more-forms] (pad-secondary sec))))

(defn- map-entry-for
  "Resolves sec's value for key k as a MapEntry, defaulting to nil when sec
  isn't associative or doesn't contain k."
  [k sec]
  (MapEntry/create k
                   (when (and (associative? sec)
                              (contains? sec k))
                     (get sec k))))

(defn- map-children
  "Builds [primary secondary...] child tuples for a map-like form (maps and
  records), pairing form's entries with each secondary by key rather than
  position."
  [form more-forms]
  (for [[k v] form]
    (into [(MapEntry/create k v)]
          (for [sec more-forms]
            (map-entry-for k sec)))))

(defn- set-children
  "Builds [primary secondary...] child tuples for a set form, pairing each
  element with its match in each secondary set by value rather than by
  position, since set iteration order isn't semantically meaningful and
  can't be relied on to align two independently-built sets. Each
  secondary's set-ness is resolved once here rather than once per element."
  [form more-forms]
  (let [secs (for [sec more-forms]
               (when (set? sec)
                 sec))]
    (for [e form]
      (into [e]
            (for [sec secs]
              (when (and sec (contains? sec e))
                e))))))

(defn- reduce-form
  [build-fn inner outer acc form children more-forms]
  (let [[acc vec-form] (reduce (fn do-walk
                                 [[acc-in built] child]
                                 (let [[acc-out item-out] (apply inner acc-in child)]
                                   [acc-out (conj built item-out)]))
                               [acc []]
                               children)]
    (apply outer acc (with-meta (build-fn vec-form) (meta form)) more-forms)))

(defn walk-reduce
  "Traverses form, an arbitrary data structure, while accumulating a result.
  Inner and outer are functions of (acc, item, & secondary-items) returning
  [acc item]. Recognizes all Clojure data structures. Consumes seqs as with
  doall.

  Accepts optional trailing secondary collections (more-forms). form alone
  drives traversal shape/order; each collection in more-forms is walked in
  tandem, following the same path form's traversal takes, and its
  corresponding item is passed as an extra trailing argument to inner/outer.
  Only form's items are transformed and rebuilt; items from more-forms are
  read-only context.

  Pairing: maps/records pair secondaries by key (missing/non-associative ->
  nil); sets pair secondaries by value/membership rather than position,
  since set iteration order is not semantically meaningful (missing/
  non-set -> nil); vectors/lists/seqs pair by index (out-of-range/
  non-sequential -> nil). With no secondary collections, behaves exactly
  as before."
  [inner outer acc form & more-forms]
  (cond
    (list? form)
    (reduce-form (partial apply list) inner outer acc form
                 (positional-children form more-forms) more-forms)

    (instance? IMapEntry form)
    (let [key-seq (for [sec more-forms]
                    (when (instance? IMapEntry sec)
                      (key sec)))
          val-seq (for [sec more-forms]
                    (when (instance? IMapEntry sec)
                      (val sec)))
          [acc key-out] (apply inner acc (key form) key-seq)
          [acc val-out] (apply inner acc (val form) val-seq)]
      (apply outer acc (MapEntry/create key-out val-out) more-forms))

    (seq? form)
    (let [form (doall form)]
      (reduce-form seq inner outer acc form
                   (positional-children form more-forms) more-forms))

    (or (instance? IRecord form) (map? form))
    (reduce-form (if (instance? IRecord form)
                   (partial into form)
                   (partial into (empty form)))
                 inner outer acc form
                 (map-children form more-forms) more-forms)

    (set? form)
    (reduce-form (partial into (empty form)) inner outer acc form
                 (set-children form more-forms) more-forms)

    (coll? form)
    (reduce-form (partial into (empty form)) inner outer acc form
                 (positional-children form more-forms) more-forms)

    :else
    (apply outer acc form more-forms)))

(defn postwalk-reduce
  "Performs a depth-first, post-order traversal of form, calling f with acc
  on each sub-form; f returns [new-acc new-form]. Recognizes all Clojure
  data structures. Consumes seqs as with doall.

  Accepts optional trailing secondary collections (more-forms), walked in
  tandem with form (see walk-reduce for pairing rules). f is called as
  (apply f acc item secondary-item...) and still returns [new-acc new-item],
  transforming only the primary item. With no secondary collections,
  behaves exactly as before."
  [f acc form & more-forms]
  (apply walk-reduce (partial postwalk-reduce f) f acc form more-forms))

(defn prewalk-reduce
  "Like postwalk-reduce, but does pre-order traversal.

  Accepts optional trailing secondary collections (more-forms), walked in
  tandem with form (see walk-reduce for pairing rules). With no secondary
  collections, behaves exactly as before."
  [f acc form & more-forms]
  (let [[acc form] (apply f acc form more-forms)]
    (apply walk-reduce (partial prewalk-reduce f)
           (fn [acc item & _secondary-items] [acc item])
           acc form more-forms)))

(ns walkr.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [walkr.core :as w]))

(deftest simple-prewalk-and-postwalk-test
  (let [input [{:foo {:bar {:value #{"cOlD"}}
                      :other "hOt"}}
               {:foo {:bar {:value "ColDer"}
                      :other "hoTteR"}}]
        handler (fn [acc item]
                  (if (string? item)
                    [(conj acc (str/lower-case item)) (str/upper-case item)]
                    [acc item]))]
    (is (= [#{"cold" "hot" "colder" "hotter"}
            [{:foo {:bar {:value #{"COLD"}}
                    :other "HOT"}}
             {:foo {:bar {:value "COLDER"}
                    :other "HOTTER"}}]]
           (w/prewalk-reduce handler #{} input)
           (w/postwalk-reduce handler #{} input)))))

(deftest test-list-traversal
  (testing "Basic list traversal"
    (is (= [[1 2 3 4] '(2 3 4 5)]
           (w/prewalk-reduce
            (fn [acc item]
              (if (number? item)
                [(conj acc item) (inc item)]
                [acc item]))
            []
            '(1 2 3 4))))))

(deftest test-map-traversal
  (testing "Map traversal with key and value modification"
    (is (= [[:a :b :c] {:A 2 :B 4 :C 6}]
           (w/prewalk-reduce
            (fn [acc item]
              (cond
                (keyword? item) [(conj acc item) (-> item name str/upper-case keyword)]
                (number? item) [acc (* 2 item)]
                :else [acc item]))
            []
            {:a 1 :b 2 :c 3})))))

(deftest test-nested-structures
  (testing "Complex nested structure traversal"
    (let [input {:a [1 2 {:b 3}]
                 :c #{4 5}
                 :d '(6 7)}]
      (is (= [[1 2 3 4 5 6 7]
              {:A [2 3 {:B 4}]
               :C #{5 6}
               :D '(7 8)}]
             (w/prewalk-reduce
              (fn [acc item]
                (cond
                  (number? item) [(conj acc item) (inc item)]
                  (keyword? item) [acc (-> item name str/upper-case keyword)]
                  :else [acc item]))
              []
              input))))))

(deftest test-metadata-preservation
  (testing "Metadata preservation during traversal"
    (let [input (with-meta [1 2 3] {:test true})]
      (is (= [[1 2 3] [2 3 4]]
             (w/prewalk-reduce
              (fn [acc item]
                (if (number? item)
                  [(conj acc item) (inc item)]
                  [acc item]))
              []
              input)))
      (is (= {:test true}
             (meta (second (w/prewalk-reduce
                            (fn [acc item]
                              (if (number? item)
                                [(conj acc item) (inc item)]
                                [acc item]))
                            []
                            input))))))))

(deftest test-empty-structures
  (testing "Handling empty structures"
    (is (= [[] []] (w/prewalk-reduce (fn [acc x] [acc x]) [] [])))
    (is (= [[] {}] (w/prewalk-reduce (fn [acc x] [acc x]) [] {})))
    (is (= [[] '()] (w/prewalk-reduce (fn [acc x] [acc x]) [] '())))
    (is (= [[] #{}] (w/prewalk-reduce (fn [acc x] [acc x]) [] #{})))))

(deftest test-prewalk-order
  (is (= [[[1 2 {:a 3} (list 4 [5])]
           1 2 {:a 3} [:a 3] :a 3 (list 4 [5])
           4 [5] 5]
          [1 2 {:a 3} (list 4 [5])]]
         (w/prewalk-reduce
          (fn [acc form]
            [(conj acc form) form])
          []
          [1 2 {:a 3} (list 4 [5])]))))

(deftest test-postwalk-order
  (is (= [[1 2
           :a 3 [:a 3] {:a 3}
           4 5 [5] (list 4 [5])
           [1 2 {:a 3} (list 4 [5])]]
          [1 2 {:a 3} (list 4 [5])]]
         (w/postwalk-reduce
          (fn [acc form]
            [(conj acc form) form])
          []
          [1 2 {:a 3} (list 4 [5])]))))

(deftest test-tandem-map-matching-keys
  (testing "Two-map tandem walk pairs values by key, not iteration order"
    (is (= #{[1 100] [2 200]}
           (set (first (w/postwalk-reduce
                        (fn [acc item & secs]
                          (if (number? item)
                            [(conj acc (into [item] secs)) item]
                            [acc item]))
                        []
                        {:a 1 :b 2}
                        {:b 200 :a 100}))))))
  (testing "keys are still visited by f and paired trivially with themselves"
    (is (= #{[:a :a] [:b :b]}
           (set (first (w/postwalk-reduce
                        (fn [acc item & secs]
                          (if (keyword? item)
                            [(conj acc (into [item] secs)) item]
                            [acc item]))
                        []
                        {:a 1 :b 2}
                        {:a 10 :b 20})))))))

(deftest test-tandem-map-missing-and-extra-keys
  (testing "Secondary map missing a key yields nil; extra secondary keys are ignored"
    (is (= #{[1 10] [2 nil]}
           (set (first (w/postwalk-reduce
                        (fn [acc item & secs]
                          (if (number? item)
                            [(conj acc (into [item] secs)) item]
                            [acc item]))
                        []
                        {:a 1 :b 2}
                        {:a 10 :c 99})))))))

(deftest test-tandem-vector-mismatched-lengths
  (testing "Secondary vector shorter than primary yields nil for missing indexes"
    (is (= [[1 10] [2 20] [3 nil]]
           (first (w/postwalk-reduce
                   (fn [acc item & secs]
                     (if (number? item)
                       [(conj acc (into [item] secs)) item]
                       [acc item]))
                   []
                   [1 2 3]
                   [10 20]))))))

(deftest test-tandem-three-collections
  (testing "N-ary tandem walk with three collections (not hardcoded binary)"
    (is (= [[1 10 100] [2 20 200]]
           (first (w/postwalk-reduce
                   (fn [acc item & secs]
                     (if (number? item)
                       [(conj acc (into [item] secs)) item]
                       [acc item]))
                   []
                   [1 2]
                   [10 20]
                   [100 200]))))))

(deftest test-tandem-nil-secondary
  (testing "A wholly nil secondary collection resolves to nil at every position"
    (is (= [[1 nil] [2 nil]]
           (first (w/postwalk-reduce
                   (fn [acc item & secs]
                     (if (number? item)
                       [(conj acc (into [item] secs)) item]
                       [acc item]))
                   []
                   [1 2]
                   nil))))))

(deftest test-tandem-set-membership
  (testing "Sets pair by value/membership, not by (arbitrary) iteration order"
    (is (= #{[1 1] [2 nil] [3 3]}
           (set (first (w/postwalk-reduce
                        (fn [acc item & secs]
                          (if (number? item)
                            [(conj acc (into [item] secs)) item]
                            [acc item]))
                        []
                        #{1 2 3}
                        #{3 1 99})))))))

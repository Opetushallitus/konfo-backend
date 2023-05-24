(ns konfo-backend.elastic-tools-test
  (:require [clojure.test :refer :all]
            [konfo-backend.elastic-tools :as tools]))

(defn- create-search-fn [times]
  (fn[index mapper & query-parts]
    (swap! times inc)
    {:hits {:total {:value 99999}
            :hits [{:sort [@times (- @times 1)]}]}}))

(defn- mapper [input] input)

(deftest elastic-tools-test
  (testing "calls search twice"
    (let [times (atom 0)
          search-fn (create-search-fn times)]
      (tools/do-search-after search-fn nil mapper 10001 200 [])
      (is (= @times 2))))

  (testing "calls search ten times"
    (let [times (atom 0)
          search-fn (create-search-fn times)
          result (tools/do-search-after search-fn nil mapper 11800 200 [])]
      (is (= result {:hits {:total {:value 99999}
                            :hits [{:sort [10 9]}]}}))
      (is (= @times 10))))

  (testing "short circuits if from is over total"
    (let [times (atom 0)
          search-fn (create-search-fn times)
          result (tools/do-search-after search-fn nil mapper 100000 10 [])]
      (is (= result {:hits []}))
      (is (= @times 1)))))

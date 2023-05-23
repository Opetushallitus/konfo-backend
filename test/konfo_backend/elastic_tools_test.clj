(ns konfo-backend.elastic-tools-test
  (:require [clojure.test :refer :all]
            [konfo-backend.elastic-tools :as tools]))

(defn- create-search-fn [times]
  (fn[index mapper & query-parts]
    (swap! times inc)
    {:hits {:hits [{:sort [@times (- @times 1)]}]}}))

(deftest elastic-tools-test
  (testing "calls search twice"
    (let [times (atom 0)
          search-fn (create-search-fn times)]
      (tools/do-search-after search-fn nil nil 10001 200 [])
      (is (= @times 2))))

  (testing "calls search ten times"
    (let [times (atom 0)
          search-fn (create-search-fn times)
          result (tools/do-search-after search-fn nil nil 11800 200 [])]
      (is (= result {:hits {:hits [{:sort [10 9]}]}}))
      (is (= @times 10)))))

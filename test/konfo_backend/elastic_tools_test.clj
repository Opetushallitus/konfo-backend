(ns konfo-backend.elastic-tools-test
  (:require [clojure.test :refer :all]
            [clj-http.client :as http]
            [konfo-backend.elastic-tools :as tools]))

(defn- create-search-fn [times]
  (fn [index mapper & query-parts]
    (swap! times inc)
    {:hits {:total {:value 99999}
            :hits [{:sort [@times (- @times 1)]}]}}))

(defn- mapper [input] input)

(deftest elastic-tools-test
  (with-redefs [http/post {:id "2343242"}]
    (testing "calls search twice"
      (let [times (atom 0)
            search-fn (create-search-fn times)]
        (tools/do-search-after-paged search-fn nil mapper 10000 200 [])
        (is (= @times 2))))

    (testing "calls search ten times"
      (let [times (atom 0)
            search-fn (create-search-fn times)
            result (tools/do-search-after-paged search-fn nil mapper 11600 200 [])]
        (is (= result {:hits {:total {:value 99999}
                              :hits [{:sort [10 9]}]}}))
        (is (= @times 10))))

    (testing "short circuits if from is over total"
      (let [times (atom 0)
            search-fn (create-search-fn times)
            result (tools/do-search-after-paged search-fn nil mapper 100000 10 [])]
        (is (= result {:hits []}))
        (is (= @times 1))))

    (testing "gathers results not just single page"
      (let [times (atom 0)
            search-fn (create-search-fn times)
            result (tools/do-search-after-concanate-results search-fn nil mapper 11000 200 [])]
        (is (= @times 7))
        (is (= (count result) 6))))))

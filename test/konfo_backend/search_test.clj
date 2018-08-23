(ns konfo-backend.search-test
  (:require [clojure.test :refer :all]
            [konfo-backend.search.search :refer :all]))

(deftest search-test
  (testing "Search tools"
    (testing "constraints all"
      (is (= (constraints :koulutustyyppi "ako,amm" :paikkakunta "tampere" :kieli "fi,sv")
             {:kieli "fi,sv" :paikkakunta "tampere" :koulutustyyppi "ako,amm"})))
    (testing "constraints without kieli"
      (is (= (constraints :koulutustyyppi "ako,amm" :paikkakunta "tampere")
             {:paikkakunta "tampere" :koulutustyyppi "ako,amm"})))))

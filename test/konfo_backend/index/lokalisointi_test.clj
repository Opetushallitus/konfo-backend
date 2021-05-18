(ns konfo-backend.index.lokalisointi-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(deftest lokalisointi-test
  (testing "Get lokalisointi"
    (with-redefs [clj-elasticsearch.elastic-connect/get-document (fn [x y & z] {:found   true,
                                                                                :_source {:lng         y
                                                                                          :tyyppi      "lokalisointi"
                                                                                          :translation {:moi "moi"}}})]
      (let [response (get-ok "/konfo-backend/translation/fi")]
        (is (= response {:moi "moi"}))))))
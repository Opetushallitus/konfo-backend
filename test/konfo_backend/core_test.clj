(ns konfo-backend.core-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [konfo-backend.core :refer :all]
            [ring.mock.request :as mock]
            [clj-test-utils.elasticsearch-mock-utils :as utils]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once utils/mock-embedded-elasticsearch-fixture)

(deftest core-test
  (testing "Healthcheck API test"
    (let [response (app (mock/request :get "/konfo-backend/healthcheck"))]
      (is (= (:status response) 200))))

  (testing "Oppilaitos 404 search test"
    (let [response (app (mock/request :get "/konfo-backend/oppilaitos/123123"))]
      (is (= (:status response) 404)))))

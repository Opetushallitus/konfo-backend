(ns konfo-backend.core-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [konfo-backend.core :refer :all]
            [ring.mock.request :as mock]))

(intern 'clj-log.access-log 'service "konfo-backend")

(deftest healthcheck-test
  (testing "Healthcheck API test"
    (let [response (app (mock/request :get "/konfo-backend/healthcheck"))]
      (is (= 200 (:status response))))))

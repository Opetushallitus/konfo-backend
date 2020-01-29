(ns konfo-backend.contentful.updater-test
  (:require [clojure.test :refer :all]
            [konfo-backend.contentful.updater-api :refer :all]
            [ring.mock.request :as mock])
  (:import (com.contentful.java.cda Cache CDAClient CDAService)
           (org.mockito Mockito)
           (java.util.concurrent Executor)))

(intern 'clj-log.access-log 'service "konfo-backend-updater")

(defonce client (Mockito/mock CDAClient))

(deftest updater-test
  (testing "Healthcheck API test"
    (let [response ((konfo-updater-api client) (mock/request :get "/konfo-backend-updater/healthcheck"))]
      (is (= (:status response) 200))))

  (testing "Unauthorized access"
    (let [response ((konfo-updater-api client) (mock/request :post "/konfo-backend-updater/update"))]
      (is (= (:status response) 401))))

  (testing "Start update"
    (let [response ((konfo-updater-api client)
                    (mock/header
                       (mock/request :post "/konfo-backend-updater/update")
                       "Authorization"
                       "Basic b3BoOm9waA=="))]
      (is (= (:status response) 200)))))

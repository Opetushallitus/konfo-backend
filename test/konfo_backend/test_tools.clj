(ns konfo-backend.test-tools
  (:require
    [clojure.test :refer :all]
    [clj-elasticsearch.elastic-connect :as e]
    [ring.mock.request :as mock]
    [konfo-backend.core :refer :all]
    [konfo-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
    [cheshire.core :as cheshire]))

(defn ->keywordized-response-body
  [response]
  (fixture/->keywordized-json (slurp (:body response))))

(defn get-and-check-status
  [url expected-status]
  (let [response (app (mock/request :get url))]
    (is (= (:status response) expected-status))
    response))

(defn get-ok
  [url]
  (->keywordized-response-body (get-and-check-status url 200)))

(defn get-not-found
  [url]
  (get-and-check-status url 404))

(defn debug-pretty
  [json]
  (println (cheshire/generate-string json {:pretty true})))

(defn refresh-and-wait
  [indexname timeout]
  (e/refresh-index indexname)
  (Thread/sleep timeout))
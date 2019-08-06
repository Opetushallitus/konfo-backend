(ns konfo-backend.test-tools
  (:require
    [clojure.test :refer :all]
    [clj-elasticsearch.elastic-connect :as e]
    [ring.mock.request :as mock]
    [konfo-backend.core :refer :all]
    [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
    [cheshire.core :as cheshire]))

(defn ->keywordized-response-body
  [response]
  (fixture/->keywordized-json (slurp (:body response))))

(defn get-and-check-status
  [url expected-status]
  (let [response (app (mock/request :get url))]
    (is (= (:status response) expected-status))
    response))

(defn post-and-check-status
  [url expected-status]
  (let [response (app (mock/request :post url))]
    (is (= (:status response) expected-status))
    response))

(defn get-ok
  [url]
  (->keywordized-response-body (get-and-check-status url 200)))

(defn get-not-found
  [url]
  (get-and-check-status url 404))

(defn get-bad-request
  [url]
  (get-and-check-status url 400))

(defn debug-pretty
  [json]
  (println (cheshire/generate-string json {:pretty true})))

(defn refresh-and-wait
  [indexname timeout]
  (e/refresh-index indexname)
  (Thread/sleep timeout))

(defn post-ok
  [url]
  (->keywordized-response-body (post-and-check-status url 200)))

(defn query-params->string
  [& {:as query-params}]
  (if (not (empty? query-params))
    (str "?" (clojure.string/join "&" (map #(str (name (key %)) "=" (val %)) query-params)))
    ""))

(defn url-with-query-params
  [url & query-params]
  (str url (apply query-params->string query-params)))
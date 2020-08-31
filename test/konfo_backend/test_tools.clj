(ns konfo-backend.test-tools
  (:require
    [clojure.test :refer :all]
    [clj-elasticsearch.elastic-connect :as e]
    [ring.mock.request :as mock]
    [konfo-backend.core :refer :all]
    [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
    [cheshire.core :as cheshire]
    [clj-time.coerce :as coerce]
    [clj-time.core :as time]))

(defn ->keywordized-response-body
  [response]
  (try
    (fixture/->keywordized-json (slurp (:body response)))
    (catch Exception e
      (println e)
      (:body response))))

(defn get-ok-or-print-schema-error
  [url]
  (let [response (app (mock/request :get url))
        status   (:status response)]
    (if (= 200 status)
      (->keywordized-response-body response)
      (is (= status 200) (cheshire/generate-string (->keywordized-response-body response) {:pretty true})))))

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

(defn now-in-millis
  []
  (coerce/to-long (time/now)))
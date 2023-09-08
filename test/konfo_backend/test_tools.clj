(ns konfo-backend.test-tools
  (:require
   [clojure.test :refer :all]
   [clj-test-utils.generic :refer [run-proc]]
   [clj-elasticsearch.elastic-connect :as e]
   [clj-elasticsearch.elastic-utils :as e-utils]
   [ring.mock.request :as mock]
   [ring.util.codec :as codec]
   [konfo-backend.core :refer :all]
   [cheshire.core :refer [parse-string, generate-string]]
   [clojure.walk :refer [keywordize-keys]]
   [clj-time.coerce :as coerce]
   [clj-time.core :as time]
   [clj-time.format :as format]
   [clojure.string :as string])
  (:import (org.joda.time DateTimeUtils)))

(defn ->keywordized-response-body
  [response]
  (try
    (keywordize-keys (parse-string (slurp (:body response))))
    (catch Exception e
      (println e)
      (:body response))))

(defn get-ok-or-print-schema-error
  [url]
  (let [response (app (mock/request :get url))
        status   (:status response)]
    (if (= 200 status)
      (->keywordized-response-body response)
      (is (= status 200) (generate-string (->keywordized-response-body response) {:pretty true})))))

(defn get-and-check-status
  [url expected-status]
  (let [response (app (mock/request :get url))]
    (is (= expected-status (:status response)))
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

(defn get-internal-error
  [url]
  (get-and-check-status url 500))

(defn refresh-and-wait
  [indexname timeout]
  (e/refresh-index indexname)
  (Thread/sleep timeout))

(defn post-ok
  [url]
  (->keywordized-response-body (post-and-check-status url 200)))

(defn url-with-query-params
  [url & query-params] 
  (str url (if query-params (str "?" (codec/form-encode (apply array-map query-params))) "")))

(defn now-in-millis
  []
  (coerce/to-long (time/now)))

(defn elastic-empty? []
  (let [url (e-utils/elastic-url "_all" "_count")
        count (:count (e-utils/elastic-get url))]
    (= count 0)))

(defn prepare-elastic-test-data [& args]
  (let [e-host (string/replace e-utils/elastic-host #"127\.0\.0\.1|localhost" "host.docker.internal")]
    (println "Importing elasticsearch data...")
    (if (elastic-empty?)
      (run-proc "test/resources/load_elastic_dump.sh" e-host (str (if (:no-data args) "" "data,") "mapping,alias,settings,template"))
      (println "Elasticsearch not empty. Data already imported. Doing nothing."))))

(defn with-elastic-dump
  [test]
  (prepare-elastic-test-data)
  (test))

(defonce formatter (format/with-zone (format/formatter "yyyy-MM-dd'T'HH:mm:ss") (time/time-zone-for-id "Europe/Helsinki")))

; Muunnetaan lokaali timestamp UTC-millisekunneiksi, jotta voidaan väärentää järjestelmän kello olemaan
; UTC-ajassa antamalla lokaali timestamp
(defn local-timestamp-to-utc-millis [timestamp]
  (coerce/to-long (time/to-time-zone (format/parse formatter timestamp) time/utc)))

(defn set-fixed-time [timestamp]
  (DateTimeUtils/setCurrentMillisFixed (local-timestamp-to-utc-millis timestamp)))
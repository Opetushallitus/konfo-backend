(ns konfo-backend.test-tools
  (:require
    [clojure.test :refer :all]
    [clojure.java.shell :refer [sh]]
    [clj-elasticsearch.elastic-connect :as e]
    [clj-elasticsearch.elastic-utils :as e-utils]
    [ring.mock.request :as mock]
    [konfo-backend.core :refer :all]
    [cheshire.core :refer [parse-string, generate-string]]
    [clojure.walk :refer [keywordize-keys]]
    [clj-time.coerce :as coerce]
    [clj-time.core :as time]
    [clojure.string :as string]))

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

(defn debug-pretty
  [json]
  (println (generate-string json {:pretty true})))

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

(defn elastic-empty? []
  (let [url (e-utils/elastic-url "_all" "_count")
        count (:count (e-utils/elastic-get url))]
    (= count 0)))

(defn prepare-elastic-test-data [& args]
  (let [e-host (string/replace e-utils/elastic-host #"127\.0\.0\.1|localhost" "host.docker.internal")]
    (println "Importing elasticsearch data...")
    (if (elastic-empty?)
      (let [p (sh "test/resources/load_elastic_dump.sh" e-host (str (if (:no-data args) "" "data,") "mapping,analyzer,alias,settings,template"))]
        (println (:err p))
        (println (:out p)))
      (println "Elasticsearch not empty. Data already imported. Doing nothing."))))

(defn with-elastic-dump
  [test]
  (prepare-elastic-test-data)
  (test))

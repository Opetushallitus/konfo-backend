(ns konfo-backend.test-tools
  (:require
    [clj-elasticsearch.elastic-connect :as e])
  (:import (pl.allegro.tech.embeddedelasticsearch EmbeddedElastic PopularProperties)
           (java.net ServerSocket)))

(def embedded-elastic (atom nil))

(defn start-embedded-elasticsearch [port]
  (reset! embedded-elastic (-> (EmbeddedElastic/builder)
                               (.withElasticVersion "6.0.0")
                               (.withSetting PopularProperties/HTTP_PORT port)
                               (.withSetting PopularProperties/CLUSTER_NAME "my_cluster")
                               (.build)))
  (.start @embedded-elastic))

(defn stop-elastic-test []
  (.stop @embedded-elastic))

(defn random-open-port []
  (try
    (with-open [socket (ServerSocket. 0)]
      (.getLocalPort socket))
    (catch Exception e (random-open-port))))

(defn init-test-logging []
  (intern 'clj-log.error-log 'test true)
  (intern 'clj-log.error-log 'verbose false))

(defn init-elastic-test []
  (let [port (random-open-port)]
    (init-test-logging)
    (intern 'clj-elasticsearch.elastic-utils 'elastic-host (str "http://localhost:" port))
    (start-embedded-elasticsearch port)))

(defn refresh-and-wait
  [indexname timeout]
  (e/refresh-index indexname)
  (Thread/sleep timeout))
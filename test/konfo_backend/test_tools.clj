(ns konfo-backend.test-tools
  (:import (pl.allegro.tech.embeddedelasticsearch EmbeddedElastic PopularProperties)))

(def embedded-elastic (atom nil))

(defn start-embedded-elasticsearch []
  (reset! embedded-elastic (-> (EmbeddedElastic/builder)
                               (.withElasticVersion "6.0.0")
                               (.withSetting PopularProperties/TRANSPORT_TCP_PORT 6666)
                               (.withSetting PopularProperties/HTTP_PORT 9900)
                               (.withSetting PopularProperties/CLUSTER_NAME "my_cluster")
                               (.build)))
  (.start @embedded-elastic))

(defn stop-elastic-test []
  (.stop @embedded-elastic))


(defn init-test-logging []
  (intern 'clj-log.error-log 'test true)
  (intern 'clj-log.error-log 'verbose false))

(defn init-elastic-test []
  (init-test-logging)
  (intern 'clj-elasticsearch.elastic-utils 'elastic-host "http://localhost:9900")
  (start-embedded-elasticsearch))

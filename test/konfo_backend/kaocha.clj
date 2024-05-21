(ns konfo-backend.kaocha
  (:require clj-elasticsearch.elastic-utils
            [clj-test-utils.elasticsearch-docker-utils :refer [start-elasticsearch stop-elasticsearch]]
            kaocha.runner))

(defn run [& args]
  (try
    (start-elasticsearch)
    (apply kaocha.runner/-main args)
    (finally
      (stop-elasticsearch))))

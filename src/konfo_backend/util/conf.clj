(ns konfo-backend.util.conf
  (:require [cprop.core :refer [load-config]]
            [cprop.source :as source]
            [mount.core :refer [defstate start]]))

(defonce env (load-config :merge [(source/from-system-props) (source/from-env)]))

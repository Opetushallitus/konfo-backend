(ns konfo-backend.config
  (:require
    [cprop.core :refer [load-config]]
    [cprop.source :as source]))

(defonce config (load-config :resource "konfo-backend.edn" :merge [(source/from-system-props) (source/from-env)]))
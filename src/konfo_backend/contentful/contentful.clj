(ns konfo-backend.contentful.contentful
  (:require
   [konfo-backend.config :refer [config]]
   [konfo-backend.contentful.json :refer [gson]])
  (:import (com.contentful.java.cda CDAContentType CDAClient CDAEntry CDAAsset)))

(defonce include-level 2)

(defn create-contentful-client [preview?]
  (let [space-id     (:contentful-space-id config)
        access-token (if preview?
                       (:contentful-preview-token config)
                       (:contentful-access-token config))]
    (-> (CDAClient/builder)
        (.setSpace space-id)
        (.setToken access-token)
        (.setEndpoint (when preview? "https://preview.contentful.com"))
        (.build))))

(defn get-entries-raw [client content-type locale]
  (let [query (doto
               (.fetch
                 client
                 CDAEntry)
                (.include include-level)
                (.withContentType content-type)
                (.where "locale" locale))
        ]
    (-> query
        (.all)
        (.items))))

(defn get-content-types [client]
  (let [query  (doto
                (.fetch
                  client
                  CDAContentType))
        result (-> query
                   (.all))]
    result))

(defn get-assets-raw [client locale]
  (let [query  (doto
                (.fetch
                  client
                  CDAAsset)

                 (.where "locale" locale))
        result (-> query
                   (.all)
                   (.items))]
    result))

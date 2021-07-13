(ns konfo-backend.contentful.contentful
  (:require
   [konfo-backend.config :refer [config]]
   [konfo-backend.contentful.json :refer [gson]])
  (:import (com.contentful.java.cda CDAContentType CDAClient CDAEntry CDAAsset)))

(defonce include-level 3)

(defn create-contentful-clients
      [preview?]
      (let [access-token (if preview?
                             (:contentful-preview-token config)
                             (:contentful-access-token config))
            access-token-en (if preview?
                             (:contentful-preview-token-en config)
                             (:contentful-access-token-en config))]
           {:fisv (-> (CDAClient/builder)
                      (.setSpace (:contentful-space-id config))
                      (.setEnvironment (:contentful-environment-id config))
                      (.setToken access-token)
                      (.setEndpoint (when preview? "https://preview.contentful.com"))
                      (.build))
            :en   (-> (CDAClient/builder)
                      (.setSpace (:contentful-space-id-en config))
                      (.setEnvironment (:contentful-environment-id-en config))
                      (.setToken access-token-en)
                      (.setEndpoint (when preview? "https://preview.contentful.com"))
                      (.build))}))

(defn get-entries-raw [client content-type locale]
  (let [query (doto
               (.fetch client CDAEntry)
                (.include include-level)
                (.withContentType content-type)
                (.where "locale" locale))]
    (-> query
        (.all)
        (.items))))

(defn get-content-types [client]
  (-> (.fetch client CDAContentType)
      (.all)))

(defn get-assets-raw [client locale]
  (let [query (doto
               (.fetch client CDAAsset)
                (.where "locale" locale))]
    (-> query
        (.all)
        (.items))))

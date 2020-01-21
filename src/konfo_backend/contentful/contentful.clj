(ns konfo-backend.contentful.contentful
  (:require
    [konfo-backend.config :refer [config]]
    [konfo-backend.contentful.json :refer [gson]]
    [clojure.reflect :as cr]
    [clojure.pprint :as pp])
  (:import (com.contentful.java.cda CDAContentType CDAArray CDAClient FetchQuery AbsQuery CDAEntry CDAAsset CDAResource CDASpace)
           (com.google.gson Gson GsonBuilder JsonElement JsonObject JsonArray JsonSerializationContext JsonSerializer)
           (com.contentful.java.cda TransformQuery)
           (java.util List)))

(defonce space-id (:contentful-space-id config))
(defonce access-token (:contentful-access-token config))
(defonce preview-token (:contentful-preview-token config))
(defonce include-level 2)

(defonce client
  (-> (CDAClient/builder)
      (.setSpace space-id)
      (.setToken access-token)
      (.build)))

(defonce preview-client
  (-> (CDAClient/builder)
      (.setSpace space-id)
      (.setToken preview-token)
      (.setEndpoint "https://preview.contentful.com")
      (.build)))

(defn get-entries-raw [content-type locale preview?]
  (let [query  (doto
                (.fetch
                  (if preview?
                    preview-client
                    client)
                  CDAEntry)
                (.include include-level)
                (.withContentType content-type)
                (.where "locale" locale))
        ]
    (-> query
               (.all)
               (.items))))

(defn get-entries [content-type locale preview?]
    (.toJson gson (get-entries-raw content-type locale preview?)))

(defn get-content-types []
  (let [query  (doto
                (.fetch
                  client
                  CDAContentType))
        result (-> query
                   (.all))]
    result))

(defn get-entry [content-type id locale preview?]
  (let [query  (doto
                (.fetch
                  (if preview?
                    preview-client
                    client)
                  CDAEntry)
                (.include include-level)
                (.withContentType content-type)
                (.where "locale" locale))
        result (-> query
                   (.one id))]
    (.toJson gson result)))

(defn get-assets-raw [locale preview?]
  (let [query  (doto
                (.fetch
                  (if preview?
                    preview-client
                    client)
                  CDAAsset)

                 (.where "locale" locale))
        result (-> query
                   (.all)
                   (.items))]
    result))

(defn get-assets [locale preview?]
  (.toJson gson (get-assets-raw locale preview?)))


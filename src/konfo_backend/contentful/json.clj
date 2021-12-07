(ns konfo-backend.contentful.json
  (:require
   [konfo-backend.config :refer [config]]
   [clj-time.core :as t]
   [clojure.tools.logging :as log]
   [clj-time.format :as f])
  (:import (com.contentful.java.cda CDAEntry CDAAsset)
           (com.google.gson Gson GsonBuilder JsonObject JsonArray JsonSerializer)
           (java.util List)))

(defn write-reference [object entry]

  (doto object
    (.addProperty "id" (str (.getAttribute entry "id")))
    (#(when-let [name (.getField entry "name")]
        (.addProperty % "name" (str name))))
    (.addProperty "type"
      (str
        (some-> entry
                (.getAttribute "contentType")
                (.get "sys")
                (.get "id"))))))


(defn new-formatter [fmt-str]
  (f/formatter fmt-str (t/time-zone-for-id "Europe/Helsinki")))

(def finnish-format (new-formatter "d.M.yyyy 'klo' HH:mm"))
(def swedish-format (new-formatter "d.M.yyyy 'kl.' HH:mm"))
(def english-format (new-formatter "MMM. d, yyyy 'at' hh:mm a z"))

(defn- parse-date-time
  [s]
  (when s
    (let [tz (t/time-zone-for-id "Europe/Helsinki")
          fmt (f/formatter "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")]
      (try
        (t/to-time-zone (f/parse fmt s) tz)
        (catch Exception e
          (log/error (str "Unable to parse" s) e))))))

(defn write-timestamps [object entry]
  (let [created-at (str (.getAttribute entry "createdAt"))
        updated-at (str (.getAttribute entry "updatedAt"))
        formatoi   (fn [parsed]
                     (doto (JsonObject.)
                       (.addProperty "fi" (str (f/unparse finnish-format parsed)))
                       (.addProperty "sv" (str (f/unparse swedish-format parsed)))
                       (.addProperty "en" (str (f/unparse english-format parsed)))))]
  (doto object
    (.addProperty "created" created-at)
    (.addProperty "updated" updated-at)
    (.add "formatoituCreated" (some-> (parse-date-time created-at)
                                      (formatoi)))
    (.add "formatoituUpdated" (some-> (parse-date-time updated-at)
                                      (formatoi))))))

(defn write-array [object field entry]
  (let [array (doto (JsonArray.)
                (#(doseq [item (.getField entry field)]
                    (let [o (JsonObject.)]
                      (write-reference o item)
                      (.add % o)))))]
    (doto object
      (.add field array))))

(defn write-asset [object asset url-converter]
  (let [[new-url original-url _ _] (url-converter asset)]
    (doto object
      (.addProperty "id" (str (.getAttribute asset "id")))
      (.addProperty "url" (str new-url))
      (.addProperty "description" (str (.getField asset "description")))
      (.addProperty "original" (str original-url))
      (.addProperty "type" "asset")
      (.addProperty "name" (str (.title asset))))))

(defn write-field [object field entry markdown-converter url-converter]
  (let [value (.getField entry field)]
    (condp instance? value
           CDAEntry (.add object field
                      (-> (JsonObject.)
                          (write-reference value)))
           List (if (instance? CDAEntry (first value))
                  (write-array object field entry)
                  (.addProperty object field (str (.toJson (Gson.) value))))
           CDAAsset (.add object field
                      (-> (JsonObject.)
                          (write-asset value url-converter)))
           (.addProperty object field (markdown-converter entry field (str value))))))

(defn write-fields [object entry markdown-converter url-converter]
  (doseq [field (.keySet (.rawFields entry))]
    (write-field object field entry markdown-converter url-converter))
  object)

(defn create-entry-adapter [markdown-converter url-converter]
  (reify
    JsonSerializer
    (serialize [_ entry type context]
      (-> (JsonObject.)
          (write-reference entry)
          (write-timestamps entry)
          (write-fields entry markdown-converter url-converter)))))

(defn create-asset-adapter [url-converter]
  (reify
    JsonSerializer
    (serialize [_ entry type context]
      (-> (JsonObject.)
          (write-timestamps entry)
          (write-asset entry url-converter)))))

(defonce EntryAdapter
  (create-entry-adapter (fn [_ _ value] value) identity))

(defonce AssetAdapter
  (create-asset-adapter (fn [cda-asset] [(.url cda-asset) (.url cda-asset) nil])))

(defn create-gson [markdown-converter url-converter]
  (->
   (doto (GsonBuilder.)
     (.registerTypeAdapter CDAEntry (create-entry-adapter markdown-converter url-converter))
     (.registerTypeAdapter CDAAsset (create-asset-adapter url-converter)))
   (.create)))

(defonce gson
  (->
   (doto (GsonBuilder.)
     (.registerTypeAdapter CDAEntry EntryAdapter)
     (.registerTypeAdapter CDAAsset AssetAdapter))
   (.create)))

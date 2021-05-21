(ns konfo-backend.contentful.contentful-updater
  (:require
   [clojure.string :as str]
   [konfo-backend.config :refer [config]]
   [konfo-backend.contentful.contentful :as contentful]
   [konfo-backend.contentful.s3 :as s3]
   [ring.adapter.jetty :refer [run-jetty]]
   [konfo-backend.tools :refer [comma-separated-string->vec]]
   [konfo-backend.contentful.json :refer [create-gson]]
   [clj-log.access-log :refer [with-access-logging]]
   [compojure.api.sweet :refer :all]
   [clj-http.client :as client]
   [ring.middleware.cors :refer [wrap-cors]]
   [cheshire.core :as cheshire]
   [lambdaisland.uri :refer [uri join]]
   [clojure.java.io :as io]
   [ring.util.http-response :refer :all]
   [environ.core :refer [env]]
   [clojure.tools.logging :as log])
  (:import (com.contentful.java.cda.image ImageOption$Format ImageOption)
           (com.contentful.java.cda CDAAsset)
           (javax.imageio ImageIO)
           (java.awt.image BufferedImage)
           (java.io ByteArrayOutputStream)))

(defonce max-width 1280)
(defonce max-height 1080)
(defonce ttl-manifest 0)
(defonce ttl-asset 1)

(defn image-link? [[_ _ link]]
  (and (str/starts-with? link "//")
       (not (str/ends-with? link ".svg"))))

(defn create-fake-image []
  (let [img    (BufferedImage. 256 256 BufferedImage/TYPE_INT_RGB)
        buffer (ByteArrayOutputStream.)]
    (ImageIO/write img "JPEG" buffer)
    (.toByteArray buffer)))

(defn fetch->image [image-url allow-fail?]
  (let [url (str "https:" image-url)]
    (try
      (let [image (client/get url {:as :byte-array})
            headers (:headers image)]
        [(:body image) (get headers "Content-Type")])
      (catch Exception e
        (log/error (str "Error while fetching image " url ": " (.getMessage e)))
        (if allow-fail?
          [(create-fake-image) "image/jpeg"]
          (throw e))))))

(defn add-query-params-to-uri [uri params]
  (reduce-kv (fn [u k v]
                 (if (str/includes? u "?")
                   (str u "&" (name k) "=" v)
                   (str u "?" (name k) "=" v)))
    uri
    params))

(defn resize-image? [image]
  (let [image-input (ImageIO/read (io/input-stream image))
        width       (.getWidth image-input)
        height      (.getHeight image-input)
        scale?      (or (> width max-width)
                        (> height max-height))]
    (when scale?
          (let [w-scale          [max-width (Math/floor (* height (/ max-width width)))]
                h-scale          [(Math/floor (* width (/ max-height height))) max-height]
                w-scale-smaller? (< (* (first w-scale) (second w-scale))
                                   (* (first h-scale) (second h-scale)))]
            (if w-scale-smaller?
              {:w max-width}
              {:h max-height})))))

(defn fetch-and-transform [url allow-fail?]
  (if (or (str/ends-with? url ".svg") (str/ends-with? url "watch"))
    [false url (fetch->image url allow-fail?)]
    (let [transformed-url (add-query-params-to-uri url {:fm "jpg"})
          [image mime] (fetch->image transformed-url allow-fail?)
          params    (resize-image? image)]
      (if params
        (let [new-transformed-url (add-query-params-to-uri transformed-url params)]
          [true new-transformed-url (fetch->image new-transformed-url allow-fail?)])
        [true transformed-url [image mime]]))))

(defn transform-url-if-needed [^CDAAsset cda-asset]
  (let [mime-type (.mimeType cda-asset)]
    (when-let [transformed-image (when (and (.startsWith mime-type "image")
                                            (not= mime-type "image/svg+xml"))
                                       (.urlForImageWith
                                         cda-asset
                                         (into-array ImageOption
                                           [(ImageOption/formatOf ImageOption$Format/jpg)])))]
      (let [[image _] (fetch->image transformed-image false)

            image-input      (ImageIO/read (io/input-stream image))
            width            (.getWidth image-input)
            height           (.getHeight image-input)
            scale?           (or (> width max-width)
                                 (> height max-height))
            w-scale          [max-width (Math/floor (* height (/ max-width width)))]
            h-scale          [(Math/floor (* width (/ max-height height))) max-height]
            w-scale-smaller? (< (* (first w-scale) (second w-scale))
                               (* (first h-scale) (second h-scale)))
            final-transform  (.urlForImageWith
                               cda-asset
                               (into-array ImageOption
                                 (concat
                                  (if scale?
                                    [(if w-scale-smaller?
                                       (ImageOption/widthOf max-width)
                                       (ImageOption/heightOf max-height))]
                                    [])
                                  [(ImageOption/formatOf ImageOption$Format/jpg)])))
            md5              (s3/calculate-md5 (or (and (= transformed-image final-transform)
                                                        image)
                                                   (first (fetch->image final-transform false))))]
        (log/info (str "processing image " final-transform " scaling = " scale? ", md5 = " md5))
        [final-transform md5]))))

(defn get-content-type-with-field-types [client]
  (->> (.items (contentful/get-content-types client))
       (map (fn [t]
                [(.id t) (into {} (for [f (.fields t)]
                                    {(.id f) (.type f)}))]))
       (into {})))

(defn markdown->links [markdown]
  (let [matcher (re-matcher #"\[(?<text>[^\]]*)\]\((?<link>[^\)]*)\)" markdown)]
    (loop [match (re-find matcher)
           result []]
      (if-not match
        result
        (recur (re-find matcher)
          (conj result match))))))

(defn strip-scheme-and-host [url]
  (subs (str (assoc (uri url)
                    :scheme nil
                    :host nil)) 1))

(defn store->s3 [s3-client ttl content-type key obj]
  (log/info (str "Putting " key " with ttl " ttl " (hours)"))
  (when s3-client
        (s3/put-object ttl s3-client key obj content-type)
        (log/info (str "Wrote " key " to bucket " (-> config :s3 :bucket-name)))))

(defn create-gson-serializer [s3-client url-cache content-types]
  (let [markdown-conv (fn [id markdown]
                          (if-let [links (seq (filter image-link? (markdown->links markdown)))]
                            (reduce (fn [t [_ _ original]]
                                        (if-let [l (get @url-cache original)]
                                          (do
                                            (.replaceAll t original (first l)))
                                          (do
                                            (log/warn (str "Creating fake asset for entry " id))
                                            (let [[transformed? transformed-url [image content-type]] (fetch-and-transform original true)
                                                  md5    (s3/calculate-md5 image)
                                                  s3-url (strip-scheme-and-host (if transformed?
                                                                                  (str original ".jpg")
                                                                                  original))]
                                              (when (not= (s3/get-object-md5 s3-client s3-url) md5)
                                                    (log/info (str "Stored S3 " s3-url))
                                                    (store->s3 s3-client ttl-asset content-type s3-url image))
                                              (swap! url-cache assoc original [s3-url original transformed-url md5])
                                              (.replaceAll t original s3-url))
                                            )
                                          )
                                        )
                                    markdown
                                    links)
                            markdown))]
    (create-gson
      (fn [entry field value]
          (let [field-type (get-in content-types [(-> entry
                                                      .contentType
                                                      .id) field])]
            (or (and (= field-type "Text")
                     (markdown-conv (.id entry) value))
                value)))
      (fn [cda-asset]
          (let [original (.url cda-asset)]
            (or (get @url-cache original)
                (let [[transformed-url md5] (transform-url-if-needed cda-asset)
                      new-url (strip-scheme-and-host (if transformed-url
                                                       (str original ".jpg")
                                                       original))]
                  (swap! url-cache assoc original [new-url original transformed-url md5])
                  [new-url original transformed-url md5])))))))

(defn asset-store-handler [client s3-client gson url-cache _ locale base-url existing-url]
  (let [existing     (s3/get-object s3-client existing-url)
        latest-raw   (contentful/get-assets-raw client locale)
        latest       (.toJson gson latest-raw)
        latest-obj (some-> latest
                           (cheshire/parse-string))
        key          (str base-url "asset" ".json")]
    (do
      (doseq [asset latest-obj]
        (let [s3-url   (get asset "url")
              original (get asset "original")
              [_ _ transformed md5] (get @url-cache original)
              old-md5  (s3/get-object-md5 s3-client s3-url)]
          (when (or (nil? md5) (not= old-md5 md5))
            (log/info (str "MD5 " old-md5 (if (= old-md5 md5) " == " " != ") md5))
            (let [[image content-type] (fetch->image (or transformed original) false)]
              (store->s3 s3-client ttl-asset content-type s3-url image)))))
      (comment (= existing latest)
        (if (nil? latest)
          (log/error (str "No asset found for locale " locale " in space " (:contentful-space-id config) " with token " (:contentful-access-token config)))
          (log/warn (str "Existing asset in bucket " (-> config :s3 :bucket-name) " in url " existing-url " is identical to new asset from space " (:contentful-space-id config))))
        )
      (store->s3 s3-client ttl-asset "application/json; charset=utf-8" key (.getBytes latest))
      key)))

(defn content-store-handler [client s3-client gson _ content-type locale base-url existing-url]
  (let [existing   (s3/get-object s3-client existing-url)
        latest-raw (contentful/get-entries-raw client content-type locale)
        latest     (.toJson gson latest-raw)
        key        (str base-url content-type ".json")]
    (if (not= existing latest)
      (do
        (store->s3 s3-client ttl-asset "application/json; charset=utf-8" key (.getBytes latest))
        key)
      existing-url)))

(defn contentful->s3 [client started-timestamp & args]
  (let [s3-client     (s3/create-client)]
    (try
      (log/info (str "timestamp: " started-timestamp ", args=" args))
      (let [update-id        started-timestamp
            content-types    (get-content-type-with-field-types client)
            url-cache        (atom {})
            gson             (create-gson-serializer s3-client url-cache content-types)
            store->s3        (fn [ttl content-type key obj]
                                 (log/info (str "Putting " key " with ttl " ttl " (hours)"))
                                 (when s3-client
                                       (s3/put-object ttl s3-client key obj content-type)
                                       (log/info (str "Wrote " key " to bucket " (-> config :s3 :bucket-name)))))
            manifest-str     (s3/get-object s3-client "manifest.json")
            manifest         (some-> manifest-str
                                     (cheshire/parse-string))
            locales          ["fi" "sv"]
            resources        (concat
                              [["asset" "fi" asset-store-handler]
                               ["asset" "sv" asset-store-handler]]
                              (for [[t _] content-types
                                    l locales]
                                [t l content-store-handler]))
            new-manifest     (reduce
                              (fn [r [content-type locale store-latest-fn]]
                                  (let [existing-url (get-in manifest [content-type locale] nil)
                                        base-url     (str update-id "/" locale "/")
                                        latest-url   (store-latest-fn
                                                       client
                                                       s3-client
                                                       gson
                                                       url-cache
                                                       content-type
                                                       locale
                                                       base-url
                                                       existing-url)]
                                    (assoc-in r [content-type locale]
                                      (if (= existing-url latest-url)
                                        existing-url
                                        latest-url))))
                              {}
                              resources)
            new-manifest-str (cheshire/generate-string new-manifest)]
        (if (= manifest-str new-manifest-str)
          (log/info "No new updates found in manifest!")
          (do
            (log/info "New updates found in manifest!")
            (store->s3 ttl-manifest "application/json; charset=utf-8" "manifest.json" (.getBytes new-manifest-str)))))
      (catch Exception e
        (.printStackTrace e)
        (log/error (str "Contentful updated halted due to exception: " e)))
      (finally
        (when s3-client
              (.shutdown s3-client))))))

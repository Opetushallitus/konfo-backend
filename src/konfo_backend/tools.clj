(ns konfo-backend.tools
  (:require
    [cheshire.core :as cheshire]
    [clojure.tools.logging :as log]
    [clojure.string :refer [blank? split lower-case trim]]
    [clj-time.format :as format]
    [clj-time.coerce :as coerce]
    [clj-time.core :as core]
    [clj-time.core :as time]))

(defonce debug-pretty true)

(defn log-pretty
  [json]
  (when debug-pretty
    (log/debug (cheshire/generate-string json {:pretty true}))))

(defn reduce-merge-map
  [f coll]
  (reduce merge {} (map f coll)))

(defn not-blank?
  [str]
  (not (blank? str)))

(defn julkaistu?
  [e]
  (and (not (nil? e)) (= "julkaistu" (:tila e))))

(defn julkaistut
  [coll]
  (filter julkaistu? coll))

(def kouta-date-time-formatter (format/formatter "yyyy-MM-dd'T'HH:mm"))

(defn ->kouta-date-time-string
  [date-time]
  (format/unparse kouta-date-time-formatter date-time))

(defn kouta-date-time-string->date-time
  [string]
  (format/parse kouta-date-time-formatter string))

(defn long->date-time
  [long]
  (coerce/from-long long))

(defn current-time-as-kouta-format
  []
  (->kouta-date-time-string (long->date-time (System/currentTimeMillis))))

(defn within?
  [gte time lt]
  (core/within? (core/interval gte lt) time))

(defn hakuaika-kaynnissa?
  [hakuaika]
  (let [gte (kouta-date-time-string->date-time (or (:gte hakuaika) (:alkaa hakuaika)))
        lt  (kouta-date-time-string->date-time (or (:lt hakuaika) (:paattyy hakuaika)))]
    (within? gte (long->date-time (System/currentTimeMillis)) lt)))

(defn now-in-millis
  []
  (coerce/to-long (time/now)))

(defn koodi-uri-no-version
  [koodi-uri]
  (first (split koodi-uri #"#")))

(defn ammatillinen?
  [e]
  (= "amm" (:koulutustyyppi e)))

(defn amm-osaamisala?
  [e]
  (= "amm-osaamisala" (:koulutustyyppi e)))

(defn amm-tutkinnon-osa?
  [e]
  (= "amm-tutkinnon-osa" (:koulutustyyppi e)))

(defn comma-separated-string->vec
  [s]
  (->> (some-> s (split #","))
       (remove blank?)
       (map trim)
       (vec)))

(defn contains-element?
  [coll e]
  (some? (first (filter #(= e %) coll))))

(defn remove-element
  [coll e]
  (remove #(= e %) coll))

;TODO Tämä mäppäily pitäisi tehdä indeksoijassa (laitetaan koulutustyypiksi hakuindeksiin myös amm-muu kyseisille koulutuksille)
(defn amm-muu->alatyypit
  [coll]
  (if (contains-element? coll "amm-muu")
    (-> coll (remove-element "amm-muu") (conj "amm-tutkinnon-osa" "amm-osaamisala"))
    coll))

(defn ->koodi-with-version-wildcard
  [koodi]
  (str koodi "#*"))

(defn ->lower-case-vec
  [coll]
  (vec (map lower-case coll)))

(defn rename-key
  [coll old new]
  (-> coll
      (assoc new (old coll))
      (dissoc old)))
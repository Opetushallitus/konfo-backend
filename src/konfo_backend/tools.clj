(ns konfo-backend.tools
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [clj-time.format :as format]
            [clj-time.coerce :as coerce]
            [clj-time.core :as time]))

(defonce pretty-logging true)

(defonce timezone-fi (time/time-zone-for-id "Europe/Helsinki"))

(defn log-pretty
  [json]
  (when pretty-logging (log/debug (cheshire/generate-string json {:pretty true}))))

(defn reduce-merge-map [f coll] (reduce merge {} (map f coll)))

(defn julkaistu? [e] (and (not (nil? e)) (= "julkaistu" (:tila e))))

(defn julkaistut [coll] (filter julkaistu? coll))

(defn- draft-view-allowed
  [entity draft?]
  (and draft? (not (nil? entity)) (= "tallennettu" (:tila entity)) (:esikatselu entity)))

(defn allowed-to-view [entity draft?] (or (draft-view-allowed entity draft?) (julkaistu? entity)))

(defonce kouta-date-time-formatter (format/with-zone (format/formatter "yyyy-MM-dd'T'HH:mm") timezone-fi))

(defn ->kouta-date-time-string [date-time] (format/unparse kouta-date-time-formatter date-time))

(defn kouta-date-time-string->date-time [string] (format/parse kouta-date-time-formatter string))

(defn long->date-time [long] (coerce/from-long long))

(defn current-time-as-kouta-format
  []
  (->kouta-date-time-string (time/now)))

(defn within?
  [gte time lt]
  (if (nil? lt) (time/after? time gte) (time/within? (time/interval gte lt) time)))

(defn hakuaika-kaynnissa?
  [hakuaika]
  (let [gte (when (not (nil? (:alkaa hakuaika)))
              (kouta-date-time-string->date-time (:alkaa hakuaika)))
        lt (when (not (nil? (:paattyy hakuaika)))
             (kouta-date-time-string->date-time (:paattyy hakuaika)))]
    (if (nil? gte)
      false
      (within? gte (time/now) lt))))

(defn hakuaika-menneisyydessa?
  [hakuaika]
  (let [lt (when (not (nil? (:paattyy hakuaika)))
             (kouta-date-time-string->date-time (:paattyy hakuaika)))]
    (if (nil? lt)
      false
      (time/after? (time/now) lt))))

(defn get-hakukohde-from-hakutiedot [hakutiedot hakukohdeOid]
  (->> hakutiedot
       (mapcat (fn [hakutieto] (:hakukohteet hakutieto)))
       (filter (fn [hakukohdetieto] (= (:hakukohdeOid hakukohdetieto) hakukohdeOid)))
       first))

(defn  hakutieto-hakukohde-haku-kaynnissa? [hakukohdetieto]
  (and (julkaistu? hakukohdetieto)
       (boolean (some (fn [hakuaika] (hakuaika-kaynnissa? hakuaika)) (:hakuajat hakukohdetieto)))))

(defn toteutus-haku-kaynnissa?
  [toteutus]
  (let [hakutiedot (get-in toteutus [:hakutiedot])
        toteutuksenHakuaika (get-in toteutus [:metadata :hakuaika])]
    (if (empty? hakutiedot)
      (hakuaika-kaynnissa? toteutuksenHakuaika)
      (some (fn [hakutieto]
              (some hakutieto-hakukohde-haku-kaynnissa?
                    (:hakukohteet hakutieto)))
            hakutiedot))))

(defn hit-haku-kaynnissa?
  [hit]
  (let [hakutiedot (get-in hit [:hakutiedot])
        toteutuksenHakuaika (:toteutusHakuaika hit)]
    (if (empty? hakutiedot)
      (hakuaika-kaynnissa? toteutuksenHakuaika)
      (some (fn [hakutieto]
              ; search-indeksissä hakutiedoilla ei ole hakukohteita, vaan hakuajat on hakutiedon juuressa
              (some (fn [hakuaika] (hakuaika-kaynnissa? hakuaika)) (:hakuajat hakutieto)))
            hakutiedot))))

(defn now-in-millis [] (coerce/to-long (time/now)))

(defn koodi-uri-no-version [koodi-uri] (str/replace koodi-uri #"#\d*$" ""))

(defn ammatillinen? [e] (= "amm" (:koulutustyyppi e)))

(defn amm-osaamisala? [e] (= "amm-osaamisala" (:koulutustyyppi e)))

(defn amm-tutkinnon-osa? [e] (= "amm-tutkinnon-osa" (:koulutustyyppi e)))

(defn osaamismerkki? [e] (= "vapaa-sivistystyo-osaamismerkki" (:koulutustyyppi e)))

(defn comma-separated-string->vec
  [s]
  (->> (some-> s
               (str/split #","))
       (remove str/blank?)
       (map str/trim)
       (vec)))

(defn contains-element? [coll e] (some? (first (filter #(= e %) coll))))

(defn remove-element [coll e] (remove #(= e %) coll))

(defn ->koodi-with-version-wildcard [koodi] (str koodi "#*"))

(defn ->lower-case-vec [coll] (vec (map str/lower-case coll)))

(defn rename-key
  [coll old new]
  (-> coll
      (assoc new (old coll))
      (dissoc old)))

(defn assoc-if [m k v p?] (if p? (assoc m k v) m))

(defn debug-pretty
  [json]
  (println (cheshire/generate-string json {:pretty true})))

(defn remove-nils [record]
  (apply merge (for [[k v] record :when (not (nil? v))] {k v})))


(ns konfo-backend.tools
  (:require [cheshire.core :as cheshire]
            [clojure.tools.logging :as log]
            [clojure.string :as str]
            [konfo-backend.util.time :as time]))

(defonce pretty-logging true)

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

(defn hakuaika-kaynnissa?
  [hakuaika]
  (let [gte (when (not (nil? (:alkaa hakuaika)))
              (time/kouta-date-time-string->date-time (:alkaa hakuaika)))
        lt (when (not (nil? (:paattyy hakuaika)))
             (time/kouta-date-time-string->date-time (:paattyy hakuaika)))]
    (time/currently-in-between? gte lt)))

(defn hakuaika-menneisyydessa?
  [hakuaika]
  (let [lt (when (not (nil? (:paattyy hakuaika)))
             (time/kouta-date-time-string->date-time (:paattyy hakuaika)))]
    (time/currently-after? lt)))

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
  (let [hakutiedot (get-in hit [:hakutiedot])]
    (boolean (some (fn [hakutieto]
            ; search-indeksissä hakutiedoilla ei ole hakukohteita, vaan hakuajat on hakutiedon juuressa
                     (some (fn [hakuaika] (hakuaika-kaynnissa? hakuaika)) (:hakuajat hakutieto)))
                   hakutiedot))))

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

(defn ->lower-case [val] (if (string? val) (str/lower-case val) val))

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

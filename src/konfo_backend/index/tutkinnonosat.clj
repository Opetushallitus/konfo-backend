(ns konfo-backend.index.tutkinnonosat
  (:require
    [konfo-backend.tools :refer [koodi-uri-no-version]]
    [konfo-backend.elastic-tools :refer [get-source search]]
    [konfo-backend.tools :refer [now-in-millis]]))

;TODO tilan pitäisi olla "julkaistu" eikä "valmis"

(defonce index "tutkinnonosat")

(def tutkinnonosat-search (partial search index))

(defn get
  [id]
  (get-source index id))

(defonce source [:id])

(defn- ->id-query
  [ids]
  (let [terms (if (= 1 (count ids))
                {:term  {:id (first ids)}}
                {:terms {:id (vec ids)}})]
    {:bool {:must terms}}))

(defn- parse-kuvaukset
  [result]
  result)

(defn get-tutkinnonosat-by-ids
  [ids]
  (tutkinnonosat-search parse-kuvaukset
                   :_source source
                   :size (count ids)
                   :query (->id-query ids)))

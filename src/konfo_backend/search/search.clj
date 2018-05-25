(ns konfo-backend.search.search
  (:require
    [konfo-backend.search.koulutus :as koulutus]
    [konfo-backend.search.oppilaitos :as oppilaitos]
    [clojure.tools.logging :as log]))

(defn search-koulutus
  [keyword page size]
  (koulutus/text-search keyword page size))

(defn search-oppilaitos
  [keyword page size]
  (let [oids (koulutus/oid-search keyword)]
    (oppilaitos/text-search keyword oids page size)))
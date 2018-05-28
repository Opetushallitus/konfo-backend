(ns konfo-backend.search.search
  (:require
    [konfo-backend.search.koulutus :as koulutus]
    [konfo-backend.search.oppilaitos :as oppilaitos]
    [clojure.tools.logging :as log]))

(defn constraints [& { :keys [oppilaitostyyppi paikkakunta]}]
  (-> {}
      (cond-> oppilaitostyyppi (assoc :oppilaitostyyppi oppilaitostyyppi))
      (cond-> paikkakunta (assoc :paikkakunta paikkakunta))))

(defn search-koulutus
  [keyword page size constraints]
  (let [oids (oppilaitos/filter-organisaatio-oids constraints)]
    (koulutus/text-search keyword page size oids constraints)))

(defn search-oppilaitos
  [keyword page size constraints]
  (let [oids (koulutus/oid-search keyword)]
    (oppilaitos/text-search keyword page size oids constraints)))
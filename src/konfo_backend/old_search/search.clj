(ns konfo-backend.old-search.search
  (:require
    [konfo-backend.old-search.toteutus :as toteutus]
    [konfo-backend.old-search.oppilaitos :as oppilaitos]
    [konfo-backend.old-search.koulutus :as koulutus]
    [clojure.tools.logging :as log]))

(defn- not-blank [s] (not (clojure.string/blank? s)))

(defn constraints [& { :keys [koulutustyyppi paikkakunta kieli]}]
  (-> {}
      (cond-> (not-blank koulutustyyppi) (assoc :koulutustyyppi koulutustyyppi))
      (cond-> (not-blank paikkakunta) (assoc :paikkakunta paikkakunta))
      (cond-> (not-blank kieli) (assoc :kieli kieli))))

(defn search-toteutus
  [keyword lng page size constraints]
  (let [oids (oppilaitos/filter-organisaatio-oids lng constraints)]
    (toteutus/text-search keyword lng page size oids constraints)))

(defn search-oppilaitos
  [keyword lng page size constraints]
  (let [oids (toteutus/filter-organisaatio-oids keyword lng constraints)]
    (oppilaitos/text-search keyword lng page size oids constraints)))

(defn search-koulutus
  [keyword lng page size constraints]
  (let [oppilaitokset (oppilaitos/filter-organisaatio-oids lng constraints)
        toteutukset (toteutus/filter-komo-oids keyword lng oppilaitokset constraints)]
    (koulutus/koulutus-search keyword lng page size toteutukset constraints)))

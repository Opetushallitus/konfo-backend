(ns konfo-backend.search.search
  (:require
    [konfo-backend.search.koulutus :as koulutus]
    [konfo-backend.search.oppilaitos :as oppilaitos]
    [clojure.tools.logging :as log]))

(defn- not-blank [s] (not (clojure.string/blank? s)))

(defn constraints [& { :keys [koulutustyyppi paikkakunta kieli]}]
  (-> {}
      (cond-> (not-blank koulutustyyppi) (assoc :koulutustyyppi koulutustyyppi))
      (cond-> (not-blank paikkakunta) (assoc :paikkakunta paikkakunta))
      (cond-> (not-blank kieli) (assoc :kieli kieli))))

(defn search-koulutus
  [keyword lng page size constraints]
  (let [oids (oppilaitos/filter-organisaatio-oids lng constraints)]
    (koulutus/text-search keyword lng page size oids constraints)))

(defn search-oppilaitos
  [keyword lng page size constraints]
  (let [oids (koulutus/oid-search keyword lng constraints)]
    (oppilaitos/text-search keyword lng page size oids constraints)))
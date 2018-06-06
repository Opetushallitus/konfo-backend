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
  [keyword page size constraints]
  (let [oids (oppilaitos/filter-organisaatio-oids constraints)]
    (koulutus/text-search keyword page size oids constraints)))

(defn search-oppilaitos
  [keyword page size constraints]
  (let [oids (koulutus/oid-search keyword constraints)]
    (oppilaitos/text-search keyword page size oids constraints)))
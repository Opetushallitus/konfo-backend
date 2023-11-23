(ns konfo-backend.suosikit.suosikit
  (:require [konfo-backend.elastic-tools :refer [search]]
            [konfo-backend.index.hakukohde :as hakukohde]
            [konfo-backend.index.koulutus :as koulutus]
            [konfo-backend.index.oppilaitos :as oppilaitos]
            [konfo-backend.index.toteutus :as toteutus]
            [konfo-backend.search.response :refer [hits]]
            [konfo-backend.tools :refer :all]
            [konfo-backend.util.haku-auki :refer [with-is-haku-auki]]))

(defn get-by-hakukohde-oids
  [hakukohde-oids-seq]
  (let [hakukohde-oids (set hakukohde-oids-seq)
        toteutukset (search toteutus/index
                            hits
                            :_source ["oppilaitokset"
                                      "koulutusOid"
                                      "oid"
                                      "tila"
                                      "metadata.hakuaika"
                                      "hakutiedot.hakukohteet.nimi"
                                      "hakutiedot.hakukohteet.jarjestyspaikka"
                                      "hakutiedot.hakukohteet.hakuajat"
                                      "hakutiedot.hakukohteet.tila"
                                      "hakutiedot.hakukohteet.hakukohdeOid"]
                            :size (count hakukohde-oids)
                            :query {:bool {:filter [{:term {:tila "julkaistu"}}
                                                    {:terms {:hakutiedot.hakukohteet.hakukohdeOid hakukohde-oids}}]}})
        koulutukset (into {} (map #(vec [(:oid %) %]) (koulutus/get-many (distinct (map :koulutusOid toteutukset)))))
        oppilaitokset-res (oppilaitos/get-many (distinct (mapcat :oppilaitokset toteutukset)) false)
        osat-by-oid (mapcat #(map (fn [osa] [(:oid osa) %]) (:osat %)) oppilaitokset-res)
        oppilaitokset-by-oid (map (fn [oppilaitos] [(:oid oppilaitos) oppilaitos]) oppilaitokset-res)
        orgs-by-oid (into {} (concat osat-by-oid oppilaitokset-by-oid))]
    (->> toteutukset
         (map #(toteutus/filter-haut-and-hakukohteet % false))
         (map (fn [toteutus] (-> toteutus
                                 (#(assoc % :hakuAuki (toteutus-haku-kaynnissa? %)))
                                 (#(assoc % :hakutiedot (with-is-haku-auki (:hakutiedot %)))))))
         (map (fn [t]
                (->> (:hakutiedot t)
                     (mapcat :hakukohteet)
                     (map (fn [hk] (let [oppilaitos (get-in orgs-by-oid [(get-in hk [:jarjestyspaikka :oid]) :oppilaitos])]
                                     (-> hk
                                         (assoc :oppilaitosNimi (get-in oppilaitos [:organisaatio :nimi]))
                                         (assoc :esittely (get-in oppilaitos [:metadata :esittely]))
                                         (assoc :logo (get-in oppilaitos [:logo]))
                                         (assoc :toteutusOid (get-in t [:oid]))
                                         (assoc :tutkintonimikkeet (get-in koulutukset [(:koulutusOid t) :metadata :tutkintonimike])))))))))
         (flatten)
         (filter #(contains? hakukohde-oids (:hakukohdeOid %))))))

(defn get-vertailu-by-hakukohde-oids
  [hakukohde-oids-seq]
  (let [hakukohde-oids (set hakukohde-oids-seq)
        hakukohteet (filter julkaistu? (hakukohde/get-many hakukohde-oids))
        toteutukset-by-oid (into {} (map #(vec [(:oid %) %]) (toteutus/get-many (distinct (map :toteutusOid hakukohteet)))))
        oppilaitokset-res (oppilaitos/get-many (distinct (mapcat :oppilaitokset (vals toteutukset-by-oid))) false)
        osat-by-oid (mapcat #(map (fn [osa] [(:oid osa) %]) (:osat %)) oppilaitokset-res)
        oppilaitokset-by-oid (map (fn [oppilaitos] [(:oid oppilaitos) oppilaitos]) oppilaitokset-res)
        orgs-by-oid (into {} (concat osat-by-oid oppilaitokset-by-oid))]
    (map (fn [hakukohde] (let [toteutus-metadata (get-in toteutukset-by-oid [(:toteutusOid hakukohde) :metadata])
                               oppilaitos (get-in orgs-by-oid [(get-in hakukohde [:jarjestyspaikka :oid]) :oppilaitos])]
                           {:koulutustyyppi (get-in toteutus-metadata [:tyyppi])
                            :toteutusOid (:toteutusOid hakukohde)
                            :hakukohdeOid (:oid hakukohde)
                            :nimi (:nimi hakukohde)
                            :logo (:logo oppilaitos)
                            :hakuOid (:hakuOid hakukohde)
                            :oppilaitosNimi (get-in oppilaitos [:organisaatio :nimi])
                            :esittely (get-in oppilaitos [:metadata :esittely])
                            :osoite (-> oppilaitos
                                        (get-in [:metadata :yhteystiedot])
                                        (first)
                                        (get-in [:kayntiosoiteStr]))
                            :opiskelijoita (get-in oppilaitos [:metadata :opiskelijoita])
                            :osaamisalat (get-in toteutus-metadata [:osaamisalat])
                            :edellinenHaku (-> hakukohde
                                               (get-in [:metadata :pistehistoria])
                                               (last))
                            :valintakokeet (:valintakokeet hakukohde)
                            :toinenAsteOnkoKaksoistutkinto (:toinenAsteOnkoKaksoistutkinto hakukohde)
                            :jarjestaaUrheilijanAmmKoulutusta (get-in hakukohde [:metadata :jarjestaaUrheilijanAmmKoulutusta])
                            :lukiodiplomit (get-in toteutus-metadata [:diplomit])
                            :kielivalikoima (get-in toteutus-metadata [:kielivalikoima])}))
         hakukohteet)))
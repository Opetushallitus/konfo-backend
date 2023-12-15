(ns konfo-backend.suosikit.suosikit
  (:require [konfo-backend.elastic-tools :refer [search]]
            [konfo-backend.index.hakukohde :as hakukohde]
            [konfo-backend.index.koulutus :as koulutus]
            [konfo-backend.index.oppilaitos :as oppilaitos]
            [konfo-backend.index.toteutus :as toteutus]
            [konfo-backend.search.response :refer [hits]]
            [konfo-backend.tools :refer :all]))

(defn get-by-hakukohde-oids
  [hakukohde-oids-seq]
  (let [hakukohde-oids (set hakukohde-oids-seq)
        toteutukset (search toteutus/index
                            hits
                            :_source ["oppilaitokset"
                                      "koulutusOid"
                                      "oid"
                                      "tila"
                                      "metadata.kuvaus"
                                      "metadata.hakuaika"
                                      "hakutiedot.hakuOid"
                                      "hakutiedot.hakukohteet.nimi"
                                      "hakutiedot.hakukohteet.jarjestyspaikka"
                                      "hakutiedot.hakukohteet.hakuajat"
                                      "hakutiedot.hakukohteet.tila",
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
         (map (fn [toteutus]
                (->> (:hakutiedot toteutus)
                     (mapcat (fn [hakutieto] (map #(assoc % :hakuOid (:hakuOid hakutieto)) (:hakukohteet hakutieto))))
                     (map (fn [hk] (let [oppilaitos (get-in orgs-by-oid [(get-in hk [:jarjestyspaikka :oid]) :oppilaitos])]
                                     (-> hk
                                         (assoc :oppilaitosNimi (get-in oppilaitos [:organisaatio :nimi]))
                                         (assoc :esittely (get-in toteutus [:metadata :kuvaus]))
                                         (assoc :logo (get-in oppilaitos [:logo]))
                                         (assoc :toteutusOid (get-in toteutus [:oid]))
                                         (assoc :hakuAuki (hakutieto-hakukohde-haku-kaynnissa? hk))
                                         (assoc :tutkintonimikkeet (get-in koulutukset [(:koulutusOid toteutus) :metadata :tutkintonimike])))))))))
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
    (map (fn [hakukohde] (let [toteutus (get-in toteutukset-by-oid [(:toteutusOid hakukohde)])
                               toteutus-metadata (:metadata toteutus)
                               oppilaitos (get-in orgs-by-oid [(get-in hakukohde [:jarjestyspaikka :oid]) :oppilaitos])
                               hakutieto-hakukohde (get-hakukohde-from-hakutiedot (:hakutiedot toteutus) (:oid hakukohde))]
                           {:koulutustyyppi (get-in toteutus-metadata [:tyyppi])
                            :toteutusOid (:toteutusOid hakukohde)
                            :hakukohdeOid (:oid hakukohde)
                            :nimi (:nimi hakukohde)
                            :logo (:logo oppilaitos)
                            :hakuOid (:hakuOid hakukohde)
                            :oppilaitosNimi (get-in oppilaitos [:organisaatio :nimi])
                            :esittely (get-in toteutus-metadata [:kuvaus])
                            :osoite (-> oppilaitos
                                        (get-in [:metadata :yhteystiedot])
                                        (first)
                                        (get-in [:kayntiosoiteStr]))
                            :opiskelijoita (get-in oppilaitos [:metadata :opiskelijoita])
                            :osaamisalat (get-in toteutus-metadata [:osaamisalat])
                            :edellinenHaku (-> hakukohde
                                               (get-in [:metadata :pistehistoria])
                                               (last))
                            :hakuAuki (hakutieto-hakukohde-haku-kaynnissa? hakutieto-hakukohde)
                            :valintakokeet (:valintakokeet hakukohde)
                            :toinenAsteOnkoKaksoistutkinto (:toinenAsteOnkoKaksoistutkinto hakukohde)
                            :jarjestaaUrheilijanAmmKoulutusta (get-in hakukohde [:metadata :jarjestaaUrheilijanAmmKoulutusta])
                            :lukiodiplomit (get-in toteutus-metadata [:diplomit])
                            :kielivalikoima (get-in toteutus-metadata [:kielivalikoima])}))
         hakukohteet)))
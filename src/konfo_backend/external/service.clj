(ns konfo-backend.external.service
  (:require
    [konfo-backend.tools :refer [julkaistu?]]
    [konfo-backend.index.koulutus :as koulutus]
    [konfo-backend.index.toteutus :as toteutus]
    [konfo-backend.index.hakukohde :as hakukohde]
    [konfo-backend.index.haku :as haku]
    [konfo-backend.index.valintaperuste :as valintaperuste]))

(defn- when-satisfy
  [e pred]
  (when (pred e)
    e))

(defn- get-koulutus
  [oid]
  (some-> (koulutus/get oid false)
          (dissoc :muokkaaja :julkinen :esikatselu :organisaatiot :sorakuvausId)
          (update-in [:sorakuvaus] dissoc :muokkaaja :julkinen)))

(defn get-toteutus
  [oid]
  (some-> (toteutus/get oid false)
          (dissoc :muokkaaja :esikatselu :organisaatiot :hakutiedot)))

(defn get-hakukohde
  [oid]
  (some-> (hakukohde/get oid false)
          (dissoc :muokkaaja :esikatselu :toteutus :hakulomakeAtaruId :yhdenPaikanSaanto)))

(defn get-haku
  [oid]
  (some-> (haku/get oid)
          (dissoc :muokkaaja :organisaatiot :hakukohteenLiittamisenTakaraja :hakukohteenMuokkaamisenTakaraja :ajastettuJulkaisu :hakulomakeAtaruId)))

(defn get-valintaperustekuvaus
  [id]
  (some-> (valintaperuste/get id false)
          (dissoc :muokkaaja :esikatselu :julkinen)

(defn- get-koulutukset-by-oids
  [oids]
  (when (seq oids)
    (->> (koulutus/get-many oids ["esikatselu" "julkinen" "muokkaaja" "organisaatiot" "toteutukset"]))))

(defn- get-toteutukset-by-oids
  [oids]
  (when (seq oids)
    (->> (toteutus/get-many oids ["hakutiedot" "muokkaaja" "organisaatiot" "esikatselu"])
         (filter julkaistu?)
         (vec))))

(defn- get-haut-by-oids
  [oids]
  (when (seq oids)
    (->> (haku/get-many oids ["muokkaaja" "organisaatiot" "hakukohteenLiittamisenTakaraja" "hakukohteenMuokkaamisenTakaraja" "ajastettuJulkaisu" "hakulomakeAtaruId" "hakukohteet"])
         (filter julkaistu?)
         (vec))))

(defn- get-hakukohteet-by-toteutus-oids
  [toteutusOids]
  (when (seq toteutusOids)
    (->> (hakukohde/get-many-by-terms :toteutusOid toteutusOids ["toteutus" "muokkaaja" "yhdenPaikanSaanto" "valintaperuste" "hakulomakeAtaruId" "esikatselu"])
         (filter julkaistu?)
         (vec))))

(defn- get-hakukohteet
  [oids]
  (when (seq oids)
    (->> (hakukohde/get-many oids ["toteutus" "muokkaaja" "yhdenPaikanSaanto" "valintaperuste" "hakulomakeAtaruId" "esikatselu"])
         (filter julkaistu?)
         (vec))))

(defn koulutus
  [oid toteutukset? hakukohteet? haut?]
  (when-let [koulutus (get-koulutus oid)]
    (let [toteutukset (when toteutukset? (get-toteutukset-by-oids (vec (map :oid (:toteutukset koulutus)))))
          hakukohteet (when (or haut? hakukohteet?) (get-hakukohteet-by-toteutus-oids (vec (map :oid (:toteutukset koulutus)))))
          haut        (when haut? (get-haut-by-oids (vec (map :hakuOid hakukohteet))))]
      (cond-> (dissoc koulutus :toteutukset)
              toteutukset? (assoc :toteutukset toteutukset)
              hakukohteet? (assoc :hakukohteet hakukohteet)
              haut?        (assoc :haut haut)))))

(defn toteutus
  [oid koulutus? hakukohteet? haut?]
  (when-let [toteutus (get-toteutus oid)]
    (let [koulutus (when koulutus? (-> toteutus :koulutusOid (get-koulutus) (dissoc :toteutukset)))
          hakukohteet (when (or haut? hakukohteet?) (get-hakukohteet-by-toteutus-oids [oid]))
          haut (when haut? (get-haut-by-oids (vec (map :hakuOid hakukohteet))))]
      (cond-> toteutus
              koulutus?    (assoc :koulutus koulutus)
              hakukohteet? (assoc :hakukohteet hakukohteet)
              haut?        (assoc :haut haut)))))

(defn hakukohde
  [oid koulutus? toteutus? valintaperustekuvaus? haku?]
  (when-let [hakukohde (get-hakukohde oid)]
    (let [valintaperustekuvaus (when valintaperustekuvaus?
                                 (when-let [valintaperusteId (get-in hakukohde [:valintaperuste :id])]
                                   (get-valintaperustekuvaus valintaperusteId)))
          toteutus (when (or koulutus? toteutus?) (get-toteutus (:toteutusOid hakukohde)))
          koulutus (when koulutus? (some-> toteutus :koulutusOid (get-koulutus) (dissoc :toteutukset)))
          haku (when haku? (some-> hakukohde :hakuOid (get-haku) (dissoc :hakukohteet)))]
      (cond-> (dissoc hakukohde :valintaperuste)
              (and koulutus? koulutus)                         (assoc :koulutus koulutus)
              (and toteutus? toteutus)                         (assoc :toteutus toteutus)
              (and haku? haku)                                 (assoc :haku haku)
              (and valintaperustekuvaus? valintaperustekuvaus) (assoc :valintaperustekuvaus valintaperustekuvaus)))))

(defn haku
  [oid koulutukset? toteutukset? hakukohteet?]
  (when-let [haku (get-haku oid)]
    (let [hakukohteet (when (or koulutukset? toteutukset? hakukohteet?) (get-hakukohteet (vec (map :oid (:hakukohteet haku)))))
          toteutukset (when (or koulutukset? toteutukset?) (get-toteutukset-by-oids (vec (map :toteutusOid hakukohteet))))
          koulutukset (when koulutukset? (get-koulutukset-by-oids (vec (map :koulutusOid toteutukset))))]
      (cond-> (dissoc haku :hakukohteet)
              hakukohteet? (assoc :hakukohteet hakukohteet)
              toteutukset? (assoc :toteutukset toteutukset)
              koulutukset? (assoc :koulutukset koulutukset)))))

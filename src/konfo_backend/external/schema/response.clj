(ns konfo-backend.external.schema.response
  (:require
    [schema.core :as s]
    [schema-tools.core :as st]
    [konfo-backend.external.schema.koulutus :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.toteutus :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.valintakoe :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.hakukohde :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.haku :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.liite :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.valintaperustekuvaus :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.search :refer :all :exclude [schemas]]))

(def koulutus-response-schema
  "|    KoulutusResponse:
   |      type: object
   |      $ref: '#/components/schemas/Koulutus'
   |      properties:
   |        toteutukset:
   |          type: array
   |          description: Koulutuksen toteutukset, jos pyydetty
   |          items:
   |            $ref: '#/components/schemas/Toteutus'
   |        hakukohteet:
   |          type: array
   |          description: Hakukohteet, joissa koulutukseen voi hakea, jos pyydetty
   |          items:
   |            $ref: '#/components/schemas/Hakukohde'
   |        haut:
   |          type: array
   |          description: Haut, joissa koulutukseen voi hakea, jos pyydetty
   |          items:
   |            $ref: '#/components/schemas/Haku'")

(def KoulutusResponse
  (st/merge
    Koulutus
    {(s/->OptionalKey :toteutukset) [Toteutus]
     (s/->OptionalKey :hakukohteet) [Hakukohde]
     (s/->OptionalKey :haut)        [Haku]}))

(def toteutus-response-schema
  "|    ToteutusResponse:
   |      type: object
   |      $ref: '#/components/schemas/Toteutus'
   |      properties:
   |        toteutukset:
   |          type: object
   |          description: Toteutuksen koulutus, jos pyydetty
   |          $ref: '#/components/schemas/Koulutus'
   |        hakukohteet:
   |          type: array
   |          description: Hakukohteet, joissa toteutukseen voi hakea, jos pyydetty
   |          items:
   |            $ref: '#/components/schemas/Hakukohde'
   |        haut:
   |          type: array
   |          description: Haut, joissa toteutukseen voi hakea, jos pyydetty
   |          items:
   |            $ref: '#/components/schemas/Haku'")

(def ToteutusResponse
  (st/merge
    Toteutus
    {(s/->OptionalKey :koulutus)    Koulutus
     (s/->OptionalKey :hakukohteet) [Hakukohde]
     (s/->OptionalKey :haut)        [Haku]}))

(def hakukohde-response-schema
  "|    HakukohdeResponse:
   |      type: object
   |      $ref: '#/components/schemas/Hakukohde'
   |      properties:
   |        valintaperustekuvaus:
   |          type: object
   |          description: Hakukohteen valintaperusteiden kuvaus, jos pyydetty
   |          $ref: '#/components/schemas/Valintaperustekuvaus'
   |        koulutus:
   |          type: object
   |          description: Koulutus, johon hakukohteessa voi hakea, jos pyydetty
   |          items:
   |            $ref: '#/components/schemas/Koulutus'
   |        toteutus:
   |          type: object
   |          description: Toteutus, johon hakukohteessa voi hakea, jos pyydetty
   |          $ref: '#/components/schemas/Toteutus'
   |        haku:
   |          type: object
   |          description: Haku, johon hakukohde kuuluu, jos pyydetty
   |          $ref: '#/components/schemas/Haku'")

(def HakukohdeResponse
  (st/merge
    Hakukohde
    {(s/->OptionalKey :valintaperustekuvaus) (s/maybe Valintaperustekuvaus)
     (s/->OptionalKey :koulutus)             Koulutus
     (s/->OptionalKey :toteutus)             Toteutus
     (s/->OptionalKey :haku)                 Haku}))

(def haku-response-schema
  "|    HakuResponse:
   |      type: object
   |      $ref: '#/components/schemas/Haku'
   |      properties:
   |        koulutukset:
   |          type: array
   |          description: Koulutukset, joihin haussa voi hakea, jos pyydetty
   |          items:
   |            $ref: '#/components/schemas/Koulutus'
   |        toteutukset:
   |          type: array
   |          description: Toteutukset, joihin haussa voi hakea, jos pyydetty
   |          items:
   |            $ref: '#/components/schemas/Toteutus'
   |        hakukohteet:
   |          type: array
   |          description: Hakukohteet, joihin haussa voi hakea, jos pyydetty
   |          items:
   |            $ref: '#/components/schemas/Hakukohde'")

(def HakuResponse
  (st/merge
    Haku
    {(s/->OptionalKey :koulutukset) [Koulutus]
     (s/->OptionalKey :hakukohteet) [Hakukohde]
     (s/->OptionalKey :toteutukset) [Toteutus]}))

(def koulutus-toteutus-search-response-schema
  "|    KoulutusToteutusSearchResponse:
   |      type: object
   |      properties:
   |        total:
   |          type: number
   |          description: Hakutulosten lukumäärä
   |        hits:
   |          type: array
   |          description: Hakutulokset
   |          items:
   |            $ref: '#/components/schemas/KoulutusToteutusHit'")

(def KoulutusToteutusSearchResponse
  {:total s/Int
   :hits  [KoulutusHit]})

(def KoulutusToteutusSearchResponse
  {:total s/Int
   :hits  [KoulutusHit]})

(def schemas
  (str koulutus-response-schema "\n"
       toteutus-response-schema "\n"
       hakukohde-response-schema "\n"
       haku-response-schema "\n"
       koulutus-toteutus-search-response-schema))

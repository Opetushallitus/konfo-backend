(ns konfo-backend.external.schema.valintakoe
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.toteutus-metadata :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]))

(def valintakoe-tilaisuus-schema
  "|    Valintakoetilaisuus:
   |      type: object
   |      properties:
   |        osoite:
   |          type: object
   |          description: Valintakokeen järjestämispaikan osoite
   |          allOf:
   |            - $ref: '#/components/schemas/Osoite'
   |        aika:
   |          type: array
   |          description: Valintakokeen järjestämisaika
   |          items:
   |            $ref: '#/components/schemas/Ajanjakso'
   |        jarjestamispaikka:
   |          type: object
   |          description: Valintakokeen järjestämispaikka eri kielillä. Kielet on määritetty kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'
   |        lisatietoja:
   |          type: object
   |          description: Lisätietoja valintakokeesta eri kielillä. Kielet on määritetty kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'")

(def ValintakoeTilaisuus
  {:osoite                              Osoite
   :aika                                Ajanjakso
   (s/->OptionalKey :jarjestamispaikka) Kielistetty
   (s/->OptionalKey :lisatietoja)       Kielistetty})

(def valintakoe-metadata-schema
  "|    ValintakoeMetadata:
   |      type: object
   |      properties:
   |        tietoja:
   |          type: object
   |          description: Tietoa valintakokeesta
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'
   |        liittyyEnnakkovalmistautumista:
   |          type: boolean
   |          description: Liittyykö valintakokeeseen ennakkovalmistautumista
   |        ohjeetEnnakkovalmistautumiseen:
   |          type: object
   |          description: Ohjeet valintakokeen ennakkojärjestelyihin
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'
   |        erityisjarjestelytMahdollisia:
   |          type: boolean
   |          description: Ovatko erityisjärjestelyt mahdollisia valintakokeessa
   |        ohjeetErityisjarjestelyihin:
   |          type: object
   |          description: Ohjeet valintakokeen erityisjärjestelyihin
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'")

(def ValintakoeMetadata
  {(s/->OptionalKey :tietoja)                        Kielistetty
   (s/->OptionalKey :liittyyEnnakkovalmistautumista) s/Bool
   (s/->OptionalKey :ohjeetEnnakkovalmistautumiseen) Kielistetty
   (s/->OptionalKey :erityisjarjestelytMahdollisia)  s/Bool
   (s/->OptionalKey :ohjeetErityisjarjestelyihin)    Kielistetty})

(def valintakoe-schema
  "|    Valintakoe:
   |      type: object
   |      description: Valintakokeen tiedot
   |      properties:
   |        id:
   |          type: string
   |          description: Valintakokeen yksilöivä tunniste. Järjestelmän generoima.
   |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
   |        tyyppi:
   |          type: object
   |          description: Valintakokeen tyyppi.
   |          $ref: '#/components/schemas/ValintakokeenTyyppi'
   |        nimi:
   |          type: object
   |          description: Valintakokeen nimi eri kielillä. Kielet on määritetty kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'
   |        metadata:
   |          type: object
   |          $ref: '#/components/schemas/ValintakoeMetadata'
   |        tilaisuudet:
   |          type: array
   |          description: Valintakokeen järjestämistilaisuudet
   |          items:
   |            $ref: '#/components/schemas/Valintakoetilaisuus'")

(def Valintakoe
  {:id                            s/Str
   (s/->OptionalKey :tyyppi)      (->Koodi ValintakokeenTyyppiKoodi)
   (s/->OptionalKey :nimi)        Kielistetty
   (s/->OptionalKey :metadata)    ValintakoeMetadata
   (s/->OptionalKey :tilaisuudet) [ValintakoeTilaisuus]})

(def schemas
  (str
    valintakoe-metadata-schema "\n"
    valintakoe-tilaisuus-schema "\n"
    valintakoe-schema "\n"))
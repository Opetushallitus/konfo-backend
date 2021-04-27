(ns konfo-backend.external.schema.valintaperustekuvaus
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.valintakoe :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.valintaperustekuvaus-metadata :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.sorakuvaus :refer :all :exclude [schemas]]))

(def valintaperustekuvaus-schema
  "|    Valintaperustekuvaus:
   |      type: object
   |      properties:
   |        id:
   |          type: string
   |          description: Valintaperustekuvauksen yksilöivä tunniste
   |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
   |        tila:
   |          type: string
   |          example: julkaistu
   |          enum:
   |            - julkaistu
   |          description: Valintaperustekuvauksen julkaisutila. Aina julkaistu
   |        koulutustyyppi:
   |          type: string
   |          description: Minkä tyyppisille koulutuksille valintaperustekuvaus on tarkoitettu käytettäväksi?
   |          enum:
   |            - amm
   |            - yo
   |            - amk
   |            - lk
   |            - muu
   |          example: amm
   |        hakutapa:
   |          type: object
   |          description: Valintaperustekuvaukseen liittyvä hakutapa
   |          $ref: '#/components/schemas/Hakutapa'
   |        kohdejoukko:
   |          type: object
   |          description: Valintaperustekuvaukseen liittyvä kohdejoukko. Valintaperusteen ja siihen hakukohteen kautta liittyvän haun kohdejoukon tulee olla sama
   |          $ref: '#/components/schemas/HaunKohdejoukko'
   |        kohdejoukonTarkenne:
   |          type: object
   |          description: Valintaperustekuvaukseen liittyvä kohdejoukon tarkenne
   |          $ref: '#/components/schemas/HaunKohdejoukonTarkenne'
   |        kielivalinta:
   |          type: array
   |          description: Kielet, joille valintaperustekuvauksen nimi, kuvailutiedot ja muut tekstit on käännetty
   |          items:
   |            $ref: '#/components/schemas/Kieli'
   |          example:
   |            - fi
   |            - sv
   |        nimi:
   |          type: object
   |          description: Valintaperustekuvauksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Nimi'
   |        valintakokeet:
   |          type: array
   |          description: Hakuun liittyvät valintakokeet
   |          items:
   |            $ref: '#/components/schemas/Valintakoe'
   |        metadata:
   |          type: object
   |          oneOf:
   |            - $ref: '#/components/schemas/AmmValintaperustekuvausMetadata'
   |        organisaatio:
   |          type: object
   |          description: Valintaperustekuvauksen luoneen organisaation oid
   |          $ref: '#/components/schemas/Organisaatio'
   |        modified:
   |          type: string
   |          format: date-time
   |          description: Valintaperustekuvauksen viimeisin muokkausaika
   |          example: 2019-08-23T09:55
   |        timestamp:
   |          type: number
   |          description: Valintaperustekuvauksen viimeisin indeksointiaika
   |          example: 1587537927174")

(def schemas
  valintaperustekuvaus-schema)

(def Valintaperustekuvaus
  {:id                                    s/Str
   :tila                                  Julkaistu
   :koulutustyyppi                        Koulutustyyppi
   :hakutapa                              (->Koodi HakutapaKoodi)
   :kohdejoukko                           (->Koodi HaunKohdejoukkoKoodi)
   (s/->OptionalKey :kohdejoukonTarkenne) (->Koodi HaunKohdejoukonTarkenneKoodi)
   (s/->OptionalKey :valintakokeet)       [Valintakoe]
   :kielivalinta                          [Kieli]
   :nimi                                  Kielistetty
   (s/->OptionalKey :metadata)            (s/conditional #(= "amm" (:tyyppi %)) AmmValintaperustekuvausMetadata
                                                         #(= "yo" (:tyyppi %)) YoValintaperusteKuvausMetadata
                                                         #(= "amk" (:tyyppi %)) AmkValintaperusteKuvausMetadata)
   :organisaatio                          Organisaatio
   :modified                              Datetime
   :timestamp                             s/Int})


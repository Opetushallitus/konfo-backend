(ns konfo-backend.external.schema.haku
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.koulutus-metadata :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]))

(def haku-schema
  "|    Haku:
   |      type: object
   |      properties:
   |        oid:
   |          type: string
   |          description: Haun yksilöivä tunniste
   |          example: 1.2.246.562.29.00000000000000000009
   |        tila:
   |          type: string
   |          example: julkaistu
   |          enum:
   |            - julkaistu
   |          description: Haun julkaisutila. Aina 'julkaistu'
   |        nimi:
   |          type: object
   |          description: Haun nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Nimi'
   |        hakutapa:
   |          type: object
   |          description: Haun hakutapa
   |          $ref: '#/components/schemas/Hakutapa'
   |        kohdejoukko:
   |          type: object
   |          description: Haun kohdejoukko
   |          $ref: '#/components/schemas/HaunKohdejoukko'
   |        kohdejoukonTarkenne:
   |          type: object
   |          description: Haun kohdejoukon tarkenne
   |          $ref: '#/components/schemas/HaunKohdejoukonTarkenne'
   |        hakulomaketyyppi:
   |          type: string
   |          description: Hakulomakkeen tyyppi. Kertoo, käytetäänkö Atarun (hakemuspalvelun) hakulomaketta, muuta hakulomaketta
   |            (jolloin voidaan lisätä hakulomakkeeseen linkki) tai onko niin, ettei sähkököistä hakulomaketta ole lainkaan, jolloin sille olisi hyvä lisätä kuvaus.
   |            Hakukohteella voi olla eri hakulomake kuin haulla.
   |          example: ataru
   |          enum:
   |            - ataru
   |            - ei sähköistä
   |            - muu
   |        hakulomakeKuvaus:
   |          type: object
   |          description: Hakulomakkeen kuvausteksti eri kielillä. Kielet on määritetty haun kielivalinnassa. Hakukohteella voi olla eri hakulomake kuin haulla.
   |          $ref: '#/components/schemas/Kuvaus'
   |        hakulomakeLinkki:
   |          type: object
   |          description: Hakulomakkeen linkki eri kielillä. Kielet on määritetty haun kielivalinnassa. Hakukohteella voi olla eri hakulomake kuin haulla.
   |          $ref: '#/components/schemas/Linkki'
   |        hakuajat:
   |          type: array
   |          description: Haun hakuajat. Hakukohteella voi olla omat hakuajat.
   |          items:
   |            $ref: '#/components/schemas/Ajanjakso'
   |        metadata:
   |          type: object
   |          properties:
   |            yhteyshenkilot:
   |              type: array
   |              description: Lista haun yhteyshenkilöistä
   |              items:
   |                $ref: '#/components/schemas/Yhteyshenkilo'
   |            tulevaisuudenAikataulu:
   |              type: array
   |              description: Oppijalle Opintopolussa näytettävät haun mahdolliset tulevat hakuajat
   |              items:
   |                $ref: '#/components/schemas/Ajanjakso'
   |            koulutuksenAlkamiskausi:
   |              type: object
   |              description: Koulutuksen alkamiskausi
   |              $ref: '#/components/schemas/KoulutuksenAlkamiskausi'
   |        kielivalinta:
   |          type: array
   |          description: Kielet, joille haun nimi, kuvailutiedot ja muut tekstit on käännetty
   |          items:
   |            $ref: '#/components/schemas/Kieli'
   |          example:
   |            - fi
   |            - sv
   |        organisaatio:
   |          type: object
   |          description: Haun luonut organisaatio
   |          $ref: '#/components/schemas/Organisaatio'
   |        modified:
   |           type: string
   |           format: date-time
   |           description: Haun viimeisin muokkausaika
   |           example: 2019-08-23T09:55")

(def Haku
  {:oid                            HakuOid
   (s/->OptionalKey :externalId)   s/Str
   :tila                           Julkaistu
   :nimi                           Kielistetty
   :hakutapa                       (->Koodi HakutapaKoodi)
   :kohdejoukko                    (->Koodi HaunKohdejoukkoKoodi)
   (s/->OptionalKey :kohdejoukonTarkenne) (->Koodi HaunKohdejoukonTarkenneKoodi)
   :hakulomaketyyppi               Hakulomaketyyppi
   :hakulomakeKuvaus               Kielistetty
   :hakulomakeLinkki               Kielistetty
   :hakuajat                       [Ajanjakso]
   :kielivalinta                   [Kieli]
   (s/->OptionalKey :metadata)     {:yhteyshenkilot         [Yhteyshenkilo]
                                    :tulevaisuudenAikataulu [Ajanjakso]
                                    :koulutuksenAlkamiskausi (s/maybe KoulutuksenAlkamiskausi)}
   :organisaatio                   Organisaatio
   :modified                       Datetime
   :timestamp                      s/Int})

(def schemas
  haku-schema)
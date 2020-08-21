(ns konfo-backend.external.schema.hakukohde
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.toteutus-metadata :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.valintakoe :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.haku :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.liite  :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.valintaperustekuvaus  :refer :all :exclude [schemas]]))

(def hakukohde-metadata-schema
  "|    HakukohdeMetadata:
   |      type: object
   |      properties:
   |        valintakokeidenYleiskuvaus:
   |          type: object
   |          description: Valintakokeiden yleiskuvaus eri kielillä. Kielet on määritetty hakukohteen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'")

(def HakukohdeMetadata
  {:valintakokeidenYleiskuvaus Kielistetty})

(def hakukohde-schema
  "|    Hakukohde:
   |      type: object
   |      properties:
   |        oid:
   |          type: string
   |          description: Hakukohteen yksilöivä tunniste. Järjestelmän generoima.
   |          example: 1.2.246.562.20.00000000000000000009
   |        toteutusOid:
   |          type: string
   |          description: Hakukohteeseen liitetyn toteutuksen yksilöivä tunniste.
   |          example: 1.2.246.562.17.00000000000000000009
   |        hakuOid:
   |          type: string
   |          description: Hakukohteeseen liitetyn haun yksilöivä tunniste.
   |          example: 1.2.246.562.29.00000000000000000009
   |        tila:
   |          type: string
   |          example: julkaistu
   |          enum:
   |            - julkaistu
   |          description: Hakukohteen julkaisutila. Jos hakukohde on julkaistu, se näkyy oppijalle Opintopolussa.
   |        nimi:
   |          type: object
   |          description: Hakukohteen nimi eri kielillä. Kielet on määritetty hakukohteen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'
   |        alkamiskausi:
   |          type: object
   |          description: Hakukohteen koulutuksen alkamiskausi, jos ei käytetän haun alkamiskautta
   |          $ref: '#/components/schemas/Alkamiskausi'
   |        alkamisvuosi:
   |          type: number
   |          description: Hakukohteen koulutusten alkamisvuosi, jos ei käytetä haun alkamisvuotta
   |          example: 2020
   |        kaytetaanHaunAlkamiskautta:
   |          type: boolean
   |          description: Käytetäänkö haun alkamiskautta ja -vuotta vai onko hakukohteelle määritelty oma alkamisajankohta?
   |        jarjestyspaikka:
   |          type: object
   |          description: Se organisaatio, jossa järjestetään koulutus, johon hakukohteessa voi hakea
   |          allOf:
   |            - $ref: '#/components/schemas/Organisaatio'
   |        hakulomaketyyppi:
   |          type: string
   |          description: Hakulomakkeen tyyppi. Kertoo, käytetäänkö Atarun (hakemuspalvelun) hakulomaketta, muuta hakulomaketta
   |            (jolloin voidaan lisätä hakulomakkeeseen linkki) tai onko niin, ettei sähkököistä hakulomaketta ole lainkaan, jolloin sille olisi hyvä lisätä kuvaus.
   |          example: ataru
   |          enum:
   |            - ataru
   |            - ei sähköistä
   |            - muu
   |        hakulomakeKuvaus:
   |          type: object
   |          description: Hakulomakkeen kuvausteksti eri kielillä. Kielet on määritetty haun kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        hakulomakeLinkki:
   |          type: object
   |          description: Hakulomakkeen linkki eri kielillä. Kielet on määritetty haun kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Linkki'
   |        kaytetaanHaunHakulomaketta:
   |          type: boolean
   |          description: Käytetäänkö haun hakulomaketta vai onko hakukohteelle määritelty oma hakulomake?
   |        aloituspaikat:
   |          type: integer
   |          description: Hakukohteen aloituspaikkojen lukumäärä
   |          example: 100
   |        ensikertalaisenAloituspaikat:
   |          type: integer
   |          description: Hakukohteen ensikertalaisen aloituspaikkojen lukumäärä
   |          example: 50
   |        pohjakoulutusvaatimus:
   |          type: array
   |          items:
   |            $ref: '#/components/schemas/Pohjakoulutusvaatimus'
   |          description: Lista toisen asteen hakukohteen pohjakoulutusvaatimuksista
   |        muuPohjakoulutusvaatimus:
   |          type: object
   |          description: Hakukohteen muiden pohjakoulutusvaatimusten kuvaus eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        pohjakoulutusvaatimusTarkenne:
   |          type: object
   |          description: Pohjakoulutusvaatimusten tarkempi kuvaus eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        toinenAsteOnkoKaksoistutkinto:
   |          type: boolean
   |          description: Onko hakukohteen toisen asteen koulutuksessa mahdollista suorittaa kaksoistutkinto?
   |        kaytetaanHaunAikataulua:
   |          type: boolean
   |          description: Käytetäänkö haun hakuaikoja vai onko hakukohteelle määritelty omat hakuajat?
   |        hakuajat:
   |          type: array
   |          description: Hakukohteen hakuajat, jos ei käytetä haun hakuaikoja
   |          items:
   |            $ref: '#/components/schemas/Ajanjakso'
   |        valintaperustekuvaus:
   |          type: object
   |          description: Hakukohteeseen liittyvä valintaperustekuvaus
   |          allOf:
   |            - $ref: '#/components/schemas/Valintaperustekuvaus'
   |        liitteetOnkoSamaToimitusaika:
   |          type: boolean
   |          description: Onko kaikilla hakukohteen liitteillä sama toimitusaika?
   |        liitteetOnkoSamaToimitusosoite:
   |          type: boolean
   |          description: Onko kaikilla hakukohteen liitteillä sama toimitusosoite?
   |        liitteidenToimitusaika:
   |          type: string
   |          description: Jos liitteillä on sama toimitusaika, se ilmoitetaan tässä
   |          format: date-time
   |          example: 2019-08-23T09:55
   |        liitteidenToimitustapa:
   |          type: string
   |          description: Jos liitteillä on sama toimitustapa, se ilmoitetaan tässä
   |          example: hakijapalvelu
   |          enum:
   |            - hakijapalvelu
   |            - osoite
   |            - lomake
   |        liitteidenToimitusosoite:
   |          type: object
   |          description: Jos liitteillä on sama toimitusosoite, se ilmoitetaan tässä
   |          allOf:
   |            - $ref: '#/components/schemas/LiitteenToimitusosoite'
   |        liitteet:
   |          type: array
   |          description: Hakukohteen liitteet
   |          items:
   |            $ref: '#/components/schemas/Liite'
   |        valintakokeet:
   |          type: array
   |          description: Hakukohteeseen liittyvät valintakokeet
   |          items:
   |            $ref: '#/components/schemas/Valintakoe'
   |        kielivalinta:
   |          type: array
   |          description: Kielet, joille hakukohteen nimi, kuvailutiedot ja muut tekstit on käännetty
   |          items:
   |            $ref: '#/components/schemas/Kieli'
   |          example:
   |            - fi
   |            - sv
   |        organisaatio:
   |          type: object
   |          description: Hakukohteen luonut organisaatio
   |          allOf:
   |           - $ref: '#/components/schemas/Organisaatio'
   |        modified:
   |          type: string
   |          format: date-time
   |          description: Koulutuksen viimeisin muokkausaika
   |          example: 2019-08-23T09:55
   |        timestamp:
   |          type: number
   |          description: Koulutuksen viimeisin indeksointiaika
   |          example: 1587537927174")

(s/defschema Hakukohde
  {:oid                                            HakukohdeOid
   :toteutusOid                                    ToteutusOid
   :hakuOid                                        HakuOid
   :tila                                           Julkaistu
   :nimi                                           Kielistetty
   (s/->OptionalKey :alkamiskausi)                 (->Koodi AlkamiskausiKoodi)
   (s/->OptionalKey :alkamisvuosi)                 s/Str
   :kaytetaanHaunAlkamiskautta                     s/Bool
   :jarjestyspaikka                                Organisaatio
   :kaytetaanHaunHakulomaketta                     s/Bool
   (s/->OptionalKey :hakulomaketyyppi)             Hakulomaketyyppi
   :hakulomakeKuvaus                               Kielistetty
   :hakulomakeLinkki                               Kielistetty
   (s/->OptionalKey :aloituspaikat)                s/Int
   (s/->OptionalKey :ensikertalaisenAloituspaikat) s/Int
   :pohjakoulutusvaatimus                          [(->Koodi PohjakoulutusvaatimusKoodi)]
   :pohjakoulutusvaatimusTarkenne                  Kielistetty
   :muuPohjakoulutusvaatimus                       Kielistetty
   :toinenAsteOnkoKaksoistutkinto                  s/Bool
   :kaytetaanHaunAikataulua                        s/Bool
   :hakuajat                                       [Ajanjakso]
   :liitteetOnkoSamaToimitusaika                   s/Bool
   :liitteetOnkoSamaToimitusosoite                 s/Bool
   (s/->OptionalKey :liitteidenToimitusaika)       Datetime
   (s/->OptionalKey :liitteidenToimitusosoite)     LiitteenToimitusosoite
   (s/->OptionalKey :liitteidenToimitustapa)       LiitteenToimitustapa
   :liitteet                                       [Liite]
   :valintakokeet                                  [Valintakoe]
   :kielivalinta                                   [Kieli]
   (s/->OptionalKey :metadata)                     HakukohdeMetadata
   :organisaatio                                   Organisaatio
   :modified                                       Datetime
   :timestamp                                      s/Int})

(def schemas
  (str
    hakukohde-metadata-schema "\n"
    hakukohde-schema "\n"))
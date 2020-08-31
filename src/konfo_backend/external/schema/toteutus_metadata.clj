(ns konfo-backend.external.schema.toteutus-metadata
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
    [schema-tools.core :as st]))

(def opetus-schema
  "|    Opetus:
   |      type: object
   |      properties:
   |        opetuskieli:
   |          type: array
   |          description: Lista koulutuksen toteutuksen opetuskielistä
   |          items:
   |            type: object
   |            $ref: '#/components/schemas/Opetuskieli'
   |        opetuskieletKuvaus:
   |          type: object
   |          description: Koulutuksen toteutuksen opetuskieliä tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        opetusaika:
   |          type: array
   |          description: Lista koulutuksen toteutuksen opetusajoista
   |          items:
   |            type: object
   |            $ref: '#/components/schemas/Opetusaika'
   |        opetusaikaKuvaus:
   |          type: object
   |          description: Koulutuksen toteutuksen opetusaikoja tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        opetustapa:
   |          type: array
   |          description: Lista koulutuksen toteutuksen opetustavoista
   |          items:
   |            type: object
   |            $ref: '#/components/schemas/Opetustapa'
   |        opetustapaKuvaus:
   |          type: object
   |          description: Koulutuksen toteutuksen opetustapoja tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        onkoMaksullinen:
   |          type: boolean
   |          decription: Onko koulutus maksullinen?
   |        maksullisuusKuvaus:
   |          type: object
   |          description: Koulutuksen toteutuksen maksullisuutta tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        maksunMaara:
   |          type: double
   |          description: Koulutuksen toteutuksen maksun määrä euroissa?
   |          example: 220.50
   |        koulutuksenTarkkaAlkamisaika:
   |          type: boolean
   |          description: Jos alkamisaika on tiedossa niin alkamis- ja päättymispäivämäärä on pakollinen. Muussa tapauksessa kausi ja vuosi on pakollisia tietoja.
   |          example: true
   |        koulutuksenAlkamispaivamaara:
   |          type: string
   |          description: Koulutuksen alkamisen päivämäärä
   |          example: 2019-11-20T12:00
   |        koulutuksenPaattymispaivamaara:
   |          type: string
   |          description: Koulutuksen päättymisen päivämäärä
   |          example: 2019-12-20T12:00
   |        koulutuksenAlkamiskausi:
   |          type: object
   |          description: Koulutuksen alkamiskausi
   |          $ref: '#/components/schemas/Alkamiskausi'
   |        koulutuksenAlkamisvuosi:
   |          type: string
   |          description: Koulutuksen alkamisvuosi
   |          example: 2020
   |        lisatiedot:
   |          type: array
   |          description: Koulutuksen toteutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
   |          items:
   |            type: object
   |            $ref: '#/components/schemas/KoulutusLisatieto'
   |        onkoStipendia:
   |          type: boolean
   |          description: Onko koulutukseen stipendiä?
   |        stipendinMaara:
   |          type: double
   |          description: Koulutuksen toteutuksen stipendin määrä.
   |          example: 10.0
   |        stipendinKuvaus:
   |          type: object
   |          description: Koulutuksen toteutuksen stipendiä tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        suunniteltuKestoVuodet:
   |          type: integer
   |          description: Koulutuksen suunniteltu kesto vuosina
   |          example: 2
   |        suunniteltuKestoKuukaudet:
   |          type: integer
   |          description: Koulutuksen suunniteltu kesto kuukausina
   |          example: 2
   |        suunniteltuKestoKuvaus:
   |          type: object
   |          description: Koulutuksen suunnitellun keston kuvaus eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'")

(def Opetus
  {:opetuskieli                                      [(->Koodi OpetuskieliKoodi)]
   (s/->OptionalKey :opetuskieletKuvaus)             Kielistetty
   :opetusaika                                       [(->Koodi OpetusaikaKoodi)]
   (s/->OptionalKey :opetusaikaKuvaus)               Kielistetty
   :opetustapa                                       [(->Koodi OpetustapaKoodi)]
   (s/->OptionalKey :opetustapaKuvaus)               Kielistetty
   :onkoMaksullinen                                  s/Bool
   (s/->OptionalKey :maksullisuusKuvaus)             Kielistetty
   (s/->OptionalKey :maksunMaara)                    s/Num
   :koulutuksenTarkkaAlkamisaika                     s/Bool
   (s/->OptionalKey :koulutuksenAlkamispaivamaara)   Datetime
   (s/->OptionalKey :koulutuksenPaattymispaivamaara) Datetime
   (s/->OptionalKey :koulutuksenAlkamiskausi)        (->Koodi AlkamiskausiKoodi)
   (s/->OptionalKey :koulutuksenAlkamisvuosi)        s/Num
   :lisatiedot                                       [KoulutusLisatieto]
   :onkoStipendia                                    s/Bool
   (s/->OptionalKey :stipendinMaara)                 s/Num
   (s/->OptionalKey :stipendinKuvaus)                Kielistetty
   :suunniteltuKestoVuodet                           s/Num
   :suunniteltuKestoKuukaudet                        s/Num
   (s/->OptionalKey :suunniteltuKestoKuvaus)         Kielistetty})

(def amm-osaamisala-schema
  "|    AmmOsaamisala:
   |      type: object
   |      properties:
   |        koodi:
   |          koodiUri:
   |            type: string
   |            description: Osaamisalan koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/osaamisala/1)
   |            example: osaamisala_0001#1
   |          nimi:
   |            type: object
   |            description: Osaamisalan nimi eri kielillä
   |            allOf:
   |              - $ref: '#/components/schemas/Nimi'
   |        linkki:
   |          type: object
   |          description: Osaamisalan linkki ePerusteisiin
   |          allOf:
   |            - $ref: '#/components/schemas/Linkki'
   |        otsikko:
   |          type: object
   |          description: Osaamisalan linkin otsikko eri kielillä
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'")

(def AmmOsaamisala
  {:koodi   (->Koodi #"osaamisala_\d+(#\d{1,2})?$")
   :linkki  Kielistetty
   :otsikko Kielistetty})

(def amm-toteutus-metadata-schema
  "|    AmmToteutusMetadata:
   |      type: object
   |      properties:
   |        koulutustyyppi:
   |          type: string
   |          description: Toteutuksen metatiedon tyyppi
   |          example: amm
   |          enum:
   |            - amm
   |        osaamisalat:
   |          type: array
   |          items:
   |            $ref: '#/components/schemas/AmmOsaamisala'
   |            description: Lista ammatillisen koulutuksen osaamisalojen kuvauksia
   |        kuvaus:
   |          type: object
   |          description: Toteutuksen kuvausteksti eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        opetus:
   |          type: object
   |          $ref: '#/components/schemas/Opetus'
   |        yhteyshenkilot:
   |          type: array
   |          description: Lista toteutuksen yhteyshenkilöistä
   |          items:
   |            $ref: '#/components/schemas/Yhteyshenkilo'
   |        asiasanat:
   |          type: array
   |          description: Lista toteutukseen liittyvistä asiasanoista, joiden avulla opiskelija voi hakea koulutusta Opintopolusta
   |          items:
   |            $ref: '#/components/schemas/Asiasana'
   |        ammattinimikkeet:
   |          type: array
   |          description: Lista toteutukseen liittyvistä ammattinimikkeistä, joiden avulla opiskelija voi hakea koulutusta Opintopolusta
   |          items:
   |            $ref: '#/components/schemas/Ammattinimike'")

(def asiasana-schema
  "|    Asiasana:
   |      type: object
   |      properties:
   |        kieli:
   |          type: string
   |          desciption: Asiasanan kieli
   |          allOf:
   |            - $ref: '#/components/schemas/Kieli'
   |          example: fi
   |        arvo:
   |          type: string
   |          description: Asiasana annetulla kielellä
   |          example: robotiikka")

(def ammattinimike-schema
  "|    Ammattinimike:
   |      type: object
   |      properties:
   |        kieli:
   |          type: string
   |          desciption: Ammattinimikkeen kieli
   |          allOf:
   |            - $ref: '#/components/schemas/Kieli'
   |          example: fi
   |        arvo:
   |          type: string
   |          description: Ammattinimike annetulla kielellä
   |          example: insinööri")

(def Keyword
  {:kieli Kieli
   :arvo s/Str})

(def AmmToteutusMetadata
  {:tyyppi Amm
   :osaamisalat [AmmOsaamisala]
   :kuvaus  Kielistetty
   :opetus Opetus
   :yhteyshenkilot [Yhteyshenkilo]
   :asiasanat [Keyword]
   :ammattinimikkeet [Keyword]})

(def schemas
  (str opetus-schema "\n"
       amm-osaamisala-schema "\n"
       amm-toteutus-metadata-schema "\n"
       asiasana-schema "\n"
       ammattinimike-schema))
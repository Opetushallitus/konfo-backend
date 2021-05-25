(ns konfo-backend.external.schema.toteutus-metadata
  (:require
   [schema.core :as s]
   [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
   [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
   [schema-tools.core :as st]))

(def apuraha-schema
  "|    Apuraha:
   |      type: object
   |      properties:
   |        min:
   |          type: int
   |          description: Apurahan minimi euromäärä tai minimi prosenttiosuus lukuvuosimaksusta
   |          example: 100
   |        max:
   |          type: int
   |          description: Apurahan maksimi euromäärä tai maksimi prosenttiosuus lukuvuosimaksusta
   |          example: 200
   |        yksikko:
   |          type: string
   |          description: Apurahan yksikkö
   |          enum:
   |            - euro
   |            - prosentti
   |          example: euro
   |        kuvaus:
   |          type: object
   |          description: Koulutuksen toteutuksen apurahaa tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'")

(def ApurahaYksikko (s/enum "euro" "prosentti"))

(def Apuraha
  {(s/->OptionalKey :min) (s/maybe s/Int)
   (s/->OptionalKey :max) (s/maybe s/Int)
   (s/->OptionalKey :yksikko) (s/maybe ApurahaYksikko)
   (s/->OptionalKey :kuvaus) (s/maybe Kielistetty)})

(def Maksullisuustyyppi (s/enum "maksullinen" "maksuton" "lukuvuosimaksu"))

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
   |          $ref: '#/components/schemas/Kuvaus'
   |        opetusaika:
   |          type: array
   |          description: Lista koulutuksen toteutuksen opetusajoista
   |          items:
   |            type: object
   |            $ref: '#/components/schemas/Opetusaika'
   |        opetusaikaKuvaus:
   |          type: object
   |          description: Koulutuksen toteutuksen opetusaikoja tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        opetustapa:
   |          type: array
   |          description: Lista koulutuksen toteutuksen opetustavoista
   |          items:
   |            type: object
   |            $ref: '#/components/schemas/Opetustapa'
   |        opetustapaKuvaus:
   |          type: object
   |          description: Koulutuksen toteutuksen opetustapoja tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        maksullisuustyyppi:
   |          type: string
   |          description: Maksullisuuden tyyppi
   |          enum:
   |            - 'maksullinen'
   |            - 'maksuton'
   |            - 'lukuvuosimaksu'
   |        maksullisuusKuvaus:
   |          type: object
   |          description: Koulutuksen toteutuksen maksullisuutta tarkentava kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        maksunMaara:
   |          type: double
   |          description: Koulutuksen toteutuksen maksun määrä euroissa?
   |          example: 220.50
   |        koulutuksenAlkamiskausi:
   |          type: object
   |          description: Koulutuksen alkamiskausi
   |          $ref: '#/components/schemas/KoulutuksenAlkamiskausi'
   |        lisatiedot:
   |          type: array
   |          description: Koulutuksen toteutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
   |          items:
   |            type: object
   |            $ref: '#/components/schemas/KoulutusLisatieto'
   |        onkoApuraha:
   |          type: boolean
   |          description: Onko koulutukseen apurahaa?
   |        apuraha:
   |          type: object
   |          description: Koulutuksen apurahatiedot
   |          $ref: '#/components/schemas/Apuraha'
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
   |          $ref: '#/components/schemas/Kuvaus'")

(def Opetus
  {:opetuskieli                                   [(->Koodi OpetuskieliKoodi)]
   (s/->OptionalKey :opetuskieletKuvaus)          Kielistetty
   :opetusaika                                    [(->Koodi OpetusaikaKoodi)]
   (s/->OptionalKey :opetusaikaKuvaus)            Kielistetty
   :opetustapa                                    [(->Koodi OpetustapaKoodi)]
   (s/->OptionalKey :opetustapaKuvaus)            Kielistetty
   (s/->OptionalKey :maksullisuustyyppi)          Maksullisuustyyppi
   (s/->OptionalKey :maksullisuusKuvaus)          Kielistetty
   (s/->OptionalKey :maksunMaara)                 s/Num
   (s/->OptionalKey :koulutuksenAlkamiskausi)     (s/maybe KoulutuksenAlkamiskausi)
   :lisatiedot                                    [KoulutusLisatieto]
   :onkoApuraha                                   s/Bool
   (s/->OptionalKey :apuraha)                     (s/maybe Apuraha)
   (s/->OptionalKey :suunniteltuKestoVuodet)      s/Num
   (s/->OptionalKey :suunniteltuKestoKuukaudet)   s/Num
   (s/->OptionalKey :suunniteltuKestoKuvaus)      Kielistetty})

(def korkeakoulu-osaamisala-schema
  "|    KorkeakouluOsaamisala:
   |      type: object
   |      properties:
   |        nimi:
   |          type: object
   |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. nimi
   |          $ref: '#/components/schemas/Nimi'
   |        kuvaus:
   |          type: object
   |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. kuvaus
   |          $ref: '#/components/schemas/Kuvaus'
   |        linkki:
   |          type: object
   |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkki
   |          $ref: '#/components/schemas/Linkki'
   |        otsikko:
   |          type: object
   |          description: Korkeakoulututkinnon erikoistumisalan, opintosuunnan, pääaineen tms. linkin otsikko
   |          $ref: '#/components/schemas/Teksti'")

(def KorkeakouluOsaamisala
  {:nimi    Kielistetty
   :kuvaus  Kielistetty
   :linkki  Kielistetty
   :otsikko Kielistetty})

(def toteutus-metadata-schema
  "|    ToteutusMetadata:
   |      type: object
   |      properties:
   |        kuvaus:
   |          type: object
   |          description: Toteutuksen kuvausteksti eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
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

(def korkeakoulu-toteutus-metadata-schema
  "|    KorkeakouluToteutusMetadata:
   |      allOf:
   |        - $ref: '#/components/schemas/ToteutusMetadata'
   |      properties:
   |        alemmanKorkeakoulututkinnonOsaamisalat:
   |          type: array
   |          description: Lista alemman korkeakoulututkinnon erikoistumisalojen, opintosuuntien, pääaineiden tms. kuvauksista.
   |          items:
   |            $ref: '#/components/schemas/KorkeakouluOsaamisala'
   |        ylemmanKorkeakoulututkinnonOsaamisalat:
   |          type: array
   |          items:
   |            $ref: '#/components/schemas/KorkeakouluOsaamisala'
   |          description: Lista ylemmän korkeakoulututkinnon erikoistumisalojen, opintosuuntien, pääaineiden tms. kuvauksista.")

(def yliopisto-toteutus-metadata-schema
  "|    YliopistoToteutusMetadata:
   |      allOf:
   |        - $ref: '#/components/schemas/KorkeakouluToteutusMetadata'
   |        - type: object
   |          properties:
   |            koulutustyyppi:
   |              type: string
   |              description: Koulutuksen metatiedon tyyppi
   |              example: yo
   |              enum:
   |                - yo")

(def ammattikorkea-toteutus-metadata-schema
  "|    AmmattikorkeaToteutusMetadata:
   |      allOf:
   |        - $ref: '#/components/schemas/KorkeakouluToteutusMetadata'
   |        - type: object
   |          properties:
   |            koulutustyyppi:
   |              type: string
   |              description: Koulutuksen metatiedon tyyppi
   |              example: amk
   |              enum:
   |                - amk")

(def lukio-toteutus-metadata-schema
  "|    LukioToteutusMetadata:
   |      allOf:
   |        - $ref: '#/components/schemas/ToteutusMetadata'
   |        - type: object
   |          properties:
   |            koulutustyyppi:
   |              type: string
   |              description: Koulutuksen metatiedon tyyppi
   |              example: lk
   |              enum:
   |                - lk")

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
   |            $ref: '#/components/schemas/Nimi'
   |        linkki:
   |          type: object
   |          description: Osaamisalan linkki ePerusteisiin
   |          $ref: '#/components/schemas/Linkki'
   |        otsikko:
   |          type: object
   |          description: Osaamisalan linkin otsikko eri kielillä
   |          $ref: '#/components/schemas/Teksti'")

(def AmmOsaamisala
  {:koodi   (->Koodi #"osaamisala_\d+(#\d{1,2})?$")
   :linkki  Kielistetty
   :otsikko Kielistetty})

(def amm-toteutus-metadata-schema
  "|    AmmatillinenToteutusMetadata:
   |      allOf:
   |        - $ref: '#/components/schemas/ToteutusMetadata'
   |        - type: object
   |          properties:
   |            osaamisalat:
   |              type: array
   |              items:
   |                $ref: '#/components/schemas/AmmOsaamisala'
   |              description: Lista ammatillisen koulutuksen osaamisalojen kuvauksia
   |            ammatillinenPerustutkintoErityisopetuksena:
   |              type: boolean
   |              description: Onko koulutuksen tyyppi \"Ammatillinen perustutkinto erityisopetuksena\"?
   |            koulutustyyppi:
   |              type: string
   |              description: Koulutuksen metatiedon tyyppi
   |              example: amm
   |              enum:
   |                - amm")

(def asiasana-schema
  "|    Asiasana:
   |      type: object
   |      properties:
   |        kieli:
   |          type: string
   |          desciption: Asiasanan kieli
   |          $ref: '#/components/schemas/Kieli'
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
   |          $ref: '#/components/schemas/Kieli'
   |          example: fi
   |        arvo:
   |          type: string
   |          description: Ammattinimike annetulla kielellä
   |          example: insinööri")

(def Keyword
  {:kieli Kieli
   :arvo s/Str})

(def ToteutusMetadata
  {:kuvaus  Kielistetty
   :opetus Opetus
   :yhteyshenkilot [Yhteyshenkilo]
   :asiasanat [Keyword]
   :ammattinimikkeet [Keyword]})

(def KorkeakoulutusToteutusMetadata
  (st/merge
   {:alemmanKorkeakoulututkinnonOsaamisalat [KorkeakouluOsaamisala]
    :ylemmanKorkeakoulututkinnonOsaamisalat [KorkeakouluOsaamisala]}
   ToteutusMetadata))

(def AmmToteutusMetadata
  (st/merge
   {:tyyppi Amm
    :ammatillinenPerustutkintoErityisopetuksena s/Bool
    :osaamisalat [AmmOsaamisala]}
   ToteutusMetadata))

(def YoToteutusMetadata
  (st/merge
   {:tyyppi Yo}
   KorkeakoulutusToteutusMetadata))

(def LukioToteutusMetadata
  (st/merge
    {:tyyppi Lk}
    ToteutusMetadata))

(def AmkToteutusMetadata
  (st/merge
   {:tyyppi Amk}
   KorkeakoulutusToteutusMetadata))

(def schemas
  (str apuraha-schema "\n"
       opetus-schema "\n"
       amm-osaamisala-schema "\n"
       amm-toteutus-metadata-schema "\n"
       asiasana-schema "\n"
       ammattinimike-schema "\n"
       korkeakoulu-osaamisala-schema "\n"
       toteutus-metadata-schema "\n"
       lukio-toteutus-metadata-schema "\n"
       korkeakoulu-toteutus-metadata-schema "\n"
       yliopisto-toteutus-metadata-schema "\n"
       ammattikorkea-toteutus-metadata-schema))

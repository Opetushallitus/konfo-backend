(ns konfo-backend.external.schema.koulutus-metadata
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
    [schema-tools.core :as st]))

(def schemas
  " |    AmmatillinenKoulutusMetadata:
    |      type: object
    |      properties:
    |        koulutustyyppi:
    |          type: string
    |          description: Koulutuksen metatiedon tyyppi
    |          example: amm
    |          enum:
    |            - amm
    |        kuvaus:
    |          type: object
    |          description: Koulutuksen kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
    |          allOf:
    |            - $ref: '#/components/schemas/Kuvaus'
    |        lisatiedot:
    |          type: array
    |          description: Koulutukseen liittyviä lisätietoja, jotka näkyvät oppijalle
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/KoulutusLisatieto'
    |        tutkintonimike:
    |          type: array
    |          description: Lista koulutuksen tutkintonimikkeistä
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/Tutkintonimike'
    |        koulutusala:
    |          type: array
    |          description: Lista koulutuksen koulutusaloista
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/Koulutusala1'
    |        opintojenLaajuus:
    |          type: object
    |          $ref: '#/components/schemas/OpintojenLaajuus'
    |        opintojenLaajuusyksikko:
    |          type: object
    |          $ref: '#/components/schemas/OpintojenLaajuusyksikko'
    |    KorkeakouluMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/KoulutusMetadata'
    |      properties:
    |        kuvauksenNimi:
    |          type: object
    |          description: Koulutuksen kuvaukseni nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
    |          allOf:
    |            - $ref: '#/components/schemas/Nimi'
    |        tutkintonimikeKoodiUrit:
    |          type: array
    |          description: Lista koulutuksen tutkintonimikkeistä. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/tutkintonimikekk/2)
    |          items:
    |            type: string
    |          example:
    |            - tutkintonimikekk_110#2
    |            - tutkintonimikekk_111#2
    |        opintojenLaajuusKoodiUri:
    |          type: string
    |          description: Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)
    |          example: opintojenlaajuus_40#1
    |    YliopistoKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/KorkeakouluMetadata'
    |        - type: object
    |          properties:
    |            koulutustyyppi:
    |              type: string
    |              description: Koulutuksen metatiedon tyyppi
    |              example: yo
    |              enum:
    |                - yo
    |    AmmattikorkeaKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/KorkeakouluMetadata'
    |        - type: object
    |          properties:
    |            koulutustyyppi:
    |              type: string
    |              description: Koulutuksen metatiedon tyyppi
    |              example: amk
    |              enum:
    |                - amk
    |"
  )

(def KkMetadata
  {:kuvaus                             Kielistetty
   :kuvauksenNimi                      Kielistetty
   :lisatiedot                         [KoulutusLisatieto]
   :koulutusala                        [(->Koodi Koulutusala2Koodi)]
   (s/->OptionalKey :tutkintonimike)   [(->Koodi TutkintonimikeKkKoodi)]
   (s/->OptionalKey :opintojenLaajuus) (->Koodi OpintojenLaajuusKoodi)})

(def AmmKoulutusMetadata
  {:tyyppi                                    Amm
   :kuvaus                                    Kielistetty
   :lisatiedot                                [KoulutusLisatieto]
   :koulutusala                               [(->Koodi Koulutusala1Koodi)]
   (s/->OptionalKey :tutkintonimike)          [(->Koodi TutkintonimikeKoodi)]
   (s/->OptionalKey :opintojenLaajuus)        (s/maybe (->Koodi OpintojenLaajuusKoodi))
   (s/->OptionalKey :opintojenLaajuusyksikko) (s/maybe (->Koodi OpintojenLaajuusyksikkoKoodi))})

(def AmkMetadata
  (st/merge
    {:tyyppi Amk}
    KkMetadata))

(def YoMetadata
  (st/merge
    {:tyyppi Yo}
    KkMetadata))

(comment def LkMetadata
         (st/merge
           {:tyyppi Lk}
           KkMetadata))

(comment def MuuMetadata
         (st/merge
           {:tyyppi Muu}
           KkMetadata))

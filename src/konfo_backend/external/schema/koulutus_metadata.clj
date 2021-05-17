(ns konfo-backend.external.schema.koulutus-metadata
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
    [schema-tools.core :as st]))

(def schemas
   "|    Eperuste:
    |      type: object
    |      properties:
    |        id:
    |          type: integer
    |          example: 12345
    |          description: ePerusteen id
    |        diaarinumero:
    |          type: string
    |          example: 1234-OPH-2021
    |          description: ePerusteen diaarinumero
    |        voimassaoloLoppuu:
    |          type: string
    |          example: ePerusten voimassaolon loppumishetki
    |          description: 2021-12-12T00:00:00
    |    AmmatillinenKoulutusMetadata:
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
    |          $ref: '#/components/schemas/Kuvaus'
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
    |        eperuste:
    |          type: object
    |          $ref: '#/components/schemas/Eperuste'
    |    KorkeakouluMetadata:
    |      type: object
    |      properties:
    |        kuvaus:
    |          type: object
    |          description: Koulutuksen kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
    |          $ref: '#/components/schemas/Kuvaus'
    |        lisatiedot:
    |          type: array
    |          description: Koulutukseen liittyviä lisätietoja, jotka näkyvät oppijalle
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/KoulutusLisatieto'
    |        kuvauksenNimi:
    |          type: object
    |          description: Koulutuksen kuvaukseni nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
    |          $ref: '#/components/schemas/Nimi'
    |        tutkintonimike:
    |          type: array
    |          items:
    |            #ref: '#/components/schemas/Tutkintonimike'
    |        opintojenLaajuus:
    |          type: object
    |          properties:
    |            koodiUri:
    |              type: string
    |              example: opintojenlaajuus_40
    |              description: Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)
    |            nimi:
    |              type: object
    |              description: Tutkinnon laajuuden eri kielillä.
    |              example: {\"fi\": \"Tutkinnon laajuus suomeksi\"}
    |              $ref: '#/components/schemas/Teksti'
    |        opintojenLaajuusyksikko:
    |          type: object
    |          $ref: '#/components/schemas/OpintojenLaajuusyksikko'
    |        eperuste:
    |          type: object
    |          $ref: '#/components/schemas/Eperuste'
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
    |    LukioKoulutusMetadata:
    |      type: object
    |      properties:
    |        koulutustyyppi:
    |          type: string
    |          description: Koulutuksen metatiedon tyyppi
    |          example: lk
    |          enum:
    |            - lk
    |        kuvaus:
    |          type: object
    |          description: Koulutuksen kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
    |          $ref: '#/components/schemas/Kuvaus'
    |        lisatiedot:
    |          type: array
    |          description: Koulutukseen liittyviä lisätietoja, jotka näkyvät oppijalle
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/KoulutusLisatieto'
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
    |        tutkintonimike:
    |          type: array
    |          description: Lista koulutuksen tutkintonimikkeistä
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/Tutkintonimike'
    |"
  )

(def Eperuste
  {(s/->OptionalKey :id) s/Int
   (s/->OptionalKey :diaarinumero) s/Str
   (s/->OptionalKey :voimassaoloLoppuu) s/Str })

(def KkMetadata
  {:kuvaus                             Kielistetty
   :kuvauksenNimi                      Kielistetty
   :lisatiedot                         [KoulutusLisatieto]
   :koulutusala                        [(->Koodi Koulutusala2Koodi)]
   (s/->OptionalKey :tutkintonimike)   [(s/maybe (->Koodi TutkintonimikeKkKoodi))]
   (s/->OptionalKey :opintojenLaajuus) (s/maybe (->Koodi OpintojenLaajuusKoodi))
   :opintojenLaajuusyksikko            (->Koodi OpintojenLaajuusyksikkoKoodi)
   (s/->OptionalKey :eperuste)         (s/maybe Eperuste)})

(def AmmKoulutusMetadata
  {:tyyppi                                    Amm
   :kuvaus                                    Kielistetty
   :lisatiedot                                [KoulutusLisatieto]
   :koulutusala                               [(->Koodi Koulutusala1Koodi)]
   (s/->OptionalKey :tutkintonimike)          [(->Koodi TutkintonimikeKoodi)]
   (s/->OptionalKey :opintojenLaajuus)        (s/maybe (->Koodi OpintojenLaajuusKoodi))
   (s/->OptionalKey :opintojenLaajuusNumero)  (s/maybe s/Int)
   (s/->OptionalKey :opintojenLaajuusyksikko) (s/maybe (->Koodi OpintojenLaajuusyksikkoKoodi))
   (s/->OptionalKey :eperuste)                (s/maybe Eperuste)})

(def AmkMetadata
  (st/merge
    {:tyyppi Amk}
    KkMetadata))

(def YoMetadata
  (st/merge
    {:tyyppi Yo}
    KkMetadata))

(def LukioKoulutusMetadata
  {:tyyppi                                    Lk
   :kuvaus                                    Kielistetty
   :lisatiedot                                [KoulutusLisatieto]
   :koulutusala                               [(->Koodi Koulutusala1Koodi)]
   (s/->OptionalKey :opintojenLaajuus)        (s/maybe (->Koodi OpintojenLaajuusKoodi))
   :opintojenLaajuusyksikko                   (->Koodi OpintojenLaajuusyksikkoKoodi)
   :tutkintonimike                            (->Koodi TutkintonimikeKoodi)})

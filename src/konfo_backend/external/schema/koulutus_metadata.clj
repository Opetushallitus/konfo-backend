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
    |    KoulutusMetadata:
    |      type: object
    |      properties:
    |        kuvaus:
    |          type: object
    |          description: Koulutuksen kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
    |          $ref: '#/components/schemas/Kuvaus'
    |        lisatiedot:
    |          type: array
    |          description: Koulutukseen liittyviä lisätietoja, jotka näkyvät oppijalle Opintopolussa
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/KoulutusLisatieto'
    |    TutkintoonJohtavaKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/KoulutusMetadata'
    |      type: object
    |      properties:
    |        tutkintonimike:
    |          type: array
    |          items:
    |            $ref: '#/components/schemas/Tutkintonimike'
    |        opintojenLaajuus:
    |          $ref: '#/components/schemas/OpintojenLaajuus'
    |        opintojenLaajuusyksikko:
    |          $ref: '#/components/schemas/OpintojenLaajuusyksikko'
    |    AmmatillinenKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/TutkintoonJohtavaKoulutusMetadata'
    |      type: object
    |      properties:
    |        tyyppi:
    |          type: string
    |          description: Koulutuksen metatiedon tyyppi
    |          example: amm
    |          enum:
    |            - amm
    |        eperuste:
    |          type: object
    |          $ref: '#/components/schemas/Eperuste'
    |    YliopistoKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/TutkintoonJohtavaKoulutusMetadata'
    |      type: object
    |      properties:
    |        tyyppi:
    |          type: string
    |          description: Koulutuksen metatiedon tyyppi
    |          example: yo
    |          enum:
    |            - yo
    |    AmmattikorkeaKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/TutkintoonJohtavaKoulutusMetadata'
    |      type: object
    |      properties:
    |        tyyppi:
    |          type: string
    |          description: Koulutuksen metatiedon tyyppi
    |          example: amk
    |          enum:
    |            - amk
    |    LukioKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/TutkintoonJohtavaKoulutusMetadata'
    |      type: object
    |      properties:
    |        tyyppi:
    |          type: string
    |          description: Koulutuksen metatiedon tyyppi
    |          example: lk
    |          enum:
    |            - lk
    |    ErikoislaakariKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/KoulutusMetadata'
    |      type: object
    |      properties:
    |        opintojenLaajuusNumero:
    |          type: integer
    |          description: Opintojen laajuus numeroarvona
    |          example: 10
    |        opintojenLaajuusyksikko:
    |          $ref: '#/components/schemas/OpintojenLaajuusyksikko'
    |        koulutusala:
    |          type: array
    |          description: Lista koulutuksen koulutusaloista
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/Koulutusala1'
    |    KkOpintojaksoKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/KoulutusMetadata'
    |      type: object
    |      properties:
    |        opintojenLaajuusNumero:
    |          type: integer
    |          description: Opintojen laajuus numeroarvona
    |          example: 10
    |        opintojenLaajuusyksikko:
    |          $ref: '#/components/schemas/OpintojenLaajuusyksikko'
    |        koulutusala:
    |          type: array
    |          description: Lista koulutuksen koulutusaloista
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/Koulutusala2'
    |    KkOpintokokonaisuusKoulutusMetadata:
    |      allOf:
    |        - $ref: '#/components/schemas/KoulutusMetadata'
    |      type: object
    |      properties:
    |        opintojenLaajuusNumeroMin:
    |          type: integer
    |          description: Opintojen laajuuden tai keston vähimmäismäärä numeroarvona
    |          example: 10
    |        opintojenLaajuusNumeroMax:
    |          type: integer
    |          description: Opintojen laajuuden tai keston enimmäismäärä numeroarvona
    |          example: 20
    |        opintojenLaajuusyksikko:
    |          $ref: '#/components/schemas/OpintojenLaajuusyksikko'
    |        koulutusala:
    |          type: array
    |          description: Lista koulutuksen koulutusaloista
    |          items:
    |            type: object
    |            $ref: '#/components/schemas/Koulutusala1'
    |")
  

(def Eperuste
  {(s/->OptionalKey :id) s/Int
   (s/->OptionalKey :diaarinumero) s/Str
   (s/->OptionalKey :voimassaoloLoppuu) (s/maybe s/Str)})

 (def KoulutusMetadata
   {:kuvaus                             Kielistetty
    :lisatiedot [KoulutusLisatieto]})

(def TutkintoonJohtavaKoulutusMetadata
  (st/merge KoulutusMetadata {
   :koulutusala                        [(->Koodi Koulutusala2Koodi)]
   (s/->OptionalKey :tutkintonimike)   [(s/maybe (->Koodi TutkintonimikeKoodi))]
   (s/->OptionalKey :opintojenLaajuus) (s/maybe (->Koodi OpintojenLaajuusKoodi))
   :opintojenLaajuusyksikko            (->Koodi OpintojenLaajuusyksikkoKoodi)
   s/Any                               s/Any}))

(def AmmKoulutusMetadata
  (st/merge 
   TutkintoonJohtavaKoulutusMetadata  
   {:tyyppi                                    Amm 
    (s/->OptionalKey :opintojenLaajuusNumero)  (s/maybe s/Any) 
    (s/->OptionalKey :eperuste)                (s/maybe Eperuste) 
    :koulutusala                        [(->Koodi Koulutusala1Koodi)] 
    s/Any                                      s/Any}))

(def AmkKoulutusMetadata
  (st/merge
   TutkintoonJohtavaKoulutusMetadata 
   {:tyyppi Amk
    (s/->OptionalKey :tutkintonimike)   [(s/maybe (->Koodi TutkintonimikeKkKoodi))]}))

(def AmmOpeErityisopeJaOpoKoulutusMetadata
  (st/merge
    TutkintoonJohtavaKoulutusMetadata
    {:tyyppi AmmOpeErityisopeJaOpo}))

(def YoKoulutusMetadata
  (st/merge 
   TutkintoonJohtavaKoulutusMetadata
   {:tyyppi Yo
    (s/->OptionalKey :tutkintonimike)   [(s/maybe (->Koodi TutkintonimikeKkKoodi))]}))

(def LukioKoulutusMetadata
  (st/merge 
   TutkintoonJohtavaKoulutusMetadata
   {:tyyppi Lk
    :koulutusala                        [(->Koodi Koulutusala1Koodi)]}))

(def ErikoislaakariKoulutusMetadata
  (st/merge
   KoulutusMetadata
   {:tyyppi KkOpintojakso
   (s/->OptionalKey :tutkintonimike)   [(s/maybe (->Koodi TutkintonimikeKkKoodi))]
    }))

(def KkOpintojaksoKoulutusMetadata
  (st/merge
   TutkintoonJohtavaKoulutusMetadata
   {:tyyppi KkOpintojakso}))

(def KkOpintokokonaisuusKoulutusMetadata
  (st/merge 
   KoulutusMetadata
   {:tyyppi                                    KkOpintokokonaisuus 
    (s/->OptionalKey :opintojenLaajuusNumeroMin) (s/maybe s/Num) 
    (s/->OptionalKey :opintojenLaajuusNumeroMax) (s/maybe s/Num) 
    (s/->OptionalKey :opintojenLaajuusyksikko)  (->Koodi OpintojenLaajuusyksikkoKoodi)}))

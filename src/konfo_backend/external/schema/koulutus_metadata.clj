(ns konfo-backend.external.schema.koulutus-metadata
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]))

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
    |        eperuste:
    |          type: object
    |          $ref: '#/components/schemas/Eperuste'
    |        isMuokkaajaOphVirkailija:
    |          type: boolean
    |          example: true
    |          description: Onko viimeisin muokkaaja OPH:n virkailija
    |        koulutusala:
    |          type: array
    |          description: Lista koulutuksen koulutusaloista
    |          items:
    |            type: object
    |            anyOf:
    |             - $ref: '#/components/schemas/Koulutusala1'
    |             - $ref: '#/components/schemas/Koulutusala2'
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
    |        opintojenLaajuus:
    |          $ref: '#/components/schemas/OpintojenLaajuus'
    |        opintojenLaajuusNumero:
    |          type: integer
    |          description: Opintojen laajuus numeroarvona
    |          example: 10
    |        opintojenLaajuusNumeroMin:
    |          type: integer
    |          description: Opintojen laajuuden tai keston vähimmäismäärä numeroarvona\n
    |          example: 10
    |        opintojenLaajuusNumeroMax:
    |          type: integer
    |          description: Opintojen laajuuden tai keston enimmäismäärä numeroarvona
    |          example: 20
    |        opintojenLaajuusyksikko:
    |          $ref: '#/components/schemas/OpintojenLaajuusyksikko'
    |        tutkintonimike:
    |          type: array
    |          items:
    |            $ref: '#/components/schemas/Tutkintonimike'
    |")
  

(def Eperuste
  {(s/->OptionalKey :id) s/Int
   (s/->OptionalKey :diaarinumero) s/Str
   (s/->OptionalKey :voimassaoloLoppuu) (s/maybe s/Str)})

 (def KoulutusMetadata
   {
    (s/->OptionalKey :eperuste)                   (s/maybe Eperuste)
    (s/->OptionalKey :isMuokkaajaOphVirkailija)   (s/maybe Boolean)
    (s/->OptionalKey :koulutusala)                [(s/conditional
                                                     #(boolean (re-find Koulutusala1Koodi (:koodiUri %))) (s/maybe (->Koodi Koulutusala1Koodi))
                                                     #(boolean (re-find Koulutusala2Koodi (:koodiUri %))) (s/maybe (->Koodi Koulutusala2Koodi)))]
    (s/->OptionalKey :kuvaus)                     (s/maybe Kielistetty)
    (s/->OptionalKey :lisatiedot)                 [(s/maybe KoulutusLisatieto)]
    (s/->OptionalKey :opintojenLaajuus)           (s/maybe (->Koodi OpintojenLaajuusKoodi))
    (s/->OptionalKey :opintojenLaajuusNumero)     (s/maybe s/Num)
    (s/->OptionalKey :opintojenLaajuusNumeroMin)  (s/maybe s/Num)
    (s/->OptionalKey :opintojenLaajuusNumeroMax)  (s/maybe s/Num)
    (s/->OptionalKey :opintojenLaajuusyksikko)    (->Koodi OpintojenLaajuusyksikkoKoodi)
    (s/->OptionalKey :tutkintonimike)             Tutkintonimikkeet 
    :tyyppi                                       KoutaKoulutustyyppi
    s/Any                                         s/Any
    })

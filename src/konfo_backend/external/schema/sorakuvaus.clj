(ns konfo-backend.external.schema.sorakuvaus
  (:require
   [schema.core :as s]
   [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
   [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]))

(def sorakuvaus-schema
  "|    Sorakuvaus:
   |      type: object
   |      properties:
   |        id:
   |          type: string
   |          description: SORA-kuvauksen yksilöivä tunniste
   |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
   |        koulutustyyppi:
   |          type: string
   |          $ref: '#/components/schemas/KoutaKoulutustyyppi'
   |        tila:
   |          type: string
   |          example: julkaistu
   |          enum:
   |            - julkaistu
   |          description: SORA-kuvauksen julkaisutila. Aina julkaistu.
   |        kielivalinta:
   |          type: array
   |          description: Kielet, joille SORA-kuvauksen nimi, kuvailutiedot ja muut tekstit on käännetty
   |          items:
   |            $ref: '#/components/schemas/Kieli'
   |          example:
   |            - fi
   |            - sv
   |        nimi:
   |          type: object
   |          description: SORA-kuvauksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty SORA-kuvauksen kielivalinnassa.
   |          $ref: '#/components/schemas/Nimi'
   |        metadata:
   |          type: object
   |          description: SORA-kuvauksen kuvailutiedot eri kielillä
   |          properties:
   |            kuvaus:
   |              type: object
   |              description: SORA-kuvauksen kuvausteksti eri kielillä. Kielet on määritetty kuvauksen kielivalinnassa.
   |              $ref: '#/components/schemas/Kuvaus'
   |            koulutus:
   |              type: array
   |              description: Koulutusten koodi urit ja nimet
   |              items:
   |                type: object
   |                $ref: '#/components/schemas/KoulutusKoodi'
   |            koulutusala:
   |              type: object
   |              description: Koulutusalan koodi URI ja nimi.
   |              oneOf:
   |                - $ref: '#/components/schemas/Koulutusala1'
   |                - $ref: '#/components/schemas/Koulutusala2'
   |        organisaatio:
   |          type: object
   |          description: Valintaperustekuvauksen luoneen organisaation oid
   |          $ref: '#/components/schemas/Organisaatio'
   |        modified:
   |          type: string
   |          format: date-time
   |          description: SORA-kuvauksen viimeisin muokkausaika. Järjestelmän generoima
   |          example: 2019-08-23T09:55")

(def Sorakuvaus
  {:id s/Str
   (s/->OptionalKey :externalId) s/Str
   :koulutustyyppi KoutaKoulutustyyppi
   :tila Julkaistu
   :kielivalinta [Kieli]
   :nimi Kielistetty
   :metadata {(s/optional-key :kuvaus) Kielistetty
              (s/optional-key :koulutus) [(->Koodi KoulutusKoodi)]
              (s/optional-key :koulutusala) (s/if #(re-matches Koulutusala1Koodi (:koodiUri %))
                                              (->Koodi Koulutusala1Koodi)
                                              (->Koodi Koulutusala2Koodi))
              s/Any s/Any}
   :organisaatio Organisaatio
   :modified Datetime
   s/Any s/Any})

(def schemas
  sorakuvaus-schema)

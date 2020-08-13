(ns konfo-backend.external.schema.liite
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]))

(def LiitteenToimitustapa (s/enum "hakijapalvelu" "osoite" "lomake"))

(def liitteen-toimitusosoite-schema
  "|    LiitteenToimitusosoite:
   |      type: object
   |      properties:
   |        osoite:
   |          type: object
   |          description: Liitteen toimitusosoite
   |          allOf:
   |            - $ref: '#/components/schemas/Osoite'
   |        sahkoposti:
   |          type: object
   |          description: Sähköpostiosoite, johon liite voidaan toimittaa
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'")

(def LiitteenToimitusosoite
  {:sahkoposti s/Str
   :osoite     Osoite})

(def liite-schema
  "    Liite:
      |      type: object
      |      properties:
      |        id:
      |          type: string
      |          description: Liitteen yksilöivä tunniste. Järjestelmän generoima.
      |          example: ea596a9c-5940-497e-b5b7-aded3a2352a7
      |        tyyppi:
      |          type: object
      |          description: Liitteen tyyppi.
      |          $ref: '#/components/schemas/LiitteenTyyppi'
      |        nimi:
      |          type: object
      |          description: Liitteen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Nimi'
      |        kuvaus:
      |          type: object
      |          description: Liitteen Opintopolussa näytettävä kuvaus eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
      |          allOf:
      |            - $ref: '#/components/schemas/Kuvaus'
      |        toimitusaika:
      |          type: string
      |          description: Liitteen toimitusaika, jos ei ole sama kuin kaikilla hakukohteen liitteillä
      |          format: date-time
      |          example: 2019-08-23T09:55
      |        toimitustapa:
      |          type: string
      |          description: Liitteen toimitustapa, jos ei ole sama kuin kaikilla hakukohteen liitteillä
      |          example: hakijapalvelu
      |          enum:
      |            - hakijapalvelu
      |            - osoite
      |            - lomake
      |        toimitusosoite:
      |          type: object
      |          description: Liitteen toimitusosoite, jos ei ole sama kuin kaikilla hakukohteen liitteillä
      |          allOf:
      |            - $ref: '#/components/schemas/LiitteenToimitusosoite'")

(def Liite
  {:id             s/Str                                    ;s/Uuid
   :tyyppi         (->Koodi LiitteenTyyppiKoodi)
   :nimi           Kielistetty
   :kuvaus         Kielistetty
   (s/->OptionalKey :toimitusaika)   Datetime
   (s/->OptionalKey :toimitustapa)   LiitteenToimitustapa
   (s/->OptionalKey :toimitusosoite) LiitteenToimitusosoite})

(def schemas
  (str liitteen-toimitusosoite-schema "\n"
       liite-schema))
(ns konfo-backend.external.schema.koulutus
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.koulutus-metadata :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.sorakuvaus :refer :all :exclude [schemas]]))


(def schemas
  (str
    "|    Koulutus:
     |      type: object
     |      properties:
     |        oid:
     |          type: string
     |          description: Koulutuksen yksilöivä tunniste
     |          example: 1.2.246.562.13.00000000000000000009
     |        johtaaTutkintoon:
     |          type: boolean
     |          description: Onko koulutus tutkintoon johtavaa
     |        koulutustyyppi:
     |          type: string
     |          $ref: '#/components/schemas/KoutaKoulutustyyppi'
     |        koulutukset:
     |          type: array
     |          description: Koulutusten koodi URIt ja nimet
     |          items:
     |            $ref: '#/components/schemas/KoulutusKoodi'
     |        tila:
     |          type: string
     |          example: \"julkaistu\"
     |          enum:
     |            - julkaistu
     |          description: Koulutuksen julkaisutila. Aina 'julkaistu'
     |        tarjoajat:
     |          type: array
     |          description: Koulutusta tarjoavat organisaatiot
     |          items:
     |            $ref: '#/components/schemas/Organisaatio'
     |        kielivalinta:
     |          type: array
     |          description: Kielet, joille koulutuksen nimi, kuvailutiedot ja muut tekstit on käännetty.
     |            Kaikkia tietoja (esim. organisaatioiden nimiä) ei välttämättä ole käännetty kaikille kielille.
     |          items:
     |            $ref: '#/components/schemas/Kieli'
     |          example:
     |            - fi
     |            - sv
     |        nimi:
     |          type: object
     |          description: Koulutuksen näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
     |          $ref: '#/components/schemas/Nimi'
     |        metadata:
     |          type: object
     |          $ref: '#/components/schemas/KoulutusMetadata'
     |        organisaatio:
     |          type: object
     |          description: Koulutuksen luonut organisaatio
     |          $ref: '#/components/schemas/Organisaatio'
     |        teemakuva:
     |          type: string
     |          description: Koulutuksen teemakuvan URL.
     |          example: https://konfo-files.opintopolku.fi/koulutus-teemakuva/1.2.246.562.13.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
     |        ePerusteId:
     |          type: number
     |          description: Ammatillisen koulutuksen ePerusteen id.
     |          example: 4804100
     |        sorakuvaus:
     |          type: object
     |          description: Koulutukseen liittyvä SORA-kuvaus
     |          $ref: '#/components/schemas/Sorakuvaus'
     |        modified:
     |          type: string
     |          format: date-time
     |          description: Koulutuksen viimeisin muokkausaika
     |          example: 2019-08-23T09:55
     |        timestamp:
     |          type: number
     |          description: Koulutuksen viimeisin indeksointiaika
     |          example: 1587537927174"))

(s/defschema Koulutus
  {:oid                          KoulutusOid
   (s/->OptionalKey :externalId) s/Str
   :johtaaTutkintoon             s/Bool
   :koulutustyyppi               KoutaKoulutustyyppi
   :koulutukset                  [(->Koodi KoulutusKoodi)]
   :tila                         Julkaistu
   :tarjoajat                    [Organisaatio]
   :kielivalinta                 [Kieli]
   :nimi                         Kielistetty
   :metadata                     (s/maybe KoulutusMetadata)
   :organisaatio                 Organisaatio
   (s/->OptionalKey :teemakuva)  Url
   (s/->OptionalKey :ePerusteId) s/Int
   (s/->OptionalKey :sorakuvaus) (s/maybe Sorakuvaus)
   :modified                     Datetime
   :timestamp                    s/Int
   s/Any s/Any})

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
     |          description: \"Koulutuksen tyyppi. Sallitut arvot: 'amm' (ammatillinen), 'yo' (yliopisto), 'lk' (lukio), 'amk' (ammattikorkea), 'muu' (muu koulutus)\"
     |          enum:
     |            - amm
     |            - yo
     |            - amk
     |            - lk
     |            - muu
     |          example: amm
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
     |          oneOf:
     |            - $ref: '#/components/schemas/AmmatillinenKoulutusMetadata'
     |          example:
     |            koulutustyyppi: amm
     |            koulutusalaKoodiUrit:
     |              - kansallinenkoulutusluokitus2016koulutusalataso1_054#1
     |              - kansallinenkoulutusluokitus2016koulutusalataso1_055#1
     |            kuvaus:
     |              fi: Suomenkielinen kuvaus
     |              sv: Ruotsinkielinen kuvaus
     |            lisatiedot:
     |              - otsikko:
     |                  koodiUri: koulutuksenlisatiedot_03#1
     |                  nimi:
     |                    fi: Lisätiedon otsikko suomeksi
     |                    sv: Lisätiedon otsikko ruotsiksi
     |                teksti:
     |                  fi: Opintojen suomenkielinen lisätietokuvaus
     |                  sv: Opintojen ruotsinkielinen lisätietokuvaus
     |            tutkintonimike:
     |              - koodiUri: tutkintonimikkeet_10024
     |                nimi:
     |                  fi: Tutkintonimike suomeksi
     |                  sv: Tutkintonimike ruotsiksi
     |            opintojenLaajuus:
     |              - koodiUri: opintojenlaajuus_40
     |                nimi:
     |                  fi: 40
     |                  sv: 40
     |            opintojenLaajuusyksikko:
     |              - koodiUri: opintojenlaajuusyksikko_2
     |                nimi:
     |                  fi: Opintopistettä
     |                  sv: Opintopistettä ruotsiksi
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
   :johtaaTutkintoon             s/Bool
   :koulutustyyppi               Koulutustyyppi
   :koulutukset                  [(->Koodi KoulutusKoodi)]
   :tila                         Julkaistu
   :tarjoajat                    [Organisaatio]
   :kielivalinta                 [Kieli]
   :nimi                         Kielistetty
   :metadata                     (s/conditional #(= "amm" (:tyyppi %)) AmmKoulutusMetadata
                                                #(= "yo" (:tyyppi %)) YoMetadata
                                                #(= "amk" (:tyyppi %)) AmkMetadata
                                                #(= "lk" (:tyyppi %)) LukioKoulutusMetadata )
   :organisaatio                 Organisaatio
   (s/->OptionalKey :teemakuva)  Url
   (s/->OptionalKey :ePerusteId) s/Int
   (s/->OptionalKey :sorakuvaus) (s/maybe Sorakuvaus)
   :modified                     Datetime
   :timestamp                    s/Int})

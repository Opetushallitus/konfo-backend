(ns konfo-backend.external.schema.search
  (:require
    [schema.core :as s]
    [schema-tools.core :as st]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]))

(s/defschema KoulutusToteutusHit
  {:toteutusOid         ToteutusOid
   :toteutusNimi        Kielistetty
   :oppilaitosOid       OrganisaatioOid
   :oppilaitosNimi      Kielistetty
   :kunnat              [(->Koodi KuntaKoodi)]})

(def koulutus-toteutus-hit-schema
  "|    KoulutusToteutusHit:
   |      type: object
   |      properties:
   |        oid:
   |          type: string
   |          description: Koulutuksen yksilöivä tunniste
   |          example: 1.2.246.562.13.00000000000000000009
   |        nimi:
   |          type: object
   |          description: Koulutuksen näytettävä nimi eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Nimi'
   |        kielivalinta:
   |          type: array
   |          description: Kielet, joille koulutuksen nimi, kuvailutiedot ja muut tekstit on käännetty.
   |            Kaikkia tietoja (esim. organisaatioiden nimiä) ei välttämättä ole käännetty kaikille kielille.
   |          items:
   |            $ref: '#/components/schemas/Kieli'
   |          example:
   |            - fi
   |            - sv
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
   |        koulutus:
   |          type: object
   |          description: Koulutuksen koodi uri ja nimi
   |          $ref: '#/components/schemas/KoulutusKoodi'
   |        kuvaus:
   |          type: object
   |          description: Koulutuksen kuvausteksti eri kielillä. Kielet on määritetty koulutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        organisaatio:
   |          type: object
   |          description: Koulutuksen luonut organisaatio
   |          $ref: '#/components/schemas/Organisaatio'
   |        ePerusteId:
   |          type: number
   |          description: Ammatillisen koulutuksen ePerusteen id.
   |          example: 4804100
   |        teemakuva:
   |          type: string
   |          description: Koulutuksen teemakuvan URL.
   |          example: https://konfo-files.opintopolku.fi/koulutus-teemakuva/1.2.246.562.13.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
   |        opintojenLaajuus:
   |          type: object
   |          $ref: '#/components/schemas/OpintojenLaajuus'
   |        opintojenLaajuusyksikko:
   |          type: object
   |          $ref: '#/components/schemas/OpintojenLaajuusyksikko'
   |        tutkintonimikkeet:
   |          type: array
   |          description: Lista koulutuksen tutkintonimikkeistä
   |          items:
   |            type: object
   |            $ref: '#/components/schemas/Tutkintonimike'")

(s/defschema KoulutusHit
  {:oid                                       KoulutusOid
   :nimi                                      Kielistetty
   :kielivalinta                              [Kieli]
   :koulutustyyppi                            Koulutustyyppi
   :koulutus                                  (->Koodi KoulutusKoodi)
   :kuvaus                                    (s/maybe Kielistetty)
   (s/->OptionalKey :ePerusteId)              (s/maybe s/Int)
   (s/->OptionalKey :teemakuva)               (s/maybe Url)
   (s/->OptionalKey :opintojenLaajuus)        (s/maybe (->Koodi OpintojenLaajuusKoodi))
   (s/->OptionalKey :opintojenLaajuusNumero)  (s/maybe s/Int)
   (s/->OptionalKey :opintojenLaajuusyksikko) (s/maybe (->Koodi OpintojenLaajuusyksikkoKoodi))
   (s/->OptionalKey :tutkintonimikkeet)       [(->Koodi TutkintonimikeKoodi)]
   :toteutukset                               [KoulutusToteutusHit]})

(def schemas
  koulutus-toteutus-hit-schema)
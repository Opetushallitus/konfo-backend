(ns konfo-backend.external.schema.valintaperustekuvaus-metadata
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.valintakoe :refer :all :exclude [schemas]]
    [schema-tools.core :as st]))

(def valintaperustekuvaus-metadata-schema
  "|    ValintaperustekuvausMetadata:
   |      type: object
   |      properties:
   |        valintatavat:
   |          type: array
   |          description: Lista valintaperustekuvauksen valintatavoista
   |          items:
   |            $ref: '#/components/schemas/ValintaperustekuvausValintatapa'
   |        valintakokeidenYleiskuvaus:
   |          type: object
   |          description: Valintakokeiden yleiskuvaus eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        kuvaus:
   |          type: object
   |          description: Valintaperusteen kuvaus eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        hakukelpoisuus:
   |          type: object
   |          description: Valintaperustekuvauksen hakukelpoisuus eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        lisatiedot:
   |          type: object
   |          description: Valintaperustekuvauksen lisatiedot eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        sisalto:
   |          type: array
   |          description: Valintaperusteen kuvauksen sisältö. Voi sisältää sekä teksti- että taulukkoelementtejä.
   |          items:
   |            type: object
   |            oneOf:
   |              - $ref: '#/components/schemas/SisaltoTeksti'
   |              - $ref: '#/components/schemas/SisaltoTaulukko'")

(def valintaperustekuvaus-valintatapa-schema
  "|    ValintaperustekuvausValintatapa:
   |      type: object
   |      properties:
   |        valintatapa:
   |          type: object
   |          description: Valintatapa
   |          $ref: '#/components/schemas/Valintatapa'
   |        nimi:
   |          type: object
   |          description: Valintatapakuvauksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          $ref: '#/components/schemas/Nimi'
   |        kuvaus:
   |          type: object
   |          description: Valintatavan kuvausteksti eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        sisalto:
   |          type: array
   |          description: Valintatavan sisältö. Voi sisältää sekä teksti- että taulukkoelementtejä.
   |          items:
   |            type: object
   |            oneOf:
   |              - $ref: '#/components/schemas/SisaltoTeksti'
   |              - $ref: '#/components/schemas/SisaltoTaulukko'
   |        kaytaMuuntotaulukkoa:
   |          type: boolean
   |          description: Käytetäänkö muuntotaulukkoa?
   |        kynnysehto:
   |          type: object
   |          description: Kynnysehdon kuvausteksti eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'
   |        enimmaispisteet:
   |          type: double
   |          description: Valintatavan enimmäispisteet
   |          example: 20.0
   |        vahimmaispisteet:
   |          type: double
   |          description: Valintatavan vähimmäispisteet
   |          example: 10.0")

(def amm-valintaperustekuvaus-metadata-schema
  "|    AmmValintaperustekuvausMetadata:
   |      type: object
   |      $ref: '#/components/schemas/ValintaperustekuvausMetadata'
   |      properties:
   |        tyyppi:
   |          type: string
   |          description: Valintaperustekuvauksen metatiedon tyyppi
   |          example: amm
   |          enum:
   |            - amm")

(def korkeakoulutus-valintaperustekuvaus-metadata-schema
  "|    KorkeakoulutusValintaperustekuvausMetadata:
   |      type: object
   |      $ref: '#/components/schemas/ValintaperustekuvausMetadata'
   |      properties:
   |        osaamistaustat:
   |          type: array
   |          description: Lista valintaperustekuvauksen osaamistaustoista
   |          items:
   |            type: object
   |            $ref: '#/components/schemas/Osaamistausta'
   |        kuvaus:
   |          type: object
   |          description: Valintaperustekuvauksen kuvausteksti eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          $ref: '#/components/schemas/Kuvaus'")

(def yo-valintaperustekuvaus-metadata-schema
  "|    YoValintaperustekuvausMetadata:
   |      type: object
   |      $ref: '#/components/schemas/KorkeakoulutusValintaperustekuvausMetadata'
   |      properties:
   |        tyyppi:
   |          type: string
   |          description: Valintaperustekuvauksen metatiedon tyyppi
   |          example: yo
   |          enum:
   |            - yo")

(def amk-valintaperustekuvaus-metadata-schema
  "|    AmkValintaperustekuvausMetadata:
   |      type: object
   |      $ref: '#/components/schemas/KorkeakoulutusValintaperustekuvausMetadata'
   |      properties:
   |        tyyppi:
   |          type: string
   |          description: Valintaperustekuvauksen metatiedon tyyppi
   |          example: amk
   |          enum:
   |            - amk")

(def valintatapa-sisalto-teksti-schema
  "|    SisaltoTeksti:
   |      type: object
   |      description: Tekstimuotoinen sisällön kuvaus
   |      properties:
   |        tyyppi:
   |          type: string
   |          description: Sisällön tyyppi
   |          example: teksti
   |          enum:
   |            - teksti
   |        data:
   |          type: object
   |          description: Sisältöteksti eri kielillä.
   |          $ref: '#/components/schemas/Teksti'")

(def valintatapa-sisalto-taulukko-schema
  "|    SisaltoTaulukko:
   |      type: object
   |      description: Taulukkomuotoinen sisällön kuvaus
   |      properties:
   |        tyyppi:
   |          type: string
   |          description: Sisällön tyyppi
   |          example: taulukko
   |          enum:
   |            - taulukko
   |        data:
   |          type: object
   |          description: Taulukkomuotoinen sisältö eri kielillä
   |          properties:
   |            nimi:
   |              type: object
   |              description: Taulukon Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |              $ref: '#/components/schemas/Nimi'
   |            rows:
   |              type: array
   |              description: Taukon rivit
   |              items:
   |                type: object
   |                properties:
   |                  index:
   |                    type: integer
   |                    description: Rivin järjestysnumero
   |                  isHeader:
   |                    type: boolean
   |                    description: Onko rivi otsikkorivi
   |                  columns:
   |                    type: array
   |                    description: Rivin sarakkeet
   |                    items:
   |                      type: object
   |                      properties:
   |                        index:
   |                          type: integer
   |                          description: Sarakkeen järjestysnumero
   |                        text:
   |                          type: object
   |                          description: Sarakkeen Opintopolussa näytettävä teksti eri kielillä.
   |                            Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |                          $ref: '#/components/schemas/Teksti'")

(def schemas
  (str valintatapa-sisalto-teksti-schema "\n"
       valintatapa-sisalto-taulukko-schema "\n"
       valintaperustekuvaus-metadata-schema "\n"
       valintaperustekuvaus-valintatapa-schema "\n"
       amm-valintaperustekuvaus-metadata-schema "\n"
       korkeakoulutus-valintaperustekuvaus-metadata-schema "\n"
       yo-valintaperustekuvaus-metadata-schema "\n"
       amk-valintaperustekuvaus-metadata-schema))

(def SisaltoTeksti
  {:tyyppi                   (s/eq "teksti")
   (s/->OptionalKey :data)   Kielistetty})

(def SisaltoTaulukko
  {:tyyppi                   (s/eq "taulukko")
   (s/->OptionalKey :data)   {(s/->OptionalKey :nimi) Kielistetty
                              :rows                   [{:index s/Num
                                                        :isHeader s/Bool
                                                        (s/->OptionalKey :columns) [{:index s/Num
                                                                                     (s/->OptionalKey :text) Kielistetty}]}]}})

(def Valintatapa
  {:valintatapa                            (->Koodi ValintatapaKoodi)
   (s/->OptionalKey :nimi)                 Kielistetty
   (s/->OptionalKey :kuvaus)               Kielistetty
   (s/->OptionalKey :sisalto)              [(s/if #(= "taulukko" (:tyyppi %)) SisaltoTaulukko SisaltoTeksti)]
   (s/->OptionalKey :kaytaMuuntotaulukkoa) (s/maybe s/Bool)
   (s/->OptionalKey :kynnysehto)           Kielistetty
   (s/->OptionalKey :enimmaispisteet)      (s/maybe s/Num)
   (s/->OptionalKey :vahimmaispisteet)     (s/maybe s/Num)})

(def ValintaperusteKuvausMetadata
  {(s/->OptionalKey :valintatavat)               [Valintatapa]
   (s/->OptionalKey :hakukelpoisuus)             Kielistetty
   (s/->OptionalKey :lisatiedot)                 Kielistetty
   (s/->OptionalKey :sisalto)                    [(s/if #(= "taulukko" (:tyyppi %)) SisaltoTaulukko SisaltoTeksti)]
   (s/->OptionalKey :kuvaus)                     Kielistetty
   (s/->OptionalKey :valintakokeidenYleiskuvaus) Kielistetty
   s/Any                                         s/Any})

(def AmmValintaperustekuvausMetadata
  (st/merge
     {:tyyppi Amm}
     ValintaperusteKuvausMetadata))

(def YoValintaperusteKuvausMetadata
  (st/merge
     {:tyyppi Yo}
     ValintaperusteKuvausMetadata))

(def AmkValintaperusteKuvausMetadata
  (st/merge
     {:tyyppi Amk}
     ValintaperusteKuvausMetadata))

(def AmmOpeErityisopeJaOpoValintaperusteKuvausMetadata
  (st/merge
    {:tyyppi AmmOpeErityisopeJaOpo}
    ValintaperusteKuvausMetadata))

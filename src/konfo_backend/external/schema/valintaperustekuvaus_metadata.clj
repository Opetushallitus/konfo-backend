(ns konfo-backend.external.schema.valintaperustekuvaus-metadata
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.valintakoe :refer :all :exclude [schemas]]))


(def amm-valinteperustekuvaus-metadata-schema
  "|    AmmValintaperusteMetadata:
   |      type: object
   |      properties:
   |        tyyppi:
   |          type: string
   |          description: Valintaperustekuvauksen metatiedon tyyppi
   |          example: amm
   |          enum:
   |            - amm
   |        kielitaitovaatimukset:
   |          type: array
   |          description: Lista valintaperustekuvauskuvauksen kielitaitovaatimuksista
   |          items:
   |            type: object
   |            properties:
   |              kieli:
   |                type: object
   |                description: Kielitaitovaatimuksen kieli
   |                $ref: '#/components/schemas/KielitaitovaatimusKieli'
   |              kielitaidonVoiOsoittaa:
   |                type: array
   |                description: Lista tavoista, joilla kielitaidon voi osoittaa
   |                items:
   |                  type: object
   |                  properties:
   |                    kielitaito:
   |                      type: object
   |                      description: Kielitaidon osoittaminen
   |                       $ref: '#/components/schemas/KielitaidonOsoittaminen'
   |                    lisatieto:
   |                      type: object
   |                      description: Kielitaidon osoittamisen lisätieto eri kielillä.
   |                      allOf:
   |                        - $ref: '#/components/schemas/Teksti'
   |              vaatimukset:
   |                type: array
   |                description: Lista kielitaitovaatimuksista
   |                items:
   |                  type: object
   |                  properties:
   |                    kielitaitovaatimus:
   |                      type: string
   |                      description: Kielitaitovaatimuksen tyyppi
   |                      $ref: '#/components/schemas/KielitaitovaatimusTyyppi'
   |                    kielitaitovaatimusKuvaukset:
   |                      type: array
   |                      description: Lista kielitaitovaatimusten kuvauksia eri kielillä.
   |                      items:
   |                        type: object
   |                        properties:
   |                          kielitaitovaatimusKuvaus:
   |                            type: string
   |                            description: Kielitaitovaatimuksen kuvaus
   |                            $ref: '#/components/schemas/KielitaitovaatimusTyyppiKuvaus'
   |                          kielitaitovaatimusTaso:
   |                            type: string
   |                            description: Kielitaitovaatimuksen taso
   |                            example: A
   |        valintatavat:
   |          type: array
   |          description: Lista valintaperustekuvauksen valintatavoista
   |          items:
   |            type: object
   |            properties:
   |              valintatapa:
   |                type: object
   |                description: Valintatapa
   |                $ref: '#/components/schemas/Valintatapa'
   |              nimi:
   |                type: object
   |                description: Valintatapakuvauksen Opintopolussa näytettävä nimi eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |                allOf:
   |                  - $ref: '#/components/schemas/Nimi'
   |              kuvaus:
   |                type: object
   |                description: Valintatavan kuvausteksti eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |                allOf:
   |                  - $ref: '#/components/schemas/Kuvaus'
   |              sisalto:
   |                type: array
   |                description: Valintatavan sisältö. Voi sisältää sekä teksti- että taulukkoelementtejä.
   |                items:
   |                type: object
   |                oneOf:
   |                  - $ref: '#/components/schemas/ValintatapaSisaltoTeksti'
   |                  - $ref: '#/components/schemas/ValintatapaSisaltoTaulukko'
   |              kaytaMuuntotaulukkoa:
   |                type: boolean
   |                description: Käytetäänkö muuntotaulukkoa?
   |              kynnysehto:
   |                type: object
   |                description: Kynnysehdon kuvausteksti eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |                allOf:
   |                  - $ref: '#/components/schemas/Kuvaus'
   |              enimmaispisteet:
   |                type: double
   |                description: Valintatavan enimmäispisteet
   |                example: 20.0
   |              vahimmaispisteet:
   |                type: double
   |                description: Valintatavan vähimmäispisteet
   |                example: 10.0
   |        valintakokeidenYleiskuvaus:
   |          type: object
   |          description: Valintakokeiden yleiskuvaus eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'
   |        kuvaus:
   |          type: object
   |          description: Valintaperusteen kuvaus eri kielillä. Kielet on määritetty valintaperustekuvauksen kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Kuvaus'")

(def valintatapa-sisalto-teksti-schema
  "|    ValintatapaSisaltoTeksti:
   |      type: object
   |      description: Tekstimuotoinen valintatavan sisällön kuvaus
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
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'")

(def valintatapa-sisalto-taulukko-schema
  "|    ValintatapaSisaltoTaulukko:
   |      type: object
   |      description: Taulukkomuotoinen valintatavan sisällön kuvaus
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
   |              allOf:
   |                - $ref: '#/components/schemas/Nimi'
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
   |                          allOf:
   |                            - $ref: '#/components/schemas/Teksti'")

(def schemas
  (str amm-valinteperustekuvaus-metadata-schema "\n"
       valintatapa-sisalto-teksti-schema "\n"
       valintatapa-sisalto-taulukko-schema))

(def Kielitaitovaatimus
  {:kieli                                    (->Koodi KielitaitovaatimusKieliKoodi)
   (s/->OptionalKey :kielitaidonVoiOsoittaa) [{(s/->OptionalKey :kielitaito) (s/maybe (->Koodi KielitaidonOsoitteminenKoodi))
                                               (s/->OptionalKey :lisatieto)  Kielistetty}]
   (s/->OptionalKey :vaatimukset)            [{(s/->OptionalKey :kielitaitovaatimus)          (s/maybe (->Koodi KielitaitovaatimusTyyppiKoodi))
                                               (s/->OptionalKey :kielitaitovaatimusKuvaukset) [{(s/->OptionalKey :kielitaitovaatimusKuvaus) (s/maybe (->Koodi KielitaitovaatimusTyypinKuvausKoodi))
                                                                                                (s/->OptionalKey :kielitaitovaatimusTaso)   (s/maybe s/Str)}]}]})

(def ValintatapaSisaltoTeksti
  {:tyyppi                   (s/eq "teksti")
   (s/->OptionalKey :data)   Kielistetty})

(def ValintatapaSisaltoTaulukko
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
   (s/->OptionalKey :sisalto)              [(s/if #(= "taulukko" (:tyyppi %)) ValintatapaSisaltoTaulukko ValintatapaSisaltoTeksti)]
   (s/->OptionalKey :kaytaMuuntotaulukkoa) (s/maybe s/Bool)
   (s/->OptionalKey :kynnysehto)           Kielistetty
   (s/->OptionalKey :enimmaispisteet)      (s/maybe s/Num)
   (s/->OptionalKey :vahimmaispisteet)     (s/maybe s/Num)})

(def AmmValintaperustekuvausMetadata
  {:tyyppi                                       Koulutustyyppi
   (s/->OptionalKey :kielitaitovaatimukset)      [Kielitaitovaatimus]
   (s/->OptionalKey :valintatavat)               [Valintatapa]
   (s/->OptionalKey :valintakokeidenYleiskuvaus) Kielistetty
   (s/->OptionalKey :kuvaus)                     Kielistetty})
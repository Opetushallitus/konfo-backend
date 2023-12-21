(ns konfo-backend.external.schema.common
  (:require
   [schema.core :as s]
   [schema-tools.core :as st]
   [schema-tools.openapi.core :as openapi]
   [clj-yaml.core :as yaml]
   [clojure.string :as string]
   [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]))

(defn remove-namespaces-from-titles [yml-str] (string/replace yml-str #"(?m)title: .+/(.+)$" "title: $1"))

(defn spec-paths-to-swagger-yaml [spec]
  (-> spec
      (yaml/generate-string :dumper-options {:flow-style :block})
      (remove-namespaces-from-titles)
      (string/split #"paths:\n")
      second))

(defn schema-to-swagger-yaml [schema]
  (-> schema
      (openapi/transform nil)
      ((fn [x] {:components {:schemas {(keyword (:name (meta schema))) x}}}))
      (yaml/generate-string :dumper-options {:flow-style :block})
      (remove-namespaces-from-titles)
      (string/split #"schemas:\n")
      second))

(s/defschema Kieli (s/enum "fi" "sv" "en"))

(def kieli-schema (schema-to-swagger-yaml Kieli))

(defn create-kielistetty-schema
  ([name]
   {(s/->OptionalKey :fi) (st/schema s/Str (when name {:description (format "Suomenkielinen %s, jos määritelty" name)}))
    (s/->OptionalKey :sv) (st/schema s/Str (when name {:description (format "Ruotsinkielinen %s, jos määritelty" name)}))
    (s/->OptionalKey :en) (st/schema s/Str (when name {:description (format "Englanninkielinen %s, jos määritelty" name)}))
    (s/->OptionalKey :_id) (st/schema s/Str)
    (s/->OptionalKey :_tunniste) (st/schema s/Str)})
  ([] (create-kielistetty-schema nil)))

(s/defschema Kielistetty (create-kielistetty-schema))

(s/defschema Kuvaus (create-kielistetty-schema "kuvaus"))
(s/defschema Nimi (create-kielistetty-schema "nimi"))

(s/defschema Teksti (create-kielistetty-schema "teksti"))
(s/defschema Linkki (create-kielistetty-schema "linkki"))

(def kuvaus-schema (schema-to-swagger-yaml Kuvaus))

(def nimi-schema (schema-to-swagger-yaml Nimi))

(def teksti-schema (schema-to-swagger-yaml Teksti))

(def linkki-schema (schema-to-swagger-yaml Linkki))

(def Julkaistu (s/eq "julkaistu"))

(def KoulutusOid     #"^1.2.246.562.13.\d+$")
(def ToteutusOid     #"^1.2.246.562.17.\d+$")
(def HakukohdeOid    #"^1.2.246.562.20.\d+$")
(def HakuOid         #"^1.2.246.562.29.\d+$")

(def OrganisaatioOid #"^1.2.246.562.10.\d+$")

(def kouta-koulutustyypit ["amm" "yo" "amk" "amm-ope-erityisope-ja-opo" "ope-pedag-opinnot" "kk-opintojakso"
                           "kk-opintokokonaisuus" "erikoislaakari" "erikoistumiskoulutus" "lk" "telma" "tuva"
                           "vapaa-sivistystyo-opistovuosi" "vapaa-sivistystyo-muu" "muu" "amm-osaamisala"
                           "amm-tutkinnon-osa" "amm-muu" "aikuisten-perusopetus" "taiteen-perusopetus"])

(s/defschema KoutaKoulutustyyppi (st/schema (apply s/enum kouta-koulutustyypit) {:description "Koulutuksen tyyppi"}))

(def kouta-koulutustyyppi-schema (schema-to-swagger-yaml KoutaKoulutustyyppi))

; Search-rajapinnoissa parametrina käytetty koulutustyyppi
(def konfo-koulutustyypit ["aikuisten-perusopetus"
                           "taiteen-perusopetus"
                           "vaativan-tuen-koulutukset" "koulutustyyppi_4" "tuva-erityisopetus"
                           "valmentavat-koulutukset" "tuva-normal" "telma" "vapaa-sivistystyo-opistovuosi"
                           "amm" "koulutustyyppi_26" "koulutustyyppi_11" "koulutustyyppi_12" "muu-amm-tutkinto" "amm-osaamisala" "amm-tutkinnon-osa" "amm-muu"
                           "lk"
                           "amk" "amk-alempi" "amk-ylempi" "amm-ope-erityisope-ja-opo" "amk-opintojakso-avoin" "amk-opintojakso" "amk-opintokokonaisuus-avoin" "amk-opintokokonaisuus" "amk-erikoistumiskoulutus"
                           "yo" "kandi" "kandi-ja-maisteri" "maisteri" "tohtori" "yo-opintojakso-avoin" "yo-opintojakso" "yo-opintokokonaisuus" "yo-opintokokonaisuus-avoin" "ope-pedag-opinnot" "erikoislaakari" "yo-erikoistumiskoulutus"
                           "vapaa-sivistystyo-muu"
                           "muu"])


(s/defschema KonfoKoulutustyyppi (st/schema (apply s/enum konfo-koulutustyypit) {:description "Koulutuksen tyyppi"}))

(def konfo-koulutustyyppi-schema (schema-to-swagger-yaml KonfoKoulutustyyppi))

(def Hakulomaketyyppi (s/enum "ataru" "ei sähköistä" "muu"))

(def Datetime s/Str)                                        ;TODO 2019-02-01T13:16
(def Url s/Str)                                             ;TODO

(defn ->Koodi
  [koodi]
  {:koodiUri (s/maybe koodi)
   :nimi     Kielistetty})

(s/defschema Organisaatio
  {:paikkakunta (->Koodi KuntaKoodi)
   :nimi        Kielistetty
   :oid         OrganisaatioOid
   s/Any        s/Any})

(def organisaatio-schema
  "|    Organisaatio:
   |      type: object
   |      properties:
   |        paikkakunta:
   |          type: object
   |          description: Organisaation paikkakunta.
   |          $ref: '#/components/schemas/Kunta'
   |        nimi:
   |          type: object
   |          description: Organisaation nimi eri kielillä.
   |          $ref: '#/components/schemas/Nimi'
   |        oid:
   |          type: String
   |          example: 1.2.246.562.10.00000000007
   |          description: Organisaation yksilöivä oid")


(def koulutuslisatieto-schema
  "|    KoulutusLisatieto:
   |      type: object
   |      properties:
   |        otsikko:
   |          type: object
   |          $ref: '#/components/schemas/KoulutusLisatietoKoodi'
   |        teksti:
   |          type: object
   |          description: Lisätiedon teksti eri kielillä. Kielet on määritetty kielivalinnassa.
   |          $ref: '#/components/schemas/Teksti'")

(def KoulutusLisatieto
  {:otsikko (->Koodi KoulutusLisatietoKoodi)
   :teksti  Kielistetty})

(def yhteyshenkilo-schema
  "|    Yhteyshenkilo:
   |      type: object
   |      properties:
   |        nimi:
   |          type: object
   |          description: Yhteyshenkilön nimi eri kielillä. Kielet on määritetty kielivalinnassa.
   |          $ref: '#/components/schemas/Nimi'
   |        titteli:
   |          type: object
   |          description: Yhteyshenkilön titteli eri kielillä. Kielet on määritetty kielivalinnassa.
   |          $ref: '#/components/schemas/Teksti'
   |        sahkoposti:
   |          type: object
   |          description: Yhteyshenkilön sähköpostiosoite eri kielillä. Kielet on määritetty kielivalinnassa.
   |          $ref: '#/components/schemas/Teksti'
   |        puhelinnumero:
   |          type: object
   |          description: Yhteyshenkilön puhelinnumero eri kielillä. Kielet on määritetty kielivalinnassa.
   |          $ref: '#/components/schemas/Teksti'
   |        wwwSivu:
   |          type: object
   |          description: Yhteyshenkilön www-sivun linkin osoite eri kielillä. Kielet on määritetty kielivalinnassa.
   |          $ref: '#/components/schemas/Linkki'
   |        wwwSivuTeksti:
   |          type: object
   |          description: Yhteyshenkilön www-sivun linkissä näytettävä eri kielillä. Kielet on määritetty kielivalinnassa.
   |          $ref: '#/components/schemas/Teksti'")

(def Yhteyshenkilo
  {(s/->OptionalKey :nimi)            (s/maybe Kielistetty)
   (s/->OptionalKey :titteli)         (s/maybe Kielistetty)
   (s/->OptionalKey :sahkoposti)      (s/maybe Kielistetty)
   (s/->OptionalKey :puhelinnumero)   (s/maybe Kielistetty)
   (s/->OptionalKey :wwwSivu)         (s/maybe Kielistetty)
   (s/->OptionalKey :wwwSivuTeksti)   (s/maybe Kielistetty)
   s/Any            s/Any})

(def ajanjakso-schema
  "|    Ajanjakso:
   |      type: object
   |      properties:
   |        alkaa:
   |           type: string
   |           format: date-time
   |           description: Ajanjakson alkuaika
   |           example: 2019-08-23T09:55
   |        paattyy:
   |           type: string
   |           format: date-time
   |           description: Ajanjakson päättymisaika
   |           example: 2019-08-23T09:55")

(def Ajanjakso
  {:alkaa   Datetime
   (s/->OptionalKey :paattyy) (s/maybe Datetime)
   s/Any s/Any})

(def FormatoituAikaleima
  {:fi  s/Str
   :sv  s/Str
   :en  s/Str})

(def Alkamiskausityyppi (s/enum "henkilokohtainen suunnitelma" "tarkka alkamisajankohta" "alkamiskausi ja -vuosi"))

(def koulutuksenalkamiskausi-schema
  "|    KoulutuksenAlkamiskausi:
   |      type: object
   |      properties:
   |        alkamiskausityyppi:
   |          type: string
   |          description: Alkamiskauden tyyppi
   |          enum:
   |            - 'henkilokohtainen suunnitelma'
   |            - 'tarkka alkamisajankohta'
   |            - 'alkamiskausi ja -vuosi'
   |        koulutuksenAlkamispaivamaara:
   |          type: string
   |          description: Koulutuksen tarkka alkamisen päivämäärä
   |          example: 2019-11-20T12:00
   |        koulutuksenPaattymispaivamaara:
   |          type: string
   |          description: Koulutuksen päättymisen päivämäärä
   |          example: 2019-11-20T12:00
   |        koulutuksenAlkamiskausi:
   |          type: object
   |          description: Haun koulutusten alkamiskausi. Hakukohteella voi olla eri alkamiskausi kuin haulla.
   |          $ref: '#/components/schemas/Alkamiskausi'
   |        koulutuksenAlkamisvuosi:
   |          type: string
   |          description: Haun koulutusten alkamisvuosi. Hakukohteella voi olla eri alkamisvuosi kuin haulla.
   |          example: 2020
   |        henkilokohtaisenSuunnitelmanLisatiedot:
   |          type: object
   |          description: Lisätietoa koulutuksen alkamisesta henkilökohtaisen suunnitelman mukaan.
   |          $ref: '#/components/schemas/Teksti'")

(def KoulutuksenAlkamiskausi
  {(s/->OptionalKey :alkamiskausityyppi) (s/maybe Alkamiskausityyppi)
   (s/->OptionalKey :koulutuksenAlkamispaivamaara) (s/maybe Datetime)
   (s/->OptionalKey :formatoituKoulutuksenalkamispaivamaara) (s/maybe FormatoituAikaleima)
   (s/->OptionalKey :koulutuksenPaattymispaivamaara) (s/maybe Datetime)
   (s/->OptionalKey :formatoituKoulutuksenpaattymispaivamaara) (s/maybe FormatoituAikaleima)
   (s/->OptionalKey :koulutuksenAlkamiskausi) (s/maybe (->Koodi AlkamiskausiKoodi))
   (s/->OptionalKey :koulutuksenAlkamisvuosi) (s/maybe s/Str)
   (s/->OptionalKey :henkilokohtaisenSuunnitelmanLisatiedot) (s/maybe Kielistetty)})

(def osoite-schema
  "|    Osoite:
   |      type: object
   |      properties:
   |        osoite:
   |          type: object
   |          description: Osoite eri kielillä. Kielet on määritetty kielivalinnassa.
   |          $ref: '#/components/schemas/Teksti'
   |        postinumero:
   |          type: object
   |          description: Postinumero ja -toimipaikka
   |          $ref: '#/components/schemas/Postinumero'")

(def Osoite
  {:osoite Kielistetty
   :postinumero (->Koodi PostinumeroKoodi)})

(def KieliKoodi (->Koodi KieliKoodiPattern))
(s/defschema Kielivalikoima
  {:A1Kielet [KieliKoodi]
   :A2Kielet [KieliKoodi]
   :B1Kielet [KieliKoodi]
   :B2Kielet [KieliKoodi]
   :B3Kielet [KieliKoodi]
   :aidinkielet [KieliKoodi]
   :muutKielet [KieliKoodi]})

(def schemas
  (str kouta-koulutustyyppi-schema "\n"
       konfo-koulutustyyppi-schema "\n"
       kieli-schema "\n"
       kuvaus-schema "\n"
       nimi-schema "\n"
       teksti-schema "\n"
       linkki-schema "\n"
       organisaatio-schema "\n"
       koulutuslisatieto-schema "\n"
       yhteyshenkilo-schema "\n"
       ajanjakso-schema "\n"
       koulutuksenalkamiskausi-schema "\n"
       osoite-schema))

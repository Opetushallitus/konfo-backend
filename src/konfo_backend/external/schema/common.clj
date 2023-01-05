(ns konfo-backend.external.schema.common
  (:require
   [schema.core :as s]
   [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]))

(def kieli-schema
  "|    Kieli:
   |      type: string
   |      enum:
   |        - fi
   |        - sv
   |        - en")

(def Kieli (s/enum "fi" "sv" "en"))

(def Kielistetty
  {(s/->OptionalKey :fi) s/Str
   (s/->OptionalKey :sv) s/Str
   (s/->OptionalKey :en) s/Str})

(def kuvaus-schema
  "|    Kuvaus:
   |      type: object
   |      properties:
   |        fi:
   |          type: string
   |          example: Suomenkielinen kuvaus
   |          description: \"Suomenkielinen kuvaus, jos kielivalinnassa on 'fi'\"
   |        sv:
   |          type: string
   |          example: Ruotsinkielinen kuvaus
   |          description: \"Ruotsinkielinen kuvaus, jos kielivalinnassa on 'sv'\"
   |        en:
   |          type: string
   |          example: Englanninkielinen kuvaus
   |          description: \"Englanninkielinen kuvaus, jos kielivalinnassa on 'en'\"")

(def nimi-schema
  "|    Nimi:
   |      type: object
   |      properties:
   |        fi:
   |          type: string
   |          example: Suomenkielinen nimi
   |          description: Suomenkielinen nimi, jos on olemassa
   |        sv:
   |          type: string
   |          example: Ruotsinkielinen nimi
   |          description: Ruotsinkielinen nimi, jos on olemassa
   |        en:
   |          type: string
   |          example: Englanninkielinen nimi
   |          description: Englanninkielinen nimi, jos on olemassa")

(def teksti-schema
  "|    Teksti:
   |      type: object
   |      properties:
   |        fi:
   |          type: string
   |          example: Suomenkielinen teksti
   |          description: Suomenkielinen teksti, jos on olemassa
   |        sv:
   |          type: string
   |          example: Ruotsinkielinen teksti
   |          description: Ruotsinkielinen teksti, jos on olemassa
   |        en:
   |          type: string
   |          example: Englanninkielinen teksti
   |          description: Englanninkielinen teksti, jos on olemassa")

(def linkki-schema
  "|    Linkki:
   |      type: object
   |      properties:
   |        fi:
   |          type: string
   |          example: Suomenkielinen linkki
   |          description: Suomenkielinen linkki, jos on olemassa
   |        sv:
   |          type: string
   |          example: Ruotsinkielinen linkki
   |          description: Ruotsinkielinen linkki, jos on olemassa
   |        en:
   |          type: string
   |          example: Englanninkielinen linkki
   |          description: Englanninkielinen linkki, jos on olemassa")

(def Julkaistu (s/eq "julkaistu"))

(def KoulutusOid     #"^1.2.246.562.13.\d+$")
(def ToteutusOid     #"^1.2.246.562.17.\d+$")
(def HakukohdeOid    #"^1.2.246.562.20.\d+$")
(def HakuOid         #"^1.2.246.562.29.\d+$")

(def OrganisaatioOid #"^1.2.246.562.10.\d+$")

(def Koulutustyyppi (s/enum "amm" "yo" "amk" "amm-ope-erityisope-ja-opo" "ope-pedag-opinnot" "kk-opintojakso" "kk-opintokokonaisuus" "erikoislaakari" "erikoistumiskoulutus" "lk" "telma" "tuva" "vapaa-sivistystyo-opistovuosi" "vapaa-sivistystyo-muu" "muu" "amm-osaamisala" "amm-tutkinnon-osa" "amm-muu" "aikuisten-perusopetus" "taiteiden-perusopetus"))
(def Amm            (s/eq "amm"))
(def Yo             (s/eq "yo"))
(def Amk            (s/eq "amk"))
(def Lk             (s/eq "lk"))
(def Tuva           (s/eq "tuva"))
(def Telma           (s/eq "telma"))
(def VapaaSivistystyoOpistovuosi  (s/eq "vapaa-sivistystyo-opistovuosi"))
(def VapaaSivistystyoMuu          (s/eq "vapaa-sivistystyo-muu"))
(def AikuistenPerusopetus         (s/eq "aikuisten-perusopetus"))
(def Muu                          (s/eq "muu"))
(def AmmOpeErityisopeJaOpo        (s/eq "amm-ope-erityisope-ja-opo"))
(def OpePedagOpinnot              (s/eq "ope-pedag-opinnot"))
(def KkOpintojakso                (s/eq "kk-opintojakso"))
(def KkOpintokokonaisuus          (s/eq "kk-opintokokonaisuus"))
(def Erikoislaakari               (s/eq "erikoislaakari"))
(def Erikoistumiskoulutus         (s/eq "erikoistumiskoulutus"))
(def TaiteidenPerusopetus         (s/eq "taiteiden-perusopetus"))

(def Hakulomaketyyppi (s/enum "ataru" "ei sähköistä" "muu"))

(def Datetime s/Str)                                        ;TODO 2019-02-01T13:16
(def Url s/Str)                                             ;TODO

(defn ->Koodi
  [koodi]
  {:koodiUri (s/maybe koodi)
   :nimi     Kielistetty})

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

(def Organisaatio
  {:paikkakunta (->Koodi KuntaKoodi)
   :nimi        Kielistetty
   :oid         OrganisaatioOid
   s/Any        s/Any})

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
   |          description: Yhteyshenkilön www-sivu eri kielillä. Kielet on määritetty kielivalinnassa.
   |          $ref: '#/components/schemas/Linkki'")

(def Yhteyshenkilo
  {:nimi          Kielistetty
   :titteli       Kielistetty
   :sahkoposti    Kielistetty
   :puhelinnumero Kielistetty
   :wwwSivu       Kielistetty})

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

(def schemas
  (str kieli-schema "\n"
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

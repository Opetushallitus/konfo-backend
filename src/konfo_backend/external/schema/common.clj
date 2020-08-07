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
   (s/->OptionalKey :en) s/Str })

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

(def Kuvaus Kielistetty)

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

(def Nimi Kielistetty)

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

(def Teksti Kielistetty)

(def Julkaistu (s/eq "julkaistu"))

(def KoulutusOid     #"^1.2.246.562.13.\d+$")
(def OrganisaatioOid #"^1.2.246.562.10.\d+$")

(def Koulutustyyppi (s/enum "amm" "yo" "amk" "lk" "muu"))
(def Amm            (s/eq "amm"))
(def Yo             (s/eq "yo"))
(def Amk            (s/eq "amk"))
(def Lk             (s/eq "lk"))
(def Muu            (s/eq "muu"))

(def Modified s/Str)                                        ;TODO 2019-02-01T13:16
(def Url s/Str)                                             ;TODO

(defn ->Koodi
  [koodi]
  {:koodiUri koodi
   :nimi     Kielistetty})

(def organisaatio-schema
  "|    Organisaatio:
   |      type: object
   |      properties:
   |        paikkakunta:
   |          type: object
   |          description: Organisaation paikkakunta.
   |          allOf:
   |            - $ref: '#/components/schemas/Kunta'
   |        nimi:
   |          type: object
   |          description: Organisaation nimi eri kielillä.
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'
   |        oid:
   |          type: String
   |          example: 1.2.246.562.10.00000000007
   |          description: Organisaation yksilöivä oid")

(def Organisaatio
  {:paikkakunta (->Koodi KuntaKoodi)
   :nimi        Kielistetty
   :oid         OrganisaatioOid})

(def koulutuslisatieto-schema
  "|    KoulutusLisatieto:
   |      type: object
   |      properties:
   |        otsikko:
   |          type: object
   |          allOf:
   |            - $ref: '#/components/schemas/KoulutusLisatietoKoodi'
   |        teksti:
   |          type: object
   |          description: Lisätiedon teksti eri kielillä. Kielet on määritetty kielivalinnassa.
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'")

(def KoulutusLisatieto
  {:otsikko (->Koodi KoulutusLisatietoKoodi)
   :teksti  Kielistetty})

(def schemas
  (str kieli-schema "\n"
       kuvaus-schema "\n"
       nimi-schema "\n"
       teksti-schema "\n"
       organisaatio-schema "\n"
       koulutuslisatieto-schema "\n"))
(ns konfo-backend.external.schema.koodi
  (:require
    [schema.core :as s]
    [ring.swagger.json-schema :as rjs]
    [schema-tools.core :as st]))

(def kunta-schema
  "|    Kunta:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: kunta_091
   |          description: Kunnan koodi uri koodistossa
   |        nimi:
   |          type: object
   |          description: Kunnan nimi eri kielillä.
   |          example: {\"fi\": \"Helsinki\"}
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'")

(def KuntaKoodi    #"^kunta_\d+")

(def koulutus-koodi-schema
  "|    KoulutusKoodi:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: koulutus_301102#11
   |          description: Koulutuksen koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutus/11)
   |        nimi:
   |          type: object
   |          description: Koulutuksen nimi eri kielillä.
   |          example: {\"fi\": \"IB-tutkinto\", \"sv\": \"IB-examen\"}
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'")

(def KoulutusKoodi #"^koulutus_\d{6}(#\d{1,2})?$")

(def koulutusala-1-schema
  "|    Koulutusala1:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_054#1
   |          description: Koulutusalan koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso1/1)
   |        nimi:
   |          type: object
   |          description: Koulutusalan nimi eri kielillä.
   |          example: {\"fi\": \"Koulutusala suomeksi\", \"sv\": \"Koulutusala på svenska\"}
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'")

(def Koulutusala1Koodi #"^kansallinenkoulutusluokitus2016koulutusalataso1_\d+(#\d{1,2})?$")

(def koulutusala-2-schema
  "|    Koulutusala2:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: kansallinenkoulutusluokitus2016koulutusalataso2_054#1
   |          description: Koulutusalan koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kansallinenkoulutusluokitus2016koulutusalataso2/1)
   |        nimi:
   |          type: object
   |          description: Koulutusalan nimi eri kielillä.
   |          example: {\"fi\": \"Koulutusala suomeksi\", \"sv\": \"Koulutusala på svenska\"}
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'")

(def Koulutusala2Koodi #"^kansallinenkoulutusluokitus2016koulutusalataso2_\d+(#\d{1,2})?$")

(def koulutuslisatieto-koodi-schema
  "|    KoulutusLisatietoKoodi:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          description: Lisätiedon otsikon koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/koulutuksenlisatiedot/1)
   |          example: koulutuksenlisatiedot_03#1
   |        nimi:
   |          type: object
   |          description: Koulutuksen lisatiedon otsikko eri kielillä.
   |          example: {\"fi\": \"Otsikko suomeksi\", \"sv\": \"Otsikko på svenska\"}
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'")

(def KoulutusLisatietoKoodi #"koulutuksenlisatiedot_\d+(#\d{1,2})?$")

(def tutkintonimike-schema
  "|    Tutkintonimike:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: tutkintonimikkeet_10024
   |          description: Tutkintonimikkeen koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/tutkintonimikkeet/2)
   |        nimi:
   |          type: object
   |          description: Tutkintonimikkeem nimi eri kielillä.
   |          example: {\"fi\": \"Tutkintonimike suomeksi\"}
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'")

(def TutkintonimikeKoodi #"tutkintonimikkeet_\d+(#\d{1,2})?$")

(def tutkintonimikekk-schema
  "|    TutkintonimikeKk:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: tutkintonimikekk_110
   |          description: Kk-tutkintonimikkeen koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/tutkintonimikekk/2)
   |        nimi:
   |          type: object
   |          description: Tutkintonimikkeem nimi eri kielillä.
   |          example: {\"fi\": \"Tutkintonimike suomeksi\"}
   |          allOf:
   |            - $ref: '#/components/schemas/Nimi'")

(def TutkintonimikeKkKoodi #"tutkintonimikekk_\d+(#\d{1,2})?$")

(def opitojenlaajuus-schema
  "|    OpintojenLaajuus:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: opintojenlaajuus_40
   |          description: Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuus/1)
   |        nimi:
   |          type: object
   |          description: Tutkinnon laajuuden eri kielillä.
   |          example: {\"fi\": \"Tutkinnon laajuus suomeksi\"}
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'")

(def OpintojenLaajuusKoodi #"opintojenlaajuus_\d+(#\d{1,2})?$")

(def opitojenlaajuusyksikko-schema
  "|    OpintojenLaajuusyksikko:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: opintojenlaajuus_40
   |          description: Tutkinnon laajuus. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opintojenlaajuusyksikko/1)
   |        nimi:
   |          type: object
   |          description: Tutkinnon laajuuden eri kielillä.
   |          example: {\"fi\": \"Tutkinnon laajuus suomeksi\"}
   |          allOf:
   |            - $ref: '#/components/schemas/Teksti'")

(def OpintojenLaajuusyksikkoKoodi #"opintojenlaajuusyksikko_\d+(#\d{1,2})?$")

(def schemas
  (str
    kunta-schema "\n"
    koulutus-koodi-schema "\n"
    koulutusala-1-schema "\n"
    koulutusala-2-schema "\n"
    koulutuslisatieto-koodi-schema "\n"
    tutkintonimike-schema "\n"
    tutkintonimikekk-schema "\n"
    opitojenlaajuus-schema "\n"
    opitojenlaajuusyksikko-schema))
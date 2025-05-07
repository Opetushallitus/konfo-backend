(ns konfo-backend.external.schema.koodi
  (:require [schema.core :as s]
            [konfo-backend.config :refer [config]]

            [clojure.string :as str]))

(defonce koodisto-base-url (str "https://" (:koodisto-host config)))

(def kunta-schema
  (str/replace
   "|    Kunta:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: kunta_091
    |          description: Kunnan koodi URI. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/kunta/1)
    |        nimi:
    |          type: object
    |          description: Kunnan nimi eri kielillä.
    |          example: {\"fi\": \"Helsinki\"}
    |          $ref: '#/components/schemas/Nimi'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def KuntaKoodi    #"^kunta_")

(def maakunta-schema
  (str/replace
   "|    Maakunta:
    |      type: object
    |      properties:
    |        maakoodiUri:
    |          type: string
    |          example: maakunta_01
    |          description: Maakunnan koodi URI. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/maakunta/1)
    |        nimi:
    |          type: object
    |          description: Maakunnan nimi eri kielillä.
    |          example: {\"fi\": \"Uusimaa\"}
    |          $ref: '#/components/schemas/Nimi'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def MaakuntaKoodi    #"^maakunta_")

(def koulutus-koodi-schema
  (str/replace
   "|    KoulutusKoodi:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: koulutus_301102#11
    |          description: Koulutuksen koodi URI. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/koulutus/11)
    |        nimi:
    |          type: object
    |          description: Koulutuksen nimi eri kielillä.
    |          example: {\"fi\": \"IB-tutkinto\", \"sv\": \"IB-examen\"}
    |          $ref: '#/components/schemas/Nimi'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def KoulutusKoodi #"^koulutus_")

(def koulutusala-1-schema
  (str/replace
   "|    Koulutusala1:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: kansallinenkoulutusluokitus2016koulutusalataso1_054#1
    |          description: Koulutusalan koodi URI. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/kansallinenkoulutusluokitus2016koulutusalataso1/1)
    |        nimi:
    |          type: object
    |          description: Koulutusalan nimi eri kielillä.
    |          example: {\"fi\": \"Koulutusala suomeksi\", \"sv\": \"Koulutusala på svenska\"}
    |          $ref: '#/components/schemas/Nimi'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def Koulutusala1Koodi #"^kansallinenkoulutusluokitus2016koulutusalataso1_")

(def koulutusala-2-schema
  (str/replace
   "|    Koulutusala2:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: kansallinenkoulutusluokitus2016koulutusalataso2_054#1
    |          description: Koulutusalan koodi URI. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/kansallinenkoulutusluokitus2016koulutusalataso2/1)
    |        nimi:
    |          type: object
    |          description: Koulutusalan nimi eri kielillä.
    |          example: {\"fi\": \"Koulutusala suomeksi\", \"sv\": \"Koulutusala på svenska\"}
    |          $ref: '#/components/schemas/Nimi'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def Koulutusala2Koodi #"^kansallinenkoulutusluokitus2016koulutusalataso2_")

(def koulutuslisatieto-koodi-schema
  (str/replace
   "|    KoulutusLisatietoKoodi:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          description: Lisätiedon otsikon koodi URI. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/koulutuksenlisatiedot/1)
    |          example: koulutuksenlisatiedot_03#1
    |        nimi:
    |          type: object
    |          description: Koulutuksen lisatiedon otsikko eri kielillä.
    |          example: {\"fi\": \"Otsikko suomeksi\", \"sv\": \"Otsikko på svenska\"}
    |          $ref: '#/components/schemas/Nimi'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def KoulutusLisatietoKoodi #"^koulutuksenlisatiedot_")

(def tutkintonimike-schema
  (str/replace
   "|    Tutkintonimike:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: tutkintonimikkeet_10024
    |          description: Tutkintonimikkeen koodi URI. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/tutkintonimikkeet/2)
    |        nimi:
    |          type: object
    |          description: Tutkintonimikkeem nimi eri kielillä.
    |          example: {\"fi\": \"Tutkintonimike suomeksi\"}
    |          $ref: '#/components/schemas/Nimi'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def TutkintonimikeKoodi #"^tutkintonimikkeet_")

(def tutkintonimikekk-schema
  (str/replace
   "|    TutkintonimikeKk:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: tutkintonimikekk_110
    |          description: Kk-tutkintonimikkeen koodi URI. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/tutkintonimikekk/2)
    |        nimi:
    |          type: object
    |          description: Tutkintonimikkeem nimi eri kielillä.
    |          example: {\"fi\": \"Tutkintonimike suomeksi\"}
    |          $ref: '#/components/schemas/Nimi'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def TutkintonimikeKkKoodi #"^tutkintonimikekk_")

(def opintojenlaajuus-schema
  (str/replace
   "|    OpintojenLaajuus:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: opintojenlaajuus_40
    |          description: Opintojen laajuus. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/opintojenlaajuus/1)
    |        nimi:
    |          type: object
    |          description: Opintojen laajuus eri kielillä.
    |          example: {\"fi\": \"Tutkinnon laajuus suomeksi\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def OpintojenLaajuusKoodi #"^opintojenlaajuus_")

(def opintojenlaajuusyksikko-schema
  (str/replace
   "|    OpintojenLaajuusyksikko:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: opintojenlaajuus_40
    |          description: Tutkinnon laajuus. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/opintojenlaajuusyksikko/1)
    |        nimi:
    |          type: object
    |          description: Tutkinnon laajuuden eri kielillä.
    |          example: {\"fi\": \"Tutkinnon laajuus suomeksi\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def OpintojenLaajuusyksikkoKoodi #"^opintojenlaajuusyksikko_")

(def opetuskieli-schema
  (str/replace
   "|    Opetuskieli:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: oppilaitoksenopetuskieli_1
    |          description: Opetuskieli. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/oppilaitoksenopetuskieli/1)
    |        nimi:
    |          type: object
    |          description: Opetuskieli eri kielillä.
    |          example: {\"fi\": \"suomi\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def OpetuskieliKoodi #"^oppilaitoksenopetuskieli_")

(def opetusaika-schema
  (str/replace
   "|    Opetusaika:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: opetusaikakk_1
    |          description: Opetusaika. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/opetusaikakk/1)
    |        nimi:
    |          type: object
    |          description: Opetusaika eri kielillä.
    |          example: {\"fi\": \"suomi\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def OpetusaikaKoodi #"^opetusaikakk_")

(def opetustapa-schema
  (str/replace
   "|    Opetustapa:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: opetuspaikkakk_1
    |          description: Opetustapa. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/opetuspaikkakk/1)
    |        nimi:
    |          type: object
    |          description: Opetustapa eri kielillä.
    |          example: {\"fi\": \"suomi\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def OpetustapaKoodi #"^opetuspaikkakk_")

(def alkamiskausi-schema
  (str/replace
   "|    Alkamiskausi:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: kausi_s
    |          description: Alkamiskausi. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/kausi/1)
    |        nimi:
    |          type: object
    |          description: Alkamiskausi eri kielillä
    |          example: {\"fi\": \"syksy\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def AlkamiskausiKoodi #"^kausi_")

(def pohjakoulutusvaatimus-schema
  (str/replace
   "|    Pohjakoulutusvaatimus:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: pohjakoulutusvaatimuskouta_pk
    |          description: Hakukohteen pohjakoulutusvaatimus. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/pohjakoulutusvaatimuskouta/1)
    |        nimi:
    |          type: object
    |          description: Pohjakoulutusvaatimus eri kielillä
    |          example: {\"fi\": \"syksy\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def PohjakoulutusvaatimusKoodi #"^pohjakoulutusvaatimuskouta_")

(def postinumero-schema
  (str/replace
   "|    Postinumero:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: posti_04230#2
    |          description: Postinumero. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/posti/2)
    |        nimi:
    |          type: object
    |          description: Postitoimipaikan nimi eri kielillä
    |          example: {\"fi\": \"Kerava\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def PostinumeroKoodi #"^posti_")

(def liitteen-tyyppi-schema
  (str/replace
   "|    LiitteenTyyppi:
    |      type: object
    |      properties:
    |        koodi:
    |          type: string
    |          example: liitetyypitamm_3#1
    |          description: Liitteen tyyppi. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/liitetyypitamm/1)
    |        nimi:
    |          type: object
    |          description: Liitteen tyyppi eri kielillä
    |          example: {\"fi\": \"Todistus\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def LiitteenTyyppiKoodi #"^liitetyypitamm_")

(def valintakokeen-tyyppi-schema
  (str/replace
   "|    ValintakokeenTyyppi:
    |      type: object
    |      properties:
    |        koodi:
    |          type: string
    |          example: liitetyypitamm_3#1
    |          description: Valintakokeen tyyppi. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/valintakokeentyyppi/1)
    |        nimi:
    |          type: object
    |          description: Valintakokeen tyyppi eri kielillä
    |          example: {\"fi\": \"Kuulustelu\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def ValintakokeenTyyppiKoodi #"^valintakokeentyyppi_")

(def hakutapa-schema
  (str/replace
   "|    Hakutapa:
    |      type: object
    |      properties:
    |        koodi:
    |          type: string
    |          example: hakutapa_03#1
    |          description: Hakutapa. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/hakutapa/1)
    |        nimi:
    |          type: object
    |          description: Hakutavan nimi eri kielillä
    |          example: {\"fi\": \"Yhteishaku\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def HakutapaKoodi #"^hakutapa_")

(def haun-kohdejoukko-schema
  (str/replace
   "|    HaunKohdejoukko:
    |      type: object
    |      properties:
    |        koodi:
    |          type: string
    |          example: haunkohdejoukko_03#1
    |          description: Haun kohdejoukko. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/haunkohdejoukko/1)
    |        nimi:
    |          type: object
    |          description: Haun kohdejoukon nimi eri kielillä
    |          example: {\"fi\": \"Kohdejoukko\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def HaunKohdejoukkoKoodi #"^haunkohdejoukko_")

(def haun-kohdejoukon-tarkenne-schema
  (str/replace
   "|    HaunKohdejoukonTarkenne:
    |      type: object
    |      properties:
    |        koodi:
    |          type: string
    |          example: haunkohdejoukontarkenne_03#1
    |          description: Haun kohdejoukon tarkenne. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/haunkohdejoukontarkenne/1)
    |        nimi:
    |          type: object
    |          description: Haun kohdejoukon tarkenne eri kielillä
    |          example: {\"fi\": \"Kohdejoukon tarkenne\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def HaunKohdejoukonTarkenneKoodi #"^haunkohdejoukontarkenne_")

(def valintatapa-schema
  (str/replace
   "|    Valintatapa:
    |      type: object
    |      properties:
    |        koodi:
    |          type: string
    |          example: valintatapajono_av#1
    |          description: Valintatapa. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/valintatapajono/1)
    |        nimi:
    |          type: object
    |          description: Valintatapa eri kielillä
    |          example: {\"fi\": \"Valintatapa suomeksi\"}
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def ValintatapaKoodi #"^valintatapajono_")

(def osaamistausta-schema
  (str/replace
   "|    Osaamistausta:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: osaamistausta_1
    |          description: Osaamistausta. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/osaamistausta/1)
    |        nimi:
    |          type: object
    |          description: Osaamistausta.
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def OsaamistaustaKoodi #"^osaamistausta_")

(def osaamisala-schema
  (str/replace
   "|    Osaamisala:
    |      type: object
    |      properties:
    |        koodiUri:
    |          type: string
    |          example: osaamisala_1
    |          description: Osaamisala. Viittaa [koodistoon]($KOODISTO-BASE-URL/koodisto-service/ui/koodisto/view/osaamistausta/1)
    |        nimi:
    |          type: object
    |          description: Osaamisala.
    |          $ref: '#/components/schemas/Teksti'"
   "$KOODISTO-BASE-URL"
   koodisto-base-url))

(def OsaamisalaKoodi #"^osaamisala_")

(def LukioDiplomiKoodi s/Str)

(def KieliKoodiPattern #"^kieli_")

(def schemas
  (str
   kunta-schema "\n"
   maakunta-schema "\n"
   koulutus-koodi-schema "\n"
   koulutusala-1-schema "\n"
   koulutusala-2-schema "\n"
   koulutuslisatieto-koodi-schema "\n"
   tutkintonimike-schema "\n"
   tutkintonimikekk-schema "\n"
   opintojenlaajuus-schema "\n"
   opintojenlaajuusyksikko-schema "\n"
   opetuskieli-schema "\n"
   opetustapa-schema "\n"
   opetusaika-schema "\n"
   alkamiskausi-schema "\n"
   pohjakoulutusvaatimus-schema "\n"
   postinumero-schema "\n"
   liitteen-tyyppi-schema "\n"
   valintakokeen-tyyppi-schema "\n"
   hakutapa-schema "\n"
   haun-kohdejoukko-schema "\n"
   haun-kohdejoukon-tarkenne-schema "\n"
   valintatapa-schema "\n"
   osaamistausta-schema "\n"
   osaamisala-schema))


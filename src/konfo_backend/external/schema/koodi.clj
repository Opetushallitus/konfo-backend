(ns konfo-backend.external.schema.koodi
  (:require [schema.core :as s]))

(def kunta-schema
  "|    Kunta:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: kunta_091
   |          description: Kunnan koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kunta/1)
   |        nimi:
   |          type: object
   |          description: Kunnan nimi eri kielillä.
   |          example: {\"fi\": \"Helsinki\"}
   |          $ref: '#/components/schemas/Nimi'")

(def KuntaKoodi    #"^kunta_\d+")

(def maakunta-schema
  "|    Maakunta:
   |      type: object
   |      properties:
   |        maakoodiUri:
   |          type: string
   |          example: maakunta_01
   |          description: Maakunnan koodi URI. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/maakunta/1)
   |        nimi:
   |          type: object
   |          description: Maakunnan nimi eri kielillä.
   |          example: {\"fi\": \"Uusimaa\"}
   |          $ref: '#/components/schemas/Nimi'")

(def MaakuntaKoodi    #"^maakunta_\d+")

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
   |          $ref: '#/components/schemas/Nimi'")

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
   |          $ref: '#/components/schemas/Nimi'")

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
   |          $ref: '#/components/schemas/Nimi'")

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
   |          $ref: '#/components/schemas/Nimi'")

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
   |          $ref: '#/components/schemas/Nimi'")

(def TutkintonimikeKoodi s/Any)

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
   |          $ref: '#/components/schemas/Nimi'")

(def TutkintonimikeKkKoodi #"tutkintonimikekk_\d+(#\d{1,2})?$")

(def opintojenlaajuus-schema
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
   |          $ref: '#/components/schemas/Teksti'")

(def OpintojenLaajuusKoodi #"opintojenlaajuus_\d+(#\d{1,2})?$")

(def opintojenlaajuusyksikko-schema
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
   |          $ref: '#/components/schemas/Teksti'")

(def OpintojenLaajuusyksikkoKoodi #"opintojenlaajuusyksikko_\d+(#\d{1,2})?$")

(def opetuskieli-schema
  "|    Opetuskieli:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: oppilaitoksenopetuskieli_1
   |          description: Opetuskieli. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/oppilaitoksenopetuskieli/1)
   |        nimi:
   |          type: object
   |          description: Opetuskieli eri kielillä.
   |          example: {\"fi\": \"suomi\"}
   |          $ref: '#/components/schemas/Teksti'")

(def OpetuskieliKoodi #"oppilaitoksenopetuskieli_\d+(#\d{1,2})?$")

(def opetusaika-schema
  "|    Opetusaika:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: opetusaikakk_1
   |          description: Opetusaika. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opetusaikakk/1)
   |        nimi:
   |          type: object
   |          description: Opetusaika eri kielillä.
   |          example: {\"fi\": \"suomi\"}
   |          $ref: '#/components/schemas/Teksti'")

(def OpetusaikaKoodi #"opetusaikakk_\d+(#\d{1,2})?$")

(def opetustapa-schema
  "|    Opetustapa:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: opetuspaikkakk_1
   |          description: Opetustapa. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/opetuspaikkakk/1)
   |        nimi:
   |          type: object
   |          description: Opetustapa eri kielillä.
   |          example: {\"fi\": \"suomi\"}
   |          $ref: '#/components/schemas/Teksti'")

(def OpetustapaKoodi #"opetuspaikkakk_\d+(#\d{1,2})?$")

(def alkamiskausi-schema
  "|    Alkamiskausi:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: kausi_s
   |          description: Alkamiskausi. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/kausi/1)
   |        nimi:
   |          type: object
   |          description: Alkamiskausi eri kielillä
   |          example: {\"fi\": \"syksy\"}
   |          $ref: '#/components/schemas/Teksti'")

(def AlkamiskausiKoodi #"kausi_\w+(#\d{1,2})?$")

(def pohjakoulutusvaatimus-schema
  "|    Pohjakoulutusvaatimus:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: pohjakoulutusvaatimuskouta_pk
   |          description: Hakukohteen pohjakoulutusvaatimus. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/pohjakoulutusvaatimuskouta/1)
   |        nimi:
   |          type: object
   |          description: Pohjakoulutusvaatimus eri kielillä
   |          example: {\"fi\": \"syksy\"}
   |          $ref: '#/components/schemas/Teksti'")

(def PohjakoulutusvaatimusKoodi #"pohjakoulutusvaatimuskouta_\w+(#\d{1,2})?$")

(def postinumero-schema
  "|    Postinumero:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: posti_04230#2
   |          description: Postinumero. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/posti/2)
   |        nimi:
   |          type: object
   |          description: Postitoimipaikan nimi eri kielillä
   |          example: {\"fi\": \"Kerava\"}
   |          $ref: '#/components/schemas/Teksti'")

(def PostinumeroKoodi #"posti_\d+(#\d{1,2})?$")

(def liitteen-tyyppi-schema
  "|    LiitteenTyyppi:
   |      type: object
   |      properties:
   |        koodi:
   |          type: string
   |          example: liitetyypitamm_3#1
   |          description: Liitteen tyyppi. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/liitetyypitamm/1)
   |        nimi:
   |          type: object
   |          description: Liitteen tyyppi eri kielillä
   |          example: {\"fi\": \"Todistus\"}
   |          $ref: '#/components/schemas/Teksti'")

(def LiitteenTyyppiKoodi #"liitetyypitamm_\d+(#\d{1,2})?$")

(def valintakokeen-tyyppi-schema
  "|    ValintakokeenTyyppi:
   |      type: object
   |      properties:
   |        koodi:
   |          type: string
   |          example: liitetyypitamm_3#1
   |          description: Valintakokeen tyyppi. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/valintakokeentyyppi/1)
   |        nimi:
   |          type: object
   |          description: Valintakokeen tyyppi eri kielillä
   |          example: {\"fi\": \"Kuulustelu\"}
   |          $ref: '#/components/schemas/Teksti'")

(def ValintakokeenTyyppiKoodi #"valintakokeentyyppi_\d+(#\d{1,2})?$")

(def hakutapa-schema
  "|    Hakutapa:
   |      type: object
   |      properties:
   |        koodi:
   |          type: string
   |          example: hakutapa_03#1
   |          description: Hakutapa. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/hakutapa/11)
   |        nimi:
   |          type: object
   |          description: Hakutavan nimi eri kielillä
   |          example: {\"fi\": \"Yhteishaku\"}
   |          $ref: '#/components/schemas/Teksti'")

(def HakutapaKoodi #"hakutapa_\d+(#\d{1,2})?$")

(def haun-kohdejoukko-schema
  "|    HaunKohdejoukko:
   |      type: object
   |      properties:
   |        koodi:
   |          type: string
   |          example: haunkohdejoukko_03#1
   |          description: Haun kohdejoukko. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/haunkohdejoukko/11)
   |        nimi:
   |          type: object
   |          description: Haun kohdejoukon nimi eri kielillä
   |          example: {\"fi\": \"Kohdejoukko\"}
   |          $ref: '#/components/schemas/Teksti'")

(def HaunKohdejoukkoKoodi #"haunkohdejoukko_\d+(#\d{1,2})?$")

(def haun-kohdejoukon-tarkenne-schema
  "|    HaunKohdejoukonTarkenne:
   |      type: object
   |      properties:
   |        koodi:
   |          type: string
   |          example: haunkohdejoukontarkenne_03#1
   |          description: Haun kohdejoukon tarkenne. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/haunkohdejoukontarkenne/11)
   |        nimi:
   |          type: object
   |          description: Haun kohdejoukon tarkenne eri kielillä
   |          example: {\"fi\": \"Kohdejoukon tarkenne\"}
   |          $ref: '#/components/schemas/Teksti'")

(def HaunKohdejoukonTarkenneKoodi #"haunkohdejoukontarkenne_\d+(#\d{1,2})?$")

(def valintatapa-schema
  "|    Valintatapa:
   |      type: object
   |      properties:
   |        koodi:
   |          type: string
   |          example: valintatapajono_av#1
   |          description: Valintatapa. Viittaa [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/valintatapajono/1)
   |        nimi:
   |          type: object
   |          description: Valintatapa eri kielillä
   |          example: {\"fi\": \"Valintatapa suomeksi\"}
   |          $ref: '#/components/schemas/Teksti'")

(def ValintatapaKoodi #"valintatapajono_\w+(#\d{1,2})?$")

(def osaamistausta-schema
  "|    Osaamistausta:
   |      type: object
   |      properties:
   |        koodiUri:
   |          type: string
   |          example: osaamistausta_1
   |          description: Osaamistausta. Viittaa koodistoon [koodistoon](https://virkailija.testiopintopolku.fi/koodisto-ui/html/koodisto/osaamistausta/1)
   |        nimi:
   |          type: object
   |          description: Osaamistausta.
   |          $ref: '#/components/schemas/Teksti'")

(def OsaamistaustaKoodi #"osaamistausta_\d+(#\d{1,2})?$")

(def LukioDiplomiKoodi s/Str)

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
    osaamistausta-schema))

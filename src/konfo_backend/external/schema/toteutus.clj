(ns konfo-backend.external.schema.toteutus
  (:require
    [schema.core :as s]
    [konfo-backend.external.schema.toteutus-metadata :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.common :refer :all :exclude [schemas]]
    [konfo-backend.external.schema.koodi :refer :all :exclude [schemas]]))

(def schemas
  "|    Toteutus:
   |      type: object
   |      properties:
   |        oid:
   |          type: string
   |          description: Toteutuksen yksilöivä tunniste
   |          example: 1.2.246.562.17.00000000000000000009
   |        koulutusOid:
   |          type: string
   |          description: Sen koulutuksen oid, johon toteutus liittyy
   |          example: 1.2.246.562.13.00000000000000000009
   |        tila:
   |          type: string
   |          example: julkaistu
   |          enum:
   |            - julkaistu
   |          description: Toteutuksen julkaisutila. Aina julkaistu
   |        tarjoajat:
   |          type: array
   |          description: Toteutusta tarjoavat organisaatiot
   |          items:
   |            $ref: '#/components/schemas/Organisaatio'
   |        oppilaitokset:
   |          type: array
   |          description: Toteutusta tarjoavien oppilaitosten oidit
   |          items:
   |            type: string
   |          example: 1.2.246.562.10.00000000007
   |        kielivalinta:
   |          type: array
   |          description: Kielet, joille toteutuksen nimi, kuvailutiedot ja muut tekstit on käännetty.
   |            Kaikkia tietoja (esim. organisaatioiden nimiä) ei välttämättä ole käännetty kaikille kielille.
   |          items:
   |            $ref: '#/components/schemas/Kieli'
   |          example:
   |            - fi
   |            - sv
   |        nimi:
   |          type: object
   |          description: Toteutuksen näytettävä nimi eri kielillä. Kielet on määritetty toteutuksen kielivalinnassa.
   |          $ref: '#/components/schemas/Nimi'
   |        metadata:
   |          type: object
   |          oneOf:
   |            - $ref: '#/components/schemas/AmmatillinenToteutusMetadata'
   |          example:
   |            tyyppi: amm
   |            kuvaus:
   |              fi: Suomenkielinen kuvaus
   |              sv: Ruotsinkielinen kuvaus
   |            osaamisalat:
   |              - koodiUri: osaamisala_0001#1
   |                linkki:
   |                  fi: http://osaamisala.fi/linkki/fi
   |                  sv: http://osaamisala.fi/linkki/sv
   |                otsikko:
   |                  fi: Katso osaamisalan tarkempi kuvaus tästä
   |                  sv: Katso osaamisalan tarkempi kuvaus tästä ruotsiksi
   |            opetus:
   |              opetuskieliKoodiUrit:
   |                - oppilaitoksenopetuskieli_1#1
   |              opetuskieletKuvaus:
   |                fi: Opetuskielen suomenkielinen kuvaus
   |                sv: Opetuskielen ruotsinkielinen kuvaus
   |              opetusaikaKoodiUrit:
   |                - opetusaikakk_1#1
   |              opetusaikaKuvaus:
   |                fi: Opetusajan suomenkielinen kuvaus
   |                sv: Opetusajan ruotsinkielinen kuvaus
   |              opetustapaKoodiUrit:
   |                - opetuspaikkakk_1#1
   |                - opetuspaikkakk_2#1
   |              opetustapaKuvaus:
   |                fi: Opetustavan suomenkielinen kuvaus
   |                sv: Opetustavan ruotsinkielinen kuvaus
   |              maksullisuustyyppi: maksullinen
   |              maksullisuusKuvaus:
   |                fi: Maksullisuuden suomenkielinen kuvaus
   |                sv: Maksullisuuden ruotsinkielinen kuvaus
   |              maksunMaara: 200.50
   |              koulutuksenAlkamiskausi:
   |                - alkamiskausityyppi : 'tarkka alkamisajankohta'
   |                - koulutuksenAlkamispaivamaara: 2019-11-20T12:00
   |                - koulutuksenPaattymispaivamaara: 2019-12-20T12:00
   |              lisatiedot:
   |                - otsikkoKoodiUri: koulutuksenlisatiedot_03#1
   |                  teksti:
   |                    fi: Suomenkielinen lisätietoteksti
   |                    sv: Ruotsinkielinen lisätietoteksti
   |            ammattinimikkeet:
   |              - kieli: fi
   |                arvo: insinööri
   |              - kieli: en
   |                arvo: engineer
   |            asiasanat:
   |              - kieli: fi
   |                arvo: ravintotiede
   |              - kieli: en
   |                arvo: nutrition
   |            yhteyshenkilot:
   |              - nimi:
   |                  fi: Aku Ankka
   |                  sv: Kalle Ankka
   |                titteli:
   |                  fi: Ankka
   |                  sv: Ankka ruotsiksi
   |                sahkoposti:
   |                  fi: aku.ankka@ankkalinnankoulu.fi
   |                  sv: aku.ankka@ankkalinnankoulu.fi
   |                puhelinnumero:
   |                  fi: 123
   |                  sv: 223
   |                wwwSivu:
   |                  fi: http://opintopolku.fi
   |                  sv: http://studieinfo.fi
   |        organisaatio:
   |          type: object
   |          description: Toteutuksen luonut organisaatio
   |          $ref: '#/components/schemas/Organisaatio'
   |        teemakuva:
   |          type: string
   |          description: Toteutuksen teemakuvan URL.
   |          example: https://konfo-files.opintopolku.fi/toteutus-teemakuva/1.2.246.562.13.00000000000000000009/f4ecc80a-f664-40ef-98e6-eaf8dfa57f6e.png
   |        modified:
   |           type: string
   |           format: date-time
   |           description: Toteutuksen viimeisin muokkausaika
   |           example: 2019-08-23T09:55
   |        timestamp:
   |          type: number
   |          description: Toteutuksen viimeisin indeksointiaika
   |          example: 1587537927174")

(s/defschema Toteutus
  {:oid                          ToteutusOid
   (s/->OptionalKey :externalId) s/Str
   :koulutusOid                  KoulutusOid
   :tila                         Julkaistu
   :oppilaitokset                [OrganisaatioOid]
   :tarjoajat                    [Organisaatio]
   :kielivalinta                 [Kieli]
   :nimi                         Kielistetty
   :metadata                     ToteutusMetadata
   :organisaatio                 Organisaatio
   (s/->OptionalKey :teemakuva)  Url
   :modified                     Datetime
   :timestamp                    s/Int
   s/Any s/Any})

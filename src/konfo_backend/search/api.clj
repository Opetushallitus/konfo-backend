(ns konfo-backend.search.api
  (:require
   [konfo-backend.search.koulutus.search :as koulutus-search]
   [konfo-backend.search.oppilaitos.search :as oppilaitos-search]
   [konfo-backend.search.filters :as filters]
   [compojure.api.core :refer [GET context]]
   [ring.util.http-response :refer :all]
   [clj-log.access-log :refer [with-access-logging]]
   [konfo-backend.tools :refer [comma-separated-string->vec]]))

(def paths
  "|  /search/filters:
   |    get:
   |      tags:
   |        - internal-search
   |      summary: Hae hakurajaimet
   |      description: Palauttaa kaikkien käytössä olevien hakurajainten koodit ja nimet. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /search/filters_as_array:
   |    get:
   |      tags:
   |        - internal-search
   |      summary: Hae hakurajaimet taulukkomuodossa
   |      description: Palauttaa kaikkien käytössä olevien hakurajainten koodit ja nimet taulukkomuodossa. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |  /search/koulutukset:
   |    get:
   |      tags:
   |        - internal-search
   |      summary: Hae koulutuksia
   |      description: Hakee koulutuksia annetulla hakusanalla ja rajaimilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: query
   |          name: keyword
   |          schema:
   |            type: string
   |          required: false
   |          description: Hakusana. Voi olla tyhjä, jos haetaan vain rajaimilla tai halutaan hakea kaikki.
   |            Muussa tapauksessa vähimmäispituus on 3 merkkiä.
   |          example: Hevostalous
   |        - in: query
   |          name: page
   |          schema:
   |            type: number
   |            default: 1
   |          required: false
   |          description: Hakutuloksen sivunumero
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |            default: 20
   |          required: false
   |          description: Hakutuloksen sivun koko
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |            default: fi
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |        - in: query
   |          name: sort
   |          schema:
   |            type: string
   |            default: score
   |          required: false
   |          description: Järjestysperuste. 'name' tai 'score'
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |            default: desc
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |        - in: query
   |          name: koulutustyyppi
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista koulutustyyppejä
   |          example: amm,amm-muu,yo,amk,amm-tutkinnon-osa,amm-osaamisala
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |        - in: query
   |          name: valintatapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu valintatapojen koodeja
   |          example: valintatapajono_av, valintatapajono_tv
   |        - in: query
   |          name: hakukaynnissa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on haku käynnissä
   |        - in: query
   |          name: jotpa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on JOTPA-rahoitus
   |        - in: query
   |          name: tyovoimakoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat työvoimakoulutusta
   |        - in: query
   |          name: taydennyskoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat täydennyskoulutusta
   |        - in: query
   |          name: hakutapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu hakutapojen koodeja
   |          example: hakutapa_01, hakutapa_03
   |        - in: query
   |          name: yhteishaku
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista yhteishakujen oideja
   |          example: 1.2.246.562.29.00000000000000000800
   |        - in: query
   |          name: pohjakoulutusvaatimus
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu pohjakoulutusvaatimusten koodeja
   |          example: pohjakoulutusvaatimuskonfo_am, pohjakoulutusvaatimuskonfo_102
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |        '400':
   |          description: Bad request
   |  /search/koulutus/{oid}/jarjestajat:
   |    get:
   |      tags:
   |        - internal-search
   |      summary: Hae koulutuksen tarjoajat
   |      description: Hakee annetun koulutuksen järjestäjiä. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Koulutuksen yksilöivä oid
   |          example: 1.2.246.562.13.00000000000000000001
   |        - in: query
   |          name: page
   |          schema:
   |            type: number
   |            default: 1
   |          required: false
   |          description: Hakutuloksen sivunumero
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |            default: 20
   |          required: false
   |          description: Hakutuloksen sivun koko
   |        - in: query
   |          name: tuleva
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko tulevia vai tämänhetkisiä tarjoajia.
   |            Tarjoaja on tuleva, jos se lisätty koulutukselle tarjoajaksi mutta se ei ole vielä julkaissut omaa toteutusta.
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |            default: fi
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |            default: desc
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |        - in: query
   |          name: hakukaynnissa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on haku käynnissä
   |        - in: query
   |          name: jotpa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on JOTPA-rahoitus
   |        - in: query
   |          name: tyovoimakoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat työvoimakoulutusta
   |        - in: query
   |          name: taydennyskoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat täydennyskoulutusta
   |        - in: query
   |          name: lukiopainotukset
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltuna lukiopainotusten koodeja
   |          example: lukiopainotukset_0111, lukiopainotukset_001
   |        - in: query
   |          name: lukiolinjaterityinenkoulutustehtava
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltuna lukiolinjaterityinenkoulutustehtava-koodeja
   |          example: lukiolinjaterityinenkoulutustehtava_0100, lukiolinjaterityinenkoulutustehtava_0126
   |        - in: query
   |          name: osaamisala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltuna ammatillisten osaamisalojen koodeja
   |          example: osaamisala_1756, osaamisala_3076
   |        - in: query
   |          name: oppilaitos
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltuna lista toteutusten oppilaitoksista
   |          example: 1.2.246.562.10.93483820481, 1.2.246.562.10.29176843356
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |        '400':
   |          description: Bad request
   |  /search/oppilaitokset:
   |    get:
   |      tags:
   |        - internal-search
   |      summary: Hae oppilaitoksia
   |      description: Hakee oppilaitoksia annetulla hakusanalla ja rajaimilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: query
   |          name: keyword
   |          schema:
   |            type: string
   |          required: false
   |          description: Hakusana. Voi olla tyhjä, jos haetaan vain rajaimilla tai halutaan hakea kaikki.
   |            Muussa tapauksessa vähimmäispituus on 3 merkkiä.
   |          example: Hevostalous
   |        - in: query
   |          name: page
   |          schema:
   |            type: number
   |            default: 1
   |          required: false
   |          description: Hakutuloksen sivunumero
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |            default: 20
   |          required: false
   |          description: Hakutuloksen sivun koko
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |            default: fi
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |        - in: query
   |          name: sort
   |          schema:
   |            type: string
   |            default: score
   |          required: false
   |          description: Järjestysperuste. 'name' tai 'score'
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |            default: desc
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |        - in: query
   |          name: koulutustyyppi
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista koulutustyyppejä
   |          example: amm,amm-muu,yo,amk,amm-tutkinnon-osa,amm-osaamisala
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |            default: nil
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |        - in: query
   |          name: hakukaynnissa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on haku käynnissä
   |        - in: query
   |          name: jotpa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on JOTPA-rahoitus
   |        - in: query
   |          name: tyovoimakoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat työvoimakoulutusta
   |        - in: query
   |          name: taydennyskoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat täydennyskoulutusta
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |        '400':
   |          description: Bad request
   |  /search/oppilaitos/{oid}/tarjonta:
   |    get:
   |      tags:
   |        - internal-search
   |      summary: Hae oppilaitoksen koulutustarjonnan
   |      description: Hakee annetun oppilaitoksen koulutustarjonnan. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Oppilaitoksen yksilöivä oid
   |          example: 1.2.246.562.10.12345
   |        - in: query
   |          name: page
   |          schema:
   |            type: number
   |            default: 1
   |          required: false
   |          description: Hakutuloksen sivunumero
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |            default: 20
   |          required: false
   |          description: Hakutuloksen sivun koko
   |        - in: query
   |          name: tuleva
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko tuleva vai tämänhetkinen tarjonta.
   |            Tarjoaja on tuleva, jos se lisätty koulutukselle tarjoajaksi mutta se ei ole vielä julkaissut omaa toteutusta.
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |            default: fi
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |            default: desc
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |        - in: query
   |          name: koulutustyyppi
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista koulutustyyppejä
   |          example: amm,kk,lk
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |        '400':
   |          description: Bad request
   |  /search/oppilaitoksen-osa/{oid}/tarjonta:
   |    get:
   |      tags:
   |        - internal-search
   |      summary: Hae oppilaitoksen osan koulutustarjonnan
   |      description: Hakee annetun oppilaitoksen osan koulutustarjonnan. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: path
   |          name: oid
   |          schema:
   |            type: string
   |          required: true
   |          description: Oppilaitoksen osan yksilöivä oid
   |          example: 1.2.246.562.10.12345
   |        - in: query
   |          name: page
   |          schema:
   |            type: number
   |            default: 1
   |          required: false
   |          description: Hakutuloksen sivunumero
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |            default: 20
   |          required: false
   |          description: Hakutuloksen sivun koko
   |        - in: query
   |          name: tuleva
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko tuleva vai tämänhetkinen tarjonta.
   |            Tarjoaja on tuleva, jos se lisätty koulutukselle tarjoajaksi mutta se ei ole vielä julkaissut omaa toteutusta.
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |            default: fi
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |            default: desc
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |        '400':
   |          description: Bad request
   |  /search/autocomplete:
   |    get:
   |      tags:
   |        - internal-search
   |      summary: Hae koulutuksia
   |      description: Hakee koulutuksia annetulla koulutuksen nimen osalla ja rajaimilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: query
   |          name: searchPhrase
   |          schema:
   |            type: string
   |          required: false
   |          description: Hakufraasi (nimen osa). Vähimmäispituus on 3 merkkiä.
   |          example: Hevostalous
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |            default: fi
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |        - in: query
   |          name: sort
   |          schema:
   |            type: string
   |            default: score
   |          required: false
   |          description: Järjestysperuste. 'name' tai 'score'
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |            default: desc
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |        - in: query
   |          name: koulutustyyppi
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista koulutustyyppejä
   |          example: amm,amm-muu,yo,amk,amm-tutkinnon-osa,amm-osaamisala
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |            default: nil
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |        - in: query
   |          name: hakukaynnissa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on haku käynnissä
   |        - in: query
   |          name: jotpa
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia joilla on JOTPA-rahoitus
   |        - in: query
   |          name: tyovoimakoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat työvoimakoulutusta
   |        - in: query
   |          name: taydennyskoulutus
   |          schema:
   |            type: boolean
   |            default: false
   |          required: false
   |          description: Haetaanko koulutuksia jotka ovat täydennyskoulutusta
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            application/json:
   |              schema:
   |                type: json
   |        '404':
   |          description: Not found
   |        '400':
   |          description: Bad request")


(defn- parse-constraints [constraints]
  {:koulutustyyppi        (->> (:koulutustyyppi constraints) (comma-separated-string->vec))
   :sijainti              (comma-separated-string->vec (:sijainti constraints))
   :opetuskieli           (comma-separated-string->vec (:opetuskieli constraints))
   :koulutusala           (comma-separated-string->vec (:koulutusala constraints))
   :opetustapa            (comma-separated-string->vec (:opetustapa constraints))
   :valintatapa           (comma-separated-string->vec (:valintatapa constraints))
   :hakukaynnissa         (:hakukaynnissa constraints)
   :hakutapa              (comma-separated-string->vec (:hakutapa constraints))
   :jotpa                 (:jotpa constraints)
   :tyovoimakoulutus      (:tyovoimakoulutus constraints)
   :taydennyskoulutus     (:taydennyskoulutus constraints)
   :yhteishaku            (comma-separated-string->vec (:yhteishaku constraints))
   :pohjakoulutusvaatimus (comma-separated-string->vec (:pohjakoulutusvaatimus constraints))
   :lukiopainotukset      (comma-separated-string->vec (:lukiopainotukset constraints))
   :lukiolinjaterityinenkoulutustehtava (comma-separated-string->vec (:lukiolinjaterityinenkoulutustehtava constraints))
   :osaamisala            (comma-separated-string->vec (:osaamisala constraints))
   :oppilaitos            (comma-separated-string->vec (:oppilaitos constraints))})

(defn ->search-with-validated-params
  [f keyword lng page size sort order constraints]
  (cond
    (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
    (not (some #{sort} ["name" "score"])) (bad-request "Virheellinen järjestys ('name'/'score')")
    (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys ('asc'/'desc')")
    (and (not (nil? keyword))
         (> 3 (count keyword))) (bad-request "Hakusana on liian lyhyt")
    :else (ok (f keyword
                 lng
                 page
                 size
                 sort
                 order
                 (parse-constraints constraints)))))

(defn- ->search-subentities-with-validated-params
  [f oid lng page size order tuleva constraints]
  (cond
    (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
    (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys")
    :else (if-let [result (f oid
                             lng
                             page
                             size
                             order
                             tuleva
                             (parse-constraints constraints))]
            (ok result)
            (not-found "Not found"))))

(defn ->autocomplete-search-with-validated-params
  [search-fn search-phrase lng sort order constraints]
  (cond
    (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
    (not (some #{sort} ["name" "score"])) (bad-request "Virheellinen järjestys ('name'/'score')")
    (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys ('asc'/'desc')")
    (and (not (nil? search-phrase))
         (> 3 (count search-phrase))) (bad-request "Hakusana on liian lyhyt")
    :else (ok (search-fn search-phrase
                 lng
                 sort
                 order
                 (parse-constraints constraints)))))

(def routes
  (context "/search" []

    ;; Jos muokkaat /filters-rajapintaa varmista ettei externalin vastaava rajapinta muutu samalla
    (GET "/filters" [:as request]
      (with-access-logging request (if-let [result (filters/generate-filter-counts)]
                                     (ok result)
                                     (not-found "Not found"))))

    ;; Jos muokkaat /filters_as_array-rajapintaa varmista ettei externalin vastaava rajapinta muutu samalla
    (GET "/filters_as_array" [:as request]
      (with-access-logging request (if-let [result (filters/flattened-filter-counts false)]
                                     (ok result)
                                     (not-found "Not found"))))

    (GET "/koulutukset" [:as request]
      :query-params [{keyword               :- String nil}
                     {page                  :- Long 1}
                     {size                  :- Long 20}
                     {lng                   :- String "fi"}
                     {sort                  :- String "score"}
                     {order                 :- String "desc"}
                     {koulutustyyppi        :- String nil}
                     {sijainti              :- String nil}
                     {opetuskieli           :- String nil}
                     {koulutusala           :- String nil}
                     {opetustapa            :- String nil}
                     {valintatapa           :- String nil}
                     {hakukaynnissa         :- Boolean false}
                     {jotpa                 :- Boolean false}
                     {tyovoimakoulutus      :- Boolean false}
                     {taydennyskoulutus     :- Boolean false}
                     {hakutapa              :- String nil}
                     {yhteishaku            :- String nil}
                     {pohjakoulutusvaatimus :- String nil}
                     {lukiopainotukset      :- String nil}
                     {lukiolinjaterityinenkoulutustehtava :- String nil}
                     {osaamisala            :- String nil}]
      (with-access-logging request (->search-with-validated-params
                                     koulutus-search/search
                                     keyword
                                     lng
                                     page
                                     size
                                     sort
                                     order
                                     {:koulutustyyppi koulutustyyppi
                                      :sijainti sijainti
                                      :opetuskieli opetuskieli
                                      :koulutusala koulutusala
                                      :opetustapa opetustapa
                                      :valintatapa valintatapa
                                      :hakukaynnissa hakukaynnissa
                                      :jotpa jotpa
                                      :tyovoimakoulutus tyovoimakoulutus
                                      :taydennyskoulutus taydennyskoulutus
                                      :hakutapa hakutapa
                                      :yhteishaku yhteishaku
                                      :pohjakoulutusvaatimus pohjakoulutusvaatimus
                                      :lukiopainotukset lukiopainotukset
                                      :lukiolinjaterityinenkoulutustehtava lukiolinjaterityinenkoulutustehtava
                                      :osaamisala osaamisala})))

    (GET "/koulutus/:oid/jarjestajat" [:as request]
      :path-params [oid :- String]
      :query-params [{tuleva                :- Boolean false}
                     {page                  :- Long 1}
                     {size                  :- Long 20}
                     {lng                   :- String "fi"}
                     {order                 :- String "asc"}
                     {sijainti              :- String nil}
                     {opetuskieli           :- String nil}
                     {koulutusala           :- String nil}
                     {opetustapa            :- String nil}
                     {valintatapa           :- String nil}
                     {hakukaynnissa         :- Boolean false}
                     {jotpa                 :- Boolean false}
                     {tyovoimakoulutus      :- Boolean false}
                     {taydennyskoulutus     :- Boolean false}
                     {hakutapa              :- String nil}
                     {yhteishaku            :- String nil}
                     {pohjakoulutusvaatimus :- String nil}
                     {lukiopainotukset      :- String nil}
                     {lukiolinjaterityinenkoulutustehtava :- String nil}
                     {osaamisala            :- String nil}
                     {oppilaitos            :- String nil}]
      (with-access-logging request (->search-subentities-with-validated-params
                                     koulutus-search/search-koulutuksen-jarjestajat
                                     oid
                                     lng
                                     page
                                     size
                                     order
                                     tuleva
                                     {:koulutustyyppi nil
                                      :sijainti sijainti
                                      :opetuskieli opetuskieli
                                      :koulutusala koulutusala
                                      :opetustapa opetustapa
                                      :valintatapa valintatapa
                                      :hakukaynnissa hakukaynnissa
                                      :jotpa jotpa
                                      :tyovoimakoulutus tyovoimakoulutus
                                      :taydennyskoulutus taydennyskoulutus
                                      :hakutapa hakutapa
                                      :yhteishaku yhteishaku
                                      :pohjakoulutusvaatimus pohjakoulutusvaatimus
                                      :lukiopainotukset lukiopainotukset
                                      :lukiolinjaterityinenkoulutustehtava lukiolinjaterityinenkoulutustehtava
                                      :osaamisala osaamisala
                                      :oppilaitos oppilaitos})))

    (GET "/oppilaitokset" [:as request]
      :query-params [{keyword               :- String nil}
                     {page                  :- Long 1}
                     {size                  :- Long 20}
                     {lng                   :- String "fi"}
                     {sort                  :- String "score"}
                     {order                 :- String "desc"}
                     {koulutustyyppi        :- String nil}
                     {sijainti              :- String nil}
                     {opetuskieli           :- String nil}
                     {koulutusala           :- String nil}
                     {opetustapa            :- String nil}
                     {valintatapa           :- String nil}
                     {hakukaynnissa         :- Boolean false}
                     {jotpa                 :- Boolean false}
                     {tyovoimakoulutus      :- Boolean false}
                     {taydennyskoulutus     :- Boolean false}
                     {hakutapa              :- String nil}
                     {yhteishaku            :- String nil}
                     {pohjakoulutusvaatimus :- String nil}
                     {lukiopainotukset      :- String nil}
                     {lukiolinjaterityinenkoulutustehtava :- String nil}
                     {osaamisala            :- String nil}]
      (with-access-logging request (->search-with-validated-params oppilaitos-search/search
                                                                   keyword
                                                                   lng
                                                                   page
                                                                   size
                                                                   sort
                                                                   order
                                                                   {:koulutustyyppi koulutustyyppi
                                                                    :sijainti sijainti
                                                                    :opetuskieli opetuskieli
                                                                    :koulutusala koulutusala
                                                                    :opetustapa opetustapa
                                                                    :valintatapa valintatapa
                                                                    :hakukaynnissa hakukaynnissa
                                                                    :jotpa jotpa
                                                                    :tyovoimakoulutus tyovoimakoulutus
                                                                    :taydennyskoulutus taydennyskoulutus
                                                                    :hakutapa hakutapa
                                                                    :yhteishaku yhteishaku
                                                                    :pohjakoulutusvaatimus pohjakoulutusvaatimus
                                                                    :lukiopainotukset lukiopainotukset
                                                                    :lukiolinjaterityinenkoulutustehtava lukiolinjaterityinenkoulutustehtava
                                                                    :osaamisala osaamisala})))

    (GET "/oppilaitos/:oid/tarjonta" [:as request]
      :path-params [oid :- String]
      :query-params [{tuleva         :- Boolean false}
                     {page           :- Long 1}
                     {size           :- Long 20}
                     {lng            :- String "fi"}
                     {order          :- String "asc"}
                     {koulutustyyppi :- String nil}
                     {sijainti       :- String nil}
                     {opetuskieli    :- String nil}
                     {koulutusala    :- String nil}
                     {opetustapa     :- String nil}]
      (with-access-logging request (->search-subentities-with-validated-params oppilaitos-search/search-oppilaitoksen-tarjonta
                                                                               oid
                                                                               lng
                                                                               page
                                                                               size
                                                                               order
                                                                               tuleva
                                                                               {:koulutustyyppi koulutustyyppi
                                                                                :sijainti sijainti
                                                                                :opetuskieli opetuskieli
                                                                                :koulutusala koulutusala
                                                                                :opetustapa opetustapa
                                                                                :valintatapa nil
                                                                                :hakukaynnissa nil
                                                                                :hakutapa nil
                                                                                :yhteishaku nil
                                                                                :pohjakoulutusvaatimus nil
                                                                                :lukiopainotukset nil
                                                                                :lukiolinjaterityinenkoulutustehtava nil
                                                                                :osaamisala nil})))

    (GET "/oppilaitoksen-osa/:oid/tarjonta" [:as request]
      :path-params [oid :- String]
      :query-params [{tuleva         :- Boolean false}
                     {page           :- Long 1}
                     {size           :- Long 20}
                     {lng            :- String "fi"}
                     {order          :- String "asc"}]
      (with-access-logging request (cond
                                     (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
                                     (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys")
                                     :else (if-let [result (oppilaitos-search/search-oppilaitoksen-osan-tarjonta oid lng page size order tuleva)]
                                             (ok result)
                                             (not-found "Not found")))))

   (GET "/autocomplete" [:as request]
        :query-params [{searchPhrase          :- String nil}
                       {lng                   :- String "fi"}
                       {sort                  :- String "name"}
                       {order                 :- String "asc"}
                       {koulutustyyppi        :- String nil}
                       {sijainti              :- String nil}
                       {opetuskieli           :- String nil}
                       {koulutusala           :- String nil}
                       {opetustapa            :- String nil}
                       {valintatapa           :- String nil}
                       {hakukaynnissa         :- Boolean false}
                       {jotpa                 :- Boolean false}
                       {tyovoimakoulutus      :- Boolean false}
                       {taydennyskoulutus     :- Boolean false}
                       {hakutapa              :- String nil}
                       {yhteishaku            :- String nil}
                       {pohjakoulutusvaatimus :- String nil}
                       {lukiopainotukset      :- String nil}
                       {lukiolinjaterityinenkoulutustehtava :- String nil}
                       {osaamisala            :- String nil}]
        (->autocomplete-search-with-validated-params koulutus-search/autocomplete-search
                                        searchPhrase
                                        lng
                                        sort
                                        order
                                        {:koulutustyyppi koulutustyyppi
                                         :sijainti sijainti
                                         :opetuskieli opetuskieli
                                         :koulutusala koulutusala
                                         :opetustapa opetustapa
                                         :valintatapa valintatapa
                                         :hakukaynnissa hakukaynnissa
                                         :jotpa jotpa
                                         :tyovoimakoulutus tyovoimakoulutus
                                         :taydennyskoulutus taydennyskoulutus
                                         :hakutapa hakutapa
                                         :yhteishaku yhteishaku
                                         :pohjakoulutusvaatimus pohjakoulutusvaatimus
                                         :lukiopainotukset lukiopainotukset
                                         :lukiolinjaterityinenkoulutustehtava lukiolinjaterityinenkoulutustehtava
                                         :osaamisala osaamisala}))))

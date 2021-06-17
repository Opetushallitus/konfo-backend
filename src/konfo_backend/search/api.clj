(ns konfo-backend.search.api
  (:require
    [konfo-backend.search.koulutus.search :as koulutus-search]
    [konfo-backend.search.oppilaitos.search :as oppilaitos-search]
    [konfo-backend.search.filters :as filters]
    [compojure.api.core :refer [GET context]]
    [ring.util.http-response :refer :all]
    [clj-log.access-log :refer [with-access-logging]]
    [konfo-backend.tools :refer [comma-separated-string->vec amm-muu->alatyypit]]))

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
   |          default: nil
   |          example: Hevostalous
   |        - in: query
   |          name: page
   |          schema:
   |            type: number
   |          required: false
   |          description: Hakutuloksen sivunumero
   |          default: 1
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |          required: false
   |          description: Hakutuloksen sivun koko
   |          default: 20
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |          default: fi
   |        - in: query
   |          name: sort
   |          schema:
   |            type: string
   |          required: false
   |          description: Järjestysperuste. 'name' tai 'score'
   |          default: score
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |          default: desc
   |        - in: query
   |          name: koulutustyyppi
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista koulutustyyppejä
   |          example: amm,amm-muu,yo,amk,amm-tutkinnon-osa,amm-osaamisala
   |          default: nil
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |          default: nil
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |          default: nil
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |          default: nil
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |          default: nil
   |        - in: query
   |          name: valintatapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu valintatapojen koodeja
   |          example: valintatapajono_av, valintatapajono_tv
   |          default: nil
   |        - in: query
   |          name: hakukaynnissa
   |          schema:
   |            type: boolean
   |          required: false
   |          description: Haetaanko koulutuksia joilla on haku käynnissä
   |          default: false
   |        - in: query
   |          name: hakutapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu hakutapojen koodeja
   |          example: hakutapa_01, hakutapa_03
   |          default: nil
   |        - in: query
   |          name: yhteishaku
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista yhteishakujen oideja
   |          example: 1.2.246.562.29.00000000000000000800
   |          default: nil
   |        - in: query
   |          name: pohjakoulutusvaatimus
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu pohjakoulutusvaatimusten koodeja
   |          example: pohjakoulutusvaatimuskonfo_am, pohjakoulutusvaatimuskonfo_102
   |          default: nil
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
   |          required: false
   |          description: Hakutuloksen sivunumero
   |          default: 1
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |          required: false
   |          description: Hakutuloksen sivun koko
   |          default: 20
   |        - in: query
   |          name: tuleva
   |          schema:
   |            type: boolean
   |          required: false
   |          description: Haetaanko tulevia vai tämänhetkisiä tarjoajia.
   |            Tarjoaja on tuleva, jos se lisätty koulutukselle tarjoajaksi mutta se ei ole vielä julkaissut omaa toteutusta.
   |          default: false
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |          default: fi
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |          default: desc
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |          default: nil
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |          default: nil
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |          default: nil
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |          default: nil
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
   |          default: nil
   |          example: Hevostalous
   |        - in: query
   |          name: page
   |          schema:
   |            type: number
   |          required: false
   |          description: Hakutuloksen sivunumero
   |          default: 1
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |          required: false
   |          description: Hakutuloksen sivun koko
   |          default: 20
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |          default: fi
   |        - in: query
   |          name: sort
   |          schema:
   |            type: string
   |          required: false
   |          description: Järjestysperuste. 'name' tai 'score'
   |          default: score
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |          default: desc
   |        - in: query
   |          name: koulutustyyppi
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista koulutustyyppejä
   |          example: amm,amm-muu,yo,amk,amm-tutkinnon-osa,amm-osaamisala
   |          default: nil
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |          default: nil
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |          default: nil
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |          default: nil
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |          default: nil
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
   |          required: false
   |          description: Hakutuloksen sivunumero
   |          default: 1
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |          required: false
   |          description: Hakutuloksen sivun koko
   |          default: 20
   |        - in: query
   |          name: tuleva
   |          schema:
   |            type: boolean
   |          required: false
   |          description: Haetaanko tuleva vai tämänhetkinen tarjonta.
   |            Tarjoaja on tuleva, jos se lisätty koulutukselle tarjoajaksi mutta se ei ole vielä julkaissut omaa toteutusta.
   |          default: false
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |          default: fi
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |          default: desc
   |        - in: query
   |          name: koulutustyyppi
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu lista koulutustyyppejä
   |          example: amm,kk,lk
   |          default: nil
   |        - in: query
   |          name: sijainti
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu kuntien ja maakuntien koodeja
   |          example: kunta_091,maakunta_01,maakunta_03
   |          default: nil
   |        - in: query
   |          name: opetuskieli
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetuskielten koodeja
   |          example: oppilaitoksenopetuskieli_1,oppilaitoksenopetuskieli_2
   |          default: nil
   |        - in: query
   |          name: koulutusala
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu koulutusalojen koodeja
   |          example: kansallinenkoulutusluokitus2016koulutusalataso1_01, kansallinenkoulutusluokitus2016koulutusalataso1_02
   |          default: nil
   |        - in: query
   |          name: opetustapa
   |          schema:
   |            type: string
   |          required: false
   |          description: Pilkulla eroteltu opetustapojen koodeja
   |          example: opetuspaikkakk_1, opetuspaikkakk_2
   |          default: nil
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
   |          required: false
   |          description: Hakutuloksen sivunumero
   |          default: 1
   |        - in: query
   |          name: size
   |          schema:
   |            type: number
   |          required: false
   |          description: Hakutuloksen sivun koko
   |          default: 20
   |        - in: query
   |          name: tuleva
   |          schema:
   |            type: boolean
   |          required: false
   |          description: Haetaanko tuleva vai tämänhetkinen tarjonta.
   |            Tarjoaja on tuleva, jos se lisätty koulutukselle tarjoajaksi mutta se ei ole vielä julkaissut omaa toteutusta.
   |          default: false
   |        - in: query
   |          name: lng
   |          schema:
   |            type: string
   |          required: false
   |          description: Haun kieli. 'fi', 'sv' tai 'en'
   |          default: fi
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   |          default: desc
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


(defn- parse-constraints [koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakukaynnissa hakutapa yhteishaku pohjakoulutusvaatimus]
  {:koulutustyyppi        (->> koulutustyyppi (comma-separated-string->vec) (amm-muu->alatyypit))
   :sijainti              (comma-separated-string->vec sijainti)
   :opetuskieli           (comma-separated-string->vec opetuskieli)
   :koulutusala           (comma-separated-string->vec koulutusala)
   :opetustapa            (comma-separated-string->vec opetustapa)
   :valintatapa           (comma-separated-string->vec valintatapa)
   :hakukaynnissa         hakukaynnissa
   :hakutapa              (comma-separated-string->vec hakutapa)
   :yhteishaku            (comma-separated-string->vec yhteishaku)
   :pohjakoulutusvaatimus (comma-separated-string->vec pohjakoulutusvaatimus)})

(defn ->search-with-validated-params
  [f keyword lng page size sort order koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakukaynnissa hakutapa yhteishaku pohjakoulutusvaatimus]
  (let [constraints (parse-constraints koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakukaynnissa hakutapa yhteishaku pohjakoulutusvaatimus)]
    (cond
      (not (some #{lng} ["fi" "sv" "en"]))  (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
      (not (some #{sort} ["name" "score"])) (bad-request "Virheellinen järjestys ('name'/'score')")
      (not (some #{order} ["asc" "desc"]))  (bad-request "Virheellinen järjestys ('asc'/'desc')")
      (and (not (nil? keyword))
           (> 3 (count keyword)))          (bad-request "Hakusana on liian lyhyt")
      :else                                (ok (f keyword
                                                  lng
                                                  page
                                                  size
                                                  sort
                                                  order
                                                  constraints)))))

(defn- ->search-subentities-with-validated-params
  [f oid lng page size order tuleva koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakukaynnissa hakutapa yhteishaku pohjakoulutusvaatimus]
  (let [constraints (parse-constraints koulutustyyppi sijainti opetuskieli koulutusala opetustapa valintatapa hakukaynnissa hakutapa yhteishaku pohjakoulutusvaatimus)]
    (cond
      (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
      (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys")
      :else (if-let [result (f oid
                               lng
                               page
                               size
                               order
                               tuleva
                               constraints)]
              (ok result)
              (not-found "Not found")))))

(def routes
  (context "/search" []

    (GET "/filters" [:as request]
      (with-access-logging request (if-let [result (filters/generate-filter-counts)]
                                     (ok result)
                                     (not-found "Not found"))))

    (GET "/filters_as_array" [:as request]
      (with-access-logging request (if-let [result (filters/flattened-filter-counts)]
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
                        {hakutapa              :- String nil}
                        {yhteishaku            :- String nil}
                        {pohjakoulutusvaatimus :- String nil}]
         (with-access-logging request (->search-with-validated-params koulutus-search/search
                                                                      keyword
                                                                      lng
                                                                      page
                                                                      size
                                                                      sort
                                                                      order
                                                                      koulutustyyppi
                                                                      sijainti
                                                                      opetuskieli
                                                                      koulutusala
                                                                      opetustapa
                                                                      valintatapa
                                                                      hakukaynnissa
                                                                      hakutapa
                                                                      yhteishaku
                                                                      pohjakoulutusvaatimus)))

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
                        {hakutapa              :- String nil}
                        {yhteishaku            :- String nil}
                        {pohjakoulutusvaatimus :- String nil}]
         (with-access-logging request (->search-subentities-with-validated-params koulutus-search/search-koulutuksen-jarjestajat
                                                                                  oid
                                                                                  lng
                                                                                  page
                                                                                  size
                                                                                  order
                                                                                  tuleva
                                                                                  nil
                                                                                  sijainti
                                                                                  opetuskieli
                                                                                  koulutusala
                                                                                  opetustapa
                                                                                  valintatapa
                                                                                  hakukaynnissa
                                                                                  hakutapa
                                                                                  yhteishaku
                                                                                  pohjakoulutusvaatimus)))

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
                        {hakutapa              :- String nil}
                        {yhteishaku            :- String nil}
                        {pohjakoulutusvaatimus :- String nil}]
         (with-access-logging request (->search-with-validated-params oppilaitos-search/search
                                                                      keyword
                                                                      lng
                                                                      page
                                                                      size
                                                                      sort
                                                                      order
                                                                      koulutustyyppi
                                                                      sijainti
                                                                      opetuskieli
                                                                      koulutusala
                                                                      opetustapa
                                                                      valintatapa
                                                                      hakukaynnissa
                                                                      hakutapa
                                                                      yhteishaku
                                                                      pohjakoulutusvaatimus)))

    (GET "/oppilaitos/:oid/tarjonta" [:as request]
         :path-params [oid :- String]
         :query-params [{tuleva         :- Boolean false}
                        {page           :- Long 1}
                        {size           :- Long 20}
                        {lng            :- String "fi"}
                        {order          :- String"asc"}
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
                                                                                  koulutustyyppi
                                                                                  sijainti
                                                                                  opetuskieli
                                                                                  koulutusala
                                                                                  opetustapa
                                                                                  nil
                                                                                  nil
                                                                                  nil
                                                                                  nil
                                                                                  nil)))

    (GET "/oppilaitoksen-osa/:oid/tarjonta" [:as request]
         :path-params [oid :- String]
         :query-params [{tuleva         :- Boolean false}
                        {page           :- Long 1}
                        {size           :- Long 20}
                        {lng            :- String "fi"}
                        {order          :- String"asc"}]
         (with-access-logging request (cond
                                        (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
                                        (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys")
                                        :else (if-let [result (oppilaitos-search/search-oppilaitoksen-osan-tarjonta oid lng page size order tuleva)]
                                                (ok result)
                                                (not-found "Not found")))))))

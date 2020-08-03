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
   |        - search
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
   |        - search
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
   |        - search
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
   |        - search
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
   |        - search
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
   |        - search
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

(defn- ->search-with-validated-params
  [f keyword lng page size sort order koulutustyyppi sijainti opetuskieli koulutusala]
  (let [koulutustyypit      (comma-separated-string->vec koulutustyyppi)
        sijainnit           (comma-separated-string->vec sijainti)
        opetuskielet        (comma-separated-string->vec opetuskieli)
        koulutusalat        (comma-separated-string->vec koulutusala)]

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
                                                  :koulutustyyppi koulutustyypit
                                                  :sijainti       sijainnit
                                                  :opetuskieli    opetuskielet
                                                  :koulutusala    koulutusalat)))))

(def routes
  (context "/search" []

    (GET "/filters" [:as request]
      (with-access-logging request (if-let [result (filters/hierarkia)]
                                     (ok result)
                                     (not-found "Not found"))))

    (GET "/filters_as_array" [:as request]
      (with-access-logging request (if-let [result (filters/flattened-hierarkia)]
                                     (ok result)
                                     (not-found "Not found"))))

    (GET "/koulutukset" [:as request]
         :query-params [{keyword        :- String nil}
                        {page           :- Long 1}
                        {size           :- Long 20}
                        {lng            :- String "fi"}
                        {sort           :- String "score"}
                        {order          :- String "desc"}
                        {koulutustyyppi :- String nil}
                        {sijainti       :- String nil}
                        {opetuskieli    :- String nil}
                        {koulutusala    :- String nil}]
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
                                                                      koulutusala)))

    (GET "/koulutus/:oid/jarjestajat" [:as request]
         :path-params [oid :- String]
         :query-params [{tuleva         :- Boolean false}
                        {page           :- Long 1}
                        {size           :- Long 20}
                        {lng            :- String "fi"}
                        {order          :- String "asc"}]
         (with-access-logging request (cond
                                        (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
                                        (not (some #{order} ["asc" "desc"]))  (bad-request "Virheellinen järjestys")
                                        :else (if-let [result (koulutus-search/search-koulutuksen-jarjestajat oid lng page size order tuleva)]
                                                (ok result)
                                                (not-found "Not found")))))

    (GET "/oppilaitokset" [:as request]
         :query-params [{keyword        :- String nil}
                        {page           :- Long 1}
                        {size           :- Long 20}
                        {lng            :- String "fi"}
                        {sort           :- String "score"}
                        {order          :- String "desc"}
                        {koulutustyyppi :- String nil}
                        {sijainti       :- String nil}
                        {opetuskieli    :- String nil}
                        {koulutusala    :- String nil}]
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
                                                                      koulutusala)))

    (GET "/oppilaitos/:oid/tarjonta" [:as request]
         :path-params [oid :- String]
         :query-params [{tuleva         :- Boolean false}
                        {page           :- Long 1}
                        {size           :- Long 20}
                        {lng            :- String "fi"}
                        {order          :- String"asc"}]
         (with-access-logging request (cond
                                        (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
                                        (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys")
                                        :else (if-let [result (oppilaitos-search/search-oppilaitoksen-tarjonta oid lng page size order tuleva)]
                                                (ok result)
                                                (not-found "Not found")))))))
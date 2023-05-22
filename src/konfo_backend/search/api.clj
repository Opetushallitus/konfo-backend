(ns konfo-backend.search.api
  (:require
   [konfo-backend.search.rajain-definitions :refer [koulutustyyppi sijainti opetuskieli koulutusala opetustapa
                                                    valintatapa hakukaynnissa jotpa tyovoimakoulutus taydennyskoulutus
                                                    hakutapa yhteishaku pohjakoulutusvaatimus oppilaitos
                                                    lukiopainotukset lukiolinjaterityinenkoulutustehtava osaamisala
                                                    opetusaika koulutuksenkestokuukausina]]
   [konfo-backend.search.koulutus.search :as koulutus-search]
   [konfo-backend.search.oppilaitos.search :as oppilaitos-search]
   [konfo-backend.search.rajain-counts :as rajain-counts]
   [compojure.api.core :refer [GET context]]
   [ring.util.http-response :refer [bad-request not-found ok]]
   [clj-log.access-log :refer [with-access-logging]]
   [konfo-backend.tools :refer [comma-separated-string->vec]]
   [clojure.string :as string]))

(def paths (str
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
   "
            (:desc koulutustyyppi) "\n"
            (:desc sijainti) "\n"
            (:desc opetuskieli) "\n"
            (:desc koulutusala) "\n"
            (:desc opetustapa) "\n"
            (:desc opetusaika) "\n"
            (:desc koulutuksenkestokuukausina) "\n"
            (:desc valintatapa) "\n"
            (:desc hakukaynnissa) "\n"
            (:desc jotpa) "\n"
            (:desc tyovoimakoulutus) "\n"
            (:desc taydennyskoulutus) "\n"
            (:desc hakutapa) "\n"
            (:desc yhteishaku) "\n"
            (:desc pohjakoulutusvaatimus) "\n"
            "
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
   "
            (:desc sijainti) "\n"
            (:desc opetuskieli) "\n"
            (:desc koulutusala) "\n"
            (:desc opetustapa) "\n"
            (:desc opetusaika) "\n"
            (:desc koulutuksenkestokuukausina) "\n"
            (:desc valintatapa) "\n"
            (:desc hakukaynnissa) "\n"
            (:desc jotpa) "\n"
            (:desc tyovoimakoulutus) "\n"
            (:desc taydennyskoulutus) "\n"
            (:desc lukiopainotukset) "\n"
            (:desc lukiolinjaterityinenkoulutustehtava) "\n"
            (:desc osaamisala) "\n"
            (:desc oppilaitos) "\n"
            "
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
   "
            (:desc koulutustyyppi) "\n"
            (:desc sijainti) "\n"
            (:desc opetuskieli) "\n"
            (:desc koulutusala) "\n"
            (:desc opetustapa) "\n"
            (:desc opetusaika) "\n"
            (:desc koulutuksenkestokuukausina) "\n"
            (:desc valintatapa) "\n"
            (:desc hakukaynnissa) "\n"
            (:desc jotpa) "\n"
            (:desc tyovoimakoulutus) "\n"
            (:desc taydennyskoulutus) "\n"
            (:desc hakutapa) "\n"
            (:desc yhteishaku) "\n"
            (:desc pohjakoulutusvaatimus) "\n"
            "
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
   "
            (:desc koulutustyyppi) "\n"
            (:desc sijainti) "\n"
            (:desc opetuskieli) "\n"
            (:desc koulutusala) "\n"
            (:desc opetustapa) "\n"
            (:desc opetusaika) "\n"
            (:desc koulutuksenkestokuukausina) "\n"
            "
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
   |      summary: Hae koulutuksia ja oppilaitoksia ennakoivaa hakua varten
   |      description: Hakee koulutuksia ja oppilaitoksia hakusanalla ja rajaimilla. Huom.! Vain Opintopolun sisäiseen käyttöön
   |      parameters:
   |        - in: query
   |          name: searchPhrase
   |          schema:
   |            type: string
   |          required: false
   |          description: Hakusanat. Vähimmäispituus on 3 merkkiä.
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
   |          name: size
   |          schema:
   |            type: integer
   |            default: 5
   |          required: false
   |          description: Montako koulutusta ja oppilaitosta noudetaan. Oletuksena noudetaan siis 5 koulutusta ja 5 oppilaitosta.
   |        - in: query
   |          name: order
   |          schema:
   |            type: string
   |            default: desc
   |          required: false
   |          description: Järjestys. 'asc' tai 'desc'
   "
            (:desc koulutustyyppi) "\n"
            (:desc sijainti) "\n"
            (:desc opetuskieli) "\n"
            (:desc koulutusala) "\n"
            (:desc opetustapa) "\n"
            (:desc opetusaika) "\n"
            (:desc valintatapa) "\n"
            (:desc hakukaynnissa) "\n"
            (:desc jotpa) "\n"
            (:desc tyovoimakoulutus) "\n"
            (:desc taydennyskoulutus) "\n"
            (:desc hakutapa) "\n"
            (:desc yhteishaku) "\n"
            (:desc pohjakoulutusvaatimus) "\n"
            (:desc koulutuksenkestokuukausina) "\n"
            "
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
   |          description: Bad request"))


(defn- create-constraints [rajain-params]
  {:koulutustyyppi        (->> (:koulutustyyppi rajain-params) (comma-separated-string->vec))
   :sijainti              (comma-separated-string->vec (:sijainti rajain-params))
   :opetuskieli           (comma-separated-string->vec (:opetuskieli rajain-params))
   :koulutusala           (comma-separated-string->vec (:koulutusala rajain-params))
   :opetustapa            (comma-separated-string->vec (:opetustapa rajain-params))
   :opetusaika            (comma-separated-string->vec (:opetusaika rajain-params))
   :valintatapa           (comma-separated-string->vec (:valintatapa rajain-params))
   :hakukaynnissa         (:hakukaynnissa rajain-params)
   :hakutapa              (comma-separated-string->vec (:hakutapa rajain-params))
   :jotpa                 (:jotpa rajain-params)
   :tyovoimakoulutus      (:tyovoimakoulutus rajain-params)
   :taydennyskoulutus     (:taydennyskoulutus rajain-params)
   :yhteishaku            (comma-separated-string->vec (:yhteishaku rajain-params))
   :pohjakoulutusvaatimus (comma-separated-string->vec (:pohjakoulutusvaatimus rajain-params))
   :lukiopainotukset      (comma-separated-string->vec (:lukiopainotukset rajain-params))
   :lukiolinjaterityinenkoulutustehtava (comma-separated-string->vec (:lukiolinjaterityinenkoulutustehtava rajain-params))
   :osaamisala            (comma-separated-string->vec (:osaamisala rajain-params))
   :oppilaitos            (comma-separated-string->vec (:oppilaitos rajain-params))
   :koulutuksenkestokuukausina (let [numbers (map #(Integer/parseInt %) (comma-separated-string->vec (:koulutuksenkestokuukausina rajain-params)))]
                                 (when (> (count numbers) 0)
                                   (if (> (count numbers) 1)
                                     (vec [(apply min numbers) (apply max numbers)])
                                     (vec numbers))))})

(defn- invalid-koulutuksenkesto?
  [rajain-params]
  (and (not (nil? (:koulutuksenkestokuukausina rajain-params)))
       (re-find #"\D" (string/replace (:koulutuksenkestokuukausina rajain-params) "," ""))))

(defn ->search-with-validated-params
  [do-search keyword lng page size sort order rajain-params]
  (cond
    (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
    (not (some #{sort} ["name" "score"])) (bad-request "Virheellinen järjestys ('name'/'score')")
    (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys ('asc'/'desc')")
    (and (not (nil? keyword))
         (> 3 (count keyword))) (bad-request "Hakusana on liian lyhyt")
    (invalid-koulutuksenkesto? rajain-params) (bad-request "Virheellinen koulutuksen kesto")
    :else (ok (do-search keyword
                         lng
                         page
                         size
                         sort
                         order
                         (create-constraints rajain-params)))))

(defn- ->search-subentities-with-validated-params
  [do-search oid lng page size order tuleva rajain-params]
  (cond
    (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli")
    (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys")
    (invalid-koulutuksenkesto? rajain-params) (bad-request "Virheellinen koulutuksen kesto")
    :else (if-let [result (do-search oid
                                     lng
                                     page
                                     size
                                     order
                                     tuleva
                                     (create-constraints rajain-params))]
            (ok result)
            (not-found "Not found"))))

(defn with-validated-params [search-phrase lng sort order or-else-fn]
  (cond
    (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
    (not (some #{sort} ["name" "score"])) (bad-request "Virheellinen järjestys ('name'/'score')")
    (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys ('asc'/'desc')")
    (and (not (nil? search-phrase))
         (> 3 (count search-phrase))) (bad-request "Hakusana on liian lyhyt")
    :else (ok (or-else-fn))))

(def routes
  (context "/search" []

    ;; Jos muokkaat /filters-rajapintaa varmista ettei externalin vastaava rajapinta muutu samalla
    (GET "/filters" [:as request]
      (with-access-logging request (if-let [result (rajain-counts/generate-default-rajain-counts)]
                                     (ok result)
                                     (not-found "Not found"))))

    ;; Jos muokkaat /filters_as_array-rajapintaa varmista ettei externalin vastaava rajapinta muutu samalla
    (GET "/filters_as_array" [:as request]
      (with-access-logging request (if-let [result (rajain-counts/flattened-rajain-counts)]
                                     (ok result)
                                     (not-found "Not found"))))

    (GET "/koulutukset" request
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
                     {opetusaika            :- String nil}
                     {valintatapa           :- String nil}
                     {hakukaynnissa         :- Boolean false}
                     {jotpa                 :- Boolean false}
                     {tyovoimakoulutus      :- Boolean false}
                     {taydennyskoulutus     :- Boolean false}
                     {hakutapa              :- String nil}
                     {yhteishaku            :- String nil}
                     {pohjakoulutusvaatimus :- String nil}
                     {koulutuksenkestokuukausina :- String nil}]
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
                                     :opetusaika opetusaika
                                     :valintatapa valintatapa
                                     :hakukaynnissa hakukaynnissa
                                     :jotpa jotpa
                                     :tyovoimakoulutus tyovoimakoulutus
                                     :taydennyskoulutus taydennyskoulutus
                                     :hakutapa hakutapa
                                     :yhteishaku yhteishaku
                                     :pohjakoulutusvaatimus pohjakoulutusvaatimus
                                     :koulutuksenkestokuukausina koulutuksenkestokuukausina})))

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
                     {opetusaika            :- String nil}
                     {valintatapa           :- String nil}
                     {hakukaynnissa         :- Boolean false}
                     {jotpa                 :- Boolean false}
                     {tyovoimakoulutus      :- Boolean false}
                     {taydennyskoulutus     :- Boolean false}
                     {hakutapa              :- String nil}
                     {yhteishaku            :- String nil}
                     {pohjakoulutusvaatimus :- String nil}
                     {koulutuksenkestokuukausina :- String nil}
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
                                    {:sijainti sijainti
                                     :opetuskieli opetuskieli
                                     :koulutusala koulutusala
                                     :opetustapa opetustapa
                                     :opetusaika opetusaika
                                     :valintatapa valintatapa
                                     :hakukaynnissa hakukaynnissa
                                     :jotpa jotpa
                                     :tyovoimakoulutus tyovoimakoulutus
                                     :taydennyskoulutus taydennyskoulutus
                                     :hakutapa hakutapa
                                     :yhteishaku yhteishaku
                                     :pohjakoulutusvaatimus pohjakoulutusvaatimus
                                     :koulutuksenkestokuukausina koulutuksenkestokuukausina
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
                     {opetusaika            :- String nil}
                     {valintatapa           :- String nil}
                     {hakukaynnissa         :- Boolean false}
                     {jotpa                 :- Boolean false}
                     {tyovoimakoulutus      :- Boolean false}
                     {taydennyskoulutus     :- Boolean false}
                     {hakutapa              :- String nil}
                     {yhteishaku            :- String nil}
                     {pohjakoulutusvaatimus :- String nil}
                     {koulutuksenkestokuukausina :- String nil}]
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
                                                                    :opetusaika opetusaika
                                                                    :valintatapa valintatapa
                                                                    :hakukaynnissa hakukaynnissa
                                                                    :jotpa jotpa
                                                                    :tyovoimakoulutus tyovoimakoulutus
                                                                    :taydennyskoulutus taydennyskoulutus
                                                                    :hakutapa hakutapa
                                                                    :yhteishaku yhteishaku
                                                                    :pohjakoulutusvaatimus pohjakoulutusvaatimus
                                                                    :koulutuksenkestokuukausina koulutuksenkestokuukausina})))

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
                     {opetustapa     :- String nil}
                     {koulutuksenkestokuukausina :- String nil}]
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
                                                                                :koulutuksenkestokuukausina koulutuksenkestokuukausina})))

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
                     {size                  :- Long 5}
                     {sort                  :- String "score"}
                     {order                 :- String "desc"}
                     {koulutustyyppi        :- String nil}
                     {sijainti              :- String nil}
                     {opetuskieli           :- String nil}
                     {koulutusala           :- String nil}
                     {opetustapa            :- String nil}
                     {opetusaika            :- String nil}
                     {valintatapa           :- String nil}
                     {hakukaynnissa         :- Boolean false}
                     {jotpa                 :- Boolean false}
                     {tyovoimakoulutus      :- Boolean false}
                     {taydennyskoulutus     :- Boolean false}
                     {hakutapa              :- String nil}
                     {yhteishaku            :- String nil}
                     {pohjakoulutusvaatimus :- String nil}
                     {koulutuksenkestokuukausina :- String nil}]
      (with-validated-params
        searchPhrase
        lng
        sort
        order
        (fn []
          (let [search-params [searchPhrase
                               lng
                               size
                               sort
                               order
                               {:koulutustyyppi koulutustyyppi
                                :sijainti sijainti
                                :opetuskieli opetuskieli
                                :koulutusala koulutusala
                                :opetustapa opetustapa
                                :opetusaika opetusaika
                                :valintatapa valintatapa
                                :hakukaynnissa hakukaynnissa
                                :jotpa jotpa
                                :tyovoimakoulutus tyovoimakoulutus
                                :taydennyskoulutus taydennyskoulutus
                                :hakutapa hakutapa
                                :yhteishaku yhteishaku
                                :pohjakoulutusvaatimus pohjakoulutusvaatimus
                                :koulutuksenkestokuukausina koulutuksenkestokuukausina}]]
            {:koulutukset (apply koulutus-search/autocomplete-search search-params)
             :oppilaitokset (apply oppilaitos-search/autocomplete-search search-params)}))))))

(ns konfo-backend.search.api
  (:require
   [konfo-backend.external.schema.common :refer [KonfoKoulutustyyppi Nimi schema-to-swagger-yaml spec-paths-to-swagger-yaml]]
   [konfo-backend.search.rajain-definitions :refer [koulutustyyppi sijainti opetuskieli koulutusala opetustapa
                                                    valintatapa hakukaynnissa jotpa tyovoimakoulutus taydennyskoulutus
                                                    hakutapa yhteishaku pohjakoulutusvaatimus oppilaitos
                                                    lukiopainotukset lukiolinjaterityinenkoulutustehtava osaamisala
                                                    opetusaika alkamiskausi koulutuksenkestokuukausina maksullisuus
                                                    hakualkaapaivissa]]
   [konfo-backend.search.hakukohde.search :as hakukohde-search]
   [konfo-backend.search.koulutus.search :as koulutus-search]
   [konfo-backend.search.oppilaitos.search :as oppilaitos-search]
   [konfo-backend.search.rajain-counts :as rajain-counts]
   [compojure.api.core :refer [GET context]]
   [ring.util.http-response :refer [bad-request not-found ok]]
   [clj-log.access-log :refer [with-access-logging]]
   [konfo-backend.tools :refer [comma-separated-string->vec]]
   [clj-yaml.core :as yaml]
   [schema-tools.core :as st]
   [schema-tools.openapi.core :as openapi]
   [schema.core :as s]
   [clojure.string :as string]))

(def paths-str (str
               "paths:
  /search/filters:
    get:
      tags:
        - internal-search
      summary: Hae hakurajaimet
      description: Palauttaa kaikkien käytössä olevien hakurajainten koodit ja nimet. Huom.! Vain Opintopolun sisäiseen käyttöön
      responses:
        '200':
          description: Ok
          content:
            application/json:
              schema:
                type: json
        '404':
          description: Not found
  /search/filters_as_array:
    get:
      tags:
        - internal-search
      summary: Hae hakurajaimet taulukkomuodossa
      description: Palauttaa kaikkien käytössä olevien hakurajainten koodit ja nimet taulukkomuodossa. Huom.! Vain Opintopolun sisäiseen käyttöön
      responses:
        '200':
          description: Ok
          content:
            application/json:
              schema:
                type: json
        '404':
          description: Not found
  /search/koulutukset:
    get:
      tags:
        - internal-search
      summary: Hae koulutuksia
      description: Hakee koulutuksia annetulla hakusanalla ja rajaimilla. Huom.! Vain Opintopolun sisäiseen käyttöön
      parameters:
        - in: query
          name: keyword
          schema:
            type: string
          required: false
          description: Hakusana. Voi olla tyhjä, jos haetaan vain rajaimilla tai halutaan hakea kaikki.
            Muussa tapauksessa vähimmäispituus on 3 merkkiä.
          example: Hevostalous
        - in: query
          name: page
          schema:
            type: number
            default: 1
          required: false
          description: Hakutuloksen sivunumero
        - in: query
          name: size
          schema:
            type: number
            default: 20
          required: false
          description: Hakutuloksen sivun koko
        - in: query
          name: lng
          schema:
            type: string
            default: fi
          required: false
          description: Haun kieli. 'fi', 'sv' tai 'en'
        - in: query
          name: sort
          schema:
            type: string
            default: score
          required: false
          description: Järjestysperuste. 'name' tai 'score'
        - in: query
          name: order
          schema:
            type: string
            default: desc
          required: false
          description: Järjestys. 'asc' tai 'desc'
            "
            (:desc koulutustyyppi) "\n"
            (:desc sijainti) "\n"
            (:desc opetuskieli) "\n"
            (:desc koulutusala) "\n"
            (:desc opetustapa) "\n"
            (:desc opetusaika) "\n"
            (:desc koulutuksenkestokuukausina) "\n"
            (:desc maksullisuus) "\n"
            (:desc valintatapa) "\n"
            (:desc hakukaynnissa) "\n"
            (:desc jotpa) "\n"
            (:desc tyovoimakoulutus) "\n"
            (:desc taydennyskoulutus) "\n"
            (:desc hakutapa) "\n"
            (:desc yhteishaku) "\n"
            (:desc pohjakoulutusvaatimus) "\n"
            (:desc alkamiskausi) "\n"
            (:desc hakualkaapaivissa) "\n"
        "
      responses:
        '200':
          description: Ok
          content:
            application/json:
              schema:
                type: json
        '404':
          description: Not found
        '400':
          description: Bad request
  /search/koulutus/{oid}/jarjestajat:
    get:
      tags:
        - internal-search
      summary: Hae koulutuksen tarjoajat
      description: Hakee annetun koulutuksen järjestäjiä. Huom.! Vain Opintopolun sisäiseen käyttöön
      parameters:
        - in: path
          name: oid
          schema:
            type: string
          required: true
          description: Koulutuksen yksilöivä oid
          example: 1.2.246.562.13.00000000000000000001
        - in: query
          name: page
          schema:
            type: number
            default: 1
          required: false
          description: Hakutuloksen sivunumero
        - in: query
          name: size
          schema:
            type: number
            default: 20
          required: false
          description: Hakutuloksen sivun koko
        - in: query
          name: tuleva
          schema:
            type: boolean
            default: false
          required: false
          description: Haetaanko tulevia vai tämänhetkisiä tarjoajia.
            Tarjoaja on tuleva, jos se lisätty koulutukselle tarjoajaksi mutta se ei ole vielä julkaissut omaa toteutusta.
        - in: query
          name: lng
          schema:
            type: string
            default: fi
          required: false
          description: Haun kieli. 'fi', 'sv' tai 'en'
        - in: query
          name: order
          schema:
            type: string
            default: desc
          required: false
          description: Järjestys. 'asc' tai 'desc'
"
            (:desc sijainti) "\n"
            (:desc opetuskieli) "\n"
            (:desc koulutusala) "\n"
            (:desc opetustapa) "\n"
            (:desc opetusaika) "\n"
            (:desc koulutuksenkestokuukausina) "\n"
            (:desc maksullisuus) "\n"
            (:desc valintatapa) "\n"
            (:desc hakukaynnissa) "\n"
            (:desc jotpa) "\n"
            (:desc tyovoimakoulutus) "\n"
            (:desc taydennyskoulutus) "\n"
            (:desc lukiopainotukset) "\n"
            (:desc lukiolinjaterityinenkoulutustehtava) "\n"
            (:desc osaamisala) "\n"
            (:desc oppilaitos) "\n"
            (:desc alkamiskausi) "\n"
            (:desc hakualkaapaivissa) "\n"
        "
      responses:
        '200':
          description: Ok
          content:
            application/json:
              schema:
                type: json
        '404':
          description: Not found
        '400':
          description: Bad request
  /search/oppilaitokset:
    get:
      tags:
        - internal-search
      summary: Hae oppilaitoksia
      description: Hakee oppilaitoksia annetulla hakusanalla ja rajaimilla. Huom.! Vain Opintopolun sisäiseen käyttöön
      parameters:
        - in: query
          name: keyword
          schema:
            type: string
          required: false
          description: Hakusana. Voi olla tyhjä, jos haetaan vain rajaimilla tai halutaan hakea kaikki.
            Muussa tapauksessa vähimmäispituus on 3 merkkiä.
          example: Hevostalous
        - in: query
          name: page
          schema:
            type: number
            default: 1
          required: false
          description: Hakutuloksen sivunumero
        - in: query
          name: size
          schema:
            type: number
            default: 20
          required: false
          description: Hakutuloksen sivun koko
        - in: query
          name: lng
          schema:
            type: string
            default: fi
          required: false
          description: Haun kieli. 'fi', 'sv' tai 'en'
        - in: query
          name: sort
          schema:
            type: string
            default: score
          required: false
          description: Järjestysperuste. 'name' tai 'score'
        - in: query
          name: order
          schema:
            type: string
            default: desc
          required: false
          description: Järjestys. 'asc' tai 'desc'
"
            (:desc koulutustyyppi) "\n"
            (:desc sijainti) "\n"
            (:desc opetuskieli) "\n"
            (:desc koulutusala) "\n"
            (:desc opetustapa) "\n"
            (:desc opetusaika) "\n"
            (:desc koulutuksenkestokuukausina) "\n"
            (:desc maksullisuus) "\n"
            (:desc valintatapa) "\n"
            (:desc hakukaynnissa) "\n"
            (:desc jotpa) "\n"
            (:desc tyovoimakoulutus) "\n"
            (:desc taydennyskoulutus) "\n"
            (:desc hakutapa) "\n"
            (:desc yhteishaku) "\n"
            (:desc pohjakoulutusvaatimus) "\n"
            (:desc alkamiskausi) "\n"
            (:desc hakualkaapaivissa) "\n"
        "
      responses:
        '200':
          description: Ok
          content:
            application/json:
              schema:
                type: json
        '404':
          description: Not found
        '400':
          description: Bad request
  /search/oppilaitos/{oid}/tarjonta:
    get:
      tags:
        - internal-search
      summary: Hae oppilaitoksen koulutustarjonnan
      description: Hakee annetun oppilaitoksen koulutustarjonnan. Huom.! Vain Opintopolun sisäiseen käyttöön
      parameters:
        - in: path
          name: oid
          schema:
            type: string
          required: true
          description: Oppilaitoksen yksilöivä oid
          example: 1.2.246.562.10.12345
        - in: query
          name: page
          schema:
            type: number
            default: 1
          required: false
          description: Hakutuloksen sivunumero
        - in: query
          name: size
          schema:
            type: number
            default: 20
          required: false
          description: Hakutuloksen sivun koko
        - in: query
          name: tuleva
          schema:
            type: boolean
            default: false
          required: false
          description: Haetaanko tuleva vai tämänhetkinen tarjonta.
            Tarjoaja on tuleva, jos se lisätty koulutukselle tarjoajaksi mutta se ei ole vielä julkaissut omaa toteutusta.
        - in: query
          name: lng
          schema:
            type: string
            default: fi
          required: false
          description: Haun kieli. 'fi', 'sv' tai 'en'
        - in: query
          name: order
          schema:
            type: string
            default: desc
          required: false
          description: Järjestys. 'asc' tai 'desc'
"
            (:desc koulutustyyppi) "\n"
            (:desc sijainti) "\n"
            (:desc opetuskieli) "\n"
            (:desc koulutusala) "\n"
            (:desc opetustapa) "\n"
            (:desc opetusaika) "\n"
            (:desc koulutuksenkestokuukausina) "\n"
            (:desc hakualkaapaivissa) "\n"
        "
      responses:
        '200':
          description: Ok
          content:
            application/json:
              schema:
                type: json
        '404':
          description: Not found
        '400':
          description: Bad request
  /search/oppilaitoksen-osa/{oid}/tarjonta:
    get:
      tags:
        - internal-search
      summary: Hae oppilaitoksen osan koulutustarjonnan
      description: Hakee annetun oppilaitoksen osan koulutustarjonnan. Huom.! Vain Opintopolun sisäiseen käyttöön
      parameters:
        - in: path
          name: oid
          schema:
            type: string
          required: true
          description: Oppilaitoksen osan yksilöivä oid
          example: 1.2.246.562.10.12345
        - in: query
          name: page
          schema:
            type: number
            default: 1
          required: false
          description: Hakutuloksen sivunumero
        - in: query
          name: size
          schema:
            type: number
            default: 20
          required: false
          description: Hakutuloksen sivun koko
        - in: query
          name: tuleva
          schema:
            type: boolean
            default: false
          required: false
          description: Haetaanko tuleva vai tämänhetkinen tarjonta.
            Tarjoaja on tuleva, jos se lisätty koulutukselle tarjoajaksi mutta se ei ole vielä julkaissut omaa toteutusta.
        - in: query
          name: lng
          schema:
            type: string
            default: fi
          required: false
          description: Haun kieli. 'fi', 'sv' tai 'en'
        - in: query
          name: order
          schema:
            type: string
            default: desc
          required: false
          description: Järjestys. 'asc' tai 'desc'
      responses:
        '200':
          description: Ok
          content:
            application/json:
              schema:
                type: json
        '404':
          description: Not found
        '400':
          description: Bad request
  /search/autocomplete:
    get:
      tags:
        - internal-search
      summary: Hae koulutuksia ja oppilaitoksia ennakoivaa hakua varten
      description: Hakee koulutuksia ja oppilaitoksia hakusanalla ja rajaimilla. Huom.! Vain Opintopolun sisäiseen käyttöön
      parameters:
        - in: query
          name: searchPhrase
          schema:
            type: string
          required: false
          description: Hakusanat. Vähimmäispituus on 3 merkkiä.
          example: Hevostalous
        - in: query
          name: lng
          schema:
            type: string
            default: fi
          required: false
          description: Haun kieli. 'fi', 'sv' tai 'en'
        - in: query
          name: sort
          schema:
            type: string
            default: score
          required: false
          description: Järjestysperuste. 'name' tai 'score'
        - in: query
          name: size
          schema:
            type: integer
            default: 5
          required: false
          description: Montako koulutusta ja oppilaitosta noudetaan. Oletuksena noudetaan siis 5 koulutusta ja 5 oppilaitosta.
        - in: query
          name: order
          schema:
            type: string
            default: desc
          required: false
          description: Järjestys. 'asc' tai 'desc'
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
            (:desc alkamiskausi) "\n"
            (:desc maksullisuus) "\n"
            (:desc hakualkaapaivissa) "\n"
        "
      responses:
        '200':
          description: Ok
          content:
            application/json:
              schema:
                type: json
        '404':
          description: Not found
        '400':
          description: Bad request"))

(s/defschema CompactHakukohde
  {:oid (st/schema s/Str {:description "Hakukohteen yksilöivä tunniste"})
   :nimi (st/schema Nimi {:description "Hakukohteen nimi eri kielillä"})
   :hakuOid (st/schema s/Str {:description "Hakukohteeseen liitetyn haun yksilöivä tunniste"})
   :organisaatio {:nimi (st/schema Nimi {:description "Organisaation nimi eri kielillä"})}
   :toteutus {:oid (st/schema s/Str {:description "Toteutuksen yksilöivä tunniste"})}
   :koulutustyyppi (st/schema KonfoKoulutustyyppi)
   :ammatillinenPerustutkintoErityisopetuksena
   (st/schema s/Bool {:description "Onko koulutuksen tyyppi \"Ammatillinen perustutkinto erityisopetuksena\"?"})})

(s/defschema HakukohdeSearchResult
  {:total s/Int
   :hits [(st/schema CompactHakukohde {:description "Hakukohteen perustiedot"})]})

(def paths-spec
  (openapi/openapi-spec
    {:paths
     (assoc (:paths (yaml/parse-string paths-str))
            "/search/hakukohteet"
            {:get {:tags ["internal-search"]
                   :summary "Hae hakukohteita"
                   :description "Hakee (kaikki julkaistut) hakukohteet haun kohdejoukon perusteella. Huom.! Vain Opintopolun sisäiseen käyttöön"
                   :parameters [{:in "query"
                                 :name "kohdejoukko"
                                 :description "Haun kohdejoukko (koodi uri)"
                                 :required true
                                 :schema {:type "string"}}]
                   :responses {200 {:description "Ok"
                                    ::openapi/content {"application/json" (st/schema HakukohdeSearchResult)}}
                               400 {:description "Bad request"
                                    :content {"text/plain" {:schema {:type "string"
                                                                     :example "Haun kohdejoukko puuttuu"}}}}}}})}))

(def paths (spec-paths-to-swagger-yaml paths-spec))

(def schemas (string/join "\n" (map schema-to-swagger-yaml [CompactHakukohde HakukohdeSearchResult])))

(defn- parse-number-range
  [min-number max-number]
  (cond
    (and min-number max-number) [(min min-number max-number) (max min-number max-number)]
    (and (nil? min-number) max-number) [0 max-number]
    (and min-number (nil? max-number)) [min-number]
    :else []))

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
   :alkamiskausi          (comma-separated-string->vec (:alkamiskausi rajain-params))
   :koulutuksenkestokuukausina (parse-number-range (:koulutuksenkestokuukausina_min rajain-params)
                                                   (:koulutuksenkestokuukausina_max rajain-params))
   :maksuton (when (string/includes? (or (:maksullisuustyyppi rajain-params) "") "maksuton")
               ["maksuton"]) ; Tässä arvolla ei ole väliä kunhan ei ole nil
   :maksullinen (when (string/includes? (or (:maksullisuustyyppi rajain-params) "") "maksullinen")
                  {:maksullisuustyyppi "maksullinen"
                   :maksunmaara (parse-number-range (:maksunmaara_min rajain-params) (:maksunmaara_max rajain-params))})
   :lukuvuosimaksu (when (string/includes? (or (:maksullisuustyyppi rajain-params) "") "lukuvuosimaksu")
                     {:maksullisuustyyppi "lukuvuosimaksu"
                      :maksunmaara (parse-number-range (:lukuvuosimaksunmaara_min rajain-params)
                                                       (:lukuvuosimaksunmaara_max rajain-params))
                      :apuraha (if (:apuraha rajain-params) true nil)})
   :hakualkaapaivissa (:hakualkaapaivissa rajain-params)})

(defn ->search-with-validated-params
  [do-search keyword lng page size sort order rajain-params]
  (cond
    (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
    (not (some #{sort} ["name" "score"])) (bad-request "Virheellinen järjestys ('name'/'score')")
    (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys ('asc'/'desc')")
    (and (not (nil? keyword))
         (> 3 (count keyword))) (bad-request "Hakusana on liian lyhyt")
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
    :else (if-let [result (do-search oid
                                     lng
                                     page
                                     size
                                     order
                                     tuleva
                                     (create-constraints rajain-params))]
            (ok result)
            (not-found "Not found"))))

(defn with-validated-params [search-phrase lng sort order ok-fn]
  (cond
    (not (some #{lng} ["fi" "sv" "en"])) (bad-request "Virheellinen kieli ('fi'/'sv'/'en')")
    (not (some #{sort} ["name" "score"])) (bad-request "Virheellinen järjestys ('name'/'score')")
    (not (some #{order} ["asc" "desc"])) (bad-request "Virheellinen järjestys ('asc'/'desc')")
    (and (not (nil? search-phrase))
         (> 3 (count search-phrase))) (bad-request "Hakusana on liian lyhyt")
    :else (ok (ok-fn))))

(defn- ->search-hakukohteet-with-kohdejoukko [kohdejoukko do-search]
  (cond
    (or (nil? kohdejoukko)
        (empty? kohdejoukko)) (bad-request "Haun kohdejoukko puuttuu")
    (nil? (re-matches #"haunkohdejoukko_[0-9a-zA-Z]+[#0-9]*" kohdejoukko))
    (bad-request "Haun kohdejoukon arvo on virheellinen")
    :else (ok (do-search kohdejoukko))))

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
                     {alkamiskausi          :- String nil}
                     {koulutuksenkestokuukausina_min :- Number nil}
                     {koulutuksenkestokuukausina_max :- Number nil}
                     {maksullisuustyyppi    :- String nil}
                     {maksunmaara_min       :- Number nil}
                     {maksunmaara_max       :- Number nil}
                     {lukuvuosimaksunmaara_min :- Number nil}
                     {lukuvuosimaksunmaara_max :- Number nil}
                     {apuraha               :- Boolean false}
                     {hakualkaapaivissa     :- Long nil}]
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
                                     :alkamiskausi alkamiskausi
                                     :koulutuksenkestokuukausina_min koulutuksenkestokuukausina_min
                                     :koulutuksenkestokuukausina_max koulutuksenkestokuukausina_max
                                     :maksullisuustyyppi maksullisuustyyppi
                                     :maksunmaara_min maksunmaara_min
                                     :maksunmaara_max maksunmaara_max
                                     :lukuvuosimaksunmaara_min lukuvuosimaksunmaara_min
                                     :lukuvuosimaksunmaara_max lukuvuosimaksunmaara_max
                                     :apuraha apuraha
                                     :hakualkaapaivissa hakualkaapaivissa})))

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
                     {koulutuksenkestokuukausina_min :- Number nil}
                     {koulutuksenkestokuukausina_max :- Number nil}
                     {maksullisuustyyppi    :- String nil}
                     {maksunmaara_min       :- Number nil}
                     {maksunmaara_max       :- Number nil}
                     {lukuvuosimaksunmaara_min :- Number nil}
                     {lukuvuosimaksunmaara_max :- Number nil}
                     {apuraha               :- Boolean false}
                     {lukiopainotukset      :- String nil}
                     {lukiolinjaterityinenkoulutustehtava :- String nil}
                     {osaamisala            :- String nil}
                     {oppilaitos            :- String nil}
                     {alkamiskausi          :- String nil}
                     {hakualkaapaivissa     :- Long nil}]
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
                                     :koulutuksenkestokuukausina_min koulutuksenkestokuukausina_min
                                     :koulutuksenkestokuukausina_max koulutuksenkestokuukausina_max
                                     :maksullisuustyyppi maksullisuustyyppi
                                     :maksunmaara_min maksunmaara_min
                                     :maksunmaara_max maksunmaara_max
                                     :lukuvuosimaksunmaara_min lukuvuosimaksunmaara_min
                                     :lukuvuosimaksunmaara_max lukuvuosimaksunmaara_max
                                     :apuraha apuraha
                                     :lukiopainotukset lukiopainotukset
                                     :lukiolinjaterityinenkoulutustehtava lukiolinjaterityinenkoulutustehtava
                                     :osaamisala osaamisala
                                     :oppilaitos oppilaitos
                                     :alkamiskausi alkamiskausi
                                     :hakualkaapaivissa hakualkaapaivissa})))

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
                     {alkamiskausi          :- String nil}
                     {koulutuksenkestokuukausina_min :- Number nil}
                     {koulutuksenkestokuukausina_max :- Number nil}
                     {maksullisuustyyppi    :- String nil}
                     {maksunmaara_min       :- Number nil}
                     {maksunmaara_max       :- Number nil}
                     {lukuvuosimaksunmaara_min :- Number nil}
                     {lukuvuosimaksunmaara_max :- Number nil}
                     {apuraha               :- Boolean false}
                     {hakualkaapaivissa     :- Long nil}]
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
                                                                    :alkamiskausi alkamiskausi
                                                                    :koulutuksenkestokuukausina_min koulutuksenkestokuukausina_min
                                                                    :koulutuksenkestokuukausina_max koulutuksenkestokuukausina_max
                                                                    :maksullisuustyyppi maksullisuustyyppi
                                                                    :maksunmaara_min maksunmaara_min
                                                                    :maksunmaara_max maksunmaara_max
                                                                    :lukuvuosimaksunmaara_min lukuvuosimaksunmaara_min
                                                                    :lukuvuosimaksunmaara_max lukuvuosimaksunmaara_max
                                                                    :apuraha apuraha
                                                                    :hakualkaapaivissa hakualkaapaivissa})))

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
                     {koulutuksenkestokuukausina_min :- Number nil}
                     {koulutuksenkestokuukausina_max :- Number nil}
                     {hakualkaapaivissa     :- Long nil}]
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
                                                                                :koulutuksenkestokuukausina_min koulutuksenkestokuukausina_min
                                                                                :koulutuksenkestokuukausina_max koulutuksenkestokuukausina_max
                                                                                :hakualkaapaivissa hakualkaapaivissa})))

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
                     {alkamiskausi          :- String nil}
                     {koulutuksenkestokuukausina_min :- Number nil}
                     {koulutuksenkestokuukausina_max :- Number nil}
                     {maksullisuustyyppi    :- String nil}
                     {maksunmaara_min       :- Number nil}
                     {maksunmaara_max       :- Number nil}
                     {lukuvuosimaksunmaara_min :- Number nil}
                     {lukuvuosimaksunmaara_max :- Number nil}
                     {apuraha               :- Boolean false}
                     {hakualkaapaivissa     :- Long nil}]
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
                               (create-constraints {:koulutustyyppi koulutustyyppi
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
                                                    :alkamiskausi alkamiskausi
                                                    :koulutuksenkestokuukausina_min koulutuksenkestokuukausina_min
                                                    :koulutuksenkestokuukausina_max koulutuksenkestokuukausina_max
                                                    :maksullisuustyyppi maksullisuustyyppi
                                                    :maksunmaara_min maksunmaara_min
                                                    :maksunmaara_max maksunmaara_max
                                                    :lukuvuosimaksunmaara_min lukuvuosimaksunmaara_min
                                                    :lukuvuosimaksunmaara_max lukuvuosimaksunmaara_max
                                                    :apuraha apuraha
                                                    :hakualkaapaivissa hakualkaapaivissa})]]
            {:koulutukset (apply koulutus-search/autocomplete-search search-params)
             :oppilaitokset (apply oppilaitos-search/autocomplete-search search-params)}))))

    (GET "/hakukohteet" request
      :query-params [{kohdejoukko :- String nil}]
      (with-access-logging request
                           (->search-hakukohteet-with-kohdejoukko
                            kohdejoukko
                            hakukohde-search/search)))))

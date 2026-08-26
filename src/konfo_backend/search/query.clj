(ns konfo-backend.search.query
  (:require [clojure.string :as str]
            [konfo-backend.elastic-tools :refer [->from ->size]]
            [konfo-backend.search.rajain-definitions :refer [common-filters
                                                                    constraints? generate-hakutulos-aggregations generate-jarjestajat-aggregations
                                                                    generate-tarjoajat-aggregations]]
            [konfo-backend.search.tools :refer :all]
            [konfo-backend.util.time :refer [current-time-as-kouta-format]]
            [konfo-backend.tools :refer [assoc-if]]))

(defn match-all-query
  []
  {:match_all {}})

;; score_mode "max" on olennainen: search_terms sisältää yhden alkion jokaista
;; järjestäjä-toteutus-paria kohti, ja elasticsearchin oletus "avg" laskee
;; koulutuksen pisteiksi sen osumien KESKIARVON. Monen järjestäjän koulutus jää
;; siten systemaattisesti alemmas kuin yhden järjestäjän koulutus, vaikka
;; kyseessä olisi sama nimi.
(defn- ->nested-search-terms-query
  [query]
  {:nested {:path "search_terms" :score_mode "max" :query {:bool {:must query}}}})

(defn search-term-query [search-term user-lng suffixes]
  (if (not (str/blank? search-term))
    (->nested-search-terms-query (make-search-term-query search-term user-lng suffixes))
    (match-all-query)))

(defn approximate-search-term-query [search-term user-lng suffixes]
  (if (not (str/blank? search-term))
    (->nested-search-terms-query (make-approximate-search-term-query search-term user-lng suffixes))
    (match-all-query)))

(defn autocomplete-query [search-phrase user-lng suffixes]
  (if (not (str/blank? search-phrase))
    (->nested-search-terms-query (make-autocomplete-query search-phrase user-lng suffixes))
    (match-all-query)))

(defn search-with-approximate-fallback
  "Ajaa tarkan haun. Jos se ei tuota yhtään osumaa, toistaa haun likimääräisellä
   kyselyllä ja merkitsee tuloksen kentällä :approximate, jotta käyttöliittymä
   voi kertoa käyttäjälle että näytetään läheisiä osumia.

   Kaksivaiheisuuden tarkoitus on, että nykyisten toimivien hakujen tarkkuus ei
   muutu lainkaan — löysempi kysely ajetaan vain silloin kun vaihtoehtona on
   tyhjä tuloslista. Jos löysempikään kysely ei löydä mitään, palautetaan
   alkuperäinen tulos, jotta rajainlaskurit pysyvät johdonmukaisina.

   run-search saa parametrinaan valmiin kyselyn ja palauttaa jäsennellyn
   hakutuloksen."
  [search-term lng suffixes run-search]
  (let [result (run-search (search-term-query search-term lng suffixes))]
    (if (and (not (str/blank? search-term))
             (zero? (or (:total result) 0)))
      (let [approximate (run-search (approximate-search-term-query search-term lng suffixes))]
        (if (pos? (or (:total approximate) 0))
          (assoc approximate :approximate true)
          result))
      result)))

(defn post-filter-query
  ([constraints inner-hits extra-filter]
   (when (or (constraints? constraints) inner-hits)
     (let [filters (vec (flatten
                          (cond-> []
                                  (constraints? constraints) (conj (common-filters constraints (current-time-as-kouta-format)))
                                   extra-filter (conj extra-filter))))]
       {:nested (assoc-if {:path "search_terms" :query {:bool {:filter filters}}}
                           :inner_hits inner-hits inner-hits)})))
  ([constraints]
   (post-filter-query constraints nil nil)))

;OY-3870 Kenttä nimi_sort lisätty indekseihin oppilaitos-kouta-search ja koulutus-kouta-search.
(defn- ->name-sort
  [order lng]
  [{(->lng-keyword "nimi.%s.keyword" lng) {:order order :unmapped_type "string"}}
   {(->lng-keyword "nimi_sort.%s.keyword" lng) {:order order :unmapped_type "string"}}])

(defn sorts
  [sort order lng]
  (if (= "name" sort)
    (->name-sort order lng)
    (vec (concat [{:_score {:order order}}] (->name-sort "asc" lng)))))

; Käytetään sekä koulutuksen järjestäjille että oppilaitoksen tarjoajille, joissa listataan toteutuksia
(defn toteutukset-query [oid]
  {:bool {:must {:term {:oid oid}}}})

(defn toteutukset-inner-hits [lng page size order]
  (let [size (->size size)
        from (->from page size)]
    {:_source ["search_terms.koulutusOid"
               "search_terms.toteutusOid"
               "search_terms.toteutusNimi"
               "search_terms.opetuskielet"
               "search_terms.oppilaitosOid"
               "search_terms.kuva"
               "search_terms.nimi"
               "search_terms.metadata"
               "search_terms.hakutiedot"
               "search_terms.toteutusHakuaika"
               "search_terms.jarjestaaUrheilijanAmmKoulutusta"]
     :from    from
     :size    size
     :sort    {(str "search_terms.nimi." lng ".keyword") {:order order :unmapped_type "string"}}}))

(defn- aggregations
  [aggs-generator]
  {:hits_aggregation {:nested {:path "search_terms"}, :aggs (aggs-generator)}})

(defn hakutulos-aggregations
  [constraints]
  (aggregations #(generate-hakutulos-aggregations constraints)))

(defn jarjestajat-aggregations
  [constraints tuleva?]
  (aggregations #(generate-jarjestajat-aggregations constraints tuleva?)))

(defn tarjoajat-aggregations
  [constraints tuleva?]
  (aggregations #(generate-tarjoajat-aggregations constraints tuleva?)))

(ns konfo-backend.search.external-query
  (:require [konfo-backend.search.tools :refer :all]
            [clojure.string :refer [lower-case]]
            [konfo-backend.tools :refer [not-blank? current-time-as-kouta-format
                                         ten-months-past-as-kouta-format ->lower-case-vec]]
            [konfo-backend.config :refer [config]]))


(defn- ->external-terms-query
  [key coll]
  (if (= 1 (count coll))
    {:term  {(keyword key) (lower-case (first coll))}}
    {:terms {(keyword key) (->lower-case-vec coll)}}))

(defn- some-hakuaika-kaynnissa
  []
{:should [{:bool {:filter [{:range {:search_terms.toteutusHakuaika.alkaa {:lte (current-time-as-kouta-format)}}}
                           {:bool  {:should [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}},
                                             {:range {:search_terms.toteutusHakuaika.paattyy {:gt (current-time-as-kouta-format)}}}]}}]}}
          {:nested {:path "search_terms.hakutiedot.hakuajat"
                    :query {:bool {:filter [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte (current-time-as-kouta-format)}}}
                                            {:bool  {:should [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}},
                                                              {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt (current-time-as-kouta-format)}}}]}}]}}}}]})

(defn- some-past-hakuaika-still-viable
  []
  { :nested {:path "search_terms.hakutiedot.hakuajat"
             :query {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}},
                                     {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt (ten-months-past-as-kouta-format)}}}]}}}})

(defn- hakuaika-filter-query ([inner-query]
    (if (nil? inner-query)
      {:bool (some-hakuaika-kaynnissa)}
      {:bool (conj (some-hakuaika-kaynnissa) {:must {:nested {:path "search_terms.hakutiedot" :query {:bool {:filter inner-query}}}}})}))
  ([]
  (hakuaika-filter-query nil)))

(defn- external-hakutieto-query
  ([haku-kaynnissa inner-query]
   (if haku-kaynnissa (hakuaika-filter-query inner-query)
       {:nested {:path "search_terms.hakutiedot"
                 :query {:bool {:filter (vec (remove nil? [(some-past-hakuaika-still-viable) inner-query]))}}}}))
    ([haku-kaynnissa]
   (external-hakutieto-query haku-kaynnissa nil)))

(defn- haku-kaynnissa-not-already-included?
  [constraints]
  (every? false? [(hakutapa? constraints)
                  (pohjakoulutusvaatimus? constraints)
                  (valintatapa? constraints)
                  (yhteishaku? constraints)]))

(defn- external-filters
  [constraints]
  (let [haku-kaynnissa (haku-kaynnissa? constraints)
        ; NOTE haku-käynnissä rajainta halutaan käyttää vain jos jotain muuta hakutietorajainta ei ole käytössä (koska se sisältyy niihin jos ne on käytössä)
        use-haku-kaynnissa (and haku-kaynnissa
                                (haku-kaynnissa-not-already-included? constraints))]

    (cond-> []
            (koulutustyyppi? constraints)        (conj (->external-terms-query :search_terms.koulutustyypit.keyword           (:koulutustyyppi constraints)))
            (opetuskieli? constraints)           (conj (->external-terms-query :search_terms.opetuskielet.keyword             (:opetuskieli constraints)))
            (sijainti? constraints)              (conj (->external-terms-query :search_terms.sijainti.keyword                 (:sijainti constraints)))
            (koulutusala? constraints)           (conj (->external-terms-query :search_terms.koulutusalat.keyword             (:koulutusala constraints)))
            (opetustapa? constraints)            (conj (->external-terms-query :search_terms.opetustavat.keyword              (:opetustapa constraints)))

            ; NOTE hakukäynnissä rajainta EI haluta käyttää jos se sisältyy muihin rajaimiin (koska ao. rivit käyttäytyvät OR ehtoina)
            use-haku-kaynnissa                   (conj (external-hakutieto-query true))
            (hakutapa? constraints)              (conj (external-hakutieto-query haku-kaynnissa (->external-terms-query :search_terms.hakutiedot.hakutapa                 (:hakutapa constraints))))
            (pohjakoulutusvaatimus? constraints) (conj (external-hakutieto-query haku-kaynnissa (->external-terms-query :search_terms.hakutiedot.pohjakoulutusvaatimukset (:pohjakoulutusvaatimus constraints))))
            (valintatapa? constraints)           (conj (external-hakutieto-query haku-kaynnissa (->external-terms-query :search_terms.hakutiedot.valintatavat             (:valintatapa constraints))))
            (yhteishaku? constraints)            (conj (external-hakutieto-query haku-kaynnissa (->external-terms-query :search_terms.hakutiedot.yhteishakuOid            (:yhteishaku constraints)))))))

(defn generate-external-search-params
  [suffixes search-params usr-lng]
  (for [language ["fi" "sv" "en"]
        suffix (conj suffixes nil)]
    (if (= language usr-lng)
      (str "search_terms." (:term search-params) "." language (if (nil? suffix) (str "^" (get-in config [:search-terms-boost :language-default]))
                                                                                (str "." suffix "^" (:boost search-params))))
      (str "search_terms." (:term search-params) "." language (if (nil? suffix) (str "^" (get-in config [:search-terms-boost :default]))
                                                                                (str "." suffix "^" (get-in config [:search-terms-boost :default])))))))
(defn- generate-external-keyword-query
  [usr-lng suffixes]
  (for [search-params [{:term "koulutusnimi" :boost (get-in config [:search-terms-boost :koulutusnimi])}
                       {:term "toteutusNimi" :boost (get-in config [:search-terms-boost :toteutusNimi])}
                       {:term "asiasanat" :boost (get-in config [:search-terms-boost :asiasanat])}
                       {:term "tutkintonimikkeet" :boost (get-in config [:search-terms-boost :tutkintonimikkeet])}
                       {:term "ammattinimikkeet" :boost (get-in config [:search-terms-boost :ammattinimikkeet])}
                       {:term "koulutus_organisaationimi" :boost (get-in config [:search-terms-boost :koulutus_organisaationimi])}
                       {:term "toteutus_organisaationimi" :boost (get-in config [:search-terms-boost :toteutus_organisaationimi])}]]
    (generate-external-search-params suffixes search-params usr-lng)))

(defn- external-fields
  [keyword constraints user-lng suffixes]
  (let [fields? (not-blank? keyword)
        filter? (constraints? constraints)]
    (cond-> {}
            fields? (-> (assoc :must {:multi_match {:query       keyword,
                                                    :fields      (flatten (generate-external-keyword-query user-lng suffixes))
                                                    :tie_breaker 0.9
                                                    :operator    "and"
                                                    :type        "cross_fields"}}))
            filter? (assoc :filter (external-filters constraints)))))

(defn external-query
  [keyword constraints lng suffixes]
  {:nested {:path "search_terms",
            :inner_hits {},
            :query      {:bool (external-fields keyword constraints lng suffixes)}}})


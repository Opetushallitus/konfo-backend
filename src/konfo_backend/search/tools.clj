(ns konfo-backend.search.tools
  (:require
    [konfo-backend.tools :refer [not-blank? current-time-as-kouta-format ten-months-past-as-kouta-format ->lower-case-vec]]
    [clojure.core :refer [keyword] :rename {keyword kw}]
    [clojure.string :refer [lower-case]]
    [konfo-backend.config :refer [config]]))

(defn- constraint?
  [constraints key]
  (not (empty? (key constraints))))

(defn sijainti?
  [constraints]
  (constraint? constraints :sijainti))

(defn koulutustyyppi?
  [constraints]
  (constraint? constraints :koulutustyyppi))

(defn opetuskieli?
  [constraints]
  (constraint? constraints :opetuskieli))

(defn koulutusala?
  [constraints]
  (constraint? constraints :koulutusala))

(defn opetustapa?
  [constraints]
  (constraint? constraints :opetustapa))

(defn valintatapa?
  [constraints]
  (constraint? constraints :valintatapa))

(defn hakutapa?
  [constraints]
  (constraint? constraints :hakutapa))

(defn pohjakoulutusvaatimus?
  [constraints]
  (constraint? constraints :pohjakoulutusvaatimus))

(defn haku-kaynnissa?
  [constraints]
  (true? (:hakukaynnissa constraints)))

(defn has-jotpa-rahoitus?
  [constraints]
  (true? (:jotpa constraints)))

(defn yhteishaku?
  [constraints]
  (constraint? constraints :yhteishaku))

(defn lukiopainotukset?
  [constraints]
  (constraint? constraints :lukiopainotukset))

(defn lukiolinjaterityinenkoulutustehtava?
  [constraints]
  (constraint? constraints :lukiolinjaterityinenkoulutustehtava))

(defn osaamisala?
  [constraints]
  (constraint? constraints :osaamisala))

(defn constraints?
  [constraints]
  (or (sijainti? constraints)
      (koulutustyyppi? constraints)
      (koulutusala? constraints)
      (opetuskieli? constraints)
      (opetustapa? constraints)
      (valintatapa? constraints)
      (haku-kaynnissa? constraints)
      (hakutapa? constraints)
      (has-jotpa-rahoitus? constraints)
      (yhteishaku? constraints)
      (pohjakoulutusvaatimus? constraints)))

(defn ->lng-keyword
  [str lng]
  (keyword (format str lng)))

(defn do-search?
  [keyword constraints]
  (or (not (empty keyword)) (constraints? constraints)))

(defn match-all?
  [keyword constraints]
  (not (do-search? keyword constraints)))

(defn ->terms-query
  [key coll]
  (if (= 1 (count coll))
    {:term {(keyword key) (lower-case (first coll))}}
    {:terms {(keyword key) (->lower-case-vec coll)}}))

(defn some-hakuaika-kaynnissa
  [current-time]
  {:should [{:bool {:filter [{:range {:search_terms.toteutusHakuaika.alkaa {:lte current-time}}}
                             {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}},
                                              {:range {:search_terms.toteutusHakuaika.paattyy {:gt current-time}}}]}}]}}
            {:nested {:path  "search_terms.hakutiedot.hakuajat"
                      :query {:bool {:filter [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte current-time}}}
                                              {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}},
                                                               {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt current-time}}}]}}]}}}}]})

(defn hakuaika-filter-query
  [current-time]
  {:bool (some-hakuaika-kaynnissa current-time)})

(defn hakutieto-query
  [inner-query]
  {:nested {:path  "search_terms.hakutiedot"
            :query {:bool {:filter (vec (remove nil? [inner-query]))}}}})

(defn filters
  ([constraints]
   (filters constraints (current-time-as-kouta-format)))
  ([constraints current-time]
  (cond-> []
          (koulutustyyppi? constraints) (conj (->terms-query :search_terms.koulutustyypit.keyword (:koulutustyyppi constraints)))
          (opetuskieli? constraints) (conj (->terms-query :search_terms.opetuskielet.keyword (:opetuskieli constraints)))
          (sijainti? constraints) (conj (->terms-query :search_terms.sijainti.keyword (:sijainti constraints)))
          (koulutusala? constraints) (conj (->terms-query :search_terms.koulutusalat.keyword (:koulutusala constraints)))
          (opetustapa? constraints) (conj (->terms-query :search_terms.opetustavat.keyword (:opetustapa constraints)))
          (haku-kaynnissa? constraints) (conj (hakuaika-filter-query current-time))
          (has-jotpa-rahoitus? constraints) (conj {:bool {:filter [{:term {:search_terms.hasJotpaRahoitus true}}]}})
          (hakutapa? constraints) (conj (hakutieto-query (->terms-query :search_terms.hakutiedot.hakutapa (:hakutapa constraints))))
          (pohjakoulutusvaatimus? constraints) (conj (hakutieto-query (->terms-query :search_terms.hakutiedot.pohjakoulutusvaatimukset (:pohjakoulutusvaatimus constraints))))
          (valintatapa? constraints) (conj (hakutieto-query (->terms-query :search_terms.hakutiedot.valintatavat (:valintatapa constraints))))
          (yhteishaku? constraints) (conj (hakutieto-query (->terms-query :search_terms.hakutiedot.yhteishakuOid (:yhteishaku constraints)))))))


(defn aggs-filters
  [constraints current-time filter-name]
  (cond-> []
    (koulutustyyppi? constraints) (conj (->terms-query :search_terms.koulutustyypit.keyword (:koulutustyyppi constraints)))
    (opetuskieli? constraints) (conj (->terms-query :search_terms.opetuskielet.keyword (:opetuskieli constraints)))
    (opetustapa? constraints) (conj (->terms-query :search_terms.opetustavat.keyword (:opetustapa constraints)))
    (or (= filter-name "hakukaynnissa") (haku-kaynnissa? constraints)) (conj (hakuaika-filter-query current-time))
    (or (= filter-name "jotpa") (has-jotpa-rahoitus? constraints)) (conj {:term {:search_terms.hasJotpaRahoitus true}})
    (hakutapa? constraints) (conj
                              {:nested
                               {:path "search_terms.hakutiedot"
                                :query
                                {:bool
                                 {:filter (->terms-query :search_terms.hakutiedot.hakutapa (:hakutapa constraints))}}}})
    (pohjakoulutusvaatimus? constraints) (conj
                                           {:nested
                                            {:path "search_terms.hakutiedot"
                                             :query
                                             {:bool
                                              {:filter (->terms-query
                                                         :search_terms.hakutiedot.pohjakoulutusvaatimukset
                                                         (:pohjakoulutusvaatimus constraints))}}}})
    (valintatapa? constraints) (conj
                                 {:nested
                                  {:path "search_terms.hakutiedot"
                                   :query
                                   {:bool
                                    {:filter (->terms-query :search_terms.hakutiedot.valintatavat (:valintatapa constraints))}}}})
    (yhteishaku? constraints) (conj
                                {:nested
                                 {:path "search_terms.hakutiedot"
                                  :query
                                  {:bool
                                   {:filter (->terms-query :search_terms.hakutiedot.yhteishakuOid (:yhteishaku constraints))}}}})))

(defn hakutieto-filters
  [inner-query current-time constraints]
  (vec (concat
    [{:nested {:path "search_terms.hakutiedot" :query {:bool {:filter inner-query}}}}]
    (aggs-filters constraints current-time ""))))

(defn terms-filters
  [inner-query current-time constraints]
  (vec (concat
         [inner-query]
         (aggs-filters constraints current-time ""))))

(defn term-filters
  [inner-query current-time constraints]
  {:bool {:filter (aggs-filters constraints current-time "")}})

(defn generate-search-params
  [suffixes search-params usr-lng]
  (for [language ["fi" "sv" "en"]
        suffix (conj suffixes nil)]
    (if (= language usr-lng)
      (str "search_terms." (:term search-params) "." language (if (nil? suffix) (str "^" (get-in config [:search-terms-boost :language-default]))
                                                                                (str "." suffix "^" (:boost search-params))))
      (str "search_terms." (:term search-params) "." language (if (nil? suffix) (str "^" (get-in config [:search-terms-boost :default]))
                                                                                (str "." suffix "^" (get-in config [:search-terms-boost :default])))))))

(defn generate-keyword-query
  [usr-lng suffixes]
  (for [search-params [{:term "koulutusnimi" :boost (get-in config [:search-terms-boost :koulutusnimi])}
                       {:term "toteutusNimi" :boost (get-in config [:search-terms-boost :toteutusNimi])}
                       {:term "asiasanat" :boost (get-in config [:search-terms-boost :asiasanat])}
                       {:term "tutkintonimikkeet" :boost (get-in config [:search-terms-boost :tutkintonimikkeet])}
                       {:term "ammattinimikkeet" :boost (get-in config [:search-terms-boost :ammattinimikkeet])}
                       {:term "koulutus_organisaationimi" :boost (get-in config [:search-terms-boost :koulutus_organisaationimi])}
                       {:term "toteutus_organisaationimi" :boost (get-in config [:search-terms-boost :toteutus_organisaationimi])}]]
    (generate-search-params suffixes search-params usr-lng)))

(defn fields
  [keyword constraints user-lng suffixes]
  (let [fields? (not-blank? keyword)
        filter? (constraints? constraints)]
    (cond-> {}
      fields? (-> (assoc :must {:multi_match {:query       keyword,
                                              :fields      (flatten (generate-keyword-query user-lng suffixes))
                                              :tie_breaker 0.9
                                              :operator    "and"
                                              :type        "cross_fields"}}))
      filter? (assoc :filter (filters constraints)))))

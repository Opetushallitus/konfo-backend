(ns konfo-backend.search.tools
  (:require
   [konfo-backend.tools :refer [not-blank? current-time-as-kouta-format ->lower-case-vec]]
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

(defn tyovoimakoulutus?
  [constraints]
  (true? (:tyovoimakoulutus constraints)))

(defn taydennyskoulutus?
  [constraints]
  (true? (:taydennyskoulutus constraints)))

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

(defn oppilaitos?
  [constraints]
  (constraint? constraints :oppilaitos))

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
      (tyovoimakoulutus? constraints)
      (taydennyskoulutus? constraints)
      (yhteishaku? constraints)
      (oppilaitos? constraints)
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
  (let [search-field (keyword (str "search_terms." key))]
    (if (= 1 (count coll))
      {:term {search-field (lower-case (first coll))}}
      {:terms {search-field (->lower-case-vec coll)}})))

(defn keyword-terms-query
  [field coll]
  (->terms-query (str field ".keyword") coll))

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
  [nested-field-name field-name constraint]
  {:nested
   {:path "search_terms.hakutiedot"
    :query
    {:bool
     {:filter (->terms-query (str nested-field-name "." field-name) constraint)}}}})

(defn tyoelama-filters-query
  [constraints]
  (let [tyoelama-should (cond-> []
                          (has-jotpa-rahoitus? constraints) (conj {:term {:search_terms.hasJotpaRahoitus true}})
                          (tyovoimakoulutus? constraints) (conj {:term {:search_terms.isTyovoimakoulutus true}})
                          (taydennyskoulutus? constraints) (conj {:term {:search_terms.isTaydennyskoulutus true}}))]
    (when (seq tyoelama-should) {:bool {:should tyoelama-should}})))

(defn filters
  [constraints current-time]
  (let [tyoelama-bool-filter (tyoelama-filters-query constraints)]
    (cond-> []
      (koulutustyyppi? constraints) (conj (keyword-terms-query "koulutustyypit" (:koulutustyyppi constraints)))
      (opetuskieli? constraints) (conj (keyword-terms-query "opetuskielet" (:opetuskieli constraints)))
      (sijainti? constraints) (conj (keyword-terms-query "sijainti" (:sijainti constraints)))
      (koulutusala? constraints) (conj (keyword-terms-query "koulutusalat" (:koulutusala constraints)))
      (opetustapa? constraints) (conj (keyword-terms-query "opetustavat" (:opetustapa constraints)))
      (haku-kaynnissa? constraints) (conj (hakuaika-filter-query current-time))
      (not (nil? tyoelama-bool-filter)) (conj tyoelama-bool-filter)
      (hakutapa? constraints) (conj (hakutieto-query "hakutiedot" "hakutapa" (:hakutapa constraints)))
      (pohjakoulutusvaatimus? constraints) (conj (hakutieto-query
                                                  "hakutiedot"
                                                  "pohjakoulutusvaatimukset"
                                                  (:pohjakoulutusvaatimus constraints)))
      (oppilaitos? constraints) (conj (keyword-terms-query "oppilaitosOid" (:oppilaitos constraints)))
      (valintatapa? constraints) (conj (hakutieto-query "hakutiedot" "valintatavat" (:valintatapa constraints)))
      (yhteishaku? constraints) (conj (hakutieto-query "hakutiedot" "yhteishakuOid" (:yhteishaku constraints))))))

(defn nested-filters
  [inner-query nested-path current-time constraints]
  (vec (distinct (concat
                  [{:nested {:path (str "search_terms." nested-path) :query {:bool {:filter inner-query}}}}]
                  (filters constraints current-time)))))

(defn terms-filters
  [inner-query current-time constraints]
  (vec (concat
        [inner-query]
        (filters constraints current-time))))

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
      filter? (assoc :filter (filters constraints (current-time-as-kouta-format))))))

(defn generate-wildcard-query
  [search-phrase-token user-lng]
  {:wildcard {(keyword (str "search_terms.koulutusnimi." user-lng ".keyword")) {:value (str "*" (lower-case search-phrase-token) "*")}}})

(defn wildcard-query-fields
  [search-phrase constraints user-lng]
  (let [search-phrase-tokens (split search-phrase #" ")
        query {:must (vec (map #(generate-wildcard-query % user-lng) search-phrase-tokens))}]
    (if (constraints? constraints)
      (assoc query :filter (filters constraints (current-time-as-kouta-format)))
      query)))

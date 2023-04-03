(ns konfo-backend.search.tools
  (:require [konfo-backend.config :refer [config]]
            [konfo-backend.search.filter.filterdefs :refer [combined-jarjestaja-filters
                                                            combined-tyoelama-filter filter-definitions hakukaynnissa-filter]]
            [konfo-backend.search.filter.query-tools :refer [constraint?]]
            [konfo-backend.tools :refer [current-time-as-kouta-format
                                         not-blank?]]))

(defn- sijainti?
  [constraints]
  (constraint? constraints :sijainti))

(defn- koulutustyyppi?
  [constraints]
  (constraint? constraints :koulutustyyppi))

(defn- opetuskieli?
  [constraints]
  (constraint? constraints :opetuskieli))

(defn- koulutusala?
  [constraints]
  (constraint? constraints :koulutusala))

(defn- opetustapa?
  [constraints]
  (constraint? constraints :opetustapa))

(defn- valintatapa?
  [constraints]
  (constraint? constraints :valintatapa))

(defn hakutapa?
  [constraints]
  (constraint? constraints :hakutapa))

(defn- pohjakoulutusvaatimus?
  [constraints]
  (constraint? constraints :pohjakoulutusvaatimus))

(defn- haku-kaynnissa?
  [constraints]
  (true? (:hakukaynnissa constraints)))

(defn- has-jotpa-rahoitus?
  [constraints]
  (true? (:jotpa constraints)))

(defn- tyovoimakoulutus?
  [constraints]
  (true? (:tyovoimakoulutus constraints)))

(defn- taydennyskoulutus?
  [constraints]
  (true? (:taydennyskoulutus constraints)))

(defn- yhteishaku?
  [constraints]
  (constraint? constraints :yhteishaku))

(defn- oppilaitos?
  [constraints]
  (constraint? constraints :oppilaitos))

; Onko hakurajaimia valittuna?
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

(defn blank-search?
  [keyword constraints]
  (and (empty? keyword) (not (constraints? constraints))))

(defn tyoelama-filters-query
  [constraints]
  (let [tyoelama-should (cond-> []
                          (has-jotpa-rahoitus? constraints) (conj {:term {:search_terms.hasJotpaRahoitus true}})
                          (tyovoimakoulutus? constraints) (conj {:term {:search_terms.isTyovoimakoulutus true}})
                          (taydennyskoulutus? constraints) (conj {:term {:search_terms.isTaydennyskoulutus true}}))]
    (when (seq tyoelama-should) {:bool {:should tyoelama-should}})))

(defn filters
  [constraints current-time]
  (let [make-constraint-query
        (fn [filter-def]
          (let [constraint-vals (get constraints (:id filter-def))]
            (cond
              (and (boolean? constraint-vals) (true? constraint-vals)) (:make-query filter-def)
              (and (vector? constraint-vals) (not-empty constraint-vals)) ((:make-query filter-def) constraint-vals))))
        tyoelama-filter ((:make-query combined-tyoelama-filter) constraints)
        hakukaynnissa-filter ((:make-query hakukaynnissa-filter) constraints current-time)]
    (filterv
     some?
     (conj
      (mapv make-constraint-query filter-definitions)
      tyoelama-filter
      hakukaynnissa-filter))))

(defn inner-hits-filters
  [tuleva? constraints]
  {:bool
   {:must
    [{:term {"search_terms.onkoTuleva" tuleva?}}
     {:bool ((:make-query combined-jarjestaja-filters) constraints)}]
    :filter (filters constraints (current-time-as-kouta-format))}})

(defn- generate-search-params
  [suffixes search-params usr-lng]
  (for [language ["fi" "sv" "en"]
        suffix (conj suffixes nil)]
    (if (= language usr-lng)
      (str "search_terms." (:term search-params) "." language (if (nil? suffix) (str "^" (get-in config [:search-terms-boost :language-default]))
                                                                  (str "." suffix "^" (:boost search-params))))
      (str "search_terms." (:term search-params) "." language (if (nil? suffix) (str "^" (get-in config [:search-terms-boost :default]))
                                                                  (str "." suffix "^" (get-in config [:search-terms-boost :default])))))))

(defn- generate-keyword-query
  [usr-lng suffixes]
  (for [search-params [{:term "koulutusnimi" :boost (get-in config [:search-terms-boost :koulutusnimi])}
                       {:term "toteutusNimi" :boost (get-in config [:search-terms-boost :toteutusNimi])}
                       {:term "asiasanat" :boost (get-in config [:search-terms-boost :asiasanat])}
                       {:term "tutkintonimikkeet" :boost (get-in config [:search-terms-boost :tutkintonimikkeet])}
                       {:term "ammattinimikkeet" :boost (get-in config [:search-terms-boost :ammattinimikkeet])}
                       {:term "koulutus_organisaationimi" :boost (get-in config [:search-terms-boost :koulutus_organisaationimi])}
                       {:term "toteutus_organisaationimi" :boost (get-in config [:search-terms-boost :toteutus_organisaationimi])}]]
    (generate-search-params suffixes search-params usr-lng)))


(defn make-search-query [keyword user-lng suffixes]
  (when (not-blank? keyword) {:must {:multi_match {:query       keyword,
                                                   :fields      (flatten (generate-keyword-query user-lng suffixes))
                                                   :tie_breaker 0.9
                                                   :operator    "and"
                                                   :type        "cross_fields"}}}))

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

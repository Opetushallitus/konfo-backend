(ns konfo-backend.search.tools
  (:require [clojure.string :refer [lower-case split]]
            [konfo-backend.config :refer [config]]
            [konfo-backend.search.rajain.rajain-definitions :refer [constraints? common-filters]]
            [konfo-backend.tools :refer [current-time-as-kouta-format]]))

(defn ->lng-keyword
  [str lng]
  (keyword (format str lng)))

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


(defn make-search-term-query [keyword user-lng suffixes]
  {:multi_match {:query       keyword
                 :fields      (flatten (generate-keyword-query user-lng suffixes))
                 :tie_breaker 0.9
                 :operator    "and"
                 :type        "cross_fields"}})

(defn fields
  [keyword constraints user-lng suffixes]
  (let [fields? (not (str/blank? keyword))
        filter? (constraints? constraints)]
    (cond-> {}
      fields? (-> (assoc :must (make-search-term-query keyword user-lng suffixes)))
      filter? (assoc :filter (common-filters constraints (current-time-as-kouta-format))))))

(defn generate-wildcard-query
  [search-phrase-token user-lng]
  {:wildcard {(keyword (str "search_terms.koulutusnimi." user-lng ".keyword")) {:value (str "*" (lower-case search-phrase-token) "*")}}})

(defn wildcard-query-fields
  [search-phrase constraints user-lng]
  (let [search-phrase-tokens (split search-phrase #" ")
        query {:must (vec (map #(generate-wildcard-query % user-lng) search-phrase-tokens))}]
    (if (constraints? constraints)
      (assoc query :filter (common-filters constraints (current-time-as-kouta-format)))
      query)))

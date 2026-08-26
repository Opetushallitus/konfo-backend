(ns konfo-backend.search.tools
  (:require [konfo-backend.config :refer [config]]))

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
                       {:term "toteutus_organisaationimi" :boost (get-in config [:search-terms-boost :toteutus_organisaationimi])}
                       {:term "metadata.kunnat.nimi" :boost (get-in config [:search-terms-boost :kunta])}]]
    (generate-search-params suffixes search-params usr-lng)))

(defn- search-term-fields
  [usr-lng suffixes]
  (flatten (generate-keyword-query usr-lng suffixes)))

(defn make-search-term-query
  "Tarkka kysely: jokaisen hakusanan on osuttava. cross_fields sallii sanojen
   osumisen eri kenttiin, esim. niin että koulutuksen nimi ja oppilaitoksen nimi
   yhdessä kattavat hakulauseen."
  [keyword user-lng suffixes]
  {:multi_match {:query       keyword
                 :fields      (search-term-fields user-lng suffixes)
                 :tie_breaker 0.9
                 :operator    "and"
                 :type        "cross_fields"}})

;; Likimääräisen haun oletusarvot. Nämä voi ylikirjoittaa konfiguraatiossa
;; avaimen :search-approximate alta.
(def ^:private approximate-defaults
  {:phrase-boost         10
   :strict-boost         5
   :fuzzy-boost          1
   :minimum-should-match "2<66%"
   :fuzziness            "AUTO"})

(defn- approximate-conf
  [key]
  (let [value (get-in config [:search-approximate key])]
    (if (nil? value) (get approximate-defaults key) value)))

(defn make-approximate-search-term-query
  "Löysempi kysely, jota käytetään VAIN kun tarkka kysely ei tuota yhtään osumaa.

   Tarkka haku vaatii kaikkien hakusanojen osumista, joten yksikin tunnistamaton
   sana nollaa tuloksen. Käytännössä tämä osuu kopioi-liitä-hakuihin, joissa
   käyttäjä liittää koulutuksen nimen jostain muualta: nimi ei ole koskaan
   tarkalleen sama kuin indeksissä.

   Kolme vaihtoehtoista tapaa osua, parhaiten osuva nousee kärkeen:
     1. koko hakulause peräkkäisinä sanoina (paras osuma)
     2. tarkka kysely, eli kaikki sanat mutta missä tahansa järjestyksessä
     3. osa sanoista, kirjoitusvirheet sallien

   HUOM. cross_fields ei tue fuzziness-parametria, joten sumea vaihe on
   pakko tehdä best_fields-tyypillä."
  [keyword user-lng suffixes]
  (let [fields (search-term-fields user-lng suffixes)]
    {:bool {:minimum_should_match 1
            :should
            [{:multi_match {:query  keyword
                            :fields fields
                            :type   "phrase"
                            :slop   2
                            :boost  (approximate-conf :phrase-boost)}}
             (update-in (make-search-term-query keyword user-lng suffixes)
                        [:multi_match]
                        assoc :boost (approximate-conf :strict-boost))
             {:multi_match {:query                keyword
                            :fields               fields
                            :type                 "best_fields"
                            :minimum_should_match (approximate-conf :minimum-should-match)
                            :fuzziness            (approximate-conf :fuzziness)
                            ; Ensimmäinen merkki on täsmättävä tarkasti, muuten sumea haku
                            ; tuottaa satunnaisia lyhyitä osumia.
                            :prefix_length        1
                            :boost                (approximate-conf :fuzzy-boost)}}]}}))

(defn- autocomplete-prefix-fields
  "Nimikenttien prefix-alikentät. Ne indeksoivat pintamuodot ilman
   lemmatisointia, koska kesken kirjoitettu sana ei lemmatisoidu miksikään."
  [user-lng]
  (for [term ["koulutusnimi" "toteutusNimi" "nimi"]]
    (str "search_terms." term "." user-lng ".prefix^"
         (get-in config [:search-terms-boost (keyword term)]
                 (get-in config [:search-terms-boost :language-default])))))

(defn make-autocomplete-query
  "Autocomplete täydentää kesken kirjoitettua sanaa, joten se tarvitsee
   etuliitehaun. Tarkka kysely pidetään mukana, jotta valmiiksi kirjoitetut
   sanat osuvat edelleen myös muihin kenttiin (asiasanat, nimikkeet)."
  [search-phrase user-lng suffixes]
  {:bool {:minimum_should_match 1
          :should [(make-search-term-query search-phrase user-lng suffixes)
                   {:multi_match {:query  search-phrase
                                  :fields (autocomplete-prefix-fields user-lng)
                                  :type   "bool_prefix"}}]}})

(ns konfo-backend.search.search-term-query-unit-test
  "Yksikkötestit vapaan sanahaun kyselynmuodostukselle. Nämä eivät tarvitse
   elasticsearchia — testataan generoitua kyselyä ja kaksivaiheisen haun
   ohjauslogiikkaa."
  (:require [clojure.test :refer :all]
            [clojure.string :as str]
            [konfo-backend.search.query :refer [approximate-search-term-query
                                                autocomplete-query
                                                search-term-query
                                                search-with-approximate-fallback]]
            [konfo-backend.search.tools :refer [make-approximate-search-term-query
                                                make-autocomplete-query
                                                make-search-term-query]]))

(deftest search-term-query-test
  (testing "nested-kyselyssä on score_mode max"
    ; Oletus "avg" laskee koulutuksen pisteiksi sen osumien keskiarvon, jolloin
    ; monen järjestäjän koulutus jää systemaattisesti alemmas kuin yhden
    ; järjestäjän koulutus samalla nimellä.
    (is (= "max" (get-in (search-term-query "lähihoitaja" "fi" ["words"])
                         [:nested :score_mode])))
    (is (= "max" (get-in (approximate-search-term-query "lähihoitaja" "fi" ["words"])
                         [:nested :score_mode])))
    (is (= "max" (get-in (autocomplete-query "lähihoit" "fi" ["words"])
                         [:nested :score_mode]))))

  (testing "tyhjä hakusana tuottaa match_all-kyselyn"
    (doseq [f [search-term-query approximate-search-term-query autocomplete-query]]
      (is (= {:match_all {}} (f nil "fi" ["words"])))
      (is (= {:match_all {}} (f "   " "fi" ["words"]))))))

(deftest make-search-term-query-test
  (let [query (:multi_match (make-search-term-query "lähihoitaja" "fi" ["words"]))]
    (testing "tarkka kysely vaatii kaikkien hakusanojen osumista"
      (is (= "and" (:operator query)))
      (is (= "cross_fields" (:type query))))

    (testing "käyttäjän kielen kentät saavat suuremman boostin kuin muut kielet"
      (is (some #(= "search_terms.koulutusnimi.fi.words^20" %) (:fields query)))
      (is (some #(= "search_terms.koulutusnimi.sv.words^0.1" %) (:fields query))))))

(deftest make-approximate-search-term-query-test
  (let [clauses (get-in (make-approximate-search-term-query "lähihoitaja" "fi" ["words"])
                        [:bool :should])
        by-type (group-by #(get-in % [:multi_match :type]) clauses)]
    (testing "kolme vaihtoehtoista tapaa osua"
      (is (= 1 (get-in (make-approximate-search-term-query "x" "fi" ["words"])
                       [:bool :minimum_should_match]))
          "yhden osuminen riittää")
      (is (= 3 (count clauses)))
      (is (= #{"phrase" "cross_fields" "best_fields"} (set (keys by-type)))))

    (testing "koko hakulause peräkkäisinä sanoina saa suurimman boostin"
      (is (> (get-in (first (get by-type "phrase")) [:multi_match :boost])
             (get-in (first (get by-type "cross_fields")) [:multi_match :boost])
             (get-in (first (get by-type "best_fields")) [:multi_match :boost]))))

    (testing "sumea vaihe sallii kirjoitusvirheet ja osan sanoista"
      ; cross_fields ei tue fuzziness-parametria, joten sumea vaihe on pakko
      ; tehdä best_fields-tyypillä.
      (let [fuzzy (:multi_match (first (get by-type "best_fields")))]
        (is (= "AUTO" (:fuzziness fuzzy)))
        (is (= "2<66%" (:minimum_should_match fuzzy)))
        (is (= 1 (:prefix_length fuzzy)))))))

(deftest make-autocomplete-query-test
  (let [clauses (get-in (make-autocomplete-query "lähihoit" "fi" ["words"]) [:bool :should])
        prefix-clause (first (filter #(= "bool_prefix" (get-in % [:multi_match :type])) clauses))]
    (testing "autocomplete hakee myös kesken kirjoitetulla sanalla"
      (is (some? prefix-clause))
      (is (every? #(str/includes? % ".prefix^")
                  (get-in prefix-clause [:multi_match :fields])))
      (is (some #(str/starts-with? % "search_terms.koulutusnimi.fi.prefix")
                (get-in prefix-clause [:multi_match :fields]))))

    (testing "tarkka kysely on mukana, jotta valmiit sanat osuvat myös muihin kenttiin"
      (is (some #(= "cross_fields" (get-in % [:multi_match :type])) clauses)))))

(defn- recording-search
  "Palauttaa [ajetut-kyselyt search-fn], jossa search-fn palauttaa tulokset
   järjestyksessä ja tallentaa saamansa kyselyt."
  [results]
  (let [calls (atom [])
        remaining (atom results)]
    [calls (fn [query]
             (swap! calls conj query)
             (let [result (first @remaining)]
               (swap! remaining rest)
               result))]))

(deftest search-with-approximate-fallback-test
  (testing "kun tarkka haku tuottaa osumia, likimääräistä hakua ei ajeta"
    (let [[calls search-fn] (recording-search [{:total 5 :hits [:a]}])
          result (search-with-approximate-fallback "lähihoitaja" "fi" ["words"] search-fn)]
      (is (= 1 (count @calls)))
      (is (= {:total 5 :hits [:a]} result))
      (is (not (contains? result :approximate)))))

  (testing "kun tarkka haku ei tuota osumia, tulos otetaan likimääräisestä hausta"
    (let [[calls search-fn] (recording-search [{:total 0 :hits []}
                                               {:total 3 :hits [:b]}])
          result (search-with-approximate-fallback "Kirkkomusiikin ja musiikkitieteen opinnot"
                                                  "fi" ["words"] search-fn)]
      (is (= 2 (count @calls)))
      (is (= true (:approximate result)))
      (is (= 3 (:total result)))
      (testing "ensin ajetaan tarkka ja sitten likimääräinen kysely"
        (is (= "cross_fields" (get-in (first @calls)
                                      [:nested :query :bool :must :multi_match :type])))
        (is (some? (get-in (second @calls) [:nested :query :bool :must :bool :should]))))))

  (testing "kun kumpikaan ei tuota osumia, palautetaan tarkan haun tulos"
    (let [[calls search-fn] (recording-search [{:total 0 :hits [] :filters {:a 1}}
                                               {:total 0 :hits []}])
          result (search-with-approximate-fallback "xyzzy" "fi" ["words"] search-fn)]
      (is (= 2 (count @calls)))
      (is (not (contains? result :approximate)))
      (is (= {:a 1} (:filters result)))))

  (testing "tyhjällä hakusanalla ei yritetä likimääräistä hakua"
    (let [[calls search-fn] (recording-search [{:total 0 :hits []}])
          result (search-with-approximate-fallback nil "fi" ["words"] search-fn)]
      (is (= 1 (count @calls)))
      (is (not (contains? result :approximate))))))

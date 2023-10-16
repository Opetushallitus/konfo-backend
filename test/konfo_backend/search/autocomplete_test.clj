(ns konfo-backend.search.autocomplete-test
  (:require [clojure.test :refer [deftest is use-fixtures testing]]
            [clojure.string :refer [starts-with?]]
            [matcher-combinators.test]
            [matcher-combinators.matchers :as m]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.test-mock-data :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn autocomplete-url
  [& query-params]
  (apply url-with-query-params "/konfo-backend/search/autocomplete" query-params))

(defn autocomplete-search
  [& query-params]
  (get-ok (apply autocomplete-url query-params)))

(defn ->bad-request-body
  [& query-params]
  (:body (get-bad-request (apply autocomplete-url query-params))))

(deftest autocomplete-bad-request-test
  (with-redefs [konfo-backend.koodisto.koodisto/get-koodisto-with-cache mock-get-koodisto]
    (testing "Autocomplete search with bad requests"
      (testing "Invalid lng"
        (is (starts-with? (->bad-request-body :sijainti "kunta_618" :lng "foo") "Virheellinen kieli")))
      (testing "Invalid sort"
        (is (starts-with? (->bad-request-body :sijainti "kunta_618" :sort "foo") "Virheellinen järjestys")))
      (testing "Too short search phrase"
        (is (starts-with? (->bad-request-body :sijainti "kunta_618" :searchPhrase "fo") "Hakusana on liian lyhyt"))))))

(defonce oppilaitos-oid4 "1.2.246.562.10.00101010104")
(defonce koulutus-oid4 "1.2.246.562.13.000033")
(defonce koulutus-oid12 "1.2.246.562.13.000041")

(deftest autocomplete-with-params-test
  (testing "Autocomplete with search phrase"
    (is (match? {:koulutukset {:total 2
                               :hits [{:oid koulutus-oid12
                                       :nimi {:fi "Autoalan perustutkinto fi"
                                              :sv "Autoalan perustutkinto sv"}
                                       :toteutustenTarjoajat {:count 2}}
                                      {:oid koulutus-oid4
                                       :nimi {:fi "Automaatiotekniikka (ylempi AMK :sorakuvausId sorakuvaus-id) fi"
                                              :sv "Automaatiotekniikka (ylempi AMK :sorakuvausId sorakuvaus-id) sv"}
                                       :toteutustenTarjoajat {:count 1
                                                              :nimi {:fi "Oppilaitos fi 1.2.246.562.10.00101010104",
                                                                     :sv "Oppilaitos sv 1.2.246.562.10.00101010104"}}}]}
                 :oppilaitokset {:total 2
                                 :hits [{:oid "1.2.246.562.10.67476956288",
                                         :nimi
                                         {:fi "Jokin järjestyspaikka",
                                          :sv "Jokin järjestyspaikka sv"}}
                                        {:oid oppilaitos-oid4
                                         :nimi {:fi "Oppilaitos fi 1.2.246.562.10.00101010104"
                                                :sv "Oppilaitos sv 1.2.246.562.10.00101010104"}}]}}
                (autocomplete-search :sort "name" :order "asc" :searchPhrase "auto")))
    (testing "Autocomplete with search phrase and sijainti"
      (is (match? {:koulutukset {:total 1
                                 :hits [{:oid koulutus-oid12
                                         :nimi {:fi "Autoalan perustutkinto fi"
                                                :sv "Autoalan perustutkinto sv"}
                                         :toteutustenTarjoajat {:count 2}}]}
                   :oppilaitokset {:total 1
                                   :hits [{:oid "1.2.246.562.10.67476956288",
                                           :nimi
                                           {:fi "Jokin järjestyspaikka",
                                            :sv "Jokin järjestyspaikka sv"}}]}}
                  (autocomplete-search :sort "name" :order "asc" :searchPhrase "auto" :sijainti "kunta_297"))))))

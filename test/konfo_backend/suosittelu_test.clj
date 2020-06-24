(ns konfo-backend.suosittelu-test
  (:require [clojure.test :refer :all]
            [konfo-backend.suosittelu.algorithm :as algorithm]
            [kouta-indeksoija-service.fixture.kouta-indexer-fixture :as fixture]
            [clj-elasticsearch.elastic-connect :as c]
            [clj-elasticsearch.elastic-utils :as u]
            [konfo-backend.core :refer :all]
            [konfo-backend.search.search-test-tools :refer [koulutus-metatieto]]
            [cheshire.core :refer [parse-string]]
            [clojure.walk :refer [keywordize-keys]]
            [konfo-backend.test-tools :refer [get-ok debug-pretty]]))

(intern 'clj-log.access-log 'service "konfo-backend")
(use-fixtures :each fixture/mock-indexing-fixture)

(deftest suosittelu-algorithm-test
  (testing "Get correct recommendations from algorithm"
    (let [matrix [{:oid "1.1" :jarjestysnumero 1 :etaisyydet [2, 0, 20, 50, 7, 90, 6, 12]}
                  {:oid "1.3" :jarjestysnumero 3 :etaisyydet [20, 20, 3, 0, 7, 2, 5, 17]}]]

      (is (= [6 4 0] (algorithm/calculate-top-n-recommendations 3 matrix))))))

(deftest suosittelu-api-test
  (testing "Get correct recommendation from api"
    (fixture/add-koulutus-mock "1.2.246.562.13.0000006" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Autoalan koulutus" :metadata koulutus-metatieto)
    (fixture/add-koulutus-mock "1.2.246.562.13.0000004" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Hevosalan koulutus" :metadata koulutus-metatieto)
    (fixture/add-koulutus-mock "1.2.246.562.13.0000000" :koulutustyyppi "amm" :tila "julkaistu" :nimi "Kokin koulutus" :metadata koulutus-metatieto)

    (fixture/index-oids-without-related-indices {:koulutukset ["1.2.246.562.13.0000000" "1.2.246.562.13.0000004" "1.2.246.562.13.0000006"]})

    (u/elastic-put (u/elastic-url "suosittelu") {} {:include_type_name false})
    (doseq [r [{:oid "1.2.246.562.13.0000000" :jarjestysnumero 0 :etaisyydet [0, 99, 20, 50, 7, 90, 6, 12]}
               {:oid "1.2.246.562.13.0000001" :jarjestysnumero 1 :etaisyydet [2, 0, 20, 50, 7, 90, 6, 12]}
               {:oid "1.2.246.562.13.0000002" :jarjestysnumero 2 :etaisyydet [2, 90, 0, 50, 7, 90, 6, 12]}
               {:oid "1.2.246.562.13.0000003" :jarjestysnumero 3 :etaisyydet [20, 20, 3, 0, 7, 2, 5, 17]}
               {:oid "1.2.246.562.13.0000004" :jarjestysnumero 4 :etaisyydet [20, 20, 3, 99, 0, 2, 5, 17]}
               {:oid "1.2.246.562.13.0000005" :jarjestysnumero 5 :etaisyydet [20, 20, 3, 99, 7, 0, 5, 17]}
               {:oid "1.2.246.562.13.0000006" :jarjestysnumero 6 :etaisyydet [20, 20, 3, 99, 7, 2, 0, 17]}
               {:oid "1.2.246.562.13.0000007" :jarjestysnumero 7 :etaisyydet [20, 20, 3, 99, 7, 2, 5, 0]}]]
      (u/elastic-put (u/elastic-url "suosittelu" "_doc" (:oid r)) r))
    (c/refresh-index "suosittelu")
    (let [result (get-ok "/konfo-backend/suosittelu?koulutukset=1.2.246.562.13.0000001,1.2.246.562.13.0000003,1.2.246.562.13.44")
          oids   (vec (map :oid (:hits result)))]
      (is (= 3 (:total result)))
      (is (= "1.2.246.562.13.0000006" (first oids)))
      (is (= "1.2.246.562.13.0000004" (second oids)))
      (is (= "1.2.246.562.13.0000000" (nth oids 2)))
      (is (= result (keywordize-keys (parse-string (slurp "test/resources/suosittelu.json"))))))

    (let [result (get-ok "/konfo-backend/suosittelu?koulutukset=1.2.246.562.13.44")]
      (is (= 0 (:total result)))
      (is (= 0 (count (:hits result)))))))
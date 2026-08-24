(ns konfo-backend.index.koulutus-test
  (:require [clojure.test :refer :all]
            [clj-elasticsearch.elastic-utils :refer [elastic-post]]
            [konfo-backend.test-tools :refer :all]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn koulutus-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/koulutus/" oid) [:draft false]))

(defn koulutus-draft-url
  [oid]
  (apply url-with-query-params (str "/konfo-backend/koulutus/" oid) [:draft true]))

(deftest koulutus-test

  (let [koulutusOid1 "1.2.246.562.13.000001"
        koulutusOid3 "1.2.246.562.13.000003"
        koulutusOid5 "1.2.246.562.13.000005"
        koulutusOid6 "1.2.246.562.13.000006"
        koulutusOid99 "1.2.246.562.13.000099"]

    (testing "Get koulutus"
      (testing "ok"
        (let [response (get-ok (koulutus-url koulutusOid1))]
          (is (= koulutusOid1 (:oid response)))))
      (testing "allowed to get tallennettu when esikatselu true"
        (let [response (get-ok (koulutus-draft-url koulutusOid5))]
          (is (= koulutusOid5 (:oid response)))))
      (testing "filter julkaisemattomat toteutukset"
        (let [response (get-ok (koulutus-url koulutusOid3))]
          (is (= 3 (count (:toteutukset response))))))
      (testing "not found"
        (get-not-found (koulutus-url koulutusOid99)))
      (testing "filter not julkaistu draft when esikatselu false"
        (get-not-found (koulutus-draft-url koulutusOid6)))
      (testing "returns paikallisetTutkinnonOsat for amm-tutkinnon-osa koulutus"
        (let [koulutusOid13 "1.2.246.562.13.000013"
              response (get-ok (koulutus-url koulutusOid13))
              paikalliset (get-in response [:metadata :paikallisetTutkinnonOsat])]
          (is (= 2 (count paikalliset)))
          (is (= "123" (:opetussuunnitelmaId (first paikalliset))))
          (is (= "456" (:tutkinnonosaId (first paikalliset))))
          (is (= "Paikallinen tutkinnon osa fi" (get-in (first paikalliset) [:nimi :fi])))
          (is (= "Toinen paikallinen osa fi" (get-in (second paikalliset) [:nimi :fi])))
          (is (= "Ammattitaidon osoittamistavat fi" (get-in (first paikalliset) [:ammattitaidonosoittamistavat :fi])))
          (is (= "<p>Vaatimukset kohde fi</p><ul><li>Vaatimus 1 fi</li></ul>" (get-in (first paikalliset) [:ammattitaitovaatimukset :fi])))
          (is (= "Toinen osoittamistapa fi" (get-in (second paikalliset) [:ammattitaidonosoittamistavat :fi])))
          (is (= "<p>Kohdealue fi</p><p>Lista kohde fi</p><ul><li>Vaatimus 2 fi</li></ul>" (get-in (second paikalliset) [:ammattitaitovaatimukset :fi])))
          (is (= 15 (:opintojenLaajuusNumero (first paikalliset))))
          (is (nil? (:opintojenLaajuusNumero (second paikalliset)))))))))

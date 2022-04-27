(ns konfo-backend.index.valintaperuste-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer :all]
            [clj-log.access-log]))

(intern 'clj-log.access-log 'service "konfo-backend")

(use-fixtures :once with-elastic-dump)

(defn valintaperuste-url
  [id]
  (apply url-with-query-params (str "/konfo-backend/valintaperuste/" id) [:draft false]))

(defn valintaperuste-draft-url
  [id]
  (apply url-with-query-params (str "/konfo-backend/valintaperuste/" id) [:draft true]))

(deftest valintaperuste-test

  (let [valintaperusteId1  "31972648-ebb7-4185-ac64-31fa6b841e34"
        valintaperusteId2  "31972648-ebb7-4185-ac64-31fa6b841e35"
        valintaperusteId3  "31972648-ebb7-4185-ac64-31fa6b841e36"
        valintaperusteId4  "31972648-ebb7-4185-ac64-31fa6b841e37"
        valintaperusteId5  "31972648-ebb7-4185-ac64-31fa6b841e38"
        valintaperusteId6  "31972648-ebb7-4185-ac64-31fa6b841e39"
        valintaperusteId99 "31972648-ebb7-4185-ac64-31fa6b841e99"]

    (comment
      (fixture/add-valintaperuste-mock valintaperusteId1 :tila "julkaistu" :esikatselu "false")
      (fixture/add-valintaperuste-mock valintaperusteId2 :tila "tallennettu" :esikatselu "false")
      (fixture/add-valintaperuste-mock valintaperusteId4 :tila "tallennettu" :esikatselu "true")

      (fixture/index-oids-without-related-indices {:valintaperusteet [valintaperusteId1 valintaperusteId2 valintaperusteId4]})
      )
    (testing "Get valintaperuste"
      (testing "ok"
        (let [response (get-ok (valintaperuste-url valintaperusteId1))]
          (is (= valintaperusteId1 (:id response)))))
      (testing "get draft valintaperuste when esikatselu true"
        (let [response (get-ok (valintaperuste-draft-url valintaperusteId6))]
          (is (= valintaperusteId6 (:id response)))))
      (testing "not found"
        (get-not-found (valintaperuste-url valintaperusteId99)))
      (testing "filter not julkaistu and draft true but esikatselu false"
        (get-not-found (valintaperuste-draft-url valintaperusteId5)))
      (testing "filter not julkaistu and draft false"
        (get-not-found (valintaperuste-url valintaperusteId6))))))
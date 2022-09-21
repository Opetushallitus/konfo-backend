(ns konfo-backend.ataru.service-test
  (:require [clojure.test :refer :all]
            [konfo-backend.ataru.service :as service]))

(deftest ataru-service-test
  (testing "demo-allowed-for-hakukohde?"
    (testing "returns true when demo-allowed is true in form"
      (with-redefs [konfo-backend.ataru.client/get-form-for-haku (fn [haku-oid] {:demo-allowed true})]
        (is (true? (service/demo-allowed-for-haku? "123")))))
    (testing "returns false when demo-allowed is false in form"
      (with-redefs [konfo-backend.ataru.client/get-form-for-haku (fn [haku-oid] {:demo-allowed false})]
        (is (false? (service/demo-allowed-for-haku? "123")))))))

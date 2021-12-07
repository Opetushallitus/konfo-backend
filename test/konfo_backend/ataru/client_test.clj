(ns konfo-backend.ataru.client-test
  (:require [clojure.test :refer :all]
            [konfo-backend.ataru.client :as client])
  (:use clj-http.fake))

(defonce hakukohde-route #"http://localhost:8351/hakemus/api/hakukohde/.*")

(deftest ataru-client-test
  (testing "get-form-for-hakukohde"
    (testing "response status is 200"
      (with-fake-routes-in-isolation
        {hakukohde-route (fn [req] {:status 200 :body "{\"some-data\": 42}"})}
        (let [form (client/get-form-for-hakukohde "123")]
          (testing "returns parsed body"
            (is (= {:some-data 42} form))))))

    (testing "response status is not 200"
      (with-fake-routes-in-isolation
        {hakukohde-route (fn [req] {:status 404 :body "{\"some-data\": 42}"})}
        (testing "exception is thrown"
          (is (thrown? Exception (client/get-form-for-hakukohde "123"))))))))

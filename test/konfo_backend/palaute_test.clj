(ns konfo-backend.palaute-test
  (:require [clojure.test :refer :all]
            [konfo-backend.palaute.palaute :refer :all]
            [clj-log.access-log]
            [konfo-backend.test-tools :as tools]))

(deftest Palaute
  (testing "Palaute"
    (testing "post-palaute ja get-palautteet"
      (post-palaute 1 "testi palaute1")
      (post-palaute 2 "testi palaute2")
      (post-palaute 3 "testi palaute3")
      (let [after (System/currentTimeMillis)]
        (Thread/sleep 500)
        (post-palaute 4 "testi palaute4")
        (post-palaute 5 "testi palaute5")
        (tools/refresh-and-wait "palaute" 5000)
        (let [res1 (get-palautteet 0)
              res2 (get-palautteet after)]
          (is (= (:avg res1) 3))
          (is (= (:count res1) 5))
          (is (= (:avg res2) 9/2))
          (is (= (:count res2) 2)))))))

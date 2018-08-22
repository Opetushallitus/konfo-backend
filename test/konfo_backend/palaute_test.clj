(ns konfo-backend.palaute-test
  (:require [clojure.test :refer :all]
            [midje.sweet :refer :all]
            [konfo-backend.palaute.palaute :refer :all]
            [clj-log.access-log]
            [konfo-backend.test-tools :as tools]
            [clj-test-utils.elasticsearch-mock-utils :as utils]))

(against-background [(before :contents (utils/init-elastic-test))
                     (after :contents (utils/stop-elastic-test))]
  (facts "Palaute"
         (fact "post-palaute ja get-palautteet"
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
                   (:avg res1)
                      => 3
                   (:count res1)
                      => 5
                   (:avg res2)
                      => 9/2
                   (:count res2)
                      => 2)))))

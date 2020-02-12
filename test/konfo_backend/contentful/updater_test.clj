(ns konfo-backend.contentful.updater-test
  (:require [clojure.test :refer :all]
            [konfo-backend.contentful.updater-api :refer :all]
            [ring.mock.request :as mock]
            [konfo-backend.contentful.contentful-updater :refer [placeholder]]
            [clojure.tools.logging :as log]
            [clojure.java.io :as io]
            [clojure.string :as s])
  (:import (com.contentful.java.cda CDAClient)
           (java.io ObjectInputStream)
           (com.amazonaws.services.s3 AmazonS3)))

(intern 'clj-log.access-log 'service "konfo-backend-updater")

(defonce client (Mockito/mock CDAClient))
(defonce s3-client (Mockito/mock AmazonS3))

(defn load [f]
  (.readObject (ObjectInputStream. (io/input-stream (str "test/konfo_backend/fixture/" f)))))

(defn get-content-types [_]
  (log/info "get-content-types stub")
  (load "get-content-types.obj"))

(defn get-entries-raw [_ content-type locale]
  (log/info  "get-entries-raw stub")
  (load (str "get-entries-raw_" content-type "_" locale ".obj")))

(defn get-assets-raw [_ locale]
  (log/info "get-assets-raw stub")
  (load (str "get-assets-raw_" locale ".obj")))

(deftest updater-test
  (testing "Healthcheck API test"
    (let [response ((konfo-updater-api client) (mock/request :get "/konfo-backend-updater/healthcheck"))]
      (is (= (:status response) 200))))

  (testing "Unauthorized access"
    (let [response ((konfo-updater-api client) (mock/request :post "/konfo-backend-updater/update"))]
      (is (= (:status response) 401))))

  (testing "Start update"
    (let [files (atom [])]
      (with-redefs [konfo-backend.contentful.contentful/create-contentful-client (constantly nil)
                    konfo-backend.contentful.contentful/get-entries-raw          get-entries-raw
                    konfo-backend.contentful.contentful/get-content-types        get-content-types
                    konfo-backend.contentful.contentful/get-assets-raw           get-assets-raw
                    konfo-backend.contentful.contentful-updater/fetch->image     (constantly placeholder)
                    konfo-backend.contentful.s3/put-object                       (fn [_ _ file-key _ _] (swap! files conj file-key))]
        (konfo-backend.contentful.contentful-updater/contentful->s3
          client
          s3-client
          (System/currentTimeMillis))
        (is (some #(s/ends-with? % ".jpg.jpg") @files) "expecting modified jpg files")
        (is (some #(s/ends-with? % ".jpg") @files) "expecting jpg files")
        (is (some #(s/ends-with? % "asset.json") @files) "expecting asset.json")
        (is (some #(s/ends-with? % "manifest.json") @files) "expecting manifest.json")))))

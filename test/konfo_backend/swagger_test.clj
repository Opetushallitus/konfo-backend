(ns konfo-backend.swagger-test
  (:require [clojure.test :refer :all]
            [konfo-backend.test-tools :refer :all]
            [konfo-backend.core :refer :all]
            [ring.mock.request :as mock])
  (:import (io.swagger.v3.parser OpenAPIV3Parser)))

(intern 'clj-log.access-log 'service "konfo-backend")

(deftest swagger-test
  (testing "Swagger"
    (let [body (:body (app (mock/request :get "/konfo-backend/swagger.yaml")))
          parser (new OpenAPIV3Parser)
          result (. parser readContents body nil nil)
          open-api (. result getOpenAPI)
          messages (. result getMessages)]
      ; Parsimisen pitää onnistua (on validia YML:ää)
      (is (not (nil? open-api)))
      ; Ei virheitä tai varoituksia swaggerin parsinnasta
      (is (empty? messages)))))
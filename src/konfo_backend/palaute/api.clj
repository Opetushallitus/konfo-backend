(ns konfo-backend.palaute.api
  (:require
    [konfo-backend.palaute.palaute :as palaute]
    [compojure.api.core :refer [POST]]
    [konfo-backend.palaute.sqs :as sqs]
    [ring.util.http-response :refer :all]
    [compojure.api.core :as c]))

(defonce sqs-client (sqs/create-sqs-client))

(def paths
  "|  /palaute:
   |    post:
   |      summary: Lähetä palaute
   |      description: Lähetä palaute
   |      parameters:
   |        - in: FormData
   |          name: arvosana
   |          type: number
   |          description: Palautteen arvosana
   |        - in: FormData
   |          name: palaute
   |          type: string
   |          description: Palautteen teksti
   |        - in: FormData
   |          name: path
   |          type: string
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            text/plain:
   |              schema:
   |                type: object")

(def routes
  (c/routes
    (POST "/palaute" [:as request]
          :form-params [{arvosana :- Long nil}
                        {palaute :- String ""}
                        {path :- String ""}]
      (let [feedback {:stars      arvosana
                      :feedback   palaute
                      :path       path
                      :user-agent (get-in request [:headers "user-agent"])}]
        (palaute/send-feedback sqs-client feedback)
        (ok {})))))

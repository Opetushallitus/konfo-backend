(ns konfo-backend.palaute.api
  (:require
    [konfo-backend.palaute.palaute :as palaute]
    [konfo-backend.palaute.sqs :as sqs]
    [ring.util.http-response :refer [ok]]
    [compojure.api.core :as c]))

(defonce sqs-client (sqs/create-sqs-client))

(def paths
  "|  /palaute:
   |    post:
   |      summary: Lähetä palaute
   |      description: Lähetä palaute
   |      requestBody:
   |        content:
   |          'application/x-www-form-urlencoded':
   |            schema:
   |              properties:  
   |                arvosana:
   |                  type: number
   |                  description: Palautteen arvosana
   |                palaute:
   |                  type: string
   |                  description: Palautteen teksti
   |                path:
   |                  type: string
   |              required:
   |                - arvosana
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            text/plain:
   |              schema:
   |                type: object")

(def routes
  (c/routes
    (c/POST "/palaute" [:as request]
          :form-params [{arvosana :- Long nil}
                        {palaute :- String ""}
                        {path :- String ""}]
      (let [feedback {:stars      arvosana
                      :feedback   palaute
                      :path       path
                      :user-agent (get-in request [:headers "user-agent"])}]
        (palaute/send-feedback sqs-client feedback)
        (ok {})))))

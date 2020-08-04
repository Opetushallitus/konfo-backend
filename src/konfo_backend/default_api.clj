(ns konfo-backend.default-api
  (:require
    [compojure.api.core :as c :refer [GET POST]]
    [ring.util.http-response :refer :all]
    [schema.core :as s]
    [clojure.tools.logging :as log]))

(s/defschema ClientError {:error-message s/Str
                          :url s/Str
                          :line s/Int
                          :col s/Int
                          :user-agent s/Str
                          :stack s/Str})

(def schemas
  "|    ClientError:
   |      type: object
   |      properties:
   |        error-message:
   |          type: string
   |          description: Virheviesti
   |        url:
   |          type: string
   |          description: Url
   |        line:
   |          type: number
   |          description: Virheen rivinumero
   |        col:
   |          type: number
   |          description: Virheen kolumnin numero
   |        user-agent:
   |          type: string
   |          description: User-agent
   |        stack:
   |          type: string
   |          description: Stack trace")

(def paths
  "|  /healthcheck:
   |    get:
   |      summary: Healthcheck-rajapinta
   |      description: Healthcheck-rajapinta
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            text/plain:
   |              schema:
   |                type: string
   |  /client-error:
   |    post:
   |      summary: Client error -rajapinta
   |      description: Lokittaa clientin lähettämän virheen järjestelmän lokille
   |      requestBody:
   |        description: Clientin virhe
   |        required: true
   |        content:
   |          application/json:
   |            schema:
   |              $ref: '#/components/schemas/ClientError'
   |      responses:
   |        '200':
   |          description: Ok
   |          content:
   |            text/plain:
   |              schema:
   |                type: string")

(def routes
  (c/routes

   (GET "/healthcheck" []
        :return String
        (ok "OK"))

   (POST "/client-error" []
         :body [error-details ClientError]
         :return String
         (log/error (str "Error from client browser:\n"
                         (:error-message error-details) "\n"
                         (:url error-details) "\n"
                         "line: " (:line error-details) " column: " (:col error-details) "\n"
                         "user-agent: " (:user-agent error-details) "\n"
                         "stack trace: " (:stack error-details)))
         (ok "OK"))))
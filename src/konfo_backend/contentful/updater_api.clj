(ns konfo-backend.contentful.updater-api
  (:require
   [konfo-backend.contentful.contentful-updater :refer [contentful->s3]]
   [konfo-backend.config :refer [config]]
   [ring.adapter.jetty :refer [run-jetty]]
   [konfo-backend.tools :refer [comma-separated-string->vec]]
   [clj-log.access-log :refer [with-access-logging]]
   [ring.middleware.basic-authentication :refer [wrap-basic-authentication]]
   [ring.middleware.session :as ring-session]
   [compojure.api.sweet :refer :all]
   [compojure.api.core :refer [route-middleware]]
   [clojure.tools.logging :as log]
   [ring.util.http-response :refer :all]
   [environ.core :refer [env]]))

(defonce updater-agent (agent nil))

(defn add-content-update-job [clients]
  (log/info "Adding content update job!")
  (send-off updater-agent (partial contentful->s3 clients (System/currentTimeMillis))))

(defn authenticated? [name pass]
  (and (= name (-> config :contentful-update-username))
       (= pass (-> config :contentful-update-password))))

(defn basic-auth [site]
  (-> site
      (wrap-basic-authentication authenticated?)))

(defn konfo-updater-api [clients]
  (api
    {:swagger {:ui   "/konfo-backend-updater/swagger"
               :spec "/konfo-backend-updater/swagger.json"
               :data {:info {:title       "Konfo-backend-updater"
                             :description "Backend for Konfo koulutusinformaatio UI Updater."}}}}

    (context
     "/konfo-backend-updater"
     []

      (GET "/healthcheck" [:as request]
        :summary "Healthcheck API"
        (ok "ok"))

      (route-middleware
       [ring-session/wrap-session
        basic-auth]

       (POST "/update" [:as request]
         :summary "POST update call"
         (log/info "Contentful update requested!")
         (add-content-update-job clients)
         (ok "ok"))))))

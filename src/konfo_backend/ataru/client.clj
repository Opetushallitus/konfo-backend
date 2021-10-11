(ns konfo-backend.ataru.client
  (:require
    [konfo-backend.config :refer [config]]
    [clj-http.client :as http]))

(defonce base-url (:ataru-hakija-url config))
(defonce caller-id (:caller-id config))

(defn get-form-for-hakukohde
  [hakukohde-oid]
  (let [url          (str base-url "/hakukohde/" hakukohde-oid)
        query-params {"role" "hakija"}
        headers      {"Caller-Id" caller-id}
        response     (http/get url {:query-params query-params :headers headers :as :json :throw-exceptions false}
                       )]
    (if (= 200 (:status response))
      (:body response)
      (throw (ex-info
               "failed to get form for hakukohde"
               {:hakukohde-oid hakukohde-oid
                :ataru-response response})))))

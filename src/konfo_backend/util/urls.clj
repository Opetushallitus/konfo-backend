(ns konfo-backend.util.urls
  (:require [konfo-backend.util.conf :refer [env]])
  (:import (fi.vm.sade.properties OphProperties)))

(def ^fi.vm.sade.properties.OphProperties url-properties (atom nil))

(defn- load-config
  []
  (let [{:keys [virkailija-internal cas kouta-backend kouta-external ataru-hakija]
         :or   {virkailija-internal "" cas "" kouta-backend "" kouta-external "" ataru-hakija ""}} (:hosts env)]
    (reset! url-properties
            (doto (OphProperties. (into-array String ["/konfo-backend-oph.properties"]))
              ;(.addDefault "host-kouta-backend" kouta-backend)
              ))))

(defn resolve-url
  [key & params]
  (when (nil? @url-properties)
    (load-config))
  (.url @url-properties (name key) (to-array (or params []))))

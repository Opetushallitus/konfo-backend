(ns konfo-backend.tools)

(defn julkaistu?
  [e]
  (and (not (nil? e)) (= "julkaistu" (:tila e))))

(defn julkaistut
  [coll]
  (filter julkaistu? coll))
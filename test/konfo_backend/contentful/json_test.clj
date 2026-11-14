(ns konfo-backend.contentful.json-test
  (:require [clojure.test :refer :all]
            [cheshire.core :as cheshire]
            [konfo-backend.contentful.json :refer [gson unresolved-link?]])
  (:import (com.contentful.java.cda CDAEntry)
           (com.google.gson Gson)
           (java.util ArrayList)))

(defn- link [id]
  {"sys" {"type" "Link" "linkType" "Entry" "id" id}})

(defn- ->entry
  "Luo CDAEntryn samaan muotoon kuin Contentful SDK (Gson-mapit, oletuslokaali fi)."
  [id content-type fields]
  (let [json (cheshire/generate-string
              {"sys"               {"id" id "type" "Entry"
                                    "createdAt" "2026-11-13T13:27:31.456Z"
                                    "updatedAt" "2026-11-13T13:27:31.456Z"
                                    "contentType" {"sys" {"id" content-type}}}
               "defaultLocale"     "fi"
               "fallbackLocaleMap" {}
               "fields"            (update-vals fields (fn [v] {"fi" v}))
               "rawFields"         (update-vals fields (fn [v] {"fi" v}))})]
    (.fromJson (Gson.) ^String json CDAEntry)))

(defn- serialize [entry]
  (cheshire/parse-string (.toJson gson entry)))

(deftest unresolved-link?-test
  (is (unresolved-link? (.fromJson (Gson.) (cheshire/generate-string (link "a")) java.util.Map)))
  (is (not (unresolved-link? {"sys" {"type" "Entry"}})))
  (is (not (unresolved-link? "Link")))
  (is (not (unresolved-link? nil))))

(deftest write-field-unresolved-links-test
  (testing "List of only unresolved links is written as an empty array, not as a JSON string"
    (let [result (serialize (->entry "e1" "opintopolkuEtusivu"
                                     {"koulutustietoNostot" [(link "a") (link "b")]
                                      "nimi"                "Etusivu"}))]
      (is (= [] (get result "koulutustietoNostot")))
      (is (= "Etusivu" (get result "nimi")))))

  (testing "Unresolved links are dropped and resolved ones kept as references"
    (let [nosto  (->entry "n1" "nosto" {"name" "Europassi"})
          entry  (->entry "e1" "opintopolkuEtusivu" {"muutPalvelutNostot" []})
          _      (.setField entry "fi" "muutPalvelutNostot" (ArrayList. [nosto (link "missing")]))
          result (serialize entry)]
      (is (= [{"id" "n1" "name" "Europassi" "type" "nosto"}]
             (get result "muutPalvelutNostot")))))

  (testing "Unresolved single link field is left out"
    (let [result (serialize (->entry "e1" "opintopolkuEtusivu" {"paanosto" (link "a")}))]
      (is (not (contains? result "paanosto")))))

  (testing "Empty list is written as an empty array, not as a JSON string"
    (let [result (serialize (->entry "e1" "opintopolkuEtusivu" {"koulutustietoNostot" []
                                                                "whereShown"          []}))]
      (is (= [] (get result "koulutustietoNostot")))
      (is (= [] (get result "whereShown")))))

  (testing "Lists without links keep the previous format"
    (let [result (serialize (->entry "e1" "sivu" {"tagit" ["a" "b"]}))]
      (is (= "[\"a\",\"b\"]" (get result "tagit"))))))

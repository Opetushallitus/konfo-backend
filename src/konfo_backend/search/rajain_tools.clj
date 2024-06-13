(ns konfo-backend.search.rajain-tools
  (:require [clj-time.core :as time]
            [clojure.string :refer [join replace-first split]]
            [konfo-backend.tools :refer [->kouta-date-time-string
                                         ->lower-case
                                         ->lower-case-vec
                                         kouta-date-time-string->date-time]]))

(defn ->terms-query [key value]
  (let [term-key (keyword (str "search_terms." key))]
    (cond
      (and (coll? value) (= (count value) 1)) {:term {term-key (->lower-case (first value))}}
      (coll? value) {:terms {term-key (->lower-case-vec value)}}
      :else {:term {term-key (->lower-case value)}})))

(defn nested-terms-query
  [nested-field-name field-name constraint]
  {:nested
   {:path (str "search_terms." nested-field-name)
    :query
    {:bool
     {:filter (->terms-query (str nested-field-name "." field-name) constraint)}}}})

(defn ->boolean-term-query
  [key]
  (->terms-query key true))

(defn ->conditional-boolean-term-query
  [key required-val val]
  (when (= val required-val)
    (->terms-query key val)))

(defn onkoTuleva-query [tuleva?]
  {:term {:search_terms.onkoTuleva tuleva?}})

(defn number-range-query
  [key value]
  (when (and (vector? value) (not-empty value))
    (let [min (first value)
          max (second value)]
      {:range {(keyword (str "search_terms." key))
               (if (not (nil? max)) {:gte min :lte max} {:gte min})}})))

(defn- boolean-constraint? [constraint-val]
  (true? constraint-val))

(defn- vector-constraint? [constraint-val]
  (and (vector? constraint-val) (not-empty constraint-val)))

(defn- object-constraint? [constraint-val]
  (and (map? constraint-val) (not-empty (keys constraint-val))))

(defn- number-constraint? [constraint-val]
  (number? constraint-val))

(defn constraint? [constraint-val]
  ((some-fn boolean-constraint? vector-constraint? object-constraint? number-constraint?) constraint-val))

(defn arg-count [f]
  {:pre [(instance? clojure.lang.AFunction f)]}
  (-> f class .getDeclaredMethods first .getParameterTypes alength))

(defn make-query-for-rajain
  [constraints rajain current-time]
  (let [constraint-vals (get constraints (:id rajain))
        make-query-arg-count (arg-count (:make-query rajain))]
    (when (constraint? constraint-vals)
      ; Sallitaan make-query-funktioiden määrittely rajain_definitions:ssa joustavasti eri parametrimäärillä (0-2)
      (apply (:make-query rajain) (take make-query-arg-count [constraint-vals current-time])))))

(defn hakukaynnissa-filter-query
  [current-time]
  {:nested {:path  "search_terms.hakutiedot.hakuajat"
            :query {:bool {:filter [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte current-time}}}
                                    {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}}
                                                     {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt current-time}}}]}}]}}}})

(defn hakualkaapaivissa-filter-query
  [current-time days]
  (when days
    (let [max-time (->kouta-date-time-string (time/plus (kouta-date-time-string->date-time current-time) (time/days days)))]
      {:nested {:path "search_terms.hakutiedot.hakuajat"
                :query {:range {:search_terms.hakutiedot.hakuajat.alkaa {:gt current-time :lte max-time}}}}})))

(defn pohjakoulutusvaatimukset-filter-query
  [pohjakoulutusvaatimukset]
  (when pohjakoulutusvaatimukset
    {:nested
     {:path "search_terms.hakutiedot"
      :query
      {:bool
       {:should [{:bool {:filter (->terms-query "hakutiedot.pohjakoulutusvaatimukset" pohjakoulutusvaatimukset)}}
                 {:bool {:must_not {:exists {:field "search_terms.hakutiedot.pohjakoulutusvaatimukset"}}}}]}}}}))


(defn ->field-key [field-name]
  (str "search_terms." (name field-name)))

(defn all-must
  [conditions]
  (when-let [active-conditions (not-empty (filter some? conditions))]
    (if (> (count active-conditions) 1)
      {:bool {:filter (vec active-conditions)}}
      (first active-conditions))))

(defn- with-real-hits
  ([agg rajain-context]
   (let [reverse-nested-path (get-in rajain-context [:reverse-nested-path])]
     (assoc agg :aggs {:real_hits {:reverse_nested (if reverse-nested-path {:path reverse-nested-path} {})}})))
  ([agg]
   (with-real-hits agg nil)))

(defn- rajain-terms-agg [field-name rajain-context]
  (let [default-terms {:field field-name
                       :min_doc_count 0
                       :size 1000}]
    {:terms (merge default-terms (get-in rajain-context [:term-params]))}))

(defn- constrained-agg [constraints aggs]
  (if (not-empty constraints)
    {:filter {:bool {:filter constraints}}
     :aggs {:rajain aggs}}
    aggs))

(defn rajain-terms-aggregation
  [field-name constraints rajain-context]
  (constrained-agg
   constraints
   (with-real-hits (rajain-terms-agg field-name rajain-context) rajain-context)))

(defn bool-agg-filter [own-filter constraints rajain-context]
  (with-real-hits
    {:filter {:bool
              {:filter (remove nil? (vec (distinct (conj constraints own-filter))))}}}
    rajain-context))

(defn max-agg-filter
  ([field-name own-filter]
   (let [max-agg {:max {:field field-name}}]
     (if own-filter
       {:filter {:bool
                 {:filter [own-filter]}}
        :aggs {:max-val max-agg}}
       max-agg)))
  ([field-name]
   (max-agg-filter field-name nil)))

(defn strip-excess-nested [query common-nested-path]
  (let [query-nested-path (get-in query [:nested :path])]
    (if (and query-nested-path (= query-nested-path common-nested-path))
      (or (get-in query [:nested :query :bool :filter])
          (get-in query [:nested :query]))
      query)))

(defn get-top-level-nested-path
  [nested-path]
  (when nested-path (as-> nested-path $
                      (split $ #"\.")
                      (take 2 $)
                      (join "." $))))

(defn nested-rajain-aggregation
  [nested-path constraints rajain-agg rajain-context]
  (let [constraints-by-nest (group-by #(get-top-level-nested-path (get-in % [:nested :path])) constraints)
        root-constraints (get constraints-by-nest nil)
        own-nested-constraints (get constraints-by-nest nested-path)
        other-nested-constraints (flatten (vals (dissoc constraints-by-nest nil nested-path)))]
    (constrained-agg root-constraints
                     (constrained-agg other-nested-constraints
                                      {:nested {:path nested-path}
                                       :aggs {:rajain (constrained-agg (flatten (map #(strip-excess-nested % nested-path) own-nested-constraints))
                                                                       (with-real-hits rajain-agg rajain-context))}}))))
(defn nested-rajain-terms-aggregation
  [field-name constraints rajain-context]
  (let [nested-path (-> field-name
                        (replace-first ".keyword" "")
                        (split #"\.")
                        (drop-last) (#(join "." %)))]
    (nested-rajain-aggregation nested-path
                               constraints
                               (rajain-terms-agg field-name rajain-context)
                               rajain-context)))

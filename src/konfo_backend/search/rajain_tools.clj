(ns konfo-backend.search.rajain-tools
  (:require [clojure.string :refer [lower-case replace-first split join]]
            [clj-time.core :as time]
            [konfo-backend.tools :refer [->lower-case-vec]]))

(defn by-rajaingroup
  [rajaimet rajain-group]
  (mapv :id (filter #(= (:rajainGroupId %) rajain-group) rajaimet)))

(defn ->terms-query [key value]
  (let [term-key (keyword (str "search_terms." key))
        ->lower-case (fn [val] (if (string? val) (lower-case val) val))]
    (cond
      (and (coll? value) (= (count value) 1)) {:term {term-key (->lower-case (first value))}}
      (coll? value) {:terms {term-key (->lower-case-vec value)}}
      :else {:term {term-key (->lower-case value)}})))

(defn nested-query
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
          max (if (> (count value) 1) (get value 1 nil) nil)]
      {:range {(keyword (str "search_terms." key))
               (if (not (nil? max)) {:gte min :lte max} {:gte min})}})))

(defn- boolean-constraint? [constraint-val]
  (and (boolean? constraint-val) (true? constraint-val)))

(defn- vector-constraint? [constraint-val]
  (and (vector? constraint-val) (not-empty constraint-val)))

(defn- object-constraint? [constraint-val]
  (and (map? constraint-val) (not-empty (keys constraint-val))))

(defn make-query-for-rajain
  [constraints rajain]
  (let [constraint-vals (get constraints (:id rajain))]
    (cond
      (boolean-constraint? constraint-vals) ((:make-query rajain))
      (vector-constraint? constraint-vals) ((:make-query rajain) constraint-vals)
      (object-constraint? constraint-vals) ((:make-query rajain) constraint-vals))))

(defn constraint? [constraints key]
  (let [constraint-val (key constraints)]
    (or (boolean-constraint? constraint-val) (vector-constraint? constraint-val) (object-constraint? constraint-val))))

(defn make-combined-should-filter-query
  [constraints rajain-items]
  (let [conditions (map #(make-query-for-rajain constraints %) rajain-items)
        active-conditions (filter some? conditions)]
    (when (not-empty active-conditions)
      {:bool {:should (vec active-conditions)}})))

(defn hakukaynnissa-filter-query
  [current-time]
  {:bool {:should [{:bool {:filter [{:range {:search_terms.toteutusHakuaika.alkaa {:lte current-time}}}
                                    {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.toteutusHakuaika.paattyy"}}}},
                                                     {:range {:search_terms.toteutusHakuaika.paattyy {:gt current-time}}}]}}]}}
                   {:nested {:path "search_terms.hakutiedot.hakuajat"
                             :query {:bool {:filter [{:range {:search_terms.hakutiedot.hakuajat.alkaa {:lte current-time}}}
                                                     {:bool {:should [{:bool {:must_not {:exists {:field "search_terms.hakutiedot.hakuajat.paattyy"}}}},
                                                                      {:range {:search_terms.hakutiedot.hakuajat.paattyy {:gt current-time}}}]}}]}}}}]}})

(defn hakualkaapaivissa-filter-query
  [current-time days]
  (when days
    (let [max-time (time/plus current-time (time/days days))]
      {:bool {:should [{:bool {:filter {:range {:search_terms.toteutusHakuaika.alkaa {:gt current-time :lte max-time}}}}}
                       {:nested {:path "search_terms.hakutiedot.hakuajat"
                                 :query {:bool {:filter {:range {:search_terms.hakutiedot.hakuajat.alkaa {:gt current-time :lte max-time}}}}}}}]}})))


(defn ->field-key [field-name]
  (str "search_terms." (name field-name)))

(defn all-must
  [conditions]
  (let [active-conditions (filter some? conditions)]
    (when (not-empty active-conditions)
      (if (> (count active-conditions) 1)
        {:bool {:filter (vec active-conditions)}}
        (first active-conditions)))))

(defn- with-real-hits
  ([agg rajain-context]
   (let [reverse-nested-path (get-in rajain-context [:reverse-nested-path])]
     (assoc agg :aggs {:real_hits {:reverse_nested (if reverse-nested-path {:path reverse-nested-path} {})}})))
  ([agg]
   (with-real-hits agg nil)))

(defn- rajain-terms-agg
  [field-name rajain-context]
  (let [default-terms {:field field-name
                       :min_doc_count 0
                       :size 1000}]
    (with-real-hits {:terms (merge default-terms (get-in rajain-context [:term-params]))} rajain-context)))

(defn- constrained-agg [constraints filtered-aggs plain-aggs]
  (if (not-empty constraints)
    {:filter {:bool {:filter constraints}}
     :aggs filtered-aggs}
    plain-aggs))

(defn rajain-aggregation
  [field-name contraints rajain-context]
  (constrained-agg
   contraints
   {:rajain (rajain-terms-agg field-name rajain-context)}
   (rajain-terms-agg field-name rajain-context)))

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

(defn nested-rajain-aggregation
  [rajain-key field-name constraints rajain-context]
  (let [nested-agg {:nested  {:path (-> field-name
                                        (replace-first ".keyword" "")
                                        (split #"\.")
                                        (drop-last) (#(join "." %)))}
                    :aggs {:rajain (rajain-terms-agg field-name rajain-context)}}]
    (constrained-agg
     constraints
     {(keyword rajain-key) nested-agg}
     nested-agg)))

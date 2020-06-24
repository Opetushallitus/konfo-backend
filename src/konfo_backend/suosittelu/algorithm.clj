(ns konfo-backend.suosittelu.algorithm)

(defn- get-sum-array
  [matrix-data]
  (->> matrix-data
      (map :etaisyydet)
      (apply map +)
      (vec)))

;Find the indexes of smallest X values from an 1D iterable list
(defn- find-min-n-indices
  [values n]
  ; Distances are in range of 0 - 10 000
  (let [minVals (atom (vec (take n (repeat 10000))))
        indices (atom (vec (take n (repeat 0))))]

    (doseq [index (range (count values))
            :let [value (nth values index)]]
      (when (< value (nth @minVals (- n 1)))
        (swap! minVals #(assoc % (- n 1) value))
        (swap! indices #(assoc % (- n 1) index))

        ; Find the ranging amongst the topX
        (doseq [i (range (- n 2) -1 -1)
                :when (< value (nth @minVals i))]
          (swap! minVals #(assoc % (+ i 1) (nth @minVals i)))
          (swap! indices #(assoc % (+ i 1) (nth @indices i)))

          (swap! minVals #(assoc % i value))
          (swap! indices #(assoc % i index)))))
    @indices))

(defn- in?
  [value coll]
  (some #(= % value) coll))

(defn- get-n-relevant-indices
  [indices skip-indices n]
  (->> indices
       (filter #(not (in? % skip-indices)))
       (take n)
       (vec)))

(defn calculate-top-n-recommendations
  [n matrix-data]
  (when (seq matrix-data)
    (let [skip-indices (vec (map :jarjestysnumero matrix-data))]
      (-> matrix-data
          (get-sum-array)
          (find-min-n-indices (+ (count skip-indices) n))
          (get-n-relevant-indices skip-indices n)))))
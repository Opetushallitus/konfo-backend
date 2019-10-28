(ns konfo-backend.koodisto.koodisto)

(defn list
  [koodi]
  (if (= "maakunta" koodi)
    ["maakunta_01",
     "maakunta_02",
     "maakunta_03",
     "maakunta_04",
     "maakunta_05",
     "maakunta_06",
     "maakunta_07",
     "maakunta_08",
     "maakunta_09",
     "maakunta_10",
     "maakunta_11",
     "maakunta_12",
     "maakunta_13",
     "maakunta_14",
     "maakunta_15",
     "maakunta_16",
     "maakunta_17",
     "maakunta_18",
     "maakunta_19",
     "maakunta_20",
     "maakunta_21",
     "maakunta_99"]))
(ns alc.start-repl.impl.opts)

(set! *warn-on-reflection* true)

(defn merge-only-map-strs
  [map-strs]
  (reduce (fn [acc map-str]
            (let [read-obj (try
                             (read-string map-str)
                             (catch Exception _
                               nil))]
              (if (map? read-obj)
                (merge acc read-obj)
                acc)))
    {}
    map-strs))

(defn find-first-string
  [args]
  (->> args
    (keep (fn [a-str]
            (let [thing (try
                          (read-string a-str)
                          (catch Exception _
                            nil))]
              ;; XXX
              (when (not (map? thing))
                a-str))))
    first))

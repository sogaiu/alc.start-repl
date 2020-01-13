(ns alc.start-repl.impl.report)

(set! *warn-on-reflection* true)

(defn report
  [{:keys [:pid :port :proj-dir :res]}]
  (println)
  (if res
    (println "Repl may have started for process:")
    (println "Repl may not have started for process:"))
  (println (str "  pid: " pid "\n"
             (when proj-dir
               (str "  proj-dir: " proj-dir "\n"))
             "  port: " port "\n")))


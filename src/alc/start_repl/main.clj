(ns alc.start-repl.main
  (:require
   [alc.start-repl.core :as as.c]
   [alc.start-repl.impl.exit :as asi.e]
   [alc.start-repl.impl.opts :as asi.o])
  (:gen-class))

(set! *warn-on-reflection* true)

(defn -main
  [& args]
  (let [opts
        (if-let [first-str-opt (asi.o/find-first-string args)]
          {:proj-dir first-str-opt}
          {})
        opts (merge opts
               (asi.o/merge-only-map-strs args))]
    (as.c/start-repl opts))
  (asi.e/exit 0))

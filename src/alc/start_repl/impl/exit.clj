(ns alc.start-repl.impl.exit)

(set! *warn-on-reflection* true)

(defn exit
  ([code]
   (exit code nil))
  ([code msg]
   (when (and (not= 0 code)
           msg)
     (binding [*out* *err*]
       (println *err* msg)))
   (flush)
   (System/exit code)))

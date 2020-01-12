(ns alc.start-repl.build
  (:require
   [alc.start-repl.impl.exit :as asi.e]
   [alc.start-repl.impl.jar :as asi.j]
   [alc.start-repl.impl.util :as asi.u]
   [clojure.java.io :as cji]))

(set! *warn-on-reflection* true)

(defn -main
  [& args]
  (let [src-dir (asi.u/find-alcsr-src-dir
                  (System/getProperty "java.class.path"))
        _ (assert src-dir
            (str "\n"
              "Failed to find source directory"))
        alcsr-src-dir (.getParent (cji/file src-dir) )
        [jar-name out-dir] args
        jar-name (if jar-name jar-name
                     "start-socket-repl-agent.jar")
        out-dir (if out-dir out-dir
                    alcsr-src-dir)]
    (asi.j/build-agent-jar alcsr-src-dir out-dir
      jar-name)
    (asi.e/exit 0)))

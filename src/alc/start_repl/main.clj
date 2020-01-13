(ns alc.start-repl.main
  (:require
   [alc.start-repl.core :as as.c]
   [alc.start-repl.impl.cli :as asi.c]
   [alc.start-repl.impl.exit :as asi.e]
   [clojure.java.io :as cji])
  (:gen-class))

(set! *warn-on-reflection* true)

(def cli-options
  [["-a" "--agent-jar AGENT-JAR" "Agent jar path"
    :default nil
    :validate [#(let [thing (cji/file %)]
                  (and (.exists thing)
                    (.isFile thing)))
               "Must be an existing file"]]
   ["-d" "--proj-dir PROJ-DIR" "Project directory"
    :default nil
    :validate [#(let [thing (cji/file %)]
                  (and (.exists thing)
                    (.isDirectory thing)))
               "Must be an existing directory"]]
   ["-g" "--debug" "Debug output"]
   ["-h" "--help" "Show usage"]
   ["-i" "--pid PID" "Process identifier"
    :default nil
    :parse-fn #(Integer/parseInt %)]
   ["-p" "--port PORT" "Port number"
    :default nil
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 1024 % 65536)
               "Must be a number between 1025 and 65535 inclusive"]]])

(defn -main
  [& args]
  (let [opts
        (asi.c/parse-opts args cli-options)
        {:keys [:errors :summary]} opts]
    (if errors
      (do
        (println errors)
        (println summary)
        (asi.e/exit 1))
      (do
        (if (:help (:options opts))
          (println summary)
          (as.c/start-repl (:options opts)))
        (asi.e/exit 0)))))

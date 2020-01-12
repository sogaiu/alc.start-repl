(ns alc.start-repl.main
  (:require
   [alc.start-repl.core :as as.c]
   [alc.start-repl.impl.cli :as asi.c]
   [alc.start-repl.impl.exit :as asi.e])
  (:gen-class))

(set! *warn-on-reflection* true)

(def cli-options
  [["-a" "--agent-jar AGENT-JAR" "Agent jar path"
    :default nil]
   ["-d" "--proj-dir PROJ-DIR" "Project directory"
    :default nil] ; XXX: possibly validate as existing directory?
   ["-g" "--debug DEBUG" "Debug output"]
   ;; XXX: fill this out?
   ["-h" "--help"]
   ["-i" "--pid PID" "Process identifier"
    :default nil
    :parse-fn #(Integer/parseInt %)]
   ["-p" "--port PORT" "Port number"
    :default nil
    :parse-fn #(Integer/parseInt %)
    :validate [#(< 1024 % 65536) "Must be a number between 1025 and 65536"]]])

(defn -main
  [& args]
  (let [opts
        (asi.c/parse-opts args cli-options)
        errors (:errors opts)]
    (if-not errors
      (do
        (as.c/start-repl (:options opts))
        (asi.e/exit 0))
      (do
        ;; XXX
        (println errors)
        (asi.e/exit 1)))))

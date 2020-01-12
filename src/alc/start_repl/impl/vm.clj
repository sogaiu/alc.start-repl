(ns alc.start-repl.impl.vm
  (:require
   [alc.start-repl.impl.attach :as asi.a]))

(asi.a/ensure-attach-cp)

;; XXX: ibm has their own package apparently...
(import '[com.sun.tools.attach
          AgentInitializationException
          AgentLoadException
          VirtualMachine])

(set! *warn-on-reflection* true)

(defn instruct-vm
  [pid port agent-jar]
  (when-let [^VirtualMachine vm
             (try
               (VirtualMachine/attach (str pid))
               (catch Exception e
                 (println "attaching failed for pid:" pid)
                 (println (.getMessage e))
                 nil))]
    ;;(println "about to attempt loadAgent for:" pid)
    (let [res
          (try
            (.loadAgent vm
              agent-jar
              (str port)) ; XXX: work on the format of the passed arg here?
            true
            (catch AgentInitializationException e
              (println "AgentInitializationException, repl start doubtful")
              (println "  message:" (.getMessage e))
              (println e)
              :agent-init-ex)
            ;; XXX: seems to happen, yet loading seems successful...
            (catch AgentLoadException e
              (println "AgentLoadException, but repl may have started")
              #_(println "  message:" (.getMessage e))
              :agent-load-ex)
            ;; XXX: this can happen, yet loading may have been successful
            (catch java.io.IOException e
              (println "java.io.IOException, but repl may have started")
              #_(println "  message:" (.getMessage e))
              :io-ex)
            ;; XXX: what could happen here?
            (catch Exception e
              (println "Unexpected exception: please report to maintainer")
              (println "  message:" (.getMessage e))
              (println e)
              :unknown-ex))]
      (.detach vm)
      res)))

(defn interpret-res
  [res]
  (get {;; XXX: probable failure?
        :agent-init-ex nil
        :unknown-ex nil
        ;; XXX: possibly succeeded
        :agent-load-ex true
        :io-ex true
        ;; highest chance of success?
        true true}
    res))

(comment

  (require '[clojure.java.io :as cji])

  ;; XXX: pid not likely to be correct here
  (instruct-vm 17364 8987
    (.getPath (cji/file (System/getenv "HOME")
                "src" "alc.start-repl" "start-socket-repl-agent.jar")))

  )

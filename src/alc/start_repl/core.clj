;; NOTES
;;
;; - jvm clojure via clj works
;; - jvm clojure via lein works
;; - jvm clojure via shadow-cljs works
;;   but there is a socket repl by default, so not so important?

;; TODO:
;;
;; - test for both java 8 and 11
;; - multiple processes matching for single project
;;   - always(?) give feedback when multiple pids detected?
;; - repl features
;;   - starting prepl instead?
;;   - port auto-selection
;;     - support providing a set or range of port numbers to choose from?
;;   - trying different numbers upon bind failure?
;; - consider implications of hard-wiring an ipv4 address (127.0.0.1)
;;   - arrange to pass agent address to bind to as param?
;; - following not supported (yet?)
;;   - boot socket repl
;;     (loadAgent fails -- NoClassDefFoundError: cloure/lang/Var)
;;     it's easy to start a boot socket repl though, so may be it's not so
;;     important
;; - possible to get a good idea of whether repl started by examining
;;   external jvm's system properties -- but this may not be worth it
;; - figure out why AgentLoadException is occuring -- works, but 8 vs 11?
;; - update docs
;;   - document process of building agent jar

(ns alc.start-repl.core
  (:require
   [alc.start-repl.impl.attach :as asi.a]
   [alc.start-repl.impl.net :as asi.n]
   [alc.start-repl.impl.pid :as asi.p]
   [clojure.java.io :as cji]
   [clojure.string :as cs]))

(set! *warn-on-reflection* true)

(asi.a/ensure-attach-cp)

;; XXX: ibm has their own package apparently...
(import '[com.sun.tools.attach
          AgentInitializationException
          AgentLoadException
          VirtualMachine])

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
     (try
       (.loadAgent vm
         agent-jar
         (str port)) ; XXX: work on the format of the passed arg here?
       (catch AgentInitializationException e
         (println "AgentInitializationException, repl may not have started")
         (println "  message:" (.getMessage e))
         (println e))
       ;; XXX: seems to happen, yet loading seems successful...
       (catch AgentLoadException e
         (println "AgentLoadException, but repl may have started")
         #_(println "  message:" (.getMessage e)))
       ;; XXX: this can happen, yet loading may have been successful
       (catch java.io.IOException e
         (println "java.io.IOException, but repl may have started")
         #_(println "  message:" (.getMessage e)))
       ;; XXX: what could happen here?
       (catch Exception e
         (println "Unexpected exception: please report to maintainer")
         (println "  message:" (.getMessage e))
         (println e)))
     (.detach vm)))

;; XXX: review logic here
;; tries to determine if run from alc.start-repl's source directory
;; or via some other location
(defn find-alcsr-src-dir
  [class-path]
  (let [paths (cs/split class-path
                (re-pattern (System/getProperty "path.separator")))]
    ;; any hits among fuller paths?
    (if-let [target-dir (first (filter #(re-find #"alc\.start-repl" %)
                                 paths))]
      target-dir
      ;; any hits among relative paths?
      (first (keep (fn [path]
                     (let [file (cji/file path)]
                       (when (not (.isAbsolute file))
                         (let [full-path (.getCanonicalPath file)]
                           (when (re-find #"alc\.start-repl" full-path)
                             full-path)))))
               paths)))))

(defn start-repl
  [{:keys [:agent-jar :debug :pid :port :proj-dir]}]
  (let [agent-jar (if agent-jar agent-jar
                      ;; XXX: generate jar or use a pre-compiled one?
                      (let [jcp (System/getProperty "java.class.path")
                            src-dir (find-alcsr-src-dir jcp)
                            _ (assert src-dir "Failed to find source directory")
                            alcsr-src-dir (.getParent (cji/file src-dir))
                            ;; XXX: windows paths
                            agent-jar (str alcsr-src-dir
                                        "/start-socket-repl-agent.jar")]
                        (assert agent-jar "Failed to create agent-jar")
                        agent-jar))
        _ (when port
            (assert (asi.n/check-port port)
              (str "Port unavailable: " port)))
        port (or port (asi.n/find-port))
        _ (assert port "Failed to choose suitable port")
        ctx {:agent-jar agent-jar
             :pid pid
             :port port
             :proj-dir proj-dir}
        [ctx pids pid] (if-not pid
                         (let [ctx (asi.p/find-pids ctx)
                               pids (:pids ctx)
                               pid (first pids)]
                           [(assoc ctx :pid pid)
                            pids pid]
                           [ctx
                            [pid] pid]))]
    (assert pid
      "Failed to determine pid")
    (assert (= (count pids) 1)
      (str "Did not find exactly one matching pid: " pids))
    (instruct-vm ^String pid port agent-jar)
    ;; XXX
    (println (str "Tried to start repl for pid: " pid " on port: " port))
    (when debug ctx)))

(comment

  ;; XXX: pid not likely to be correct here
  (instruct-vm 17364 8987
    (str (System/getenv "HOME")
      "/src/alc.start-repl/start-socket-repl-agent.jar"))

  (start-repl {:debug true
               :proj-dir
               (str (System/getenv "HOME")
                 "/src/four-horsemen-of-apocalypse.sogaiu")})

  (start-repl {:debug true
               :port 8987
               :proj-dir
               (str (System/getenv "HOME")
                 "/src/augistints")})

  (start-repl {:port 8987
               :proj-dir
               (str (System/getenv "HOME")
                 "/src/augistints")})

  (start-repl {:agent-jar
               (str (System/getenv "HOME")
                 "/src/alc.start-repl/start-socket-repl-agent.jar")
               :port 8987
               :proj-dir
               (str (System/getenv "HOME")
                 "/src/augistints")})

  (start-repl {:debug true
               :proj-dir
               (str (System/getenv "HOME")
                 "/src/antoine")})

  (start-repl {:debug true
               :pid "17905"
               :proj-dir
               (str (System/getenv "HOME")
                 "/src/antoine")})
  
  (start-repl {:debug true
               :port 8888
               :proj-dir
               (str (System/getenv "HOME")
                 "/src/lumo")})

  )

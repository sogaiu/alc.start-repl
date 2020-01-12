;; NOTES
;;
;; - jvm clojure via clj works
;; - jvm clojure via lein works
;; - jvm clojure via shadow-cljs works
;;   but there is a socket repl by default, so not so important?

;; TODO:
;;
;; - test
;;   - consider how to test
;;     - consider whether babashka might be applied
;;   - test for both java 8 and 11
;;   - test different jdks
;;     - openjdk
;;     - adoptopenjdk
;;     - zulu
;;     - labsjdk
;;     - coretto
;;     - others?
;;   - multiplatform testing
;; - consider support for ordinary command line using tools.cli
;;   - any way to allow supporting clojure map (as string) to specify opts?
;; - figure out why AgentLoadException is occuring -- works, but 8 vs 11?
;; - update docs
;;   - sync docs
;; - repl features
;;   - starting prepl instead?
;;   - port auto-selection
;;     - support providing a set or range of port numbers to choose from?
;;   - trying different numbers upon bind failure?
;; - output
;;   - silent mode?
;;   - improve formatting
;; - consider implications of hard-wiring an ipv4 address (127.0.0.1)
;;   - arrange to pass agent address to bind to as param?
;; - following not supported (yet?)
;;   - boot socket repl
;;     (loadAgent fails -- NoClassDefFoundError: cloure/lang/Var)
;;     it's easy to start a boot socket repl though, so may be it's not so
;;     important
;; - possible to get a good idea of whether repl started by examining
;;   external jvm's system properties -- but this may not be worth it

(ns alc.start-repl.core
  (:require
   [alc.start-repl.impl.net :as asi.n]
   [alc.start-repl.impl.pid :as asi.p]
   [alc.start-repl.impl.util :as asi.u]
   [alc.start-repl.impl.vm :as asi.v]
   [clojure.java.io :as cji]
   [clojure.string :as cs]))

(set! *warn-on-reflection* true)

(defn start-repl
  [{:keys [:agent-jar :debug :pid :port :proj-dir]}]
  (let [agent-jar
        (if agent-jar agent-jar
            ;; XXX: generate jar or use a pre-compiled one?
            (let [jcp (System/getProperty "java.class.path")
                  src-dir (asi.u/find-alcsr-src-dir jcp)
                  _ (assert src-dir
                      (str "\n"
                        "Failed to find source directory"))
                  alcsr-src-dir (.getParent (cji/file src-dir))
                  agent-jar
                  (.getPath (cji/file alcsr-src-dir
                              "start-socket-repl-agent.jar"))]
              (assert agent-jar "Failed to find agent-jar")
              agent-jar))
        _ (when port
            (assert (asi.n/check-port port)
              (str "\n"
                "  Port unavailable: " port)))
        port (or port (asi.n/find-port))
        _ (assert port
            (str "\n"
              "  Failed to choose suitable port"))
        ctx {:agent-jar agent-jar
             :pid pid
             :port port
             :proj-dir proj-dir}
        [ctx pids pid] (if-not pid
                         (let [ctx (asi.p/find-pids ctx)
                               pids (:pids ctx)
                               pid (first pids)]
                           [(assoc ctx :pid pid)
                            pids pid])
                         [ctx
                          [pid] pid])
        proj-dir (if proj-dir proj-dir
                     (:proj-dir ctx))]
    (assert pid
      (str "\n"
        "Failed to determine pid"))
    (assert (= (count pids) 1)
      (let [pid-str (cs/join ", " pids)]
        (str "\n"
          "  Did not find exactly one matching pid: " pid-str "\n"
          "    Note, :pid argument can be used to select a target process.\n"
          "    e.g. '{:pid <pid>}'")))
    ;; for formatting...
    (let [res (asi.v/interpret-res
                (asi.v/instruct-vm ^String pid port agent-jar))]
      (println)
      (if res
        (println "Repl may have started for process:")
        (println "Repl may not have started for process:")))
    (println (str "  pid: " pid "\n"
               (when proj-dir
                 (str "  proj-dir: " proj-dir "\n"))
               "  port: " port "\n"))
    (when debug ctx)))

(comment

  (start-repl {:debug true
               :proj-dir
               (.getPath (cji/file (System/getenv "HOME")
                           "src"
                           "four-horsemen-of-apocalypse.sogaiu"))})

  (start-repl {:debug true
               :port 8985
               :proj-dir
               (.getPath (cji/file (System/getenv "HOME")
                           "src" "augistints"))})

  (start-repl {:port 8986
               :proj-dir
               (.getPath (cji/file (System/getenv "HOME")
                           "src" "augistints"))})

  (start-repl {:agent-jar
               (.getPath (cji/file (System/getenv "HOME")
                           "src" "alc.start-repl"
                           "start-socket-repl-agent.jar"))
               :port 8987
               :proj-dir
               (.getPath (cji/file (System/getenv "HOME")
                           "src" "augistints"))})

  (start-repl {:debug true
               :proj-dir
               (.getPath (cji/file (System/getenv "HOME")
                           "src" "antoine"))})

  (start-repl {:debug true
               :pid "17905" ; XXX: not likely to be correct
               :proj-dir
               (.getPath (cji/file (System/getenv "HOME")
                           "src" "antoine"))})

  ;; XXX: doesn't work with boot-based things
  (start-repl {:debug true
               :port 8888
               :proj-dir
               (.getPath (cji/file (System/getenv "HOME")
                           "src" "lumo"))})

  )

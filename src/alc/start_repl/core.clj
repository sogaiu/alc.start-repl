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
;; - command line handling
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
;;     - (loadAgent fails -- NoClassDefFoundError: cloure/lang/Var)
;;     - it's easy to start a boot socket repl though, so may be it's not so
;;       important
;;     - may be it's worth understanding why it doesn't work though
;; - possible to get a good idea of whether repl started by examining
;;   external jvm's system properties -- but this may not be worth it
;; - possible to determine proj-dir even if none specified if appropriate
;;   pid is known, but may be this is not worth it
;; - possibly resolve value of proj-dir (as full path?)
;; - could report on approach taken (e.g. if pid is specified, not going to
;;   scan for pids, if pid is not specified, will base search on proj-dir or
;;   user.dir value)

(ns alc.start-repl.core
  (:require
   [alc.start-repl.impl.net :as asi.n]
   [alc.start-repl.impl.pid :as asi.p]
   [alc.start-repl.impl.report :as asi.r]
   [alc.start-repl.impl.util :as asi.u]
   [alc.start-repl.impl.vm :as asi.v]
   [clojure.string :as cs]))

(set! *warn-on-reflection* true)

(defn start-repl
  [{:keys [:agent-jar :debug :pid :port :proj-dir]}]
  (let [agent-jar (if agent-jar agent-jar
                      ;; XXX: generate jar or use a pre-compiled one?
                      (asi.u/find-agent-jar))
        _ (assert agent-jar
            (str "\n"
              "  Failed to find agent-jar"))
        _ (when port
            (assert (asi.n/check-port port)
              (str "\n"
                "  Port unavailable: " port)))
        port (or port (asi.n/find-port))
        _ (assert port
            (str "\n"
              "  Failed to choose suitable port"))
        [pid pids proj-dir]
        (if pid
          [pid [pid] proj-dir]
          (let [proj-dir (if proj-dir proj-dir
                             ;; no pid and no proj-dir -> user.dir is proj-dir
                             (System/getProperty "user.dir"))
                pids (asi.p/find-pids proj-dir)]
            [(first pids) pids proj-dir]))
        _ (assert pid
            (str "\n"
              "  Failed to determine pid"))
        _ (assert (= (count pids) 1)
            (str "\n"
              "  Did not find 1 matching pid: " (cs/join ", " pids) "\n"
              "    Note, --pid arg can be used to select a target process.\n"
              "    e.g. '{:pid <pid>}'"))
        ctx {:agent-jar agent-jar
             :pid pid
             :pids pids
             :port port
             :proj-dir proj-dir
             :res (asi.v/instruct-vm ^String pid port agent-jar)}]
    ;; for formatting...
    (asi.r/report ctx)
    (when debug ctx)))

(comment

  (require '[clojure.java.io :as cji])

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

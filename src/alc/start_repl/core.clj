(ns alc.start-repl.core
  (:require
   [clojure.java.io :as cji]
   [clojure.string :as cs])
  ;; XXX: ibm has their own package apparently...
  (:import [com.sun.tools.attach
            AgentInitializationException
            AgentLoadException
            VirtualMachine VirtualMachineDescriptor]))

;; NOTES
;;
;; - jvm clojure via clj works
;; - jvm clojure via lein works
;; - jvm clojure via shadow-cljs works
;;   but there is a socket repl by default, so not so important?

;; TODO:
;;
;; - test for both java 8 and 11
;; - move some things out of core.clj
;; - following not supported (yet?)
;;   - boot socket repl
;;     (loadAgent fails -- NoClassDefFoundError: cloure/lang/Var)
;;     it's easy to start a boot socket repl though, so may be it's not so
;;     important
;; - figure out why AgentLoadException is occuring -- works, but 8 vs 11?
;; - sync docs

(set! *warn-on-reflection* true)

(defn clojure-jvm-matcher
  [[key value]]
  (when (re-find #"^(clojure|boot)\." key)
    [key value]))

(defn clojure-server-matcher
  [[key value]]
  (when (re-find #"^clojure\.server\.(.*)" key)
    [key value]))

(defn scan-jvms
  []
  (doall
    (reduce
      (fn [stats ^VirtualMachineDescriptor vmd]
        (if-let [^VirtualMachine vm
                 (try
                   (VirtualMachine/attach vmd)
                   (catch Exception _
                     nil))]
          (let [pid (.id vmd)
                ^java.util.Properties props (.getSystemProperties vm)
                user-dir (.getProperty props "user.dir")
                ;; XXX: may not be robust
                cmd (.getProperty props "sun.java.command")
                clojure-entries
                (keep clojure-jvm-matcher props)
                repl-entries
                (keep clojure-server-matcher props)
                results {:clojure-entries clojure-entries
                         :cmd cmd
                         :pid pid
                         :props props
                         :repl-entries repl-entries
                         :user-dir user-dir}]
            (.detach vm)
            (conj stats results))
          stats))
      []
      (VirtualMachine/list))))

;; XXX: skip leiningen process; likely there is corr process that isn't skipped
(defn clojure?
  [{:keys [:cmd :clojure-entries] :as m}]
  (when (or (and (cs/starts-with? cmd "clojure.main")
              (not (cs/starts-with? cmd
                     "clojure.main -m leiningen.core.main")))
          (some (fn [[k _]]
                  (= k "boot.class.path"))
            clojure-entries))
    m))

(defn no-repl?
  [{:keys [:repl-entries] :as m}]
  (when (= 0 (count repl-entries))
    m))

(defn paths-eq?
  [path-a path-b]
  (= (.getCanonicalPath (cji/file path-a))
    (.getCanonicalPath (cji/file path-b))))

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
         (str port)) ; XXX: work on the format of the passed arg here
       (catch AgentInitializationException e
         (println "AgentInitializationException")
         (println "message:" (.getMessage e)))
       ;; XXX: seems to happen, yet loading seems successful...
       (catch AgentLoadException e
         (println "AgentLoadException")
         (println "message:" (.getMessage e)))
       (catch Exception e
         (println "unexpected exception")
         (println "message:" (.getMessage e))))
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

(defn find-pid
  [ctx]
  (let [proj-dir (if-let [proj-dir (:proj-dir ctx)]
                   proj-dir
                   (System/getProperty "user.dir"))
        ctx (assoc ctx :proj-dir proj-dir)
        jvms (scan-jvms)
        ctx (assoc ctx :jvms jvms)
        clojures (keep clojure? jvms)
        ctx (assoc ctx :clojures clojures)
        ;; XXX: only checking clojure socket repl (not shadow-cljs or boot?)
        no-repls (keep no-repl? clojures)
        ctx (assoc ctx :no-repls no-repls)
        ;; XXX: only match things that don't seem to have socket repls
        matches (filter (fn [{:keys [:user-dir]}]
                          (paths-eq? proj-dir user-dir))
                  no-repls)
        ctx (assoc ctx :matches matches)]
    ;; XXX: decide what to do if there is more than one match
    (if (= 1 (count matches))
      (let [stat (first matches)]
        (assoc ctx
          :pid (:pid stat)))
      ctx)))

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
        ctx {:agent-jar agent-jar
             :port (or port 7659)
             :proj-dir proj-dir}
        ctx (if pid
              (assoc ctx :pid pid)
              (find-pid ctx))
        pid (if pid pid
                (:pid ctx))]
    (assert pid "Failed to determine pid")
    (instruct-vm ^String pid port agent-jar)
    (when debug ctx)))

(comment

  (scan-jvms)

  (start-repl {:debug true
               :proj-dir
               (str (System/getenv "HOME")
                 "/src/four-horsemen-of-apocalypse.sogaiu")})

  ;; XXX: pid not likely to be correct here
  (instruct-vm 17364 8987
    (str (System/getenv "HOME")
      "/src/alc.start-repl/start-socket-repl-agent.jar"))

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

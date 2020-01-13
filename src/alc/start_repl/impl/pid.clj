(ns alc.start-repl.impl.pid
  (:require
   [alc.start-repl.impl.attach :as asi.a]
   [clojure.java.io :as cji]
   [clojure.string :as cs]))

(asi.a/ensure-attach-cp)

;; XXX: ibm has their own package apparently...
(import '[com.sun.tools.attach
          VirtualMachine VirtualMachineDescriptor])

(set! *warn-on-reflection* true)

(defn clojure-jvm-matcher
  [[key value]]
  (when (re-find #"^(clojure|boot)\." key)
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
                results {:clojure-entries clojure-entries
                         :cmd cmd
                         :pid pid
                         :props props
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

(defn paths-eq?
  [path-a path-b]
  (= (.getCanonicalPath (cji/file path-a))
    (.getCanonicalPath (cji/file path-b))))

(defn own-pid
  []
  (-> (java.lang.management.ManagementFactory/getRuntimeMXBean)
    .getName
    (cs/split #"@")
    first))

(defn find-pids
  [proj-dir]
  (let [clojures (keep clojure? (scan-jvms))
        self-pid (own-pid)
        matches (filter (fn [{:keys [:pid :user-dir]}]
                          (and (not= pid self-pid) ; skip self
                            (paths-eq? proj-dir user-dir)))
                  clojures)
        pids (map #(:pid %)
               matches)]
    (when (< 1 (count matches))
      (println "*** multiple pids found ***"))
    pids))

(comment

  (own-pid)

  (scan-jvms)

  (let [ctx {:proj-dir
             (.getPath (cji/file (System/getenv "HOME")
                         "src" "augistints"))}]
    (:pids (find-pids ctx)))

  )

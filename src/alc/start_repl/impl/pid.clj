(ns alc.start-repl.impl.pid
  (:require
   [clojure.java.io :as cji]
   [clojure.string :as cs]))

;; XXX: to arrange for things to also work on java <= 8
(let [jvm-spec-ver (System/getProperty "java.vm.specification.version")]
  (when (< (Double. jvm-spec-ver) 2)
    (let [tools-jar (clojure.java.io/file (System/getProperty "java.home")
                      ".." "lib" "tools.jar")
          add-jar-path
          (fn [^java.io.File jar-path]
            (let [url (java.net.URL. (-> jar-path .toURI .toString))
                  loader (ClassLoader/getSystemClassLoader)
                  meth (.getDeclaredMethod java.net.URLClassLoader
                         "addURL" (into-array Class [java.net.URL]))]
              (.setAccessible meth true)
              (.invoke meth loader (into-array [url]))))]
      (when (.exists tools-jar)
        (add-jar-path tools-jar))))
  ;; XXX: ibm has their own package apparently...
  (import '[com.sun.tools.attach
            VirtualMachine VirtualMachineDescriptor]))

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

(defn own-pid
  []
  (->
    (java.lang.management.ManagementFactory/getRuntimeMXBean)
    .getName
    (cs/split #"@")
    first))

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
        self-pid (own-pid)
        ;; XXX: only match things that don't seem to have socket repls
        matches (filter (fn [{:keys [:pid :user-dir]}]
                          (and (not= pid self-pid) ; skip self
                            (paths-eq? proj-dir user-dir)))
                  no-repls)
        ctx (assoc ctx :matches matches)]
    ;; XXX: decide what to do if there is more than one match
    (if (= 1 (count matches))
      (let [stat (first matches)]
        (assoc ctx
          :pid (:pid stat)))
      ctx)))

(comment

  (own-pid)

  (scan-jvms)

  )

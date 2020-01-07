(ns alc.start-repl.impl.fs
  (:require
   [clojure.java.io :as cji]))

(set! *warn-on-reflection* true)

;; XXX: would be nicer if more than two arguments could be handled
;;      use the into-array part?
(defn path-join
  [root-path ^String item-path]
  (let [nio-path (java.nio.file.Paths/get root-path
                   (into-array String []))]
    (-> nio-path
      (.resolve item-path)
      .toString)))

(defn nrepl-ports?
  [dir]
  (let [port-file (cji/file (path-join dir ".nrepl-port"))
        shadow-file (cji/file (path-join
                                (path-join dir ".shadow-cljs")
                               "nrepl.port"))
        ports (if (.exists port-file)
                [(slurp port-file)]
                [])
        ports (if (.exists shadow-file)
                (conj ports (slurp shadow-file))
                ports)]
    ports))

(defn find-nrepls
  [user-dirs]
  (->> user-dirs
    (keep (fn [[pid user-dir]]
            (when-let [ports (nrepl-ports? user-dir)]
              [pid ports])))
    (into {})))

(defn shadow-ports?
  [dir]
  (let [port-file (cji/file (path-join
                              (path-join dir ".shadow-cljs")
                              "socket-repl.port"))]
    (when (.exists port-file)
      [(slurp port-file)])))

(defn find-shadow-repls
  [user-dirs]
  (->> user-dirs
    (keep (fn [[pid user-dir]]
            ;; XXX: only one shadow-cljs socket repl at a time?
            (when-let [ports (shadow-ports? user-dir)]
              [pid ports])))
    (into {})))

(defn boot-socket-repl-ports?
  [dir]
  (let [port-file (cji/file (path-join dir ".socket-port"))
        ports (if (.exists port-file)
                [(slurp port-file)]
                [])]
    ports))

(defn find-boot-socket-repls
  [user-dirs]
  (->> user-dirs
    (keep (fn [[pid user-dir]]
            (when-let [ports (boot-socket-repl-ports? user-dir)]
              [pid ports])))
    (into {})))

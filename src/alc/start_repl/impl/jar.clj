(ns alc.start-repl.impl.jar
  (:require
   [clojure.java.io :as cji]
   [clojure.java.shell :as cjs]))

;; NOTES
;;
;; - splitting the agent code up into more than one namespace caused
;;   problems for looking up classes at runtime -- for the moment,
;;   everything to be loaded has been placed in a single namespace:
;;
;;   alc.start-repl.agent

;; TODO:
;;
;; - consider including source code in jar
;; - document building process

(defn build-agent-jar
  [src-dir out-dir jar-name]
  (let [classes-dir (cji/file src-dir "classes")
        classes-path (.getPath classes-dir)]
    ;; XXX: classes directory needs to exist and it's likely to be less
    ;;      problematic if empty
    (assert (.delete classes-dir)
      (str classes-path " non-empty, please empty"))
    (.mkdir classes-dir)
    ;; XXX: clear old files?
    (when (.exists classes-dir)
      (let [jar-path
            (.getPath (cji/file out-dir jar-name))]
        ;; create class files
        (cjs/with-sh-dir src-dir
          (cjs/sh "clj" "-Sforce"
            "-e" "(compile 'alc.start-repl.agent)"))
        ;; create jar with appropriate manifest
        (cjs/with-sh-dir src-dir
          (cjs/sh "jar" "cfmv"
            jar-path
            "agent.mf"
            "-C" "classes"
            "."))
        (.getPath (cji/file out-dir jar-name))))))

(comment

  (build-agent-jar
    (.getPath (cji/file (System/getenv "HOME")
                "src" "alc.start-repl"))
    (System/getProperty "java.io.tmpdir")
    "start-socket-repl-agent.jar")

  (let [src-dir
        (.getPath (cji/file (System/getProperty "HOME")
                    "src" "alc.start-repl"))
        out-dir src-dir]
    (build-agent-jar src-dir
      out-dir "start-socket-repl-agent.jar"))

  )

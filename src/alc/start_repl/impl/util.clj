(ns alc.start-repl.impl.util
  (:require
   [clojure.java.io :as cji]
   [clojure.string :as cs]))

(set! *warn-on-reflection* true)

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

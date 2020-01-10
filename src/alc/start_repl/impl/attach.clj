(ns alc.start-repl.impl.attach
  (:require
   [clojure.java.io :as cji]))

(defn ensure-attach-cp
  []
  ;; XXX: to arrange for things to also work on java <= 8
  (let [jvm-spec-ver (System/getProperty "java.vm.specification.version")]
    (when (< (Double. jvm-spec-ver) 2)
      (let [tools-jar (clojure.java.io/file (System/getProperty "java.home")
                        ".." "lib" "tools.jar")
            add-jar-path
            (fn [^java.io.File jar-path]
              (let [url (-> jar-path .toURI .toURL)
                    loader (ClassLoader/getSystemClassLoader)
                    meth (.getDeclaredMethod java.net.URLClassLoader
                           "addURL" (into-array Class [java.net.URL]))]
                (.setAccessible meth true)
                (.invoke meth loader (into-array [url]))))]
        (when (.exists tools-jar)
          (add-jar-path tools-jar))))))

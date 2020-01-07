(ns alc.start-repl.impl.parse)

(set! *warn-on-reflection* true)

;; example values:
;;
;; "{\\:port,3579,\\:accept,clojure.core.server/repl}"
;; "{\\:port 8235 \\:accept clojure.core.server/repl}" ; leiningen
(defn parse-clojure-server-str
  [map-str]
  (let [[_ _ port] (re-find #":port(,| )(\d+)" map-str)
        [_ _ flavor] (re-find #":accept(,| )([^, }]+)" map-str)]
    [port flavor]))

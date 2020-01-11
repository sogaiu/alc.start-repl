(ns alc.start-repl.impl.net
  (:import
   [java.net InetAddress ServerSocket UnknownHostException]))

(set! *warn-on-reflection* true)

(defn check-port
  [port]
  (when-let [addr
             (try
               ;; XXX: possibly want to allow address specification
               (InetAddress/getByAddress (bytes (byte-array [127 0 0 1])))
               (catch UnknownHostException _
                 ;; XXX
                 (println "failed to determine localhost address")
                 nil))]
    (when-let [^ServerSocket sock
               (try
                 (ServerSocket. port 0 addr)
                 (catch java.io.IOException _
                   ;; XXX
                   (println "failed to create socket for:" port)
                   nil))]
      (let [received-port (.getLocalPort sock)]
        (.close sock)
        received-port))))

(defn find-port
  []
  (check-port 0))

(comment

  (find-port)

  (check-port 53)

  (check-port 2357)

  (check-port 9000)

  (check-port 9001)

  )

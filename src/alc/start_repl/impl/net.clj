(ns alc.start-repl.impl.net
  (:import
   [java.net InetAddress ServerSocket UnknownHostException]))

(set! *warn-on-reflection* true)

(defn get-and-close-port
  [^ServerSocket sock]
  (let [port (.getLocalPort sock)]
    (.close sock)
    port))

(defn check-port
  [port]
  (let [addr (try
               (InetAddress/getLocalHost)
               (catch UnknownHostException _
                 ;; XXX
                 (println "failed to determine localhost address")
                 nil))]
    (when addr
      (when-let [sock (try
                        (ServerSocket. port 0 addr)
                        (catch java.io.IOException _
                          ;; XXX
                          (println "failed to listen on localhost port:" port)
                          nil))]
        (get-and-close-port sock)))))

(defn find-port
  []
  (when-let [addr (try
                    (InetAddress/getLocalHost)
                    (catch UnknownHostException _
                      ;; XXX
                      (println "failed to determine localhost address")
                      nil))]
    (when-let [sock (try
                      (ServerSocket. 0 0 addr)
                      (catch java.io.IOException _
                        ;; XXX
                        (println "failed to listen on localhost address")
                        nil))]
      (get-and-close-port sock))))

(comment

  (find-port)

  (check-port 53)

  (check-port 9000)

  )

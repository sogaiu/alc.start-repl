(ns alc.start-repl.agent
  (:require
    [clojure.core.server :as ccs]
    [clojure.main :as cm])
  (:import
    [clojure.lang LineNumberingPushbackReader LispReader$ReaderException])
  ;; XXX: important for getting VirtualMachine/loadAgent to work?
  (:gen-class
   :methods [^:static [agentmain [String] void]]))

;; TODO:
;;
;; - consider implications of hard-wiring an ipv4 address (127.0.0.1)

;; in java >= 9, (.getContextClassLoader (Thread/currentThread)) returns nil
;; when we need it to be otherwise; that messes up clojure.main/repl
;;
;; so the following modification to clojure.main/repl uses
;; (ClassLoader/getSystemclassloader) instead
;;
;; adapted from clojure source code, EPL 1 applies
(defn cm-ish-repl
  "Generic, reusable, read-eval-print loop. By default, reads from *in*,
  writes to *out*, and prints exception summaries to *err*. If you use the
  default :read hook, *in* must either be an instance of
  LineNumberingPushbackReader or duplicate its behavior of both supporting
  .unread and collapsing CR, LF, and CRLF into a single \\newline. Options
  are sequential keyword-value pairs. Available options and their defaults:

     - :init, function of no arguments, initialization hook called with
       bindings for set!-able vars in place.
       default: #()

     - :need-prompt, function of no arguments, called before each
       read-eval-print except the first, the user will be prompted if it
       returns true.
       default: (if (instance? LineNumberingPushbackReader *in*)
                  #(.atLineStart *in*)
                  #(identity true))

     - :prompt, function of no arguments, prompts for more input.
       default: repl-prompt

     - :flush, function of no arguments, flushes output
       default: flush

     - :read, function of two arguments, reads from *in*:
         - returns its first argument to request a fresh prompt
           - depending on need-prompt, this may cause the repl to prompt
             before reading again
         - returns its second argument to request an exit from the repl
         - else returns the next object read from the input stream
       default: repl-read

     - :eval, function of one argument, returns the evaluation of its
       argument
       default: eval

     - :print, function of one argument, prints its argument to the output
       default: prn

     - :caught, function of one argument, a throwable, called when
       read, eval, or print throws an exception or error
       default: repl-caught"
  [& options]
  (let [cl (ClassLoader/getSystemClassLoader)]
    (.setContextClassLoader (Thread/currentThread) 
      (clojure.lang.DynamicClassLoader. cl)))
  (let [{:keys [init need-prompt prompt flush read eval print caught]
         :or {init        #()
              need-prompt (if (instance? LineNumberingPushbackReader *in*)
                            #(.atLineStart ^LineNumberingPushbackReader *in*)
                            #(identity true))
              prompt      cm/repl-prompt
              flush       flush
              read        cm/repl-read
              eval        eval
              print       prn
              caught      cm/repl-caught}}
        (apply hash-map options)
        request-prompt (Object.)
        request-exit (Object.)
        read-eval-print
        (fn []
          (try
            (let [read-eval *read-eval*
                  input (try
                          (cm/with-read-known (read request-prompt
                                                request-exit))
                          (catch LispReader$ReaderException e
                            (throw (ex-info nil 
                                     {:clojure.error/phase
                                      :read-source} e))))]
             (or (#{request-prompt request-exit} input)
                 (let [value (binding [*read-eval* read-eval] (eval input))]
                   (set! *3 *2)
                   (set! *2 *1)
                   (set! *1 value)
                   (try
                     (print value)
                     (catch Throwable e
                       (throw (ex-info nil
                                {:clojure.error/phase
                                 :print-eval-result} e)))))))
           (catch Throwable e
             (caught e)
             (set! *e e))))]
    (cm/with-bindings
     (try
      (init)
      (catch Throwable e
        (caught e)
        (set! *e e)))
     (prompt)
     (flush)
     (loop []
       (when-not 
       	 (try (identical? (read-eval-print) request-exit)
	  (catch Throwable e
	   (caught e)
	   (set! *e e)
	   nil))
         (when (need-prompt)
           (prompt)
           (flush))
         (recur))))))

(defn repl
  []
  (cm-ish-repl
    :init ccs/repl-init
    :read ccs/repl-read))

(defn -agentmain [^String args]
  (println "Loading agentmain")
  (let [port (or (try
                   (Integer. args)
                   (catch Exception _
                     nil))
               7659)
        accept-name "alc.start-repl.agent/repl"
        repl-name (str "alcsr-" port)
        prop-name (str "alc.start-repl." repl-name)
        sock (try
               (ccs/start-server
                 {:address "127.0.0.1" ; XXX: hard-wiring ipv4 ok?
                  :name repl-name
                  :port port
                  :accept (symbol accept-name)})
               (catch Exception e
                 (println "Problem starting socket repl server")
                 (println (.getMessage e))))]
    (when sock
      (let [prop-val (str "{"
                       ":port " port " "
                       ":accept " accept-name
                       "}")]
        (println "setting system property")
        (println "prop-name:" prop-name)
        (println "prop-val:" prop-val)
        (System/setProperty prop-name prop-val)))
    (println "Finishing agentmain")
    nil))

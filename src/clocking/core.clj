(ns clocking.core
  (:require [clojure.java.io :as io]
            [clojure.core.async :as async]
            [clojure.string :as string]

            [digest :as d]
            [clj-pgp.core :as pgp]
            [clj-pgp.generate :as pgp-gen]
            [clj-pgp.signature :as pgp-sig]

            [clj-sockets.core :as sock]

            [taoensso.tufte :as tufte :refer [p profiled profile]]))

;;;;; Dummy Data
(defn random-string [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 102))))))

(defn fresh-keypair []
  (pgp-gen/generate-keypair (pgp-gen/ec-keypair-generator "secp160r2") :ecdsa))

(defn get-free-port []
  (let [socket (java.net.ServerSocket. 0)]
    (.close socket)
    (.getLocalPort socket)))

;;;;; Basic profiling
(tufte/add-basic-println-handler! {})
(defn sha256 [inp] (p :sha256 (d/sha-256 inp)))
(defn ecdsa [keypair inp]
  (let [sig (p :ecdsa-sign (pgp-sig/sign inp keypair))]
    (p :ecdsa-verify (pgp-sig/verify inp sig keypair))))

(defn tcp-send [port inp]
  (p :tcp-send
     (let [s (sock/create-socket "localhost" port)]
       (sock/write-to s (str inp "\n")))))

(defn tcp-receive [serv]
  (p :tcp-receive (sock/read-line serv))
  serv)

;;;;; Battery status
(defn list-batteries [& {:keys [bat-dir] :or {bat-dir "/sys/class/power_supply/"}}]
  (->> (io/file bat-dir)
       .listFiles
       (map #(.getName %))
       (filter #(string/starts-with? % "BAT"))
       (map #(str bat-dir %))))
(defn bslurp [bat fname & {:keys [transform] :or {transform identity}}]
  (transform (slurp (str bat "/" fname))))
(defn charging-status [bat]
  (bslurp bat "status" :transform #(->> % string/trim string/lower-case keyword)))
(defn battery-status []
  (->> (list-batteries)
       (map (fn [b]
              (let [bslurp #(bslurp b % :transform clojure.edn/read-string)]
                [b {:energy {:now (bslurp "energy_now") :full (bslurp "energy_full") :design (bslurp "energy_full_design")}
                    :power {:now (bslurp "power_now")}
                    :voltage {:now (bslurp "voltage_now") :min (bslurp "voltage_min_design")}
                    :capacity (bslurp "capacity") :status (charging-status b)}])))
       (into {})))

;;;;; Profiling
(defn profile-tcp [serv port inp]
  (let [t (async/thread
            (->> serv
                 sock/listen
                 tcp-receive
                 sock/close-socket))]
    (tcp-send port inp)))

(defn profile! [ct]
  (let [inp (random-string 1000)
        keypair (fresh-keypair)]
    (let [before (battery-status)
          free-port (get-free-port)
          server (sock/create-server free-port)]
      (profile
       {} (dotimes [_ ct]
            (profile-tcp server free-port inp)
            (sha256 inp)
            (ecdsa keypair inp)))
      {:before before :after (battery-status)})))


(defn tap! [label thing]
  (println label)
  thing)

;; (def t (async/thread (->> (sock/create-server 9871) sock/listen (#(do (println (sock/read-line %)) %)) sock/close-socket)))
;; (def s (sock/create-socket "localhost" 9871))
;; (sock/write-to s "This is a line\n")

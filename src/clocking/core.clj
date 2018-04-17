(ns clocking.core
  (:require [clojure.java.io :as io]
            [digest :as d]
            [clj-pgp.core :as pgp]
            [clj-pgp.generate :as pgp-gen]
            [clj-pgp.signature :as pgp-sig]

            [taoensso.tufte :as tufte :refer [p profiled profile]]))

;;;;; Dummy Data
(defn random-string [len]
  (apply str (take len (repeatedly #(char (+ (rand 26) 102))))))

(defn fresh-keypair []
  (pgp-gen/generate-keypair (pgp-gen/ec-keypair-generator "secp160r2") :ecdsa))

;;;;; Basic profiling
(tufte/add-basic-println-handler! {})
(defn sha256 [inp] (p :sha256 (d/sha-256 inp)))
(defn ecdsa [keypair inp]
  (let [sig (p :ecdsa-sign (pgp-sig/sign inp keypair))]
    (p :ecdsa-verify (pgp-sig/verify inp sig keypair))))

;;;;; Battery status
(def bat "/sys/class/power_supply/BAT0/")
(defn bslurp [name] (clojure.edn/read-string (slurp (str bat name))))
(defn charging-status []
  (keyword (clojure.string/lower-case (clojure.string/trim (slurp (str bat "status"))))))
(defn battery-status []
  {:charge {:now (bslurp "charge_now") :full (bslurp "charge_full") :design (bslurp "charge_full_design")}
   :current {:now (bslurp "current_now")}
   :voltage {:now (bslurp "voltage_now") :min (bslurp "voltage_min_design")}
   :capacity (bslurp "capacity") :status (charging-status)})

;;;;; Profiling
(defn profile! [ct]
  (let [inp (random-string 1000)
        keypair (fresh-keypair)]
    (let [before (battery-status)]
      (profile
       {} (dotimes [_ ct]
            (sha256 inp)
            (ecdsa keypair inp)))
      {:before before :after (battery-status)})))

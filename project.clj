(defproject clocking "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]

                 [digest "1.4.8"]
                 [mvxcvi/clj-pgp "0.9.0"]
                 [clj-sockets "0.1.0"]
                 [com.taoensso/tufte "1.4.0"]]
  :main clocking.core
  :aot [clocking.core])

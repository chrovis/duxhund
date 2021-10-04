(defproject com.chrovis/duxhund "0.1.0-SNAPSHOT"
  :description "DUX4 fusions finder"
  :url "https://github.com/chrovis/duxhund"
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :dependencies [[org.clojure/clojure "1.10.3"]
                 [clj-sub-command "0.6.0"]
                 [cljam "0.8.2"]]
  :repl-options {:init-ns duxhund.core}
  :main duxhund.cli
  :profiles {:dev
             {:global-vars {*warn-on-reflection* true}}
             :uberjar
             {:aot :all
              :uberjar-name "duxhund.jar"}})

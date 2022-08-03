(defproject com.chrovis/duxhund "0.1.2"
  :description "DUX4 fusions finder"
  :url "https://github.com/chrovis/duxhund"
  :license {:name "GPL-3.0-or-later"
            :url "https://www.gnu.org/licenses/gpl-3.0.html"}
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [clj-sub-command "0.6.0"]
                 [cljam "0.8.3"]]
  :repl-options {:init-ns duxhund.core}
  :main duxhund.cli
  :profiles {:dev
             {:global-vars {*warn-on-reflection* true}}
             :uberjar
             {:aot :all
              :uberjar-name "duxhund.jar"}})

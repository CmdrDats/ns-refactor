(defproject ns-refactor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.4.0"]
                 [midje "1.4.0"]
                 [swank-clojure/swank-clojure "1.4.2"]
                 [org.clojure/tools.logging "0.2.3"]
                 [ns-reloader "0.1.0-SNAPSHOT"]]
  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public/"}
  :main ns-refactor.core)

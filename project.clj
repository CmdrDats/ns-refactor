(defproject ns-refactor "0.1.0-SNAPSHOT"
  :description "FIXME: write description"
  :url "http://example.com/FIXME"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[midje "1.4.0" :exclusions [org.clojure/clojure]]
                 [swank-clojure/swank-clojure "1.4.2" :exclusions [org.clojure/clojure]]
                 [org.clojure/tools.logging "0.2.3" :exclusions [org.clojure/clojure] ]
                 [ns-reloader "0.1.0-SNAPSHOT" :exclusions [org.clojure/clojure]]
                 [noir "1.2.2" :exclusions [org.clojure/clojure org.clojure/tools.namespace]]
                 [enlive "1.0.1"]
                 [dorothy "0.0.3" :exclusions [org.clojure/clojure]]]
  :repositories {"sonatype-oss-public"
                 "https://oss.sonatype.org/content/groups/public/"}
  :main ns-refactor.core)

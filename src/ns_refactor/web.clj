(ns ns-refactor.web
  (:use clojure.tools.logging)
  (:use noir.core)
  (:use hiccup.core)
  (:use hiccup.page-helpers)
  (:require [net.cgrand.enlive-html :as html])
  (:require [noir.server :as server])
  (:require [noir.session :as session])
  (:require [noir.response :as response])
  (:require [dorothy.core :as graph])
  (:require [ns-refactor.reader :as reader])
  )

(defn start [port]
  (server/start port))

(defn fixname
  [nm]
  (.replaceAll nm "[^A-Za-z0-9./-]" "" ))

(defn collapse-data
  [structures]
  (for [struct structures
        form (:forms struct)
        [n fset] (:fns (:deps form))
        fnm fset
        :let
        [nsm (:ns struct)
         req-map (or (:require (:deps nsm)) {})]]
    [(fixname (str (:form nsm) "/" (:form form)))
     (fixname (str (req-map n) "/" (.getName fnm)))]))

(defn subgraph-locals
  [struct]
  (graph/subgraph
   (str "cluster_" (fixname (.getName (:file struct))))
   (concat
    [{:color :blue, :label (.getName (:file struct)) }
     (graph/node-attrs {:style :filled})]
    (for [form (:forms struct)
          fnm  (:local-tlf (:deps form))
          :let
          [nsm (:ns struct)]]
      [(fixname (str (:form nsm) "/" (:form form)))
       (fixname (str (:form nsm) "/" (.getName fnm)))])
    (for [form (:forms struct)]
      [(fixname (str (:form (:ns struct)) "/" (:form form)))])
    
    )))

(defpage "/graph" []
  (let [prj (reader/read-folder ["src"])
        data
        (concat
          (for [struct (:structures prj)]
            (subgraph-locals struct))
         (collapse-data (:structures prj))
         )
        graph (-> (graph/digraph "Structure " (concat [{:rankdir "LR"}] data)) graph/dot (graph/render {:format :svg}))
                   ]
    (response/content-type "image/svg+xml" graph)))




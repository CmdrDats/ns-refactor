(ns ns-refactor.core
  (:use [midje.sweet])
  (:use [clojure.tools.logging])
  (:require [swank.swank])
  (:require [ns-refactor.web :as web])
  (:require [ns-reloader.core :as tracker])
  (:require [ns-reloader.scaffold :as scaf]))

(defn move-item
  "Move an indexed item in a vector to another position."
  [vec fromidx toidx]
  (if (= fromidx toidx)
    vec
    (let [item (nth vec fromidx)
          left (subvec vec 0 fromidx)
          right (subvec vec (inc fromidx))]
      (if (< toidx fromidx)
        (concat (subvec left 0 toidx) [item] (subvec left toidx) right) ; Insert item in left part
        (let [modidx (- toidx fromidx)]
          (concat left (subvec right 0 modidx) [item] (subvec right modidx)))))))
(fact (move-item [1 2 3 4] 3 0) => [4 1 2 3])
(fact (move-item [1 2 3 4] 0 3) => [2 3 4 1])
(fact (move-item [1 2 3 4] 2 2) => [1 2 3 4])


(defn -main
  [& args]
  (swank.swank/start-repl 4005 :host "0.0.0.0")
  ;(scaf/start-tracker-with-scaffold ["src" "checkouts"] 2500 #(info "Reloading..."))
  (web/start 8080)
  )












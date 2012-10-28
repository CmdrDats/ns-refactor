(ns ns-refactor.reader
  "All the file reading and writing functions"
  (:use [midje.sweet])
  (:require [clojure.set :as set])
  (:require [clojure.java.io :as io])
  (:import (java.io PushbackReader))
  (:import [clojure.lang LineNumberingPushbackReader]))

(defn get-deps-from-nsform
  [form]
  (let [deps (filter #(and (list? %) (contains? #{:require :use :import} (first %))) form)]
    deps))

(defn find-form-deps
  "Find symbol dependencies from a given form"
  {:tests [(fact (find-form-deps '(this (is a cmp/test))) => #{'this 'is 'a 'cmp/test})]}
  [form]
  (loop [current form
         more []
         result #{}]
    (let [symbols (filter symbol? current)
          lists (filter coll? current)
          appended (set/union result (set symbols))
          extra-lists (concat more lists)]
      (cond
       (not-empty extra-lists) (recur (first extra-lists) (rest extra-lists) appended)
       :else appended))))

(defn split-dependencies
  "Runs through a forms dependencies and splits out java methods, possible java constructor, clojure.core and namespace calls."
  {:tests [(fact (split-dependencies 'ns-refactor.example '(map #(stk/call) (^JarEntry get-jar-entry) File. .isFile "Random string"  12 1M)) => {:methods #{'.isFile}, :classes #{'File. 'JarEntry}, :core #{'map}, :fns {"stk" #{'stk/call}}, :local #{'get-jar-entry 'fn*}})]}
  [n deps]
  (let [flattened (filter symbol? (flatten deps))]
    (loop [entry (first (flatten flattened))
           more (rest (flatten flattened))
           methods #{}
           classes #{}
           fns {}
           local #{}]
      (if (nil? entry)
        ;; Finish recursion
        {:methods methods :classes classes :core (set (fns 'clojure.core)) :fns  (dissoc fns 'clojure.core) :local local}
        (let [ename (name entry)
              resolved (resolve entry)]
          (cond
           ;; If we find a tag, check them individually
           (get (meta entry) :tag nil)
           (recur (with-meta entry {}) (conj more (:tag (meta entry))) methods classes fns local)
           ;; If it could not be resolved, check java method or class
           (and resolved (instance? clojure.lang.Var resolved) (.startsWith (name (.getName (.ns resolved))) "clojure."))
           (recur (first more) (rest more) methods classes (update-in fns [(.getName (.ns resolved))] #(if %1 (conj %1 %2) #{%2}) entry) local)
           ;; Method
           (.startsWith ename ".")
           (recur (first more) (rest more) (conj methods entry) classes fns local)
           ;; Class
           (.endsWith ename ".")
           (recur (first more) (rest more) methods (conj classes entry) fns local)
           (Character/isUpperCase (first (name entry)))
           (recur (first more) (rest more) methods (conj classes entry) fns local)
           ;; Namespaced
           (.getNamespace entry)
           (recur (first more) (rest more) methods classes (update-in fns [(.getNamespace entry)] #(if %1 (conj %1 %2) #{%2}) entry) local)
           ;; Local
           :else
           (recur (first more) (rest more) methods classes fns (conj local entry))))
        ))))

(defn cts
  [contents start & [end]]
  (apply str (interpose (System/getProperty "line.separator")
                        (if end
                          (subvec contents start end)
                          (subvec contents start))))
  )

(defn parse-raw-structure
  "Parse a file and return the dependency map for it"
  [rdr contents]
  (loop [idx 0
         ns nil
         structure {}]
    (let [form (try (read rdr) (catch Exception e nil))
          line (.getLineNumber rdr)]
      (cond
       (nil? form) (assoc structure :tail (cts contents idx nil))
       (and (list? form) (= 'ns (first form)))
       (recur line (second form) (assoc structure :ns {:start idx :end line :form (second form) :deps (get-deps-from-nsform form) :contents (cts contents idx line)} :meta (meta (second form))))
       (and (list? form))
       (recur line ns (assoc structure :forms (conj (get structure :forms []) {:start idx :end line :form (second form) :deps (split-dependencies ns (rest (rest form))) :meta (meta (second form)) :contents (cts contents idx line)})))
       :else
       ;; We want to catch anything else and include it as part of the
       ;; output.
       (recur idx ns structure)))))

(defn top-level-forms
  "Given a parsed file structure, return a set of top level form names in this file"
  [structure]
  (set (map :form (:forms structure))))

(defn split-tlf
  "Takes a set of top level function names and a parsed form and pulls the top level functions out into :local-tlf"
  [tlf form]
  (let [local (get-in form [:deps :local])
        local-tlf (set/intersection tlf local)]
    (-> form
        (assoc-in [:deps :local-tlf] local-tlf)
        (assoc-in [:deps :local] (set/difference local local-tlf)))))

(defn split-top-level-forms
  "Given a parsed file structure, pull the top level forms out of the :local's"
  [structure]
  (let [tlf (top-level-forms structure)]
    (assoc structure :forms (map (partial split-tlf tlf) (:forms structure)))))

(defn slurp-file
  "Pull a file into a sequence of strings"
  [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (reduce conj [] (line-seq rdr))))

(defn read-file
  [filename]
  (let [file (java.io.File. filename)
        contents (slurp-file file)]
    (with-open [rdr (LineNumberingPushbackReader. (io/reader file))]
      (split-top-level-forms (parse-raw-structure rdr contents)))))

(defn write-file
  [filename structure]
  (spit filename (str (apply str (interpose (System/getProperty "line.separator") (concat [(get-in structure [:ns :contents])] (map :contents (:forms structure)) [(:tail structure)]))))))

;; Comment at the end of the file.

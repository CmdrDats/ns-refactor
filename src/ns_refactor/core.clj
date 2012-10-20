(ns ns-refactor.core
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
  {:tests [(fact (split-dependencies 'ns-refactor.core '(map (^JarEntry get-deps-from-nsform) File. .isFile)) => {:methods #{'.isFile} :classes #{'JarEntry 'File.} :core #{'map} :fns {'ns-refactor.core #{'get-deps-from-nsform}} :unknown #{}})]}
  [n deps]
  (let [flattened (filter symbol? (flatten deps))]
    (loop [entry (first (flatten flattened))
           more (rest (flatten flattened))
           methods #{}
           classes #{}
           fns {}
           unknown #{}]
      (if (nil? entry)
        ;; Finish recursion
        {:methods methods :classes classes :core (set (fns 'clojure.core)) :fns  (dissoc fns 'clojure.core) :unknown unknown}
        (let [resolved (resolve n entry)]
          (cond
           ;; If we find a tag, check them individually
           (get (meta entry) :tag nil)
           (recur (with-meta entry {}) (conj more (:tag (meta entry))) methods classes fns unknown)
           ;; If it could not be resolved, check java method or class
           (nil? resolved)
           (cond
            ;; Method
            (.startsWith (name entry) ".")
            (recur (first more) (rest more) (conj methods entry) classes fns unknown)
            ;; Class
            (.endsWith (name entry) ".")
            (recur (first more) (rest more) methods (conj classes entry) fns unknown)
            (Character/isUpperCase (first (name entry)))
            (recur (first more) (rest more) methods (conj classes entry) fns unknown)
            ;; Ignore
            :else
            (recur (first more) (rest more) methods classes fns (conj unknown entry)))
           (class? resolved)
           (recur (first more) (rest more) methods (conj classes entry) fns unknown)
           :else
           (recur (first more) (rest more) methods classes (update-in fns [(.getName (.ns resolved))] #(if %1 (conj %1 %2) #{%2}) entry) unknown)))
        )))
  )

(defn parse-file
  "Parse a file and return the dependency map for it"
  [rdr]
  (loop [idx 0
         ns nil
         structure {}]
    (let [form (try (read rdr) (catch Exception e nil))
          line (.getLineNumber rdr)]
      (cond
       (nil? form) structure
       (and (list? form) (= 'ns (first form)))
       (recur (inc line) (second form) (assoc structure :ns {:start idx :end line :form (second form) :deps (get-deps-from-nsform form)}))
       (and (list? form))
       (recur (inc line) ns (assoc structure :forms (conj (get structure :forms []) {:start idx :end line :form (second form) :deps (split-dependencies ns (rest (rest form)))})))
       :else
       (recur (inc line) ns structure)))))

(defn slurp-file
  "Pull a file into a sequence of strings"
  [file]
  (with-open [rdr (clojure.java.io/reader file)]
    (reduce conj [] (line-seq rdr))))

(defn move-item
  "Move an indexed item in a vector to another position."
  {:tests [(fact (move-item [1 2 3 4] 3 0) => [4 1 2 3])
           (fact (move-item [1 2 3 4] 0 3) => [2 3 4 1])
           (fact (move-item [1 2 3 4] 2 2) => [1 2 3 4])]}
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

(defn read-file
  [filename]
  (let [file (java.io.File. filename)
        contents (slurp-file file)]
    (with-open [rdr (LineNumberingPushbackReader. (io/reader file))]
      (parse-file rdr))))




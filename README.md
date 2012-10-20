# ns-refactor

ns-refactor is an app designed to quickly refactor and re-work your clojure projects.

A couple of broad goals:

 - Be able to move functions from one namespace to another and have the clojure import declarations automatically updated where needed.
 - Rename functions and have the dependency code automatically updated.
 - Automatically clean up unused :import, :use or :require's in the namespace declarations
 - Automatically include new namespace :require's into your ns declarations
 - Automatically create new clojure files with namespace declarations if you simply add a :require to it
 - On request, create new function definitions by referring to them.
 - Warn against unconventional namespace :require shortnames
 - On request, re-order functions in a namespace by function dependencies.
 - Graph function level dependencies so that you can have a better overview of what functions belong where, or if you need to carve new namespaces.
 - Mark a function as a published function, so that ns-refactor will refuse to do automatic refactoring to it and warn you if you change the function definition.
 - Learns your preferred namespace :require :as conventions so that it knows what dependencies to add.
 - It is editor-agnostic. It only relies on the fact that you have clj source code.
 - For automatic dependency management tasks, it assumes you are using lein, as it will look for a project.clj

## Notice

This project is still heavily under construction. The readme is built first to provide a clear outline of the goals for ns-refactor.

## Installation

Simply checkout and `lein run [path]` ns-refactor:

```
git clone https://github.com/CmdrDats/ns-refactor.git
cd ns-refactor
lein run [paths-to-your-projects]
```

You can provide multiple paths for ns-refactor to monitor to handle multiple projects at once.

This will fire up ns-refactor, monitoring the project you have specified. Alternatively, you can add `[ns-refactor "1.0.0-SNAPSHOT"]` to your project.clj and fire it off using:

```clojure
(ns-refactor.inline/start)
```

## Usage

### Moving functions between namespaces

If you have this :

```clojure
(ns myapp.core
  (:require [myapp.util :as util]))

(defn add [x y]
  (+ x y))

(defn use-add [n]
  (add n n))
```

And you'd really prefer to have your `add` function in the myapp.util namespace, you can add a metadata hint to it:

```clojure
(ns myapp.core
  (:require [myapp.util :as util]))

(defn #{:refactor/moveto 'myapp.util} add [x y]
  (+ x y))

(defn use-add [n]
  (add n n))
```

This will move the function to there and do a direct dependency search, include :require's in ns declarations that need it and change the calling functions to call it, so you'll end up with :

```clojure
(ns myapp.core
  (:require [myapp.util :as util]))

(defn use-add [n]
  (util/add n n))
```

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

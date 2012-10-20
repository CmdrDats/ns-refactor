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
 - Rename :require shortname within a namespace
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

And you'd really prefer to have your `add` function in the myapp.util namespace, you can add a metadata hint, `#{:refactor/move 'myapp.util}` to it:

```clojure
(ns myapp.core
  (:require [myapp.util :as util]))

(defn #{:refactor/move 'myapp.util} add [x y]
  (+ x y))

(defn use-add [n]
  (add n n))
```

This will move the function to there and do a direct dependency search, include :require's in ns declarations that need it and change the calling functions to call it, so you'll end up with :

```clojure
(ns myapp.util)

(defn add [x y]
  (+ x y))
```

```clojure
(ns myapp.core
  (:require [myapp.util :as util]))

(defn use-add [n]
  (util/add n n))
```

Notice that the metadata :refactor/move is removed in the process.

### Rename functions

Again, if you have:

```clojure
(defn add [x y]
  (+ x y))

(defn use-add [n]
  (add n n))
```

You can add a metadata hint `#{:refactor/rename 'adding}` and to the add function, like so :

```clojure
(defn #{:refactor/rename 'adding} add [x y]
  (+ x y))

(defn use-add [n]
  (add n n))
```

And you'll end up with:

```clojure
(defn adding [x y]
  (+ x y))

(defn use-add [n]
  (adding n n))
```

Of course, this is a trivial example, but the point is that it will work with references across namespaces and, if you've setup multiple projects, across projects.

### Automatically clean up :import, :use or :require's in the namespace declarations

This will happen automatically unless you include `#^{:refactor/opts #{:preserve}}` on your namespace or `:use`, `:require` or `:import` declaration.

### Automatically include new namespaces into your ns declaration

If you have:

```clojure
(ns myapp.util)

(defn add [x y]
  (+ x y))
```

```clojure
(ns myapp.core)

(defn use-add [n]
  (util/add n n))
```

ns-refactor will automatically adjust your namespace declaration to:

```clojure
(ns myapp.core
  (:require [myapp.util :as util]))
```

based on the fact that util has probably previously been used to refer to myapp.util or it's the only match for the function in your classpath. If there is ambiguity, it will leave things unchanged.

### Create new namespaces

If you :require a namespace that doesn't exist, ns-refactor will create the clj file in your source folder with the ns declaration in it so that:

```clojure
(ns myapp.core
  (:require [myapp.util :as util]))
```

will create the src/myapp/util.clj file with contents :

```clojure
(ns myapp.util)
```

Of course, the automatic removal of unused namespaces will, in time delete your :require, but you can continue and just refer to it as if it was there and it will get added when your first function call that needs it is there.


### On request, create new function defs

```clojure
(ns myapp.core)

(defn #^{refactor/create-fns} use-add [n]
  (util/add-fn n n))
```

This will create the `add-fn` fn in `myapp.util` - creating the namespace if it needs to:

```clojure
(ns myapp.util)

(defn add-fn [x y])
```

If you have multiple calls to `use-add` within a marked function:

```clojure
(ns myapp.core)

(defn #^{refactor/create-fns} use-add [n]
  (- (util/add-func n n n) (util/add-func n n))
```

It will try create the `add-fn` with a best fit for all the calls:

```clojure
(ns myapp.util)

(defn add-fn [x y & [z]])
```

Similarly calling `add-fn` with keyword pairs, ala `(add-fn n :id 4 :name "john")` will attempt to destructure it:

```clojure
(ns myapp.util)

(defn add-fn [x & {:keys [id name] :as y}])
```

### Warn against unconventional namespace shortnames

When you use `(:require [myapp.util :as util])` in one part of your project, and then later `(:require [myapp.util :as utils])` in a different namespace, it will prepend the :require with a `#^{:refactor/different-shortname 'util}`

### Rename :require shortname

If you add `#^{:refactor/rename-short 'utl}` to a :require it will change it throughout your project, aborting if it would clash with another namespace.

Example:

```clojure
(ns myapp.core
  #^{:refactor/rename-short 'myutl} (:require [myapp.util :as util]))
  
(defn use-add [n]
  (util/add-fn n n))
```

Will result in :

```clojure
(ns myapp.core
  (:require [myapp.util :as myutl]))
  
(defn  use-add [n]
  (myutl/add-fn n n))
```

### Re-order functions by dependencies

### Graph function level dependencies

### Mark/protect published functions

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

# ns-refactor

ns-refactor is an app designed to quickly refactor and re-work your clojure projects.

A couple of broad goals:

 - Be able to move functions from one namespace to another and have the clojure import declarations automatically updated where needed.
 - Automatically clean up unused :import, :use or :require's in the namespace declarations
 - Automatically include new namespace :require's into your ns declarations
 - Automatically create new clojure files with namespace declarations if you simply add a :require to it
 - On request, create new function definitions by referring to them.
 - Warn against unconventional namespace :require shortnames
 - On request, re-order functions in a namespace by function dependencies.
 - Graph function level dependencies so that you can have a better overview of what functions belong where, or if you need to carve new namespaces.
 - It is editor-agnostic. It only relies on the fact that you have clj source code.
 - For automatic dependency management tasks, it assumes you are using lein, as it will look for a project.clj
 
## Usage

Simply checkout ns-refactor

## License

Copyright © 2012 FIXME

Distributed under the Eclipse Public License, the same as Clojure.

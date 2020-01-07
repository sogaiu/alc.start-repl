# alc start-repl

## Purpose

Start a socket repl for a running Clojure process, from the command line.

## Prerequisites

* Some JDK (using >= 8 here)
* clj / clojure

## Use Cases

* Accidentally evaluated long-running or infinite computation, but no running networked REPL.  Don't give up -- get access to what is going on in a running Clojure process via a newly started socket repl.

* Trying out a project that provides nrepl access but no socket repl.  Avoid initial study and early modification of a project.clj file just to add a socket repl.

* Have multiple Clojure projects and working on more than one at once happens eventually.  Avoid hard-wiring tcp port numbers into deps.edn that may 1) conflict and 2) be tedious to lookup.

## Quick Trial

Start a socket REPL on port 7650:

```
$ cd some-clojure-project-dir
$ # arrange for project to be running, e.g. via clj or lein
$ clj -Sdeps '{:deps {alc.start-repl {:git/url "https://github.com/sogaiu/alc.start-repl" :sha "a8439821794183041fc07d11be647610d448b88b"}}}' -m alc.start-repl.main '{:port 7650}'
```

## Usage

Edit the `:aliases` section of `~/.clojure/deps.edn` to contain:

```
   :alc.start-repl
   {
    :extra-deps {sogaiu/alc.start-repl
                 {:git/url "https://github.com/sogaiu/alc.start-repl"
                  :sha "a8439821794183041fc07d11be647610d448b88b"
    :main-opts ["-m" "alc.start-repl.main"]
   }
```

Assuming a running process for a Clojure project, start a socket REPL on port 8888:

```
$ cd /home/alice/a-clj-proj-dir
$ clj -A:alc.start-repl '{:port 8888}'
```

-OR-

```
$ clj -A:alc.start-repl '{:port 8888 :proj-dir "/home/alice/a-clj-proj-dir"}'
```

## Technical Details

In short, loading socket-repl-starting JVM bytecode into a running Clojure-running JVM process.

Specifically, via the loadAgent method of the VirtualMachine class in the JDK's attach API to load an appropriately prepared jar file.

## References

* Pure Clojure implementation of a Java agent by dgopstein.  He generously shared his implementation and provided a helpful explanation:

  <https://dgopstein.github.io/articles/clojure-javaagent>

* A more general tool that works with JVM processes that don't necessarily have a Clojure inside:

  <https://github.com/djpowell/liverepl>

  However, it doesn't appear to work with Java >= 9.  (FWIW, looks like it can be fixed by tweaking some class loader related things.)

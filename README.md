# alc start-repl

## Purpose

Start a socket repl for a running Clojure process, from the command line.

## Prerequisites

* Some JDK (using >= 8 here)
* clj / clojure

## Use Cases

* Having multiple Clojure projects and working on more than one at once happens eventually.  Avoid hard-wiring tcp port numbers into deps.edn that may 1) conflict and 2) be tedious to lookup.

* Accidentally evaluated long-running or infinite computation, but no running networked REPL.  Don't give up -- get access to what is going on in a running Clojure process via a newly started socket repl.

* Trying out a project that provides nrepl access but no socket repl.  Avoid initial study and early modification of a project.clj file just to add a socket repl.

## Quick Trial

Suppose there is a running Clojure process with project directory `/home/alice/a-clj-proj-dir`.

Start a socket REPL on port 7650:

```
$ cd /home/alice/a-clj-proj-dir
$ clj -Sdeps '{:deps {alc.start-repl {:git/url "https://github.com/sogaiu/alc.start-repl" :sha "4de1af76d669f51e6e553937885192a404207e11"}}}' -m alc.start-repl.main --port 7650
```

## Usage

Edit the `:aliases` section of `~/.clojure/deps.edn` to contain:

```
   :alc.start-repl
   {
    :extra-deps {sogaiu/alc.start-repl
                 {:git/url "https://github.com/sogaiu/alc.start-repl"
                  :sha "4de1af76d669f51e6e553937885192a404207e11"}}
    :main-opts ["-m" "alc.start-repl.main"]
   }
```

For the following examples, assume a running Clojure process with project directory `/home/alice/a-clj-proj-dir`:

Start a socket REPL on port 8888:

```
$ cd /home/alice/a-clj-proj-dir
$ clj -A:alc.start-repl --port 8888
```

-OR-

```
$ clj -A:alc.start-repl --port 8888 --proj-dir /home/alice/a-clj-proj-dir
```

Start a socket REPL, letting the system choose a port number:

```
$ cd /home/alice/a-clj-proj-dir
$ clj -A:alc.start-repl
```

## Technical Details

In short, loading socket-repl-starting JVM bytecode into a running Clojure-running JVM process.

Specifically, via the `loadAgent` method of the `VirtualMachine` class in the JDK's Attach API to load an appropriately prepared jar file.

## Notes

* For `loadAgent`, despite exceptions being thrown, a repl may have been started.  Haven't figured out why yet.

* It's possible some JDKs don't have the Attach API or it is available under a different guise (possibly some of IBM's JDKs?).  In the former case I don't know if there's much one can do, but in the latter case, appropriate modfications may be sufficient to get things working.

* For some reason, `clojure.main/repl` doesn't work as-is in the context of `loadAgent` for recent JDKs (JDK >= 9?).  This appears to have something to do with class loaders.  Used a modified version of `clojure.main/repl` to cope.  It's not clear yet whether the changes have some unforeseen side-effects.

## Building the JAR

To build the jar file used by `loadAgent`:

* Ensure that the `alc.start-repl` project directory has an empty subdirectory named `classes`.

* From the same project directory:

```
$ clj -m alc.start-repl.build
```

If all went well, this should produce a file named `start-socket-repl-agent.jar` in the project directory.

Details of the compilation process can be found in `alc.start-repl.impl.jar`.

## Acknowledgments

* Pure Clojure implementation of a Java agent by dgopstein.  He generously shared his implementation and provided a helpful explanation:

  <https://dgopstein.github.io/articles/clojure-javaagent>

* A more general tool that works with JVM processes that don't necessarily have a Clojure inside:

  <https://github.com/djpowell/liverepl>

  However, it doesn't appear to work with Java >= 9.  (FWIW, looks like it can be fixed by tweaking some class loader related things, and I may have gotten it working.)

* A modified version of clojure.tools.cli is included.

* A modified version of clojure.main/repl is included.

## Related

* [clj-async-profiler](https://github.com/clojure-goes-fast/clj-async-profiler) uses `loadAgent` from `VirtualMachine` as well.  It may have a better approach to ensuring the Attach API is available.

* [jattach](https://github.com/apangin/jattach) can be used to load the agent jar in this project without an installation of OpenJDK:

  ```
  No installed JDK required, works with just JRE.
  ```

  Perhaps the code can be adapted for use with GraalVM's native image so this project might be a target of native image too.

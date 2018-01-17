The term "build tool" is used to describe a host of different pieces of
software, each with their own features, uses and complexity. It is sometimes
hard to see the forest for the trees: in the midst of minifying Javascript,
compiling C++, or generating protobuf sources, what is a build tool all about?

In this post, I will argue that fundamentally a build-tool models the same thing
as a pure functional program. The correspondence between the two is deep, and in
studying it we can get new insights into both build-tooling and functional
programming.

-------------------------------------------------------------------------------

"Build tool" is a bit of a catch-all phrase used to describe software that does
a host of different concrete tasks:

- Generates source code defining Java classes from your `.proto` files
- Generating Spritesheets or
  [Mipmaps](https://en.wikipedia.org/wiki/Mipmap) from images
- Downloads dependencies from remote package repositories like Maven, PyPI or
  NPM
- Zipping up a folder full of files for deployment
- Deploys your code to a remote service
- Runs linters, style-checkers & static analyzers
- Compiles your Java into Bytecode
- Compiles your Java ([GWT](http://www.gwtproject.org/)) into Javascript
- Compiles your Scala into Javascript
- Compiles your Typescript into Javascript
- Compiles your Javascript (ES6) into Javascript (ES5)

Apart from these concrete tasks (and compiling *everything* into Javascript these
days...) , there are a lot of cross-cutting considerations that build tools tend to
take care of: things that are not specific to any one task, but apply to the
project as a whole:

- Given a task the user wants to run, make sure all tasks it depends on
  are run first, in the correct order

- Cache the output of tasks so they aren't repeated unnecessarily, but flush the
  caches when necessary and re-build incrementally to make sure the output is
  up-to-date

- Parallelize the tasks that can be run in parallel

- Watch the input source files for changes and automatically re-run the
  downstream tasks that depend on them

- [DRY](https://en.wikipedia.org/wiki/Don%27t_repeat_yourself) up repetitive
  groups of tasks: every Scala module may have a `compile` and `package` task,
  every Javascript module may have `minify` `lint` and `test`, but I don't want
  to have to write these tasks over and over for each one

- Delegate work to third-party tools: compilers, linters, packagers

While
[different build tools](http://www.lihaoyi.com/post/WhatsinaBuildTool.html) have
different syntaxes, different features or different capabilities, basically all
of them manage these same core competencies.

## The Build Graph

At it's simplest, a build tool is just runs a single task, taking some input and
producing some output, e.g.:

```text
            compile
Java Source ------> Java Bytecode 
```

Every time you change the `Java Source`, you want your build tool to `compile`
it into `Java Bytecode`. Simple.

However, you quickly find you need more than one operation to be performed:
perhaps you need to package up your Java Bytecode into `.jar` files for
deployment, after first being put through an obfuscator like
[Proguard](https://www.guardsquare.com/en/proguard) to protect your proprietary
IP:

```text
       compile          proguard                     package
Source ------> Bytecode -------> Obfuscated Bytecode ------> Release Jars 
```

Now, every time you change the `Source`, your build tool should put it through
all of `compile`, `proguard` and `package` it into the final `Release Jars`; or
perhaps, if you're just testing locally, you can skip the `proguard`/`package`
steps and run the raw `Bytecode`. Your build tool must be flexible enough you
can tell it exactly which output you need (`Bytecode`, `Obfuscated Bytecode`, or
`Release Jar`s) and only do as much work necessary to get to that step.

As your project grows, you may realize you may need to integrate even more
things into the pipeline:

- You want to generate some source code from `.proto` files; these should get
  `compile`d together with the normal `Source` code, but re-generated every time
  you change the `.proto`s

- You want to have a set of non-obfuscated `Test Jars` for internal deployments
  & QA, where you don't need to worry about IP but want the stack traces to be
  easier to understand. These `Test Jars` must also contain
  [Source Jars](https://stackoverflow.com/questions/30686312/what-is-a-sources-jar)
  full of source code for better integration with your QA team's IDEs and
  debuggers

- You want to integrate open-source libraries into your project! These may be
  shipped as `.jar` files full of bytecode that you download from Maven Central
  using their
  [Maven Coordinates](https://maven.apache.org/pom.html#Maven_Coordinates),
  which you need to include your final jars (but no need to obfuscate)

Now, your build graph may look like this:

```text
       compile          proguard                     package
Source ------> Bytecode -------> Obfuscated Bytecode ------> Release Jars
 |        |            \                              /
 |        |             \                            /
 | Generated Source      \         Test Jars        /
 |        ^               \        ^               /
 |        | protoc         \      /|              /
 |        |               ,-'----'  \            /
 |   Proto Files         / package   \          / 
 |                      /             |        /
 '----------------> Source Jars       Libraries
  package                             ^       
                                     /
     Maven Coordinates -------------'
                         download   
```

Now, you as-the-programmer still want to do the same things as before: you want
to tell your build tool

- *Give me the `Test Jars`*

And you want your build tool to be able to figure out it needs to

- `package` the `Source` into `Source Jars`
- `protoc` the `Proto Files` into `Generated Source`
- `compile` the `Source` and `Generated Source` files into `Bytecode`
- `package` the `Bytecode` and `include` the `Source Jars` and `Libraries` into
  the file `Test Jars`

Furthermore, next time you change either the `Source` files, the `protoc` files,
or tweak the `Maven Coordinates` of the open-source libraries, you want your
build tool to do the absolute minimum work required to bring the `Test Jars` up
to date. And you want the tool to do that work in parallel.

While trivial projects may have a one-step build or a linear "asset pipeline",
any non-trivial project ends up with a graph of build-steps and
intermediate-results similar to the one shown above. And this example is pretty
trivial as far as real-world builds go!

## Getting to the Build Graph

Fundamentally, most builds tools end up modeling the same data structure. In
[make](https://en.wikipedia.org/wiki/Make_(software)), each rule you specify the
name of the output followed by the the names of the inputs, with the action to
transform inputs to outputs on the following line:

```make
hello: main.o factorial.o hello.o
    g++ main.o factorial.o hello.o -o hello

main.o: main.cpp
    g++ -c main.cpp
```

With [rake](https://martinfowler.com/articles/rake.html), you specify the
`input`s and `output` as the header for each `task`, followed by a block of Ruby
code that transforms the input to the output:

```ruby
task :build_refact => [:clean] do
  target = SITE_DIR + 'refact/'
  mkdir_p target, QUIET
  require 'refactoringHome'
  OutputCapturer.new.run {run_refactoring}
end

file 'build/dev/rake.html' => 'dev/rake.xml' do |t|
  require 'paper'
  maker = PaperMaker.new t.prerequisites[0], t.name
  maker.run
end
```

With [SBT](http://www.scala-sbt.org/), you specify the output but the inputs to
a task are automatically inferred by who you call `.value` on, in the body of a
Scala block that transforms the input to the output:

```scala
assembly in Test := {
  val dest = target.value/"amm"
  IO.copyFile(assembly.value, dest)
  import sys.process._
  Seq("chmod", "+x", dest.getAbsolutePath).!
  dest
}
```

There are as many ways of defining the build graph as there are build tools.
However, at it's core the build graph - a
[directed, acyclic graph](https://en.wikipedia.org/wiki/Directed_acyclic_graph)
of build steps and intermediate results, is core to basically every tool out
there.

Given the wealth of possibilities, it might beg the question: what is the
simplest, most straightforward way in which someone may use code to define a
directed acyclic graph?

## Functional Programs are Directed Acyclic Graphs

It turns out that the simplest way to define a directed acyclic graph
data-structure in code is with a
[pure functional program](http://www.lihaoyi.com/post/WhatsFunctionalProgrammingAllAbout.html).

Here is the simple 1-step build from earlier:

```text
            compile
Java Source ------> Java Bytecode 
```

And here it is as a functional (Python) program:

```python
source = ...
bytecode = compile(source)
```

Ignore for the moment the question of how the program is *executed*: for now we
just want to focus on the *structure*. The (small) directed graph above tells
you that `Java Source` is passed through the `compile` function, and results in
`Bytecode`. The two-line Python snippet expresses the same thing.

The multi-stage "pipeline" build can be similarly translated, from a ASCII
graph:

```text
       compile          proguard                     package
Source ------> Bytecode -------> Obfuscated Bytecode ------> Release Jars 
```

To a short Python snippet:

```python
source = ...
bytecode = compile(source)
obfuscated_bytecode = proguard(bytecode)
release_jars = package(obfuscated_bytecode)
```

Again, we ignore how the program will *run*, and just focus on how the program
is *structured*. Both the ASCII graph and functional-python-snippet have the
same structure: `source` being put through a number of transformations
(`compile`, `proguard`, `package`) to create a sequence of named results
(`bytecode`, `obfuscated_bytecode`, `release_jars`) each dependent on the
previous.

Lastly, we can look at the most complex build-graph above, and see how it might
be expressed as a functional Python program. From ASCII art:

```text
       compile          proguard                     package
Source ------> Bytecode -------> Obfuscated Bytecode ------> Release Jars
 |        |            \                              /
 |        |             \                            /
 | Generated Source      \         Test Jars        /
 |        ^               \        ^               /
 |        | protoc         \      /|              /
 |        |               ,-'----'  \            /
 |   Proto Files         / package   \          / 
 |                      /             |        /
 '----------------> Source Jars       Libraries
  package                             ^       
                                     /
     Maven Coordinates -------------'
                         download   
```

To functional Python:

```python
source = ...
proto_files = ...
maven_coordinates = [...]

libraries = download(maven_coordinates)
generated_source = protoc(proto_files)
bytecode = compile(source, generated_source)
source_jars = package(source)
test_jars = package(source_jars, bytecode, libraries)
obfuscated_bytecode = proguard(bytecode)
release_jars = package(obfuscated_bytecode, libraries)
```

This Python snippet is non-trivial, just like the ASCII graph above it. Unlike
the earlier snippets, we have some build steps like `compile` which take
multiple inputs, which we model as a function call taking multiple arguments.
You also have some intermediate results like `Bytecode` which are used in
multiple build steps, which is reflected by the `bytecode` value being passed
into multiple functions.

But if you draw the line from identifier to identifier, you'll find that this
results in exactly the same directed acyclic graph data structure we saw
earlier!

I call these Python snippets *functional* because each snippet is made of pure
functions with no side effects: the only inputs they take are their arguments,
and their result is their return value. We're assuming `compile`ing the `source`
code won't sneakily mutate the `source` and cause it to behave differently the
second time, or delete it so that trying to `package` it into `source_jars`
fails. We assume that running `protoc` to make `generated_sources` from
`proto_files` won't cause our `maven_coordinates` to change.

The code is structured as functions, whose only inputs are passed as arguments,
and only output is their return value, with no side-effects. Doesn't matter if
it's written in Python or Scala or Haskell, that is what makes it functional.

I have showed above how any build, from simple to complex, can be generally
modeled as a directed acyclic graph. We have also seen how the directed acyclic
graph happens to be the *exact* data structure that underlies pure-functional
code. If that is the case, why don't people write their builds in
pure-functions, whether in Python or something else?

It turns out that you *can* write a build tool with pure functions. In fact, all
you need is the appropriate set of implementations: a `download` function that
downloads things from Maven Central. A `protoc` function that invokes the
Protobuf code generator. A `package` function that zips things into jars. And
then you run `python script.py`:

```python
source = ...
proto_files = ...
maven_coordinates = [...]

libraries = download(maven_coordinates)
generated_source = protoc(proto_files)
bytecode = compile(source, generated_source)
source_jars = package(source)
test_jars = package(source_jars, bytecode, libraries)
obfuscated_bytecode = proguard(bytecode)
release_jars = package(obfuscated_bytecode, libraries)
```

And it will generate your `test_jars`, your `release_jars`, and everything else,
invoking the individual build steps in the correct order to create the results.
Many people who have worked on large projects would have no doubt encounted
jury-rigged Python scripts that do exactly that!

It turns out that you do not need to give your build-steps strings to serve as
unique IDs, or explicitly specify lists of dependencies so the tool knows what
each step. In the structure of this pure-functional Python snippet, we already
have all the information necessary for all the build tools in the world to work
with!

## Why not a Build-Tool of Pure Functions?

If you can write a Python script using pure functions to serve as a build tool,
why doesn't everyone do that? That certainly is simpler than learning a whole
new language/syntax/semantics/ecosystem just to invoke some functions in the
correct order.

It turns out, while the above Python script with naive
`download`/`protoc`/`package`/etc. implementations can work, it isn't a very
*good* build tool because it misses out on some of the things we've come to expect:

- I want my build tool to cache intermediate results and re-use them; the above
  snippet re-evaluates everything each time, which is slow and unnecessary

- Python (and most other languages!) typically run sequentially, whereas I
  *know* that some steps do not affect each other and can be run in parallel

- I can't ask the Python script which files it's interested in, so I can watch
  just those; the only thing I can do with the script is run it, not ask
  questions!

These three points, while simple to state, are enormously important. Nobody
likes waiting minutes each time to re-download the same libraries over and over,
or re-compile code that hasn't changed. That would be a deal-breaker for any
build tool, regardless of simplicity: I'll happily put up with awkward Makefile
syntax or magic Ruby/Rake incantations if it will save me time in my
build-test-debug iteration cycle.

Even traditional "functional programming" languages like Haskell or OCaml
default to a single-threaded, un-cached batch-oriented, non-queryable
programming model that's manifestly unsuitable for build tooling.

However, if those three points above are the only reason not to use a build-tool
which models your build as pure-functions, then perhaps there is hope.

The most crucial part of any piece of software is the user-facing data-model and
internal data-model. Algorithms can be adjusted, implementations can be
optimized or tweaks, but changing either data-model is enormously difficult. The
data-model of your program permeates every decision, every API, and every user
interface.

If we think that pure-functions is the simplest way of modeling the essential
complexity of a build's directed acyclic graph, then an interpreter/evaluator
could be written to take the existing pure-functional code, and execute it with
the caching, parallelism and queryability necessary to make a good build tool
out of it.

Alternately, you could leverage an existing programming-language/interpreter,
and try to capture enough of the program's call-graph so that at run-time you
can re-construct it and evaluate it in parallel.

[Mill](https://github.com/lihaoyi/mill#-mill-) is an open-source Scala build tool using
the latter strategy to try and follow these principles.

## Mill

Mill is a work-in-progress build tool using the Scala programming language. It
is a general-purpose Task runner: Scala compilation is delegated to the Scala
compiler, Maven dependency resolution is delegated to
[Coursier](https://github.com/coursier/coursier), and you can of course delegate
to any number of external processes. What Mill aims to do, is to let you define
your build graph as pure-functional program, while still achieving the caching,
parallelism, and queryability needed to make a good build tool.

The above Python snippet translated to Mill's Scala syntax would look something
like this:

```scala
def source = T.sources{ ... }
def proto_files = T.sources{ ... }
def maven_coordinates = T{ ... }

def libraries = T{ download(maven_coordinates()) }
def generated_source = T{ protoc(proto_files()) }
def bytecode = T{ compile(source(), generated_source()) }
def source_jars = T{ packageJar(source()) }
def test_jars = T{ packageJar(source_jars(), bytecode(), libraries()) }
def obfuscated_bytecode = T{ proguard(bytecode()) }
def release_jars = T{ packageJar(obfuscated_bytecode(), libraries()) }
```

Immediately, you will notice some extraneous syntax not seen in the Python
version earlier: the Scala language needs the `def` keyword for local
definitions, every definition's right-hand-side is wrapped in a `T{...}` block,
and every call-site has a trailing `()`, e.g. `proto_files()` and `source()`.
This extra syntax is the cost of trying to leverage an existing language, rather
than building a whole new interpreter.

Despite the additional syntax, the *structure* of this mill build is exactly the
same as the pure-functional Python snippet we saw earlier. And it *works*:

- You can run `mill libraries` to resolve the given `maven_coordinates` and
  download the necessary jars, which will then be cached until the
  `maven_coordinates` change. `mill bytecode` similarly will make the
  `generated_sources` from `proto_files`, and compile them together with the
  input `source`, and only re-compiling if one of them changes.

- Cached intermediate results are persisted to disk at easy-to-find paths, in a
  easy-to-read and easy-to-use format.

- You can query the build graph to find out what sources are used (directly or
  indirectly) by a certain task, and re-run only the affected tasks when any of
  them change.

All of this works today. Automatically parallelizing the build isn't in there
yet, but it will be soon.

There is a lot of machinery that goes on behind the scenes in order to make Mill
work. Mill needs to automatically infer a unique label for every named task, to
ensure it has a place on disk to cache it's data. Mill uses as sort of
[Free Applicative](https://www.youtube.com/watch?v=H28QqxO7Ihc) to reify and
model the build graph, and uses Scala AST Macros to let you write code in a
straightforward way without using the Applicative `zipMap` all over the place.
Mill's modules are just `object`s with bundles of related `Task`s, and Mill uses
implicits to propagate context such that every `Task` knows it's name and place
on disk.

But to a user of Mill, all that can be ignored: what matters to a user is that
Mill allows you to define a build graph via the call-graph of a pure-functional
program.

While virtually every other build tool models the build as a directed acyclic
graph in some way, I'm not aware of any which leverage the call-graph of a
pure-functional program to model the build. Mill, while still a work in
progress, is my attempt at doing so.

Mill is a work in progress, and not quite ready for prime time. But if you are
tired of Scala's venerable [SBT build tool](http://www.scala-sbt.org/) and are
interested in a Scala build tool that simplifies the complexities of a build
graph using the call-graph of a pure-functional program, come by our
[Gitter Room](https://gitter.im/lihaoyi/mill) and let's collaborate!

## First Class Functions in a Build

One interesting intersection between the worlds of build-tooling and
pure-functional programming is the idea of a first-class function. For example,
given the smallest Python snippet we had earlier:

```python
source = ...
bytecode = compile(source)
```

A first-class function means that `compile` is not some magic built-in, but can
itself be assigned to a variable, or come from somewhere else, perhaps returned
from somewhere else:

```python
source = ...
compile = local_file("bin/compiler.jar")
bytecode = compile(source)
```

In functional programming, first-class functions allow for things like
[higher-order functions](https://en.wikipedia.org/wiki/Higher-order_function)
and lots of other cool things. However, their application in a pure-functional
build tool is even more interesting!

Up until this point, we have assumed that the build-steps like `protoc`,
`compile` or `proguard` are sort of magic: they are just "there", implemented as
part of the build tool. However, this is unsatisfying for a number of reasons:

- A build tool can't come built-in with *every* function someone will need. At
  some point, they'll need custom functions

- If your "custom functions" are non-trivial, as things like the Proguard
  Obfuscator are non-trivial, you probably want all the same properties that
  apply to your "main build" to also apply to building your "custom functions":
  parallel builds, caching, etc.

- If the implementation of your "custom function" changes, how do you know which
  caches to invalidate? If they are just functions living in your
  Python/Java/etc. program, there is no way to analyze which functions a change
  to your program affects. The only safe thing to do is to invalidate *all*
  caches when the "build program" changes: a frustrating operation for large
  projects which may take a while to re-build from clean!

However, if "custom functions" were simply executable build artifacts like any
other, then all these problems are solved:

- If the user wants some "custom function" that's not built in, such as a Kotlin
  compiler? Build it, and then execute it.

- The build of your "custom function" is slow? It happens in parallel and is
  cached, so hopefully won't bother you too much

- If the implementation of your "custom function" changes, you know exactly
  which build-steps it is used in, and can invalidate only-those without
  invalidating all the others!

The [Bazel Build](https://bazel.build/) tool is the single build tool I am aware
of that gets this particular point right. At it's core, Bazel's build steps
aren't defined as builtin-functions that operate on the build artifacts, as we
have done above:

```python
bytecode = compile(source, generated_source)
```

Instead, Bazel's build steps (called *actions*) are usually defined as running
an `executable`, which you may have built earlier, on some `inputs`, which you
also may have built earlier:

```python
ctx.run(
    executable = ctx.executable.compile
    inputs = [ctx.files.source, ctx.files.generated_source],
    ...
)
```

As a result, any executable that a user can build can be itself used in a build
step: extending a Bazel build with a newly-built executable is no different from
building anything else: you get all the same parallelism, caching and
queryability when building such extensions as you do when building your "main"
project. `compile`/`proguard`/`package`, rather than being builtin operations to
your "build program", become just another intermediate result to build in the
process of building your project.

While the syntax may differ in arbitrary ways (e.g. with all the `ctx.blah`
prefixes), you may recognize the introduction of `ctx.run` "builtin" function as
exactly the same transformation you often see in Lisp textbooks, from:

```lisp
(operation input1 input2)
```

To

```lisp
(apply operation input1 input2)
```

The good old `apply`/`eval` duality, code as data, data as code. But in the land
of build-tools, this provides a special elegance and brand-new set of benefits
from what you may be familiar with from Lisp/functional-programming land!

## Conclusion

Build tools are diverse and varied, but once you dig through all the cruft,
whether Bazel's
[meta](https://docs.bazel.build/versions/master/skylark/macros.html)-[meta](https://docs.bazel.build/versions/master/skylark/rules.html)
[pseudo-python interpreter](https://docs.bazel.build/versions/master/skylark/language.html)
or SBT's
[four-dimensional two-layer execution model](http://www.lihaoyi.com/post/SowhatswrongwithSBT.html)
they all look the same underneath: a directed acyclic graph of build-steps and
intermediate results, which is executed in parallel, cached and analyzed to
provide the efficient, minimally-flaky, maximally-snappy experience people
associate with "good" build tools.

It turns out that this directed acyclic graph is exactly the [same data model as
is represented by pure-functional code](http://www.lihaoyi.com/post/WhatsFunctionalProgrammingAllAbout.html),
regardless of language!

I think there is a missing piece in the land of build tools: a build tool that
really leverages this isomorphism between builds and pure-functional programming
to cut through unnecessary ceremony and maximize familiarity.

A build tool that does away with assigning IDs via strings, or specifying lists
of dependencies per-task, to instead leverage the identifiers and dependencies
already evident in the structure of the pure-functional program.

A build tool that let's people write the same `foo = func(bar, baz, qux)` that
they've been writing for years, but let them execute that code in parallel, caching
intermediate results, and query the code (without running it!) to know exactly
who `foo` depends on.

A build tool where the pure-function call-graph and the build-dependency-graph
are one and the same, and a programmer can use the same "jump to definition" and
"find usages" shortcuts they know and love to navigate their way through the
structure of the build.

[Mill](https://github.com/lihaoyi/mill#-mill-) is one attempt at doing so. It's
still somewhat rough and incomplete, but due to the principles laid out in this
post, I have reason to believe we'll be finally able to create a build tool that
makes intuitive sense to any programmer.
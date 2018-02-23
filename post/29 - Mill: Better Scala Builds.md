Mill is a new build tool for Scala: it compiles your Scala code, packages it,
runs it, and caches things to avoid doing unnecessary work. Mill aims to be
better than Scala's venerable old [SBT build tool](https://www.scala-sbt.org/),
learning from
[it's mistakes](http://www.lihaoyi.com/post/SowhatswrongwithSBT.html) and
building upon ideas from
[functional programming](http://www.lihaoyi.com/post/BuildToolsasPureFunctionalPrograms.html)
to come up with a build tool that is fast, flexible, and easy to understand and
use. This post will explore what makes Mill interesting to a Scala developer who
is likely already using SBT

-------------------------------------------------------------------------------

The [main Mill documentation](http://www.lihaoyi.com/mill/index.html) already
covers getting started with Mill and how to use it, and early blog posts cover
in detail how we can
[improve upon with SBT](http://www.lihaoyi.com/post/SowhatswrongwithSBT.html)
and design
[intuitive build tools](http://www.lihaoyi.com/post/BuildToolsasPureFunctionalPrograms.html).
This post will cover the highlights of what makes Mill a nice build tool, in
practice, to someone in-the-trenches trying to make their build do the right
thing .

A build tool like SBT contains a *lot* of functionality. Mill supports most of
the common workflows in SBT, but in many cases does them better, faster, and in
a more user-friendly way.

For many of these examples, I will be comparing the SBT and Mill builds for the
[Ammonite](https://github.com/lihaoyi/Ammonite) project.


## Command-line Friendliness

SBT has always had a bit of awkwardness passing arguments to main methods or
test suites from the command line:

```bash
sbt "main/run arg1 arg2 arg3"
sbt "main/test-only arg1 arg2 arg3"
```

With Mill, it's a lot cleaner:

```bash
mill main.run arg1 arg2 arg3
mill main.test arg1 arg2 arg3
```

Mill follows the command line convention that every other interpreter follows:

```text
interpreter [interpreter-flags] interpreted-command [command-flags]

java        -cp Foo.jar         foo.Main             arg1 arg2 arg3
python      -u                  hello.py             arg1 arg2 arg3
```

For example, here is a comparison of how you pass a test selector to the
[uTest](https://github.com/lihaoyi/utest) test framework using SBT and Mill:

```bash
sbt "core/test-only -- mypackage.MySuite.myTestCase"
mill core.test mypackage.MySuite.myTestCase
```

And turning on watch-and-re-evaluate:

```bash
sbt "~core/test-only -- mypackage.MySuite.myTestCase"
mill --watch core.test mypackage.MySuite.myTestCase
```

Thus, when evaluating targets or commands using Mill, you tend to have to worry
a whole lot less about quoting, escaping or syntax than you do using SBT. Rather
than re-inventing it's own command-line convention, Mill falls in line with the
conventions you are likely already familiar with. You should hopefully find
using Mill from the command line simple and predictable, with much less quoting
and escaping that you have to worry about.

## External-tool Friendliness

Unlike SBT, Mill does not aim to be the center of your world: you can happily
evaluate Mill tasks from bash or external scripts without worrying too much
about [fixed overhead or performance](#fixed-overhead). You do not need to
integrate everything into SBT, but rather can call out to Mill from whatever
other scripts or tools you are already using.

This philosophy extends to far more than just performance!

Mill's `out/` folder is simple and predictable:

- Given a task `foo.bar.baz` or `foo[bar].baz`
- It's output will be in the folder `out/foo/bar/baz/`

This output includes both a `meta.json` as well as a `dest/` folder that
contains any output files. As an external tool, you trivially parse this
`meta.json` file to find exactly the results you want from the output of a
command.

If you want to figure out why a build is "going wrong", you can [inspect
the task graph](#inspecting-tasks) and open up the relevant JSON files in
[Vim](http://www.vim.org/) to see exactly what data is being passed where,
without needing to open up a special Mill console for debugging.

You can also ask Mill to print out the JSON output for a particular task
directly via the `mill show foo.bar.baz` syntax, e.g.

```bash
Ammonite$ mill show ops[2.12.4].scalaVersion
"2.12.4"

Ammonite$ mill show ops[2.12.4].allSourceFiles
[
    {"path": "~/Github/Ammonite/ops/src/main/scala/ammonite/ops/Extensions.scala"},
    {"path": "~/Github/Ammonite/ops/src/main/scala/ammonite/ops/FileOps.scala"},
    {"path": "~/Github/Ammonite/ops/src/main/scala/ammonite/ops/Model.scala"},
    {"path": "~/Github/Ammonite/ops/src/main/scala/ammonite/ops/Path.scala"},
    {"path": "~/Github/Ammonite/ops/src/main/scala/ammonite/ops/PathUtils.scala"},
    {"path": "~/Github/Ammonite/ops/src/main/scala/ammonite/ops/Shellout.scala"},
    {"path": "~/Github/Ammonite/ops/src/main/scala/ammonite/ops/package.scala"}
]
```

This is in contrast with SBT which outputs `show`ed values in an ad-hoc
hard-to-parse format

```text
lihaoyi Ammonite$ sbt "show ops/sources"
[warn] Executing in batch mode.
[warn]   For better performance, hit [ENTER] to switch to interactive mode, or
[warn]   consider launching sbt without any commands, or explicitly passing 'shell'
[info] Loading project definition from /Users/lihaoyi/Dropbox/Github/Ammonite/project
[info] Set current project to ammonite (in build file:/Users/lihaoyi/Dropbox/Github/Ammonite/)
[info] * /Users/lihaoyi/Dropbox/Github/Ammonite/ops/src/main/scala/ammonite/ops/Model.scala
[info] * /Users/lihaoyi/Dropbox/Github/Ammonite/ops/src/main/scala/ammonite/ops/FileOps.scala
[info] * /Users/lihaoyi/Dropbox/Github/Ammonite/ops/src/main/scala/ammonite/ops/Extensions.scala
[info] * /Users/lihaoyi/Dropbox/Github/Ammonite/ops/src/main/scala/ammonite/ops/PathUtils.scala
[info] * /Users/lihaoyi/Dropbox/Github/Ammonite/ops/src/main/scala/ammonite/ops/Path.scala
[info] * /Users/lihaoyi/Dropbox/Github/Ammonite/ops/src/main/scala/ammonite/ops/package.scala
[info] * /Users/lihaoyi/Dropbox/Github/Ammonite/ops/src/main/scala/ammonite/ops/Shellout.scala
[info] * /Users/lihaoyi/Dropbox/Github/Ammonite/project/Constants.scala
[success] Total time: 0 s, completed 22 Feb, 2018 9:15:09 PM
```

Mill also automatically redirects all unnecessary debug output to stderr, leaving 
the pristine un-contaminated JSON output to stdout for you to parse or pipe around.

Thus, external tools do not need a "Mill integration" or "Mill plugin" in order to
extract data from a Mill build. Anyone with a JSON parser can shell out to Mill and
extract the data they need, and with Mill's low [fixed overhead](#fixed-overhead)
such an operation is both fast and convenient.

## Jump to Definition

Imagine you see a piece of SBT code in your IDE:

```scala
// Aggregate source jars into the assembly classpath, so that the
// `source` macro can find their sources and highlight/display them.
(fullClasspath in Runtime) ++= {
  (updateClassifiers in Runtime).value
    .configurations
    .find(_.configuration == Runtime.name)
    .get
    .modules
    .flatMap(_.artifacts)
    .collect{case (a, f) if a.classifier == Some("sources") => f}
},
```

You aren't familiar with this code, so you wonder: what was `fullClasspath`
before I added this stuff to it? And what does `updateClassifiers` do?

Your IDE likely has jump-to-definition, and you heard it has SBT support, so you
put your mouse on `fullClasspath` and `updateClassifiers` and hit `Ctrl-B`
(IntelliJ's hotkey) to jump to it's definition and see what their values are.

You are then faced with the following results in `keys.scala`:

```scala
val externalDependencyClasspath = TaskKey[Classpath]("external-dependency-classpath", "The classpath consisting of library dependencies, both managed and unmanaged.", BMinusTask)
val dependencyClasspath = TaskKey[Classpath]("dependency-classpath", "The classpath consisting of internal and external, managed and unmanaged dependencies.", BPlusTask)
val fullClasspath = TaskKey[Classpath]("full-classpath", "The exported classpath, consisting of build products and unmanaged and managed, internal and external dependencies.", BPlusTask)
val trackInternalDependencies = SettingKey[TrackLevel]("track-internal-dependencies", "The level of tracking for the internal (inter-project) dependency.", BSetting)
val exportToInternal = SettingKey[TrackLevel]("export-to-internal", "The level of tracking for this project by the internal callers.", BSetting)
```
```scala
val evictionWarningOptions = SettingKey[EvictionWarningOptions]("eviction-warning-options", "Options on eviction warnings after resolving managed dependencies.", DSetting)
val transitiveUpdate = TaskKey[Seq[UpdateReport]]("transitive-update", "UpdateReports for the internal dependencies of this project.", DTask)
val updateClassifiers = TaskKey[UpdateReport]("update-classifiers", "Resolves and optionally retrieves classified artifacts, such as javadocs and sources, for dependency definitions, transitively.", BPlusTask, update)
val transitiveClassifiers = SettingKey[Seq[String]]("transitive-classifiers", "List of classifiers used for transitively obtaining extra artifacts for sbt or declared dependencies.", BSetting)
val updateSbtClassifiers = TaskKey[UpdateReport]("update-sbt-classifiers", "Resolves and optionally retrieves classifiers, such as javadocs and sources, for sbt, transitively.", BPlusTask, updateClassifiers)
```

You can see their type, maybe a short comment saying what it's meant to do, but
where is the *code*? After all, the code is what tells you what something
*actually* does!

Depending on how expert you are at SBT, you may find yourself in
[Defaults.scala](https://www.scala-sbt.org/0.13/sxr/sbt/Defaults.scala.html)

If you are lucky, you may spend some time grepping around before you find the
relevant definition:

```scala
updateClassifiers := (Def.task {
  val s = streams.value
  val is = ivySbt.value
  val mod = (classifiersModule in updateClassifiers).value
  val c = updateConfiguration.value
  val app = appConfiguration.value
  val out = is.withIvy(s.log)(_.getSettings.getDefaultIvyUserDir)
  val uwConfig = (unresolvedWarningConfiguration in update).value
  val depDir = dependencyCacheDirectory.value
  withExcludes(out, mod.classifiers, lock(app)) { excludes =>
    val uwConfig = (unresolvedWarningConfiguration in update).value
    val logicalClock = LogicalClock(state.value.hashCode)
    val depDir = dependencyCacheDirectory.value
    val artifacts = update.value.toSeq.toVector
    IvyActions.updateClassifiers(is, GetClassifiersConfiguration(mod, excludes, c, ivyScala.value), uwConfig, LogicalClock(state.value.hashCode), Some(depDir), artifacts, s.log)
  }
} tag (Tags.Update, Tags.Network)).value
```

Now, if you're curious for any of the other things that it's calling `.value` on
(`streams`, `ivySbt`, `mod`, `updateConfiguration`, ...) you have to repeat the
same dance: maybe one or two jumps, and then mostly `Cmd-F` and praying you find
what you want. Grep, grep, grep...

Why is that?

The reason why jump-to-definition doesn't take you immediately to the relevant
piece of code, is that SBT's execution model is basically the same as assigning
`null` to a variable and then assigning the *real* value to it later, somewhere
else.

Of course, your "variable" is called a `TaskKey` instead of a `var`, your
assignment is called `:=` instead of `=`, "null" is called something else.

But fundamentally you are assigning `null` to a variable and filling in the real
value later, somewhere else.

Given that execution model, it's not surprising jump-to-definition is useless
and you find yourself grepping. If you programmed with `var`s initialized to
`null` in your own code, you would find yourself grepping too!

You will undoubtedly meet Mill code that is initially as confusing as the SBT
code you met above:

```scala
def repositories = super.repositories ++ Seq(
  MavenRepository("https://oss.sonatype.org/content/repositories/releases")
)

def devAssembly = T{
  assemblyBase(
    Agg.from(assemblyClasspath().flatten.map(_.path)),
    (scalalib.testArgs() ++ scalajslib.testArgs() ++ scalaworker.testArgs()).mkString(" ")
  )
}
```

But once you start jumping to definition, things are better. Jumping to
`super.repositories` brings you to:

```scala
def repositories: Seq[Repository] = Seq(
  Cache.ivy2Local,
  MavenRepository("https://repo1.maven.org/maven2")
)
```

`assemblyClasspath` to:

```scala
def assemblyClasspath = mill.define.Task.traverse(assemblyProjects)(_.runClasspath)
```

`scalaworker.testArgs` to:

```scala
def testArgs = Seq(
  "-DMILL_SCALA_WORKER=" + runClasspath().map(_.path).mkString(",")
)
```

`runClasspath` to:

```scala
def runClasspath = T{
  upstreamRunClasspath() ++
  Agg(compile().classes) ++
  resources() ++
  unmanagedClasspath() ++
  resolveDeps(T.task{runIvyDeps() ++ scalaLibraryIvyDeps() ++ transitiveIvyDeps()})()
}
```

While of these snippets are trivially understandable, each one offers a glimpse
into how the returned value is computed, and provides more places you could jump
into if you want to dig deeper. You still have to work to understand it, but
where in SBT you may hit a wall, in Mill even complex builds can be explored in
a straightforward fashion using your IDE.

The reason Mill lets you do this is itself interesting: by following a
[pure-functional evaluation model](http://www.lihaoyi.com/post/WhatsFunctionalProgrammingAllAbout.html),
Mill ensures that the source-code-level call-graph of your various Tasks mirrors
exactly the runtime dependency graph of the instantiated Tasks, as well as the
final data-flow graph of how data flows between them. That is why Mill can
provide a useful jump-to-definition while SBT, with it's mutable evaluation
model, cannot.

While Mill's builds can get complicated, having a useful jump-to-definition
capability means even complex Mill builds are much easier to understand than the
SBT equivalent.

## Built in capabilities

There are two big things you want a build tool to do:

- Build something you can deploy/run without the build tool
- Publish the thing you built to package repositories for others to build upon

SBT, surprisingly, does *neither* of these things by default!

You used to need the [SBT Native Packager](https://stackoverflow.com/a/26542862)
plugin's `stage` command to create an entrypoint to run your Scala code without
SBT. If you want a fat jar, you need the
[SBT Assembly](https://github.com/sbt/sbt-assembly) plugin. If you want to
publish to Sonatype, you need the [SBT PGP](https://github.com/sbt/sbt-pgp)
plugin, and if you want to actually *release* the code you published you need
the [SBT Sonatype](https://github.com/xerial/sbt-sonatype) plugin. Maybe you
need the [SBT Release](https://github.com/sbt/sbt-release) plugin to help you do
the "run one thing after another" thing.

That's five plugins, and we've barely satisfied what *anyone wants a build tool to
do by default*.

None of the above tasks are particularly complicated; anyone should be able to
write a bash script that runs some classfiles, zip things into a jar, or send
sonatype some bytes and JSON to make it publish what you want.

These are not hard problems.

Mill comes with
[far-jar-ing & sonatype publish/releasing](http://www.lihaoyi.com/mill/index.html#deploying-your-code)
by default. It is Barely a few hundred lines of code to implement these
capabilities, and it does not make sense to require a fistful of plugins just to
do such common tasks.

Anyone using Mill can immediately deploy their code, whether as a fat jar to
some webserver or pulically to maven central, without needing to deal with a
mess of plugins and configuration. If you want something "custom", like making
your project's `publishVersion` depend on the current git hash, you can
trivially implement it yourself using an
[Input task](http://www.lihaoyi.com/mill/page/tasks.html#inputs).

## Fixed Overhead

```bash
Ammonite$ time sbt ops/compile
real	0m12.861s

Ammonite$ time sbt ops/compile
real	0m6.564s

Ammonite$ time sbt ops/compile
real	0m7.122s
```
```bash
Ammonite$ time mill ops[2.12.4].compile
real	0m15.846s

Ammonite$ time mill ops[2.12.4].compile
real	0m1.910s

Ammonite$ time mill ops[2.12.4].compile
real	0m1.967s
```

Both SBT and Mill cache output and avoid re-compilation if the inputs don't
change.

In both cases above, you can see SBT and Mill taking 10+ seconds for a "cold"
compile of the [Ammonite-Ops](http://ammonite.io/#Ammonite-Ops) codebase. In
both cases, the time drops drastically once the codebase has already been
compiled. But Mill's cold no-op compile time is much lower than SBT's no-op
compile time.

While Mill's 1.9 second fixed overhead isn't "fast", it is much more bearable
than SBT's 6-7+ second fixed overhead for a no-op action. This makes Mill much
more pleasant to use from the external command line than SBT is, since you spend
much less time waiting for your build tool even before it has to do any real
work.

## Watch & Re-evaluate

```bash
Ammonite$ sbt ~ops/compile |  ts '%H:%M:%.S'
08:19:28.516823 2. Waiting for source changes... (press enter to interrupt)

Ammonite$ echo "" >> ops/src/main/scala/ammonite/ops/FileOps.scala; echo "" | ts '%H:%M:%.S'
08:19:45.100620

08:19:46.209882 [info] Compiling 1 Scala source...

Ammonite$ echo "" >> ops/src/main/scala/ammonite/ops/FileOps.scala; echo "" | ts '%H:%M:%.S'
08:20:06.127597

08:20:06.752445 [info] Compiling 1 Scala source...

Ammonite$ echo "" >> ops/src/main/scala/ammonite/ops/FileOps.scala; echo "" | ts '%H:%M:%.S'
08:20:12.949252

08:20:13.963233 [info] Compiling 1 Scala source...
```
```bash
Ammonite$ mill --watch ops[2.12.4].compile 2>&1 | ts '%H:%M:%.S'
08:23:52.509628 Watching for changes to 10 files... (Ctrl-C to exit)

Ammonite$ echo "" >> ops/src/main/scala/ammonite/ops/FileOps.scala; echo "" | ts '%H:%M:%.S'
08:24:05.200828

08:24:05.346854 [info] Compiling 1 Scala source...

Ammonite$ echo "" >> ops/src/main/scala/ammonite/ops/FileOps.scala; echo "" | ts '%H:%M:%.S'
08:24:23.743401

08:24:23.902898 [info] Compiling 1 Scala source...

Ammonite$ echo "" >> ops/src/main/scala/ammonite/ops/FileOps.scala; echo "" | ts '%H:%M:%.S'
08:24:33.851489

08:24:33.931748 [info] Compiling 1 Scala source...
```

Mill's `--watch` feature picks up changes much faster than SBT's `~`; in the
above trials SBT takes on average `0.91` seconds to pick up the change, while
Mill takes `0.13` seconds. That is because Mill knows *exactly* which files
serve as inputs to the tasks you wish to evaluate, and can poll exactly those
files at a higher frequency without significant overhead.

Mill's `--watch` feature is also able to pick up changes in `build.sc`,
re-evaluate your build file, then re-evaluate your selected task automatically.
SBT's `~` isn't able to pick up changes in your build file, forcing you to
manually interrupt the `~` to reload the latest changes to your build.

## Error Messages

```bash
Ammonite$ sbt op/compile
[warn] Executing in batch mode.
[warn]   For better performance, hit [ENTER] to switch to interactive mode, or
[warn]   consider launching sbt without any commands, or explicitly passing 'shell'
[info] Loading project definition from /Users/lihaoyi/Dropbox/Github/Ammonite/project
[info] Set current project to ammonite (in build file:/Users/lihaoyi/Dropbox/Github/Ammonite/)
[error] Expected ID character
[error] Not a valid command: op
[error] Expected project ID
[error] Expected configuration
[error] Expected ':' (if selecting a configuration)
[error] Expected key
[error] Not a valid key: op
[error] op/compile
[error]   ^

Ammonite$ sbt ops/compiles
[warn] Executing in batch mode.
[warn]   For better performance, hit [ENTER] to switch to interactive mode, or
[warn]   consider launching sbt without any commands, or explicitly passing 'shell'
[info] Loading project definition from /Users/lihaoyi/Dropbox/Github/Ammonite/project
[info] Set current project to ammonite (in build file:/Users/lihaoyi/Dropbox/Github/Ammonite/)
[error] Expected ':' (if selecting a configuration)
[error] Not a valid key: compiles (similar: compilers, compile, compileInputs)
[error] ops/compiles
[error]             ^
```
```bash
Ammonite$ mill op[2.12.4].compile
Cannot resolve op. Did you mean ops?

Ammonite$ mill ops[2.12.4].compiles
Cannot resolve ops[2.12.4].compiles. Did you mean ops[2.12.4].compile?
```

Mill provides much better error messages than SBT if you mis-type things at the
command line. One reason Mill can do this is that it's internal data model is
much simpler than SBTs: Mill has a single hierarchy of tasks organized via
`foo.bar.baz` or `foo[cross].baz`. That means when something doesn't line up,
Mill can more accurately guess what you were trying to do, in contrast with SBT
which doesn't know if you mis-typed a project ID, configuration, or key.

Mill also provides better error messages when you make syntax mistakes in your
build file:

```diff
diff --git a/build.sbt b/build.sbt
 crossScalaVersions := Seq(
-  "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7", "2.11.8", "2.11.9"
+  "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7", "2.11.8", "2.11.9",
 )
```
```diff
diff --git a/build.sc b/build.sc
 import mill._, scalalib._
-val binCrossScalaVersions = Seq("2.11.11", "2.12.4")
+val binCrossScalaVersions = Seq("2.11.11", "2.12.4",)
 val fullCrossScalaVersions = Seq(
```

```bash
Ammonite$ sbt ops/compile
[warn] Executing in batch mode.
[warn]   For better performance, hit [ENTER] to switch to interactive mode, or
[warn]   consider launching sbt without any commands, or explicitly passing 'shell'
[info] Loading project definition from /Users/lihaoyi/Dropbox/Github/Ammonite/project
[error] [/Users/lihaoyi/Dropbox/Github/Ammonite/build.sbt]:7: illegal start of simple expression
[error] [/Users/lihaoyi/Dropbox/Github/Ammonite/build.sbt]:374: ')' expected but eof found.
```
```bash
Ammonite$ mill ops[2.12.4].compiles
build.sc:2:52 expected WSChars | Comment | Newline | ")"
val binCrossScalaVersions = Seq("2.11.11", "2.12.4",)
                                                   ^
```

Or runtime errors:

```diff
diff --git a/build.sbt b/build.sbt

+???
+
```
```diff
diff --git a/build.sc b/build.sc
@@ -6,2 +6,5 @@ val fullCrossScalaVersions = Seq(
 )
+
+???
+
```

SBT output:

```text
Ammonite$ sbt ops/compile
[warn] Executing in batch mode.
[warn]   For better performance, hit [ENTER] to switch to interactive mode, or
[warn]   consider launching sbt without any commands, or explicitly passing 'shell'
[info] Loading project definition from /Users/lihaoyi/Dropbox/Github/Ammonite/project
scala.NotImplementedError: an implementation is missing
	at scala.Predef$.$qmark$qmark$qmark(Predef.scala:252)
	at $16a24a9303fcf4f4b34b$.$sbtdef(/Users/lihaoyi/Dropbox/Github/Ammonite/build.sbt:9)
	at sun.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
	at sun.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:62)
	at sun.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43)
	at java.lang.reflect.Method.invoke(Method.java:498)
	at sbt.compiler.Eval$.getValue(Eval.scala:464)
	at sbt.compiler.Eval$$anonfun$4.apply(Eval.scala:97)
	at sbt.compiler.Eval$$anonfun$4.apply(Eval.scala:97)
	at sbt.EvaluateConfigurations$$anonfun$evaluateDslEntry$1.apply(EvaluateConfigurations.scala:185)
	at sbt.EvaluateConfigurations$$anonfun$evaluateDslEntry$1.apply(EvaluateConfigurations.scala:183)
	at sbt.EvaluateConfigurations$$anonfun$evaluateSbtFile$1$$anonfun$11.apply(EvaluateConfigurations.scala:128)
	at sbt.EvaluateConfigurations$$anonfun$evaluateSbtFile$1$$anonfun$11.apply(EvaluateConfigurations.scala:128)
	at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:244)
	at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:244)
	at scala.collection.immutable.List.foreach(List.scala:318)
	at scala.collection.TraversableLike$class.map(TraversableLike.scala:244)
	at scala.collection.AbstractTraversable.map(Traversable.scala:105)
	at sbt.EvaluateConfigurations$$anonfun$evaluateSbtFile$1.apply(EvaluateConfigurations.scala:128)
	at sbt.EvaluateConfigurations$$anonfun$evaluateSbtFile$1.apply(EvaluateConfigurations.scala:122)
	at sbt.Load$.sbt$Load$$loadSettingsFile$1(Load.scala:775)
	at sbt.Load$$anonfun$sbt$Load$$memoLoadSettingsFile$1$1.apply(Load.scala:781)
	at sbt.Load$$anonfun$sbt$Load$$memoLoadSettingsFile$1$1.apply(Load.scala:780)
	at scala.collection.MapLike$class.getOrElse(MapLike.scala:128)
	at scala.collection.AbstractMap.getOrElse(Map.scala:58)
	at sbt.Load$.sbt$Load$$memoLoadSettingsFile$1(Load.scala:780)
	at sbt.Load$$anonfun$loadFiles$1$2.apply(Load.scala:788)
	at sbt.Load$$anonfun$loadFiles$1$2.apply(Load.scala:788)
	at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:244)
	at scala.collection.TraversableLike$$anonfun$map$1.apply(TraversableLike.scala:244)
	at scala.collection.mutable.ResizableArray$class.foreach(ResizableArray.scala:59)
	at scala.collection.mutable.ArrayBuffer.foreach(ArrayBuffer.scala:47)
	at scala.collection.TraversableLike$class.map(TraversableLike.scala:244)
	at scala.collection.AbstractTraversable.map(Traversable.scala:105)
	at sbt.Load$.loadFiles$1(Load.scala:788)
	at sbt.Load$.discoverProjects(Load.scala:799)
	at sbt.Load$.discover$1(Load.scala:585)
	at sbt.Load$.sbt$Load$$loadTransitive(Load.scala:633)
	at sbt.Load$$anonfun$loadUnit$1.sbt$Load$$anonfun$$loadProjects$1(Load.scala:482)
	at sbt.Load$$anonfun$loadUnit$1$$anonfun$40.apply(Load.scala:485)
	at sbt.Load$$anonfun$loadUnit$1$$anonfun$40.apply(Load.scala:485)
	at sbt.Load$.timed(Load.scala:1025)
	at sbt.Load$$anonfun$loadUnit$1.apply(Load.scala:485)
	at sbt.Load$$anonfun$loadUnit$1.apply(Load.scala:459)
	at sbt.Load$.timed(Load.scala:1025)
	at sbt.Load$.loadUnit(Load.scala:459)
	at sbt.Load$$anonfun$25$$anonfun$apply$14.apply(Load.scala:311)
	at sbt.Load$$anonfun$25$$anonfun$apply$14.apply(Load.scala:310)
	at sbt.BuildLoader$$anonfun$componentLoader$1$$anonfun$apply$4$$anonfun$apply$5$$anonfun$apply$6.apply(BuildLoader.scala:91)
	at sbt.BuildLoader$$anonfun$componentLoader$1$$anonfun$apply$4$$anonfun$apply$5$$anonfun$apply$6.apply(BuildLoader.scala:90)
	at sbt.BuildLoader.apply(BuildLoader.scala:140)
	at sbt.Load$.loadAll(Load.scala:365)
	at sbt.Load$.loadURI(Load.scala:320)
	at sbt.Load$.load(Load.scala:316)
	at sbt.Load$.load(Load.scala:305)
	at sbt.Load$$anonfun$4.apply(Load.scala:146)
	at sbt.Load$$anonfun$4.apply(Load.scala:146)
	at sbt.Load$.timed(Load.scala:1025)
	at sbt.Load$.apply(Load.scala:146)
	at sbt.Load$.defaultLoad(Load.scala:39)
	at sbt.BuiltinCommands$.liftedTree1$1(Main.scala:548)
	at sbt.BuiltinCommands$.doLoadProject(Main.scala:548)
	at sbt.BuiltinCommands$$anonfun$loadProjectImpl$2.apply(Main.scala:540)
	at sbt.BuiltinCommands$$anonfun$loadProjectImpl$2.apply(Main.scala:540)
	at sbt.Command$$anonfun$applyEffect$1$$anonfun$apply$2.apply(Command.scala:59)
	at sbt.Command$$anonfun$applyEffect$1$$anonfun$apply$2.apply(Command.scala:59)
	at sbt.Command$$anonfun$applyEffect$2$$anonfun$apply$3.apply(Command.scala:61)
	at sbt.Command$$anonfun$applyEffect$2$$anonfun$apply$3.apply(Command.scala:61)
	at sbt.Command$.process(Command.scala:93)
	at sbt.MainLoop$$anonfun$1$$anonfun$apply$1.apply(MainLoop.scala:96)
	at sbt.MainLoop$$anonfun$1$$anonfun$apply$1.apply(MainLoop.scala:96)
	at sbt.State$$anon$1.doX$1(State.scala:183)
	at sbt.State$$anon$1.process(State.scala:190)
	at sbt.MainLoop$$anonfun$1.apply(MainLoop.scala:96)
	at sbt.MainLoop$$anonfun$1.apply(MainLoop.scala:96)
	at sbt.ErrorHandling$.wideConvert(ErrorHandling.scala:17)
	at sbt.MainLoop$.next(MainLoop.scala:96)
	at sbt.MainLoop$.run(MainLoop.scala:89)
	at sbt.MainLoop$$anonfun$runWithNewLog$1.apply(MainLoop.scala:68)
	at sbt.MainLoop$$anonfun$runWithNewLog$1.apply(MainLoop.scala:63)
	at sbt.Using.apply(Using.scala:24)
	at sbt.MainLoop$.runWithNewLog(MainLoop.scala:63)
	at sbt.MainLoop$.runAndClearLast(MainLoop.scala:46)
	at sbt.MainLoop$.runLoggedLoop(MainLoop.scala:30)
	at sbt.MainLoop$.runLogged(MainLoop.scala:22)
	at sbt.StandardMain$.runManaged(Main.scala:109)
	at sbt.xMain.run(Main.scala:38)
	at xsbt.boot.Launch$$anonfun$run$1.apply(Launch.scala:109)
	at xsbt.boot.Launch$.withContextLoader(Launch.scala:128)
	at xsbt.boot.Launch$.run(Launch.scala:109)
	at xsbt.boot.Launch$$anonfun$apply$1.apply(Launch.scala:35)
	at xsbt.boot.Launch$.launch(Launch.scala:117)
	at xsbt.boot.Launch$.apply(Launch.scala:18)
	at xsbt.boot.Boot$.runImpl(Boot.scala:41)
	at xsbt.boot.Boot$.main(Boot.scala:17)
	at xsbt.boot.Boot.main(Boot.scala)
[error] scala.NotImplementedError: an implementation is missing
[error] Use 'last' for the full log.
Project loading failed: (r)etry, (q)uit, (l)ast, or (i)gnore?
```

Mill output:

```text
Ammonite$ mill ops[2.12.4].compile
Compiling /Users/lihaoyi/Dropbox/Github/Ammonite/build.sc
scala.NotImplementedError: an implementation is missing
  scala.Predef$.$qmark$qmark$qmark(Predef.scala:284)
  ammonite.$file.build.$init$(build.sc:8)
  ammonite.$file.build$.<init>(build.sc:53)
  ammonite.$file.build$.<clinit>(build.sc)
```

There is no reason why SBT couldn't do the same, but Mill gets this nice error
reporting for free since it builds upon
[Ammonite Scala Scripts](http://ammonite.io/#ScalaScripts)

## Defining Tasks

```scala
lazy val sampleTask = taskKey[Int]("A sample task.")

sampleTask := {
   val sum = 1 + 2
   println("sum: " + sum)
   sum
}
```

SBT makes it somewhat awkward to define a new task. You first need to define a
`taskKey`, then assign the body of the task to it via `:=`.

Mill let's you simply `def` a function with `T{...}` (for cached Targets) or
`T.command{...}` (for un-cached Commands) and you have a task you can
immediately list, inspect and evaluate from the command line:

```scala
def sampleTarget = T{ 
  val sum = 1 + 2
  println("sum: " + sum)
  sum
}

def sampleCommand() = T.command{ 
  val sum = 1 + 2
  println("sum: " + sum)
  sum
}  
```

With SBT, your `sampleTask` now can be called with any subproject, even though
you may only want it to apply to one, and when applied to the others is
illegal/invalid/meaningless. With Mill, you can choose where `sampleTarget` or
`sampleCommand` are defined: top-level, within a single module, or in a shared
trait so it applies to multiple modules. You have control over exactly what your
build looks like, so you can make sure invalid tasks simply do not exist.

## Caching

SBT requires you to figure out caching of a task yourself, perhaps using
[FileFunction.cached](https://stackoverflow.com/a/25410646) and hoping to pick a
cache folder name that nobody else has picked lest you have cache collisions.
This means that most people writing SBT custom tasks do not bother caching as
aggressively as they could be (using `FileFunction.cached` is awkward, picking
filesystem names is hard) and portions of your build tend to re-evaluate over
and over even if no inputs changed.

Mill caches any Targets (defined with `T{...}` blocks) automatically, so
`sampleTarget` above only runs once since it has no inputs that can change:

```bash
Ammonite$ mill sampleTarget
sum: 3
Ammonite$ mill sampleTarget
Ammonite$ mill sampleTarget
```

While `sampleCommand` runs every time:

```bash
Ammonite$ mill sampleCommand
sum: 3
Ammonite$ mill sampleCommand
sum: 3
Ammonite$ mill sampleCommand
sum: 3
```

This means that with Mill, you spend much less time waiting for unnecessary
re-evaluations. In SBT you may be used to `update` triggering spuriously, or
`compile` taking some time to notice nothing has changed, or `assembly` printing
out it's wall-of-text and doing work even when the assembly has already been
made. In Mill, all these things are aggressively cached, entirely automatically,
and neither the person writing tasks nor the person using them needs to pay any
attention to make it work.

## Cross Building

```scala
val binCrossScalaVersions = Seq("2.11.11", "2.12.4")
val fullCrossScalaVersions = Seq(
  "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7", "2.11.8", "2.11.9", "2.11.11",
  "2.12.0", "2.12.1", "2.12.2", "2.12.3", "2.12.4"
)

object runtime extends Cross[RuntimeModule](binCrossScalaVersions:_*)
class RuntimeModule(val crossScalaVersion: String) extends AmmModule{
  ...
}
object interp extends Cross[InterpModule](fullCrossScalaVersions:_*)
class InterpModule(val crossScalaVersion: String) extends AmmModule{
  ...
}
```
```bash
Ammonite$ mill resolve amm.runtime[_]
amm.runtime[2.11.11]
amm.runtime[2.12.4]

Ammonite$ mill resolve amm.interp[_]
amm.interp[2.11.3]
amm.interp[2.11.4]
amm.interp[2.11.5]
amm.interp[2.11.6]
amm.interp[2.11.7]
amm.interp[2.11.8]
amm.interp[2.11.9]
amm.interp[2.11.11]
amm.interp[2.12.0]
amm.interp[2.12.1]
amm.interp[2.12.2]
amm.interp[2.12.3]
amm.interp[2.12.4]
```

Unlike SBT, Mill's cross-builds aren't "magic" global settings, but simply a
list of modules. Different modules can be cross-built against different sets of
`crossScalaVersion`s, which you can select using the `mill ops[2.12.4].compile`
syntax we saw earlier and can list using `mill resolve amm.ops[_]`.

Mill's cross builds aren't limited to scala versions; you can cross build
against any list of values. Mill's own build has the various
`scalajslib.jsbridges`, that it uses the integrate with the Scala.js tooling,
cross-built against the two sets of Scala.js versions: `0.6.x` and `1.0.0-Mx`:

```scala
object scalajslib extends MillModule {
  ...
  object jsbridges extends Cross[JsBridgeModule]("0.6", "1.0")
  class JsBridgeModule(scalajsBinary: String) extends MillModule{
    def moduleDeps = Seq(scalajslib)
    def ivyDeps = scalajsBinary match {
      case "0.6" =>
        Agg(
          ivy"org.scala-js::scalajs-tools:0.6.22",
          ivy"org.scala-js::scalajs-sbt-test-adapter:0.6.22",
          ivy"org.scala-js::scalajs-js-envs:0.6.22"
        )
      case "1.0" =>
        Agg(
          ivy"org.scala-js::scalajs-tools:1.0.0-M2",
          ivy"org.scala-js::scalajs-sbt-test-adapter:1.0.0-M2",
          ivy"org.scala-js::scalajs-env-nodejs:1.0.0-M2"
        )
    }
  }
}
```
```bash
mill$ mill resolve scalajslib.jsbridges[_]
scalajslib.jsbridges[0.6]
scalajslib.jsbridges[1.0]
```

Mill's cross-build system allows you to do some things which are entirely
impossible using SBT's cross builds, such as depending on multiple different
cross-builds of a cross-built modules from a non-cross-built module:

```scala
object scalajslib extends MillModule {

  def moduleDeps = Seq(scalalib)

  def testArgs = T{
    val mapping = Map(
      "MILL_SCALAJS_BRIDGE_0_6" -> jsbridges("0.6").compile().classes.path,
      "MILL_SCALAJS_BRIDGE_1_0" -> jsbridges("1.0").compile().classes.path
    )
    scalaworker.testArgs() ++ (for((k, v) <- mapping.toSeq) yield s"-D$k=$v")
  }
}
```

Here, I want to use both the `0.6` and `1.0` cross-builds of the `jsbridges`
module from `scalajslib`, which is itself not cross built. I'm not bundling them
up on the classpath, just including them as jar-files to manipulate at runtime.
Consider how you might achieve the same thing using SBT, e.g. having a single
non-cross-built subproject which uses the 2.10.6, 2.11.11 and 2.12.4 `assembly`s
of another SBT subproject as plain old files.

SBT's cross builds are also
[exponential in runtime](https://github.com/sbt/sbt/issues/3735), and trying to
run `sbt +scalaVersion` against a SBT subproject cross-built across:

```scala
crossScalaVersions := Seq(
  "2.11.3", "2.11.4", "2.11.5", "2.11.6", "2.11.7", "2.11.8", "2.11.9",
  "2.11.11", "2.12.0", "2.12.1", "2.12.2", "2.12.3", "2.12.4"
)
```

Takes forever. Mill's cross-built modules do not suffer this issue.


## Exploring your Build

```bash
Ammonite$ sbt tasks
[warn] Executing in batch mode.
[warn]   For better performance, hit [ENTER] to switch to interactive mode, or
[warn]   consider launching sbt without any commands, or explicitly passing 'shell'
[info] Loading project definition from /Users/lihaoyi/Dropbox/Github/Ammonite/project
[info] Set current project to ammonite (in build file:/Users/lihaoyi/Dropbox/Github/Ammonite/)

This is a list of tasks defined for the current project.
It does not list the scopes the tasks are defined in; use the 'inspect' command for that.
Tasks produce values.  Use the 'show' command to run the task and print the resulting value.

  checkPgpSignatures   Checks the signatures of artifacts to see if they are trusted.
  clean                Deletes files produced by the build, such as generated sources, compiled classes, and task caches.
  compile              Compiles sources.
  console              Starts the Scala interpreter with the project classes on the classpath.
  consoleProject       Starts the Scala interpreter with the sbt and the build definition on the classpath and useful imports.
  consoleQuick         Starts the Scala interpreter with the project dependencies on the classpath.
  copyResources        Copies resources to the output directory.
  doc                  Generates API documentation.
  package              Produces the main artifact, such as a binary jar.  This is typically an alias for the task that actually does the packaging.
  packageBin           Produces a main artifact, such as a binary jar.
  packageDoc           Produces a documentation artifact, such as a jar containing API documentation.
  packageSrc           Produces a source artifact, such as a jar containing sources and resources.
  publish              Publishes artifacts to a repository.
  publishLocal         Publishes artifacts to the local Ivy repository.
  publishLocalSigned   Publishing all artifacts to a local repository, but SIGNED using PGP.
  publishM2            Publishes artifacts to the local Maven repository.
  publishSigned        Publishing all artifacts, but SIGNED using PGP.
  run                  Runs a main class, passing along arguments provided on the command line.
  runMain              Runs the main class selected by the first argument, passing the remaining arguments to the main method.
  test                 Executes all tests.
  testOnly             Executes the tests provided as arguments or all tests if no arguments are provided.
  testQuick            Executes the tests that either failed before, were not run or whose transitive dependencies changed, among those provided as arguments.
  update               Resolves and optionally retrieves dependencies, producing a report.
```

SBT's `tasks` command isn't very useful. It only show a subset of tasks, doesn't
show many useful ones (like `assembly`), shows some that aren't generally useful
(like `checkPgpSigntures` or `publishLocalSigned`), and generally doesn't show
you what tasks are valid where (e.g. not every SBT subproject can be published)

SBT has separate commands to list `projects` and `commands`.

Mill on the other hand has a single `resolve` command that can be used to
explore the build hierarchy interactively.

```bash
Ammonite$ mill resolve _
ops
terminal
amm
shell
integration
sshd
resolve
describe
all
show

Ammonite$ mill resolve ops._
ops[2.11.11]
ops[2.12.4]

Ammonite$ mill resolve ops[2.12.4]._
ops[2.12.4].test
ops[2.12.4].compile
ops[2.12.4].allIvyDeps
ops[2.12.4].externalSources
ops[2.12.4].jar
ops[2.12.4].sources
ops[2.12.4].resources
...

Ammonite$ mill resolve __.compile
terminal[2.11.11].compile
terminal[2.11.11].test.compile
terminal[2.12.4].compile
terminal[2.12.4].test.compile
amm.repl[2.11.3].compile
amm.repl[2.11.3].test.compile
amm.repl[2.11.4].compile
amm.repl[2.11.4].test.compile
...

Ammonite$ mill resolve __[2.12.4].compile
terminal[2.12.4].compile
amm[2.12.4].compile
amm.repl[2.12.4].compile
amm.runtime[2.12.4].compile
amm.interp[2.12.4].compile
amm.util[2.12.4].compile
shell[2.12.4].compile
integration[2.12.4].compile
sshd[2.12.4].compile
ops[2.12.4].compile
```

Mill makes it very easy to explore your build and see exactly what you have
available. You an also take the tasks resolved by any of these queries and
evaluate them simply by ommitting the `resolve` command:

```bash
Ammonite$ mill __[2.12.4].compile
```

## Inspecting Tasks

```bash
Ammonite$ sbt "inspect ops/compile"
[warn] Executing in batch mode.
[warn]   For better performance, hit [ENTER] to switch to interactive mode, or
[warn]   consider launching sbt without any commands, or explicitly passing 'shell'
[info] Loading project definition from /Users/lihaoyi/Dropbox/Github/Ammonite/project
[info] Set current project to ammonite (in build file:/Users/lihaoyi/Dropbox/Github/Ammonite/)
[info] Task: sbt.inc.Analysis
[info] Description:
[info]      Compiles sources.
[info] Provided by:
[info]      {file:/Users/lihaoyi/Dropbox/Github/Ammonite/}ops/compile:compile
[info] Defined at:
[info]      (sbt.Defaults) Defaults.scala:286
[info] Dependencies:
[info]      ops/compile:manipulateBytecode
[info]      ops/compile:incCompileSetup
[info] Reverse dependencies:
[info]      ops/compile:products
[info]      ops/compile:discoveredMainClasses
[info]      ops/compile:printWarnings
[info]      ops/compile:discoveredSbtPlugins
[info] Delegates:
[info]      ops/compile:compile
[info]      ops/*:compile
[info]      {.}/compile:compile
[info]      {.}/*:compile
[info]      */compile:compile
[info]      */*:compile
[info] Related:
[info]      amm/compile:compile
[info]      readme/test:compile
[info]      sshd/test:compile
[info]      integration/compile:compile
[info]      ammInterp/test:compile
[info]      singleCrossBuilt/compile:compile
[info]      ammonite/test:compile
[info]      readme/compile:compile
[info]      shell/test:compile
[info]      ammRepl/compile:compile
```
```bash
Ammonite$ mill describe ops[2.12.4].compile
[1/1] describe
ops[2.12.4].compile(ScalaModule.scala:130)
Inputs:
    ops[2.12.4].scalaVersion
    ops[2.12.4].allSourceFiles
    ops[2.12.4].scalaCompilerBridgeSources
    ops[2.12.4].compileClasspath
    ops[2.12.4].scalaCompilerClasspath
    ops[2.12.4].scalacOptions
    ops[2.12.4].scalacPluginClasspath
    ops[2.12.4].javacOptions
    ops[2.12.4].upstreamCompileOutput
```

Mill lets you `describe` any arbitary task to find out where it's defined and
what it depends on. Unlike SBT it does not need to provide information about
`Provided by`, `Delegates` or `Related`: Mill tasks are simple nodes, living in
a single build-hierarchy and task-graph, and have no concept of delegation or
configurations or
[four-dimensional scopes](http://eed3si9n.com/4th-dimension-with-sbt-013). Mill
currently doesn't display the `Reverse dependencies`, but could be made to show
them in future.

## Aggregating

SBT relies on
[aggregation](https://www.scala-sbt.org/0.13/docs/Multi-Project.html#Aggregation)
to save keystrokes: you make subproject A aggregate subprojects B C and D, so
you can run any task (e.g. `test`) on B C and D simply by running `A/test`.

This is fine when you have a single `root` project that aggregates a few others,
but when things get more complex, it breaks down. You often want to run
different tasks on different sets of subprojects: you may want to `publish` all
your libraries, `assembly` only your application bundles, but `test` everything.
Due to how SBT [Cross Building](#cross-building) works, you may need to run one
cross-build against one set of modules but a different cross build against
another set. When that happens, you end up making fake subprojects that serve no
purpose other than aggregation:

```scala
lazy val singleCrossBuilt = project
  .in(file("target/singleCrossBuilt"))
  .aggregate(ops, terminal, ammUtil, ammRuntime)
  .settings(dontPublishSettings)

lazy val fullCrossBuilt = project
  .in(file("target/fullCrossBuilt"))
  .aggregate(shell, amm, sshd, ammInterp, ammRepl)
  .settings(dontPublishSettings)


lazy val published = project
  .in(file("target/published"))
  .aggregate(fullCrossBuilt, singleCrossBuilt)
  .settings(dontPublishSettings)
```

Here, we have some SBT subprojects, with their own source folders, test folders,
compilation logic, dependency resolution, and many more things. But we are
ignoring all of that as invalid, just to use the subprojects as aggregators!
That goes entirely against the principle of
[making invalid states unrepresentable](https://fsharpforfunandprofit.com/posts/designing-with-types-making-illegal-states-unrepresentable/).

With Mill, you can instead use it's query syntax e.g. `mill __.compile` or `mill
__.test` to run compiles or tests against all relevant modules, or more narrow
queries e.g. `mill foo.__.compile` to compile more limited sets (all modules
under `foo`). Cross-building works by default even if different modules have
different sets of cross builds, and if these queries aren't sufficiently
flexible you can simply define a Target or Command that forces evaluation of the
tasks you want to aggregate:

```scala
def compileAll() = T.command{
  amm.ops("2.12.4").compile()
  amm.runtime("2.12.4").compile()
  amm.interp("2.12.4").compile()
  amm.repl("2.12.4").compile()
  amm.shell("2.12.4").compile()
  amm.sshd("2.12.4").compile()
}

def testAll() = T.command{
  amm.ops("2.12.4").compile()
  amm.runtime("2.12.4").test()
  amm.interp("2.12.4").test()
  amm.repl("2.12.4").test()
  amm.shell("2.12.4").test()
  amm.sshd("2.12.4").test()
  amm.integration("2.12.4").test()
}
```

Now you can simply run `mill compileAll` or `mill testAll` to achieve what you
want, without having a bunch of un-used vestigal subprojects hanging around uselessly.

## Conclusion

SBT does a lot of things.

Mill tries to do everything you want to do using SBT, but often lets you do it
better: faster, easier, simpler.

There are many things you *can* do with SBT that you often don't *want* to do:
Mill often leaves these out. Mill's data model and execution model is much
simpler than SBT's: rather than N ways of doing things, you often just have 1
way to get what you want.

On the flip side, there are many things that people generally want, that SBT
does not provide. Simple things: faster initialization, automatically reloading
build files if they change, or packaging for deployment or publishing to Maven
Central. Mill provides these things by default

Mill is still a young project, but is already extremely usable. If you think
it's time to try something other than SBT, give
[Mill](http://www.lihaoyi.com/mill/) a try!

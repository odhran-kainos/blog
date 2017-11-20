SBT is the default build tool for the Scala programming community: you can build
Scala using other tools, but the vast majority of the community uses SBT.
Despite that, nobody seems to *like* SBT: people say it's confusing,
complicated, and opaque. This post will deeply analyze what exactly it is about
SBT that people don't like, so we can build a consensus around the problems and
a foundation for how we can make things better in future.

-------------------------------------------------------------------------------

SBT used to be called the "Simple Build Tool". However, it is anything but
simple! Eventually the irony grew too great to bear, and it was retroactively
renamed the "Scala Build Tool", and now the acronym "SBT" is officially just a
name that doesn't stand for anything.

SBT is both widely used and widely panned. Some choice quotes about SBT that
I've seen around the community:

> sbt's syntax is just honestly sinful. It looks like something the germans
> would have come up with... 70 yrs ago... in a submarine.

- [Reddit](https://www.reddit.com/r/scala/comments/473imk/its_absurd_that_sbt_has_simple_in_its_name/d0albez/)


> SBT's bizarre abstraction and unreadable syntax is a huge, frustrating
> obstacle to adopting Scala---especially because it's one of the first things
> you encounter when you start out.

- [Reddit](https://www.reddit.com/r/scala/comments/5a6muj/sbt_makes_me_want_to_give_up_scala/d9fvwj1/)

> it is very difficult to know how to get SBT to do something that isn't a very
> slight variant on something you already have a formulaic solution for.

- [Scala Users](https://contributors.scala-lang.org/t/asking-for-your-feedback-on-sbt-scala-center-announcement/738/7?u=lihaoyi)

> In a large project with -30 modules, watching sources (eg. ~compile) even in
> just one of the modules often freezes for up to 20 seconds, doing nothing
> before responding to changes. Other tasks like testOnly can also suffer from
> this but usually with <10 sec of lag.

- [Scala Users](https://contributors.scala-lang.org/t/asking-for-your-feedback-on-sbt-scala-center-announcement/738/41?u=lihaoyi)

These comments aren't always objective, peer-reviewed analyses. Judgements are
subjective, facts are wrong, people's understanding is incomplete.

But the confusion and frustration are real.

SBT has a lot of superficial issues. These are things that are problematic for
users, but are only skin deep, and can be easily fixed. Things like:

- The difference between `run in (Test, myproject)` and `myproject/test:run`
- Leaking implementation details like `Def.task{...}.taskValue`
  (https://github.com/sbt/sbt/pull/2943)
- Syntactic bugs, like `lazy val (js, jvm) = crossProject()` not being allowed
- The unusual evaluation order of `foo.value` expressions

These superficial syntactic problems are easy to fix, and many are already on
their way out. SBT has already deprecated the zoo of `<+=` `<++=` `<<=` `<<+=`
operators in favor of just `:=`, and is
[unifying](https://github.com/sbt/sbt/pull/3434) the `foo in bar` and `foo/bar`
syntax. SBT's [blank line](https://stackoverflow.com/q/21780787/871202) problem
is now thankfully ancient history.

However, it turns out that for all the complaints about SBT syntax, they are
probably the *least* of SBT's problems as a build tool.

This post will dig deep into what, exactly, is it that makes SBT confusing,
complicated and slow. From that analysis, you should come away with a clear
picture of exactly what features and design-choices make SBT pick up the bad
reputation that it has, and how a "better build tool" (whether a future version
of SBT, or an entirely new tool) might work to avoid these pitfalls.


## Unwanted Data Model

At its core, SBT is built around a grid of tasks: every task can be applied to
every subproject. Overall, this results in a 2D grid of possible tasks, most of
which are invalid.

### The 2D Grid

For example, consider the set of valid task/subproject combinations in a project
I've worked on: a typical Scala.js client/server webapp with a shared core
library.

|           | coreJS | coreJVM | client | server |
|:----------|:-------|:--------|:-------|:-------|
| compile   | Y      | Y       | Y      | Y      |
| run       |        |         |        | Y      |
| test      | Y      | Y       | Y      | Y      |
| console   |        | Y       |        | Y      |
| assembly  |        |         |        | Y      |
| fastOptJS |        |         | Y      |        |
| assembly  |        |         |        | Y      |
| reStart   |        |         |        | Y      |

Above, the cells with `Y` indicate the ones which are valid and meaningful.
`fastOptJS` on the `coreJVM` project is obviously invalid, but there are others
which are technically valid but meaningless: I *never* want to run `assembly` on
the `coreJVM` subproject, only on the `server` subproject which needs to be
deployed, and SBT-revolver's `reStart` command is similarly useless on the
`coreJVM` subproject.

Thus it is difficult to know what tasks are defined for what subprojects! SBT
lets you combine them all in any combination, and only later on does it error
out if a task is not defined.

What ends up happening in practice is that any user or team using SBT ends up
making their own ad-hoc whitelist of the task/subproject combinations that are
meaningful. *"`amm/test:run` is how you start a test console, `amm/test:assembly`
gives you an executable jar you can play with, everything else under `amm/test`
probably doesn't work so don't touch it."*

Apart from violating the rule of "make illegal states unrepresentable", and
causing people to have to structure/document which cells in the 2D grid are
valid/meaningful for their project, the fact the settings/tasks are in a flat
namespace results in silly name collisions, such as
https://github.com/scala-js/scala-js/issues/1050.

SBT's data model is actually not a 2D grid, but a 4D (4 dimensional!) grid, as
described [here](http://eed3si9n.com/4th-dimension-with-sbt-013). That does not
affect the points above, except meaning that SBT's data model even more
confusing than the above description makes it seem, with even more of the 4D
grid being filled with invalid or meaningless cells.

### The Hierarchy

A data model that would better fit most builds is not a 4-dimensional grid, but
a hierarchy.

Fundamentally, any software project is laid out in a hierarchy:

- Files grouped together into folders
- Classes grouped together in nested packages
- Methods grouped into classes,
- Control-flow structures are nested and grouped together in methods

There are some niche cases where you are cross-building or cross-testing your
project against a matrix, and you really do want to model it as a
multi-dimensional grid. But those are the exception, not the norm.

Even some cases where you may think of modelling things as a grid can also be
modelled as a hierarchy without any difficulty. For example, the fact that you
can `compile` and `test` any subproject within your build does form a nice grid:

|         | coreJS | coreJVM | client | server |
|:--------|:-------|:--------|:-------|:-------|
| compile | Y      | Y       | Y      | Y      |
| test    | Y      | Y       | Y      | Y      |


But it forms a nice hierarchy too:

```
core/
    jvm/src/
        main/
        test/
    js/src/
        main/
        test/
client/src/
    main/
    test/
server/src/
    main/
    test/
        
```

This isn't a hypothetical arrangement: you probably are already organizing
things in such a hierarchy on disk! If it's good enough for the filesystem, it's
good enough for a build tool. And of course, there are countless filesystem
hierarchies that fit very poorly into a grid-based data-model, resulting in a
mostly-invalid grid such as the one shown above.

A heterogenous hierarchy would allow you to define exactly which parts of your
build have which operations, avoiding the proliferation of invalid actions, as
well as avoiding name collisions caused by SBT's flat namespace.

[Bazel](https://bazel.build/) does the right thing here: a build-task hierarchy
that matches precisely the folder-structure of the filesystem, where different
folders have different operations available depending on what the folder
contains. No poking around a huge 2D task/subproject grid filled with invalid or
meaningless cells.

--------------------------------------------------------------------------------

There is nothing inherently wrong with a multi-dimensional grid. In some cases,
such as modelling a chess game, a 2D (or 4D) grid is exactly what you want.

Building software projects is not such a case.

## Too Many Layers of Interpretation

SBT has an unusual execution model. It basically has three layers:

- **Layer 1**: Run Scala code to construct the `Seq[Setting]` that gets passed
  to each subproject
- **Layer 2**: Interpret `Seq[Setting]`, with all the `:=` and `++=` operations,
  to create Task graph
- **Layer 3**: Interpret Task Graph to actually do the work, scheduling the work
  in a topological order, and caching where necessary

These are totally separate phases in the execution of an SBT build: each layer
runs sequentially after the previous layer completes, with no overlap between
them.

Each of these layers is pretty deep and complicated:

- Trying to trace through the **Layer 1** Scala code that is constructing the
  `Seq[Setting]` for a subproject is difficult

- Following the interpretation of **Layer 2**'s settings to figure out what ends
  up being bound to each key to is difficult

- Figuring how in **Layer 3** SBT is going to execute the task you told it to
  execute, along with all its dependencies, is difficult

Effectively, when writing SBT code you need to be aware of all three layers of
interpretation in your head, each of which *alone* is already non-trivial to
understand. While each layer in isolation isn't terribly hard to follow, trying
to follow all three at the same time is definitely extremely challenging.

It should thus be no surprise that SBT is harder to follow than "straight-line"
code that runs top-to-bottom and just does the things it needs to do directly.

### Other libraries with multiple layers of interpretation

SBT is not the only Scala library which has multiple layers of interpretation. I
have personally written some too, including:

- [FastParse](https://github.com/lihaoyi/fastparse), which asks you to construct
  a `Parser[T]` structure, which is then interpreted to parse some input

- [Scalatags](https://github.com/lihaoyi/scalatags), which asks you to construct
  a `Frag` structure, which is then interpreted to render HTML

However, in both these cases there are only two layers of interpretation. And in
both cases one of the layers is thin enough you don't have to worry about it:

- In Scalatags, **Layer 1** may contain pretty complex Scala code that you use
  to construct the HTML structure, but **Layer 2** is simple: basically taking
  the HTML data structure you constructed, and spitting it out as a string.

- FastParse's has the opposite profile: FastParse's **Layer 2** is reasonably
  complex: interpreting the `Parser[T]` object to parse some input, with
  control-flow and a program-counter. But FastParse's **Layer 1** is simple: you
  generally do not use "full Scala" when constructing your `Parser[T]` objects,
  even though in theory you can.

Both Scalatags and Fastparse, despite having a two-layered execution model, are
simple enough that they can be migrated to a one-layer execution model with no
user-visible changes ([here](https://github.com/lihaoyi/scalatags/pull/166) and
[here](https://github.com/lihaoyi/fasterparser/blob/c4ad8ad7dcc7acce68679e2088fef326b78f3094/src/test/scala/fasterparser/JsonTests.scala)).
You basically never find yourself debugging problems in Scalatag's **Layer 2**
execution or Fastparse's **Layer 1**.

There are many other libraries based around multiple layers of interpretation:
libraries structured monadically tend to construct a `FooMonad[T]` object, which
is then interpreted (e.g. using Scalaz's `.unsafePerformSync`) to do the actual
work. However, there are very few libraries with more than two layers of
interpretation, and I have not seen any other libraries with **three** layers of
interpretation, each as deep and complicated as SBT's layers are!

### Semantic Duplication

SBT's **Layer 2** interpreter is basically a full-fledged programming language:
it walks a sequence of instructions, manipulating, mutating and reassigning
variables (the `taskKey`s and `settingKey`s). This results in several concepts
in **Layer 2** which look strikingly similar to concepts in **Layer 1**
(directly executing Scala code to construct the `Seq[Setting]`):

- SBT's `mySettingKey` vs Scala's `val myValue`
- SBT's `foo := bar` assignment vs Scala's `val foo = bar`
- SBT's `foo += 1` mutation vs Scala's `var foo = ...; foo += 1`
- SBT's scope-delegation (letting you fall back to broader scopes) vs Scala's
  lexical scope-delegation (which also lets you access values defined in broader
  scopes)

Fundamentally, understanding the concepts of scoping, mutation, and overriding
in any programming language is difficult. SBT's flavor of these concepts is
similar enough to normal Scala to be just as difficult to pick up, but also
different enough that you cannot leverage your existing Scala (or any other
programming language) experience to help you. The fact that they are
similar-but-different adds additional confusion when a programmer mistakes one
for the other.

Overall, the complexity of **Layer 2**'s evaluation semantics and its
not-quite-the-same similarity to normal Scala semantics adds a huge load to
anyone trying to understand how SBT works.

### Lack of tooling support

Scala has pretty good developer tools: IntelliJ, ScalaIDE and ENSIME let you
jump to definition, see where values are being used, and generally navigate
around the call-graph and dependency-graph of a Scala program.

However, such tools only apply to **Layer 1** in the SBT interepretation stack,
since that's the level which is plain Scala. If you have problems in **Layer 2**
(e.g. settings not being bound to what they should be) or **Layer 3** (things
executing redundantly, or not being cached properly) tools like
IntelliJ/ScalaIDE/ENSIME are of no use at all.

For example, one common issue with SBT is *you are not sure what action a task
is bound to*. You may look at:

```scala
libraryDependencies += "com.lihaoyi" %% "utest" % "0.6.0"
```

And wonder: *if I'm `+=`ing to add something to `libraryDependencies`, can I jump
to the definition of `libraryDependencies` to see what was there before?*

It turns out, you cannot.

Jumping to `libraryDependencies` just brings you to:

```scala
val libraryDependencies = settingKey[Seq[ModuleID]]("Declares managed dependencies.")
```

Which tells you nothing about the value of `libraryDependencies` before the `+=`

The problem here is that the meaning of `libraryDependencies +=` isn't in
**Layer 1** (plain Scala) but in **Layer 2** (the `Seq[Setting]` interpreter).
Thus IntelliJ would bring you to the definition of `libraryDependencies` in
**Layer 1**, but it has no idea about **Layer 2**, which is the layer at which
"what is bound to `libraryDependencies`" is actually computed!

Once you are in **Layer 2** (or **Layer 3**), you are no longer writing Scala
code. All your Scala tooling no longer applies:

- Your Scala editors can't jump to where your **Layer 2** `settingKey`
  or `taskKey` values were assigned,
- Your Scala debuggers can't step through the `Seq[Setting]`
  interpreter
- You can't put `println`s in between `Setting`s to see what a `settingKey` or
  `taskKey` is bound to at a particular point in the **Layer 2** execution.

You basically have a full programming language, being interpreted within the
outer Scala program, with tons of redundant concepts, strange inter-layer
interactions, and zero tooling. This is often known as an
[Inner Platform](https://en.wikipedia.org/wiki/Inner-platform_effect).


--------------------------------------------------------------------------------

SBT is not the only library that uses a multi-layer execution model, but it is
probably the library I've seen with the deepest layers, and the most number of
them (3). Rather than writing Scala code to "do stuff", you end up programming a
meta-interpreter running Scala along with two strange, un-specced programming
languages with no tool support, all just so you can zip your classfiles into a
jar.

There is a lesson to be learned from this: if you want to use the interpreter
pattern in your library, minimize the interpretation layers in your execution
model. As few as possible, as simple as possible, push as much logic as possible
into one of the layers (e.g. **Layer 1** for Scalatags, **Layer 2** for
FastParse) so that a programmer can focus on the logic of that layer without
programming in multiple layers at once.

It is not clear to me why SBT *needs* a 3-layer meta-interpreter, when most
libraries I've worked with have at most 2 much-simpler layers. I'm not convinced
that the "build tool" domain is special enough to *need* 3 layers of
flexibility: any future build-tools should take special care to minimize this
layered interpretation complexity.

## Global, Mutable Execution Model

### Mutable Execution

SBT's execution model isn't just multi-layered, but also mutable!

Interpretation **Layer 2**, where SBT interprets the `Seq[Setting]` to create a
Task graph, is mutable:

- `taskKey`s and `settingKey`s can be bound to tasks/values (using `:=`) at any
  point in the `Seq`, and not at point of definition (since they're defined in
  **Layer 1**, before **Layer 2** even starts)
- Later `Setting`s in the `Seq` can append to `taskKey`s or `settingKey`s that
  were assigned earlier in the `Seq`, using `+=` or `++=`
- Later `Setting`s in the `Seq` can even over-write earlier bindings, using `:=`

This is not a theoretical concern: part of the reason that jump-to-definition on
a key like `libraryDependencies` doesn't give you anything useful is that the
definition of the `libraryDependencies` `settingKey` we saw earlier:

```scala
val libraryDependencies = settingKey[Seq[ModuleID]]("Declares managed dependencies.")
```


Is (apart from being in a different layer of interpretation), not unlike a
mutable `var` declaration in Scala, which "someone" will assign "later":

```scala
// Declares managed dependencies.
var libraryDependencies: Seq[ModuleID]] = null
```

If you programmed all the time using `var foo: Foo = null`, you would get a
jump-to-definition experience as useless as the experience is jumping around SBT
code. Most Scala programmers don't, because programming using mutable `var foo:
Foo = null` declarations is confusing, fragile, and makes it harder for you to
find where values *actually* are coming from. Rather than just jumping to the
definition, you have to scan all the code between "definition" and "use site" to
find the line of code which is actually initializing the damn thing.

In fact, that's exactly how it feels like trying to debug SBT builds!

### Global Namespacing

Apart from being *mutable*, keys defined in SBT are *global*: you have to ensure
your key has a unique name, across the universe of all possible keys that are
defined, or may be defined in future. Naturally, this is impossible, but you
just do your best: perhaps you
[prefix all your keys with your project name](https://github.com/scala-js/scala-js/pull/1053),
and hope nobody has a project of the same name. Or you just do nothing, hope for
the best, and if an error like https://github.com/scala-js/scala-js/issues/1050
pops up then someone has to go and rename their key.

This should be familiar to anyone who's ever programmed in a environment with a
single global namespace: writing Bash scripts, working in C, the "prefix your
names to try and avoid collisions" is a textbook solution to an old problem. It
turns out that the same problem, and same solution, applies to SBT as well.

--------------------------------------------------------------------------------

The fact that SBT's "pure Scala" **Layer 1** is "immutable" or "functional" is
mostly irrelevant. A lot of SBT's logic is in the **Layer 2** `Seq[Setting]`
interpreter, and so programmers will have to spend time understanding,
modifying, and debugging code running in **Layer 2**. And **Layer 2**, apart
from being an strange
[Inner Platform](https://en.wikipedia.org/wiki/Inner-platform_effect)
programming-language, turns out to be a programming language centered around
global mutable variables.

No wonder people find it confusing!

## Lack of Caching

By default, nothing in SBT is cached. This is the wrong default.

When you are building a project, you rarely ever want to do the same thing more
than once.

Build tools inherently involve doing many different things:

- Reading Protobuf definitions and generating Scala source code
- Resolving maven coordinates and fetching third-party dependencies
- Shading some third-party dependencies to avoid package conflicts
- Compiling source code into classfiles
- Zipping classfiles into jars
- Bundling Javascript files using Webpack
- Minifying the bundled Javascript using Uglify.js
- Generating a docker image containing your Jars, JS bundles, and other things

Varied as they are, these tasks have a lot in common:

- You can figure out what their dependencies are, so you can do them in the
  right order
- You can do them in parallel if they don't dpeend on each other
- Once you do them once, you can avoid doing them again unless there's new input

SBT hits the first two of these points, but fails the last one.

Caching in SBT is pretty ad-hoc:

- Different Tasks need to home-brew their own caching logic if they don't want
  to recompute unnecessarily (and make sure you don't accidentally collide with
  someone else's cache on the filesystem!)

- Often, people will not bother with caching, e.g. resulting in them
  re-generating the same source files over and over while working

- Sometimes, even when things are cached, they're only cached half-way: e.g.
  sbt-assembly task avoids most of the work building a jar if the inputs didn't
  change, but still does (and prints) a bunch of stuff when it should be doing
  absolutely nothing

- Sometimes, the caching logic will be buggy, e.g. resulting in SBT trying to
  re-resolve ivy dependencies all over again (taking forever...) even though
  nothing changed

While there are some things you do not want to cache (opening a console, running
the an final executable) the vast majority of things should be cached by
default.

Even Makefiles, primitive as they are, avoid running any command if the inputs
to that command did not change. This is table-stakes for any build tool. While
SBT makes some ad-hoc efforts around the slowest commands (to avoid becoming
totally unusable), it really falls short.

[Bazel](https://bazel.build/) does the right thing here: everything is cached by
default, handled transparently by the build tool, and neither the person
creating a command (e.g. `javac`) or the person integrating that command into
the build need to worry about it.

That is how it should be.

## Single-Process/Classpath Architecture

By default, everything is stuffed into SBT plugins that live in the same
classpath. This includes:

- Additional compilers (Scala.js, Scala-Native)
- Packagers (sbt-assembly)
- Publishing logic (sbt-sonatype, sbt-pgp)
- Dependency-resolvers (sbt-coursier)

This has always struck me as a bit strange: a build tool is meant to let you
download/compile/run arbitrary pieces of code in a managed way. The Scala.js
Optimizer, or the Coursier dependency resolver, are just arbitrary pieces of
code. Why, then, are they not just downloaded/compiled/run the same way any of
my own code is?

It turns out there's a small tradeoff here: if you want *type safe* access to
one of these tools, then you need the tool to be compiled and provided even
before you compile your own code (so your compiler can check the types).
However, what you gain in type-safe access, you lose in many other things:

- You are unable to load more than one version of such plugins at once (e.g.
  https://github.com/portable-scala/sbt-crossproject/issues/47)

- You cannot easily build such a plugin as part of your own build. You can use
  SBT's "meta-build", but pushing more logic into the meta-build means you may
  have to run a huge amount of code before you can even *look* at your main
  build!

Most of your build steps live in a dependency graph and can be
cached/built/parallelized automatically by the build tool. Presumably that's the
reason you are using a build tool in the first place! But by leaning heavily on
"plugins", you end up with a considerable amount of code which you "just need to
remember" to include before the main build is run, and can have arbitrary,
unmanaged changes on any part of the main build.

You end up with a bunch of plugins, each doing god-knows what, influencing the
main build in mysterious ways, interacting with (and possibly breaking) each
other through mechanisms you cannot even imagine.

The above description could apply just as easily to a pile of ad-hoc bash
scripts!

The correct solution here is to give up the type-safety, and make all these
extra tools part of your normal build, managed and structured like anything
else. The Scala.js optimizer could be resolved, fetched, and executed as part of
the normal execution of your build steps. The code to zip your classpath into an
uberjar, the code to resolve dependencies using Coursier, all could be just part
of the normal build.

You may want a few "structural" plugins: code you can include as part of your
build file to define common structures or subgraphs of the task graph relevant
to a particular project. But that code should be just that: defining the shape
of the task graph. All the heavy lifting should be delegated to normal build
steps.

By minimizing the amount of unstructured/unmanaged logic that goes in "plugins",
we maximize the amount of code we can manage in a structured, principled way as
part of our build, which is what build tools are all about.

## Conclusion

SBT has a lot of problems.

There is a lot of angst, complaints and confusion about SBT in the Scala
community. Many of the people complaining are coming from a position of
ignorance: the facts they provide are wrong, the problems they report have
solutions, their understanding of SBT is incomplete and incorrect.

But their confusion and frustration is real.

A lot of noise has been made about SBT having confusing superficial syntax, or
SBT being stuck on Scala 2.10, or not having docs. It is true that those are
problems. But as I see it, those are the least of the problems that SBT has.

People used to get confused and complain that SBT didn't have enough docs. Now
SBT has [tons of docs](http://www.scala-sbt.org/), yet people are still
confused.

Someone diving into SBT isn't going to realize that they're being confused by
the 4-dimensional 3-layer meta-interpretation model: they won't even know that
such a thing exists! Hence they'll associate their unhappiness with the things
they can see: the unusual syntax, the old version of Scala, the doc-site they
didn't understand.

But if we want to make things better, we cannot just be fixing the superficial
problems that are reported.

The problems I discussed in this post are deep. These aren't things you can
"just send a PR" or "just help them fix it". They are fundamental to what SBT
*is*: its data-model, its execution-model, its caching-model, its
extension-model. All of these, I have argued, are broken: if we changed all
these things, would it still be SBT, or something else entirely?

Tooling is one of the biggest pain points in the Scala community, and SBT is the
biggest hole in Scala tooling: tripping up newbies and frustrating old-timers
for years. Whether or not we fix these issues in SBT itself, or in a new tool
that can replace SBT, doesn't matter to me. But hopefully this analysis will help
build a consensus around what the real problems actually are, so we can take
steps to fix them for good.
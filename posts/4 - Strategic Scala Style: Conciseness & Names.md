"Naming things" is one of those traditionally "hard problems" in software
engineering. The Scala programming language gives you more tools than most
languages do to manage names: apart from picking alphanumeric names of
arbitrary length, you can also name things using operators, or in many cases
not names things at all using language features like `apply` or the `_`
placeholder parameter.

Howver, the fact that code ends up "too concise" is itself one of the most
common complaints leveled against the Scala programming language. How can we
pick the right balance of verbosity and conciseness, at the right times, to
ensure future maintainers of our software do not end up hating us?

-------------------------------------------------------------------------------

This post is part of a series, following an earlier post [Strategic Scala
Style: Principle of Least Power][1]. Like that post, this assumes you are
already proficient in the Scala programming language: you are already familiar
with most of its features, and how to use them. This post will thus entirely
skip-over the

> When *can* you use short names?

And instead focus on the follow-up question

> What *should* you use short names?

Similar to the earlier post, this covers a wide-range of topics, and is focused
on the "Vanilla Scala" use case. It's unlikely that everyone will agree with
every point, especially those using specialized libraries, but hopefully people
will find it broadly agreeable and be able to use it as a framework to insert
whatever team-specific guidelines or conventions they prefer.

Here's an overview of the philosophy

- **[Philosophy](#Philosophy)**
    - [Conciseness: not for Writing, but for Reading](#ConcisenessnotforWritingbutforReading)
    - [Huffman Encoding](#HuffmanEncoding)
    - [Human Languages](#HumanLanguages)
    - [Extremes](#Extremes)

And guidelines:

- **[Long Names vs Short Names](#LongNamesvsShortNames)**
    - [Wider scoped names should be Longer](#WiderscopednamesshouldbeLonger)
    - [More used names should be Shorter](#MoreusedNamesshouldbeShorter)
    - [Dangerous names should be Longer](#DangerousNamesshouldbeLonger)
    - [Names with source context should be Shorter](#NameswithSourceContextshouldbeShorter)
    - [Strongly-typed names should be Shorter](#StronglytypednamesshouldbeShorter)
- **[Degenerate Names](#DegenerateNames)**
    - [When to use operators?](#WhentouseOperators)
    - [When to name methods "apply"](#Whentonamemethodsapply)
    - [When to use the _ Underscore Argument?](#WhentousetheUnderscoreArgument)
- **[How do you judge names in a library?](#Howdoyoujudgenamesinalibrary)**
    - [If you are the library author](#Ifyouarethelibraryauthor)
    - [If you are the library user](#Ifyouarethelibraryuser)
- **[Case Studies](#CaseStudies)**
    - [Scalaz](#CaseStudyScalaz)
    - [Parser Combinators](#CaseStudyParserCombinators)
    - [HTTP](#CaseStudyHTTP)



## Philosophy

The basic approach to conciseness and boilerplate can be boiled down to the
following:

> Show programmers something they don't already know, but wants to know

This isn't as flippant as it at first seems: *knowing* something isn't
all-or-nothing, and how much you *want* to know depends on a great many
different factors! Nevertheless, while subjective, this isn't a hand-wavy
topic at all. There are concrete things that a programmer would already know,
and concrete things they would want to know.

Programmers already know:

- Things they've seen before in your codebase
- Things they've seen before in other codebases
- Facts they've picked up in previous jobs

Programmers want to know about:

- Things that affect what they're doing
- Things which they need to understand
- Things they are unfamiliar with
- Things that are especially dangerous, whether due to correctness, security,
  performance, etc.

This is not comprehensive, but it should give a sense of the things that people
do or do-not want to see. While not totally concrete, this list is already
pretty actionable! It turns out that you can make judgements based on what a
programmer would already know based on:

- Who you think the future programmers are going to be? You? Co-workers?
- What are their backgrounds?
- How long will they be working in your codebase?
- How long have they been familiarizing themselves with your *FeatureX*?

### Conciseness: not for Writing, but for Reading

People often justify boilerplate by saying "it's easy to write": autocomplete
might fill in long names, or even entire class-bodies in modern IDEs. Repeated
code is easy to copy & paste.

And that's all true.

Nevertheless, this view overlooks one important point in software engineering:
writing code is never the bottleneck!

Rather, time is spent reading code, or debugging misbehaving code. Nothing
right now makes *reading* extra code or *debugging* extra code as simple as
*generating* it. Sure, you have some amount of code-folding in IDEs and
good debuggers, but it still takes a great deal of effort to go through the
boilerplate and figure out where the bug is. That means even if you've saved
yourself effort generating it via copy & paste or IDE autocomplete, you're
simply setting yourself up for suffering later when something goes wrong
and you need to hunt down the error.

Although having more concise code lets you fit more things on one screen,
the fitting-more-things-on-one-screen is really a minor gain. The real gain
is from all the code you don't need to sift through for bugs or typos (even
long identifiers can house typos!) or understand while you're working. That
is the real gain

### Huffman Encoding

In many ways, the level of conciseness within an application should follow
a [Huffman Tree]: the most common things you want to do should be the most
concise, while the less common things should be more verbose.

This strategy maximizes the amount of convenience, since you'll spend most
of your time doing the more common things, and those will be concise and
convenient. For the things used less often, it's less frustrating to deal with
a small amount of boilerplate when you need them. Furthermore, this forces you
to make an explicit choice to use those less-used less-convenient things,
which is often good if they're less-used because they're error-prone,
inefficient or otherwise dangerous.

In the end, Huffman encoding and programming code has similar goals: the
eliminate meaningless content and convey as much *useful* information to the
reader as possible. Although that's not the *only* goal of the code you write,
it definitely is something to work towards.

### Human Languages

In fact, this reasoning doesn't just apply to
*programming* languages! People using human languages want the same thing,
which can be seen in how language evolves over time. For example, starting
from:

> Hello do you want to get dinner together?

The first thing to go is often the *Hello*, since it's basically meaningless
and doesn't affect the message. If you're more familiar with a person, you
may say:

> Do you want to get dinner together?

Or you may drop the *together* or the *Do you*, since it's usually obvious
that we're referring to the implicit *me* and *you*. They already know that,
so you leave it out:

> Want to get dinner?

Note the word *usually*! If your most common interaction was asking about
someone *preparing* dinner, rather than *buying-and-eating* it (for example,
in family that takes turns cooking) the same shortened phrase would mean
something totally different. In such a case, if buying-and-eating dinner
together was un-usual, you would expect someone to spell it out in more words.
This applies regardless of whether the words are said in person, over online
chat, email, or whatever.

As the familiarity increases, you drop even more from the phrase, down to

> Dinner?

And finally, if there is sufficient context (for example it's a scheduled
thing, or has already been agreed) you may say nothing at all! For example,

> \*poke\*

Would be enough for the other party to know "oh you want to go get dinner
together"

As you can see, having varying levels of conciseness isn't a property unique
to programming languages. As the *computer* doesn't care how concise you write
your code as long as it runs, the choice is then left to the future maintainer
of your code: what does *he or she* not already know? This takes judgement and
context.

### Extremes

Like in many things, in the spectrum between ultimate-conciseness and
ultimate-boilerplate, the ideal is somewhere in the middle. If you're not
convinced of that in principle, here are two examples.


First, here is some Hoon source code from the [Urbit] project:

[Urbit]: https://github.com/urbit/arvo/blob/ca0fc9e70456f27eb85dac312ab7809989df2fb1/arvo/jael.hoon#L85-L95

```
  ++  bu-ajar
    |=  pax=path
    ^+  +>
    ?~  pax  +>.$
    %=    $
      pax  t.pax
      xap  [i.pax xap]
      fes  [sef fes]
      sef  (fall (~(get by kin.sef) i.pax) *safe)
    ==
  --
```

This is written in a language you don't understand, but even for an unknown
language, this code is remarkably cryptic. And if you follow the link above,
you will see that this is not an unusual piece of code! In fact the entire
project has (or used to have) a policy of "All names shall be three-characters,
max".

Given how short the names are, it does not give a maintenance programmer any
hints to latch onto when trying to figure out the code. Of course, you can
probably figure out what everything is by grepping for their definitions and
seeing how they're used in other contexts, so it's not totally hopeless. In
fact, good IDEs can help with this, easily letting you find usages and
definitions. Nevertheless, figuring out how the code works is harder than it
needs to be!

At the other extreme, is code which has so much ceremony you don't know where
the real logic is. Below is a snippet from the
[Oracle docs](https://docs.oracle.com/javase/tutorial/uiswing/events/generalrules.html)
showing how to add an event handler in Java:

```java
someObject.addMouseListener(new MouseAdapter() {
    public void mouseClicked(MouseEvent e) {
        System.out.println("hello");
    }
});
```

Here we have five lines with lots and lots of code, with only a bit of logic
buried in the start and middle to do the real work.

This is also hard to read, but for the opposite reason as he Hoon example
above: there is so much *stuff* telling you what is going on, and it's all
true, but describing things I dont't care about. I don't *care* that we are
instantiating a `MouseAdapter`, or that the `mouseClicked` method is `public`
and `void`!

Again, it is possibly to get good at scanning this code, and mentally
collapsing it down into it's "true" meaning: on click, print something. That
is such a mechanical operation that IDEs like IntelliJ even do it for you!

Thus, we can see that having too little boilerplate and too much boilerplate
are both problematic, but for different reasons. The rest of this post aims
to help you find a happy medium between these extremes.

## Long Names vs Short Names

Here's the at-a-glance guidelines for creating identifiers. Whether you are
trying to name a class, package, function, method or variable, the same
principles apply.

How to decide the length of a name:

- [Wider scoped names should be Longer](#WiderscopednamesshouldbeLonger)
- [More used names should be Shorter](#MoreusedNamesshouldbeShorter)
- [Dangerous names should be Longer](#DangerousNamesshouldbeLonger)
- [Names with source context should be Shorter](#NameswithSourceContextshouldbeShorter)
- [Strongly-typed names should be Shorter](#StronglytypednamesshouldbeShorter)

The first and second point are valid separately, but can be combined to give
a metric of *usage density*: how dense are your uses of an API? 5 times in 1
line? 1/5 lines? 1/50? 1/5000? The denser your uses of an API, the shorter
the name should be: methods used multiple-times-per-line should consider
[using operators](#WhentouseOperators), whereas names used once per 5000 lines
can afford to be a bit longer and more verbose.

One interesting observation to make here is the second point: that how you name
something is based not just on what that thing is, but how you expect it to be
used! That makes perfect sense when you consider the [Philosophy](#Philosophy)
behind these guidelines:

- A name used more gets more familiar, and thus reduces the confusion caused
  by conciseness
- A name used more saves more boilerplate, since the reduction in size is
  multiplied by the number of usages

Nevertheless, it's worth calling out, and we'll return to this point in the
[last section of this post](#Wheredoyoucountusagepatterns).

Note that this section *totally ignores* what you put into the name! A short
name which a maintenance programmer already understands can be more helpful
than a long name which he doesn't. Nevertheless, that's a separate topic for
discussion: for now I will only discuss the *length* of the name, treating
the understandability of its contents as a black-box.

### Wider-scoped names should be Longer

Why is the name `i` ok in this example:

```scala
object Foo{
  def main(args: Array[String]) = {
    if (...){
      var i = 0
      while(i < 10) i = i * 2
      println(i)
    }
  }
}
```
But not in this one?

```scala
// Foo.scala
object Foo{
  var i = 0
}
```
```scala
// Bar.scala
object Bar{
  def main(args: Array[String]) = {
    ... i ...
  }
}
```
The answer is that in the first case, the name `i` is only usable in a tiny
part of your program. Thus, while it is short and meaningless, at a glance it
is relatively easy to see where it comes from. Thus, a programmer looking at
`i` already knows everything there is to know about it: you don't need to teach
them! But in the second example, `i` is used widely, in totally different
files. It's probably worth giving it a slightly longer name to make it less
mysterious, e.g.


```scala
// Foo.scala
object Foo{
  var usageCount = 0
}
```
```scala
// Bar.scala
object Bar{
  def main(args: Array[String]) = {
    ... usageCount ...
  }
}
```

Would be better.

Furthermore, note that this doesn't just tell you to avoid global variables
with short names, but *local* variables too, if the *local* scope is very
large! e.g. if `i` is used in a 1000 line method:


```scala
object Foo{
  def main(args: Array[String]) = {
    if (...){
      var i = 0

      ...
      ...
      ...
      ...
      ...
      ...
      ...
      ...
      ...
      ...
      300 lines
      ...
      ...
      ...
      ...
      ...
      ...
      ...
      ...
      ...
      ...
      while(i < 10) {
        ...
        ...
        ...
        ...
        ...
        100 lines
        ...
        ...
        ...
        ...
        ...
        val temp = i * 2
        ...
        ...
        ...
        ...
        ...
        100 lines
        ...
        ...
        ...
        ...
        ...
        i = temp
        ...
        ...
        ...
        ...
        ...
        100 lines
        ...
        ...
        ...
        ...
        ...
      }
      ...
      ...
      ...
      ...
      ...
      200 lines
      ...
      ...
      ...
      ...
      ...
      println(i)

      ...
      ...
      ...
      ...
      ...
      200 lines
      ...
      ...
      ...
      ...
      ...
    }
  }
}
```

It would be well worth while giving `i` a longer name!

### More-used Names should be Shorter

`println` is an ok name in this example:

```scala
object Foo{
  println(1)
}
```

But `init` is a poor name in *this* example:

```scala
// Foo.scala
object Foo{
  def cacheIt() = {
    ...
  }
}
```
```scala
// Bar.scala
import Foo._
object Bar{
  cacheIt() // Only used once
}
```

Why?

Because `println` is a very heavily used name, and so a future maintainer
could be expected to know what it means. In this case, part of the reason
it's widely used is because it's in the standard library, but even your own
methods could achieve this status within a project if they're used a lot, all
over the place.

On the other hand, `cacheIt` is only ever going to be used in one or a
few places. Thus, while it's about the same length is `println`, someone
looking later is much less likely to be familiar. Thus it's worth giving it a
slightly more verbose name:

```scala
// Foo.scala
object Foo{
  def initializeCache() = {
    ...
  }
}
```
```scala
// Bar.scala
import Foo._
object Bar{
  initializeCache() // Only used once
}
```

Or using it fully-qualified via

```scala
// Foo.scala
object Foo{
  def init() = {
    ...
  }
}
```
```scala
// Bar.scala

object Bar{
  Foo.init() // Only used once
}
```

### Dangerous Names should be Longer

This is bad:

```scala
object Foo{
  def loadDataFieldsFromOfflineCache() = {
    ...
  }
}
```

Given what it's doing, that's probably too long. Maybe call it
`loadCache` or something. On the other hand, if it's doing something like


```scala
object Foo{
  def dropTablesInProductionDatabase() = {
    ...
  }
}
```

In which case, it's ok for it to be that long. It's dangerous! In fact, making
it short is probably a bad idea:

```scala
object Foo{
  def drop() = {
   // Drops all your databases in the production environment
   ...
  }
}
```

This is *not* the kind of thing you want to miss during a code-review and
accidentally call at runtime!

While you might
not worry about someone accidentally calling `loadCache` at the wrong time, you
very much would *not* want someone calling a tersely-named `dropProd` function
at the wrong time! In fact, for something this dangerous it may even be worth
going through a bit more ceremony:
```scala
object Foo{
  object Unsafe{
    def dropTablesInProductionDatabase(iKnowWhatImDoing: Boolean = false) = {
      assert(
        iKnowWhatImDoing,
        "Are you sure you want to drop the tables in production???"
      )
      ...
    }
  }
}
```

And forcing people to call it with
`Foo.Unsafe.dropTablesInProduction(iKnowWhatImDoing=true)`. After all, you
shouldn't be doing this often, and when you do you should be double-sure you
actually want to do it!

This kind of reasoning applies in many other cases too. For example, in Scala
this is part of the standard library, but probably bad:

```scala
val vec: Vector[T] = ...
val list: List[T] = ...

println(vec(12)) // 12th element
println(list(12)) // 12th element
```

In a `Vector`, indexing with an integer is fast and expected. With a `List`,
it can take up to `O(n)` time to perform that index! Thus indexing on a `List`
is somewhat more "dangerous" than indexing on a `Vector`: you could easily
accidentally end up in quadratic performance, or worse. Better would be if
you called them via:


```scala
val vec: Vector[T] = ...
val list: List[T] = ...

println(vec(12)) // 12th element
println(list.slow(12)) // 12th element
```

That way, if you are using a `List`, you can be pretty-sure you won't end up
using a "slow" operation unless you really want to. However, *if* you actually
do really want to, they're all these for you to use under the `.slow` prefix.
That way you trade off a tiny bit of convenience for a lot of protection
against accidentally doing the "dangerous" thing.

In the first case we defined "dangerous" thing as *deleting production
databases*, while in the second we defined it as *O(n) performance you didn't
expect*, but the general principle applies in many places. If it's likely that
a programmer could do something by accident, but sometimes it still needs to be
done, provide it but under a longer, slightly-clunkier name so they have to
choose to use it consciously.

### Names with Source-Context should be Shorter

This is probably not ok:

```scala
package haoyi
case class Str()
case class Obj()
...
```

The names `Str` and `Obj` clearly mean `String` and `Object`, but what are they
*for*? Let's assume the `package haoyi` tells you nothing. On the other hand,
if we made them longer to instead be

```scala
package haoyi
case class JsonStr()
case class JsonObj()
...
```

That would tell us something about it! Or better:

```scala
package haoyi
object Json{
  case class Str()
  case class Obj()
  ...
}
```

Both of these are better, though I prefer the second.

Why are they both better, even though the last one has names as short as the
first? The answer is that where a name "lives" is just as important as what the
name "is" when someone comes along later trying to figure things out. If your
name is defined in a package, class, or object, *even if it is globally
available*, its source context tells you a lot about.

Both the second and third examples above convey the same information: That the
`Str` and `Obj` classes are somehow related to `Json`. The second via longer
names, and the third by putting the names somewhere where the context is
obvious.

In fact, this is bad:


```scala
package haoyi.json
object Json{
  case class JsonStr()
  case class JsonObj()
  ...
}
```

Someone coming along later already knows it's in the `haoyi.json` package and
has something to do with JSON, no need to say it over and over! Just once will
do, and it doesn't matter if the idea of *Json* is part of the name itself or
part of its source context.


### Strongly-typed names should be Shorter

This is bad
```scala
object Foo{
  def rotte(): Unit
}
```

But this is ok!

```scala
object Foo{
  def tpose[T, N[_] <: Seq[_], M[_] <: Seq[_]](in: M[N[T]]): N[M[T]]
}
```

In both cases, the name `tpose` is short and cryptic. However, in the first
example you have entirely no clue what it does! In the second example, you can
guess and quite likely be correct.

In general, names are not the only thing that tell you what something is. Other
information is available too, and you should control how long your name is to
compensate for a lack or a plenty of information from other sources, such as
the types. In fact, I'd argue that this is bad:

```scala
object Foo{
  def transposeNestedTraversables[T, N[_] <: Seq[_], M[_] <: Seq[_]](in: M[N[T]]): N[M[T]]
}
```

While this is ok:

```scala
def rotateApplicationLogFileOnDisk(): Unit
```

Why? Because in `transposeNestedTraversables` the second-half name is redundant
since we already know that it takes nested traversables. But in
`rotateApplicationLogFileOnDisk` the types tell you nothing, so names can be
longer.

Everything in Scala is "strongly typed", but some things are more
strongly-typed than others. Functions which take no arguments, return no
result (`Unit`)`, take their input from global-state and produce their output
via side-effects are the least "strongly typed" of Scala functions. Variables
labelled as `Any`, or functions taking `Any` or returning `Any` are the least
"strongly typed" of Scala variables. Avoid using short names for these, since
the type is weak enough that the maintenance programmer will lean more heavily
on the name when trying to figure out what it's for.

## Degenerate Names

Scala provides multiple ways to say "I don't want to give something a name".

- [You can name things using meaningless operators](#WhentouseOperators)
- [Name methods `apply` and call them via `foo(bar)` instead of `foo.methodName(bar)`](#Whentonamemethodsapply)
- [and use `_` parameter, e.g. `foo.map(_+1)` instead of `foo.map(x => x+1)`](#WhentousetheUnderscoreArgument)

It turns out, there are valid use cases for all of these! As degenerate "short
names", using these Scala language features is basically governed by the same
rules as any other name described in [Long Names vs Short Names](#LongNamesvsShortNames).
Thus, when things are [narrowly scoped](#WiderscopednamesshouldbeLonger), are
[heavily used](#MoreusedNamesshouldbeShorter), or have lots of
[source context](#NameswithSourceContextshouldbeShorter) telling you what they
do, using these features is totally fine. The sections below will elaborate on
individual features.

### When to use Operators?

For now, I am treating the operators as a black-box, as I did in the section on
[Long Names vs Short Names](#LongNamesvsShortNames), and ignoring all the soft
considerations e.g. "oh this operator reminds the user of this other
mathematical operation" considerations which can be a whole separate post.
Many operators have essentially no inherent meaning to begin with, and even
those that do often lose it when faced with a novice audience. e.g. all the
advanced mathematical symbols, now being used by people without a math degree.

Given that premise, this is probably bad

```scala
def <-+(arg: ...) = ...
```

Unless you're using `<-+` in a whole lot of places, and a maintainer would have
time to familiarize with it. On the other hand, it's not really any worse than

```scala
def v(arg: ...) = ...
```

Which is also bad, unless you're planning on using `v` in a whole lot of
places.

While in the Scala community operator names often get a bad rap, it's not
really the fact that they're operators that's the problem. Even in crazy
libraries with well-known too-many-operator-problems like SBT, replacing
the operators with non-operators wouldn't really make things better. Similarly,
while operators are hard to google, short names like `do` or `v` or `x` would
be equally hard to google!

Although operators aren't worse than really-short names, you still shouldn't
[go crazy with operators](http://www.flotsam.nl/dispatch-periodic-table.html).
Operators are short names, and you should use them - or not use
them - as you would very short alphanumeric names, following the guidelines
above.

Since operators are just really-short names, they should be used in the same
places really-short names would be used according to the logic in [Long
Names vs Short Names)(#LongNamesvsShortNames).

- [Wider-scoped names should be Longer](#WiderscopednamesshouldbeLonger), and
  operators should be avoided in scattered, globally available helpers. Prefer
  to use them for names used in specific, narrow-ish contexts so that people

- [More-used Names should be Shorter](#MoreusedNamesshouldbeShorter), and
  operators are more justified when they are used a *lot*. If you find yourself
  using a name or calling a method *multiple times per line*, over a large
  number of lines, it could be worth making the name an operator.

- [Dangerous Names should be Longer](#DangerousNamesshouldbeLonger), and you
  should never use an operator for something like "format hard disk" or
  "drop databases". Obviously, almost all functionality can be "dangerous" when
  used badly, but there is always functionality which are *inherently*
  dangerous, even when used correctly. Avoid naming these using operators.

- [Names with Source-Context should be Shorter](#NameswithSourceContextshouldbeShorter),
  and operators are acceptable in the case where someone "else" already tells
  you what's going on. For example,
  [Ammonite-Ops](http://lihaoyi.github.io/Ammonite/#Ammonite-Ops) uses the `!`
  operator to mean `Function1#apply`, which is slightly strange but acceptable
  because when you are calling `!`, you always have the left-hand-side function
  that tells you what it's doing: `ls!`, `rm!`, etc., so there shouldn't be
  much confusion.

- [Strongly-typed names should be Shorter](StronglytypednamesshouldbeShorter),
  so avoid using operators with things that deal with `Any` or `Unit`, but
  feel more free to use them in more strongly-typed code where you can be
  confident the compiler can catch screw-ups.


### When to name methods "apply"?

In Scala, you can define an `apply` method on an object that lets you use an
object "directly" as if it was a function:

```scala
object Foo{
  def apply(x: Int) = x + 1
}

Foo(2) // 3
```

This is already heavily used in the Scala standard library and in other places.
For example, every collection uses `Collection.apply` as a factory method:
`List(1, 2, 3)` is `List.apply(1, 2, 3)`, you have `Vector("hello", "world")`,
`Map(1 -> 2, 3 -> 4)`, and even non-collections e.g. `Future{...}`/
`Future.apply{...}` is how you create an asynchronous task.

In general, defining an `apply` method is basically like defining an ultimately
short name: at *zero* characters it is the most convenient, but also provides
*zero* information to a maintainer, who will have to rely on things like types
and source-context to figure out what its for. Thus, the same considerations
apply to "zero"-length `apply` methods as to any other short name:

- [Wider-scoped names should be Longer](#WiderscopednamesshouldbeLonger), this
  equally applies to `apply` which is often defined on globally-available
  companion objects. Thus, given that global names should be long, this should
  discourage you from defining an `apply` method on a companion object unless
  the following considerations must be strong enough to outweigh this
  discouragement.

- [More-used Names should be Shorter](#MoreusedNamesshouldbeShorter): you
  should be using the `apply` method a *lot*, more than any other method on the
  object it's defined on. For example, `List.apply` to construct a `List(1, 2)`
  is definitely the most heavily used method on the `List` companion object.
  Similarly, `myArray.apply` to look up an index in an array `myArray(123)` is
  definitely the most-used operation on `Array` instances.

- [Dangerous Names should be Longer](#DangerousNamesshouldbeLonger), and you
  should never write an `apply` method someone could call "by accident".
  Having a `myList.apply` to look up elements in a list is inefficient and
  not commonly used, and as described in the section on
  [Dangerous Names](#DangerousNamesshouldbeLonger) it was probably a mistake to
  make that the `apply` method on the `List` type. If an operation is risky,
  don't stuff it under `apply`

- [Names with Source-Context should be Shorter](#NameswithSourceContextshouldbeShorter),
  and usages of an `apply` method always have the source-context of who-ever
  you are calling `apply` on. For example, if you have a `Parser` class with
  a single interesting `.parse` method, you could around calling
  `myParser.parse("foo")` everywhere or you could equally decide to call the
  method `myParser.apply` and call it via `myParser("foo")`. This isn't a
  hard-and-fast rule, and many people are used to having single-abstract-method
  interfaces since that's how things have worked in Java for 25 years, but
  it's an option to consider when your class/object really-truly has a single
  "obvious" operation to use that should be privileged over others.

- [Strongly-typed names should be Shorter](#StronglytypednamesshouldbeShorter),
  so if your method is dealing with `Any`s or returning `Unit`, it probably
  shouldn't be `apply` but instead of a longer more-descriptive name.

In general, you should define an `apply` when there is a single, "obvious"
thing that an object can do, that you expect to be used much more than other
things that the object can do. Examples from the standard library include:

- `myArray(i)` being used to look up an `Array` with an index
- `myMap(k)` being used to look up a `Map` by key
- `List(...)` on the companion-object being used to construct `List`s.
- `Future(...)` on the companion-object to construct `Future`s

And examples from third-party libraries:

- [Scala.Rx](https://github.com/lihaoyi/scala.rx) uses `Rx.apply` on the
  companion to construct an `Rx`, and `myRx()` on an instance to extract
  a value from an `Rx`. Both are by-far the most common things to want to do.
- [Ammonite-Ops](http://lihaoyi.github.io/Ammonite/#Ammonite-Ops) defines a
  `read` object with a `read(...)` method to read in a file as a `String`, as
  well as `read.lines` and `read.bytes` and `read.iter` methods to read it in
  other formats. The assumption being that in most cases, you want to read in
  `String`s. Similarly, there's `ls(...)` to list files in the most common,
  basic case, and `ls.rec` and `ls.iter` for other less-used operations.



### When to use the _ Underscore Argument?

This is fine

```scala
val foo: List[Int] = ...
foo.map(_ + 1)
```
This has a name but it's pretty meaningless

```scala
val foo: List[Int] = ...
foo.map(i => i + 1)
```

This name is longer, but not any less meaningless

```scala
val foo: List[Int] = ...
foo.map(index => index + 1)
```

I mean, sure it's an `index`, but 99% of integers in your program are going
to be indexes of something at some point, so it tells you nothing you don't
already know!

This is probably an overkill
```scala
val foo: List[Int] = ...
foo.map(fooIntValue => fooIntValue + 1)
```

And this is even more verbose and just as useless
```scala
val foo: List[Int] = ...
def addOneToInt(int: Int) = int + 1
foo.map(addOneToInt)
```

In the end, there's a time and place for `_`: the ultimate short name. In many
cases, what the `_` represents is obvious, and if the programmer already
knows, you shouldn't need to keep repeating it in their face.

Like any other short name, `_` should be constrained by the same guidelines
described above: it should only be used if the scope is *very* narrow, for
something with a lot of source context around it that tells you what it is,
and which you know the static type.

Nevertheless, if your case satisfies these criterion, you shouldn't be afraid
to use it. Just like the `*poke*` dinner-call people use in [human
languages](#HumanLanguages), if the future programmer already knows enough
to know what "it" is, there's no need to belabour the point or elaborate.

## How do you judge names in a library?

So far, we've talked a lot about how you should write code based on expected
usage patterns. How if you're going to be using code more heavily you can
make more use of concise identifiers, whereas if you're going
to be using something "once in a while" you probably should fall back to more
verbose ways of doing things.

One question you might ask is: what about libraries? Those often tend to
provide a nice to use, slightly-magical interface to downstream code using
operators, but don't actually make use of the operators themselves. How do you
know if you should have this sort of magic in the interface of your library?

To me, there are multiple answers to this:

### If you are the library author

- Your library's "uses" are counted in an "expected" downstream project. If
  the library is used in a bunch of different places and there are only at most
  5 use-sites for an operator, it should be removed. If some projects use it
  heavily (10s to 100s of uses) then it's fine, even if the library itself
  never uses its own operators that much.

- Your library's test suite *should* reflect expected usages. Your test suite
  thus should make heavy enough use of your operators for them to be worth it,
  as a proxy for how much you expect downstream code to use them. If you can't
  find ways to use your operators in your test suite, I doubt anyone else can
  either.

### If you are the library user

- If *you* are the downstream project considering whether or not to use a
  library, whether the library's use of operators is good for you (or not)
  depends on how heavily *you* use the library! If a library makes heavy use
  of operators, it'll only be good for *you* if you make heavy use
  of the library.

This is a slightly surprising: whether a library is good depends on your usage
patterns, and not just on the library! But it's not entirely unexpected. After
all, you as-a-user are the one who has to familiarize yourself to the library,
and you-as-a-user are the one who gets confused when you see things you don't
understand.

That means that for a library making heavy use of operators, it
could be a pretty lousy library to use "once in a while" but simultaneously a
great library to use "heavily"! For example, as a casual user of Scalaz or
Shapeless, you may never see enough of the operator-driven APIs to familiarize
yourself, and thus may always be somewhat
uncomfortable without reaping much benefits. On the other hand, if you are
using it heavily, the discomfort will go away with familiarity and you'll get
all of the gain with none of the downsides of casual use!

This itself is an interesting message to library authors: how you design your
API should depend on how heavily you expect people to use it.

- If you expect people to use it lightly, calling one or two functions in
  random parts of their program, you should provide a "basic" API without magic
  even if it means there's some boilerplate

- If you expect people to use it heavily, either in one part of their program
  (like [FastParse](#PositiveParserCombinatorExample)) or throughout their
  program (like [Scalaz](#CaseStudyScalaz)) you should provide a convenient,
  boilerplate-free API even if it means making things shorter or using
  operators.

## Case Studies

The best way to learn is to look at examples. Especially in this kind of soft
subject, where everything is a tradeoff, examples help make things concrete
and let you make future judgements based on other people's past experiences.
Apart from the small examples scattered throughout the document above, here are
a few meaty examples that cross section-boundaries and compare multiple
competing projects trying to do the same thing.

### Case Study: Parser Combinators

Many parser-combinator libraries let the programmer define their parser using
short operator-names. Here's two examples:

#### Positive Parser Combinator Example

FastParse lets you write:

```scala
val number: P[Int] = P( CharIn('0'to'9').rep(1).!.map(_.toInt) )
val parens: P[Int] = P( "(" ~/ addSub ~ ")" )
val factor: P[Int] = P( number | parens )

val divMul: P[Int] = P( factor ~ (CharIn("*/").! ~/ factor).rep ).map(eval)
val addSub: P[Int] = P( divMul ~ (CharIn("+-").! ~/ divMul).rep ).map(eval)
val expr: P[Int]   = P( addSub ~ End )
```

Here, we have a plethora of short names! We have the `P` type and function,
the `.rep` method, the `.!` method, the `|` and `~` and `~/` methods. This
clearly uses a lot of operators. And yet, in most of the Scala community,
this is considered "ok". Why is that?

The answer comes down to the FastParse library syntax satisfying many of the
rules we specified above regarding when-to-use-short-names. In particular:

- These names are usually imported and available for very narrow parts of your
  program: basically where your parser-grammar is defined and no-where else
- Each name is used a *lot*: in this trivial example we have 4 `~`s, 3 `~/`s,
  3 `.rep`s, 3 `.!`s, and 12 `P`s. In a larger example, you get dozens of uses
- Each method is strongly-typed and returns a parser (or `P[_]`) of some
  statically-known type.

In aggregate, what does this mean? This means that a maintainer who comes
across your code in future will see crazy operators, but a very consistent
use of the same few crazy operators, constrained to a narrow part of your code.
They won't be bumping into operators in random corners of your codebase, and
after learning a relatively-small number of operators would be able to figure
out how things work.

Furthermore, even if they do get confused, the fact that the operations are all
relatively strongly-typed means that even if they fumble and make a mistake,
it's likely the compiler will tell them what went wrong with reasonable
accuracy.


#### Negative Parser Combinator Example

It's possible to mis-use operators, even in parser-combinator libraries.
Here's the list of operations from the old scala-parser-combinators project:

```scala
*[U >: T](sep: ⇒ Parser[(U, U) ⇒ U]): Parser[U]
*: Parser[List[T]]
+: Parser[List[T]]
<~[U](q: ⇒ Parser[U]): Parser[T]
>>[U](fq: (T) ⇒ Parser[U]): Parser[U]
?: Parser[Option[T]]
^?[U](f: PartialFunction[T, U]): Parser[U]
^?[U](f: PartialFunction[T, U], error: (T) ⇒ String): Parser[U]
^^[U](f: (T) ⇒ U): Parser[U]
^^^[U](v: ⇒ U): Parser[U]
andThen[A](g: (ParseResult[T]) ⇒ A): (Input) ⇒ A
append[U >: T](p0: ⇒ Parser[U]): Parser[U]
compose[A](g: (A) ⇒ Input): (A) ⇒ ParseResult[T]
filter(p: (T) ⇒ Boolean): Parser[T]
flatMap[U](f: (T) ⇒ Parser[U]): Parser[U]
into[U](fq: (T) ⇒ Parser[U]): Parser[U]
map[U](f: (T) ⇒ U): Parser[U]
named(n: String): Parser.this.type
toString(): String
withErrorMessage(msg: String): Parser[T]
withFailureMessage(msg: String): Parser[T]
withFilter(p: (T) ⇒ Boolean): Parser[T]
|[U >: T](q: ⇒ Parser[U]): Parser[U]
|||[U >: T](q0: ⇒ Parser[U]): Parser[U]
~[U](q: ⇒ Parser[U]): Parser[~[T, U]]
~![U](p: ⇒ Parser[U]): Parser[~[T, U]]
~>[U](q: ⇒ Parser[U]): Parser[U]
```

There's a lot of stuff in there! While FastParse gets by with five operators
`~` `~/` `|` `!` `?`, the old scala-parser-combinators project has fifteen!
Many of them are redundant, for example `^^` is equivalent to `map`, and `>>`
is equivalent to `flatMap`, and `*` for the `rep` function which is not listed
here because it's a function rather than a method.

While it's easy to complain, what does this mean concretely? This means that
a programmer looking into a scala-parser-combinator codebase is very likely
to bump into cryptic operators they're not familiar with, that are only used
in one or two places throughout the codebase. Things like `^^^` or `|||` or
`^?` aren't likely to be widely used, and even "common" operators like `^^` or
`>>` or `.*` or `.+` are going to be used less-than-they-could-be since they're
competing with alternative names `map` `flatMap` `rep` `rep1`. This makes these
operator names fail the criterion I set above for writing short names, and
they probably should have just fallen back to providing only-alphanumeric
methods for most of these less-used operations.

### Case Study: Scalaz

The [Scalaz](https://github.com/scalaz/scalaz) library
[uses operators heavily](http://eed3si9n.com/scalaz-cheat-sheet) for many
things:

```scala
List(1, 2) |+| List(3) assert_=== List(1, 2, 3)
```

And some people complain about the crypticness of `|+|` or `<*>`
operators. Nevertheless, although if you look at Scalaz casually you could be
scared off, there are mitigating factors that make this less sketchy than it
initially seems:

**The same operators are used for all sorts of different things**: strings,
lists, options, and countless other data structures.

```scala

@ import scalaz._, Scalaz._
import scalaz._, Scalaz._

@ List(1, 2, 3) |+| List(4, 5, 6)
res1: List[Int] = List(1, 2, 3, 4, 5, 6)

@ "one" |+| "two"
res2: String = onetwo

@ (None: Option[String]) |+| (Some("string"): Option[String])
res3: Option[String] = Some("string")

@ (Some("123"): Option[String]) |+| (Some("string"): Option[String])
res4: Option[String] = Some("123string")
```

Thus the operator,
while cryptic, would be widely used throughout your code.

**If you use Scalaz heavily you can easily find 100s of usages within your
program**, often several on the same line!

```scala
for {
  e1 <- "event 1 ok".right
  e2 <- "event 2 failed!".left[String]
  e3 <- "event 3 failed!".left[String]
} yield (e1 |+| e2 |+| e3)
```

That is definitely enough density-of-usage that the benefits from having the
operators be short and concise pays off, and giving them long names would make
your program considerably more verbose. You certainly use Scalaz's operators
much more than you would, say, [make HTTP requests](#NegativeDispatch), and so
use of short operators are much more justified.

**Scalaz's operators are all pretty strongly typed**. For example, you may
find it easy to confuse the `<*` `*>` and `<*>` operators, even after you
know what they do, since they look so similar! Nevertheless, you have the type
signatures to fall back on:

```scala
final class ApplyOps[F[_], A](self: F[A]){
  def *>[B](fb: F[B]): F[B]
  def <*[B](fb: F[B]): F[A]
  def <*>[B](fb: F[A => B]): F[B]
}
```

Which should make it pretty clear that `*>` seems to be taking a `F[A]` and
`F[B]`, throwing away the `A`, and keeping the `B`.`<*` does the
opposite, and `<*>` seems to take in some kind of transformer-function
that turns the `A` into the `B` using that function.

While strong types doesn't mean names don't matter, *does* mean you can lean
more heavily on the types than you would in a weakly-typed scenario dealing
with `Any`s and `Unit`s with side-effects. Furthermore, even if you're likely
to screw it up, the compiler will catch you:

```scala
@ (None: Option[String]) *> (Some("string"): Option[String])
res6: Option[String] = None
@ (None: Option[String]) <*> (Some("string"): Option[String])
Main.scala:2159: type mismatch;
 found   : Option[String]
 required: Option[String => ?]
(None: Option[String]) <*> (Some("string"): Option[String])
                                          ^
Compilation Failed
```

So that adds an additional safety net that makes the operator-names less
dangerous.

-------------------------------------------------------------------------------

Overall, while Scalaz gets a lot of flak for heavy use of operators, that usage
is justified under heavy usage patterns. While it has not-a-small-number of
operators and other magic (extension methods, implicit conversions, ...) you
are expected to make heavy use of these operators throughout your program, and
can expect someone working in such a codebase to become familiar with them.

Furthermore, the density of usage is enough that you often have multiple
operators on the same line, which makes it well-worth-while to keep them short!
Lastly, the fact that the operators are all pretty strongly-typed means that
even if the names are meaningless, you can lean on the types and the compiler
to help you do the right thing.

Note that a lot of this depends on *usage patterns* in *your* codebase, and
is *not* inherent to Scalaz itself! If you use Scalaz in a few places scattered
throughout your codebase, the benefits of operators go way down and the costs
(in terms of confusion due to unfamiliarity) *go up*. It's entirely reasonable
for people to have totally different opinions of the library depending on their
usage pattern, and to maximize benefit you should probably go all-in with
Scalaz, or use it not-at-all.

### Case Study: HTTP

Here, we're going to compare the APIs of two different libraries for doing
the same thing: [Scalaj-HTTP](https://github.com/scalaj/scalaj-http)
and [Databinder Dispatch](http://dispatch.databinder.net/Dispatch.html).
Although they do the same thing, they present pretty different APIs to a
developer.

#### Positive: Scalaj-HTTP

Scalaj-HTTP is a great example of a library that provides an awesome API for
newbies:

```scala
import scalaj.http._

val response = Http("http://foo.com/search").param("q","monkeys").asString
response.body
response.code
response.headers
response.cookies

Http(url).postMulti(MultiPart("photo", "headshot.png", "image/png", fileBytes)).asString
```

No cryptic operators to worry about. Everything is just an object
with methods, and the last method `.asString` or similar (there are equivalents
for binary responses) gives you your result. The methods operating on these
objects aren't particularly concise, but they're not that long either and you
can probably guess what they do just from the name, and probably be correct.

#### Negative: Dispatch

In contrast, Dispatch is a library that is
[notorious for over-using operators][dispatch-table].
Although it's gotten better in recent years, it still uses them far more than
necessary. For example, adding a POST parameter to a request involves a
mysterious `<<` operator:

```scala
val myRequest = url("http://example.com/some/path")
def myPostWithParams = myRequest << Map("key" -> "value")
def myPut = myRequest <<< myFile
```

While `<<` does save keystrokes over `.param`, whether or not its a good idea
or not comes down to our
[earlier considerations on names](#LongNamesvsShortNamesvsOperators): are the
usage patterns of Dispatch heavy enough to warrent operators for all these
methods? After all, we found that for
[Parser Combinators](#CaseStudyParserCombinators), with heavy usage operators
can be justified.

Ultimately, my judgement is that it isn't: even in the
heaviest HTTP-request-making environment I can think of - client-server
front-end web development - HTTP requests tended to happen less than once or
twice per file. You certainly make HTTP requests much less than you use
operators from [Scalaz](#CaseStudyScalaz), for exampe. My judgement is that
this usage density is less than the threshold for which I would consider
operator-powered APIs valid.

[Huffman Tree]: https://en.wikipedia.org/wiki/Huffman_coding
[dispatch-table]: http://www.flotsam.nl/dispatch-periodic-table.html
[1]: https://lihaoyi.github.io/post/StrategicScalaStylePrincipleofLeastPower.html
When programming in Scala, there are two main ways of avoiding repetition: you
can define *functions* to represent commonly-used procedures or computations,
and you can define *data-types*, e.g. using `class`es or `case class`es, to
represent commonly-used bundles of data that you tend to pass around or use
together.

Lots of people have opinions about *functions*: they should be "pure", not
too long, not be indented more than *this* much, etc. etc. etc.. Much less
has been written about what a good data-type looks like, even
though they play just as important a role in your Scala codebase. This post
will explore some of the considerations and guidelines I follow when designing
the `class`es and `case class`es that make up my Scala programs, and how you
can apply them to your own Scala code.

-------------------------------------------------------------------------------

In Scala, as in Java, everything has to live in a `class` or `object`. However,
there is a qualitative difference between the kind of `class` whose only
purpose is a place to "put code", and the kind of class which represents a
value that you instantiate and pass around your program. The latter is what
I'm going to call a *Datatype*.

Examples of data-types include:

- `java.io.File`
- `scala.Tuple2`
- `scala.concurrent.Future`
- `java.awt.Point2D`

Versus classes which aren't data-types are things such as:

- `java.lang.System`
- `scala.Predef`
- `java.lang.Math`

You instantiate data-types, pass them around, store them in values or fields
and use them later. Just as defining functions lets you avoid copy-pasting the
same logic all over your code, defining data-types lets you avoid copy-pasting
the same groups of parameters or groups of values all over your code. Rather
than passing around `xPosition: Double` and `yPosition: Double` everywhere, you
can instead pass around a `position: scala.Tuple2[Double, Double]` or
`position: java.awt.Point2D`.

Apart from the built in data-types, you will end up using data-types defined in
libraries, and find yourself defining your own as your program grows. This post
will explore some considerations and guidelines to remember when designing your
own data-types, and help you sort through the over-abundance of ways that Scala
lets you model your data. Hopefully this should help you write safer and more
maintainable Scala programs in future.

- [Opaque or Transparent?](#opaque-or-transparent)
    - [Opacity enforces Invariants](#opacity-enforces-invariants)
    - [Opacity can save on Defensiveness](#opacity-can-save-on-defensiveness)
    - [Transparency reduces Complexity](#transparency-reduces-complexity)
    - [Transparency and Opacity is a Spectrum](#transparency-and-opacity-is-a-spectrum)
- [Data-types should be Total](#data-types-should-be-total)
    - [Self Checks](#self-checks)
    - [Structural Enforcement](#structural-enforcement)
- [Data-types should be Normalized](#data-types-should-be-normalized)
    - [Manual Normalization](#manual-normalization)
    - [Automatic Normalization](#automatic-normalization)
- [Data-types should have Instant Initialization](#data-types-should-have-instant-initialization)
    - [Initializing Classes](#initializing-classes)
    - [Initializing Collections](#initializing-collections)
- [Conclusion](#conclusion)

This post is fourth in the *Strategic Scala Style* series, after [Principle
of Least Power](StrategicScalaStylePrincipleOfLeastPower.html),
[Conciseness & Names](StrategicScalaStyleConcisenessNames.html), and
[Practical Type Safety](StrategicScalaStylePracticalTypeSafety.html). Like the
others, it focuses on Vanilla Scala, without any libraries or frameworks that
would likely have their own conventions to follow. It's an intermediate-level
post, and expects the audience to be familiar with Scala's language features,
but doesn't need you to be familiar with Scala's fancy design patterns or fancy
frameworks.

Many people who have been using Scala for a while might find much of this
"obvious". Nevertheless, hopefully this post will still be useful in codifying
this "obvious" knowledge for anyone who doesn't find it obvious, and form a
basis for future discussion.
   
## Opaque or Transparent?

The first decision you have to make when designing a data type is: how opaque
should the data-type be?

- Should it hide/encapsulate it's internals?

- Should it expose all its internals for external code to see and use?

The first case corresponds to "traditional" Java-style object oriented
programming, while the second case corresponds to more a "functional" Scala
style. Both cases have their uses in a Scala program.

For example, let's say we are trying to design a `ParseError` data-type to
represent a syntax error when trying to parse some configuration file. Let's
imagine we want the following things to be available:

- The `line` and `col` the error occurred at
- Some kind of human readable `message` to explain what went wrong

We could imagine defining it as an opaque data-type, with a constructor,
initialization logic, and the necessary methods exposed:

```scala
class ParseError(index: Int, input: String){
  ... some computation ...
 
  def line: Int = ...
  def col: Int = ...
  def message: String = ...
}
```

Here, we take an `index` and an `input` when constructing the `ParseError`,
but neither of them are public. Rather, we only expose the `line`, `col`, and
`message` as `def`s for people to use. I call this *Opaque*, because someone
using `ParseError` has no visibility into how it actually works: they can only
see the few `def`s it exposes. Whatever computation is happening is totally
hidden within the body of the `ParseError`

Another way we could decide to do it is as a `case class`:

```scala
case class ParseError(line: Int, col: Int, message: String)
```

As a `case class`, all its
constructor parameters `line`, `col` and `message` are automatically public.
We do not do any computation in the body of `ParseError`; computing the `line`
and `col` and `message` from the `index` and the `input` will have to happen
*before* the `ParseError` is created, *outside* of it. I call this
*Transparent*, because someone using `ParseError` can from-the-outside can see
all there is to know about it. There is no hidden data being stored, or hidden
computation being performed. It is simply a bundle of two integers (`line` and
`col`, and a string `msg`.

In general, more internally-complex data-types with more internal invariants
are better treated as opaque `class`es, while simpler data-types are better
treated as transparent `case class`es. The following reasons illustrate why:

- [Opacity enforces Invariants](#opacity-enforces-invariants)
- [Opacity can save on Defensiveness](#opacity-can-save-on-defensiveness)
- [Transparency reduces Complexity](#transparency-reduces-complexity)

It is worth noting that
[Transparency and Opacity is a Spectrum](#transparency-and-opacity-is-a-spectrum):
it's not an entirely binary decision to choose between either side, and you can
choose a point on the transparency-opacity spectrum with the set of tradeoffs
you want.

### Opacity enforces Invariants

It's easier to enforce invariants with an opaque data-type: since construction
of the computed `def`s from the "original" `index`/`input` data is done inside
the constructor, there's no way for someone to construct a "bad" `ParseError`
with nonsensical values for `line`, `col` or `message`. If `message` is
always in some particular style, I can't accidentally make it return
`"I am Cow"` instead. If `line` or `col` are always going to be greater than zero, I
can be certain (if I trust the internals of `ParseError` itself) that they're
never going to suddenly return negative numbers.

On the other hand, with a transparent data-type, I could easily pass in
negative integers to `line` or `col`. I can pass in weird strings into
`message`, and the transparent data-type will happily expose them.

Even in transparent data-types, some invariants can be [enforced by the
structure of your data-type](#structural-enforcement). Other invariants can be
validated using [Self Checks](#self-checks). Nevertheless, in the case where
the invariants are not easily enforced structurally, and adding lots and lots
of self-checks is tedious, having the data-type simply be Opaque could be the
right answer.

### Opacity can save on Defensiveness

It is possible to [enforce invariants](#self-checks) in a transparent
data-structure such as the case class above, but it requires additional steps.
For example:

- `assert`s to ensure that the `message` is not `null`

- `assert`s to ensure that the `line` and `col` are not negative

- (Possibly) `assert`s to ensure that the `message` obeys some format

In a transparent data-type, asserts are necessary if we want to enforce these
things: who knows what random downstream code is going to try stuffing into our
`ParseError(line: Int, col: Int, message: String)` constructor! However, in an
opaque data-type, asserts are not as necessary: only the data-type itself is
able to compute these values. So we can trust - without asserts - that it will do
the right thing and no external code can interfere with it.


### Transparency reduces Complexity

While encapsulating your logic within opaque data-types seems tempting, it can
also be a curse: opaque data-types may be complex, but the complexity is
hidden, so you often don't notice it until it causes subtle problems.

For example, from the outside we might not know whether the opaque `ParseError`
holds on to a reference to the `input: String` after it is constructed. Given
that the input to a parse is often far larger than any data included in the
error message, that could cause a considerable memory leak if we unwittingly
keep a number of `ParseError`s hanging around!

Another bit of uncertainty is how the `line`, `col`, and `message` are
populated.

- Are they pre-computed once during construction?

- Are they re-computed every time you access them?

- Are they lazy, meaning the first time you access them they may be slow to
  compute, but subsequent times become fast? Depending

- How much memory they take and how long they take to compute?

These could be important questions! Certainly we could figure out the answer by digging
through the code, but it may be non-trivial to do so.

With the transparent `ParseError` modeled as a `case class`, these questions
are all answers from a glance at the signature. We can see at a glance that
`line`, `col` and `message` are all eagerly pre-computed the first time. We can
see at a glance that we don't keep any possibly-massive `input: String` hanging
around using memory. While opaque data-types help you hide complexity and
maintain invariants, transparent data-types help you remove complexity
altogether. With a `case class`, there's data sitting in it's fields, and
that's all there is to know about it.


### Transparency and Opacity is a Spectrum

As with many things, *Transparency* and *Opacity* is not a binary choice. You
can go half-way, by having part of your data-type's fields be "dumb"
constructor arguments, while other "smart" fields are computed in the
body of the class. You can even go beyond the amount of opacity in the above
"opaque" example, e.g. by hiding the constructor as `private` and only letting
people create instances through a factory method (e.g. `apply` on the companion
object):

```scala
object ParseError{
  def apply(...): ParseError = {
    ... some computation ...
    new ParseError(...)
  }
}
class ParseError private(index: Int, input: String){
  ... some computation ...
 
  def line: Int = ...
  def col: Int = ...
  def message: String = ...
}
```

In general, more internally-complex data-types with more internal invariants
are better treated as opaque `class`es, while simpler data-types are better
treated as transparent `case class`es, though there's no hard-and-fast rule.
Next time you are picking a data-type, it's worth considering where on the
transparency-opacity spectrum to place it!


## Data-Types should be Total

When people say *functions* are Total, they usually mean that no matter what
arguments you pass in to the function and satisfy the compiler, it will never
give an "invalid" result at runtime: e.g. not blowing up with an exception.
That's a handy property to have when reasoning about your code (e.g. "this code
will never throw") and though hard to reach in reality, is still worth striving
toward.

When I say a *data-type* is Total, I mean that no matter how the data-type is
constructed, it cannot be "invalid": that is to say, I shouldn't be able to
construct an instance of your `class` or `case class` that doesn't make any
sense according to what you are going to use it for. For example, here are
some example data-types which *can* possibly be invalid

```scala
case class URL(value: String)
case class EmailAddress(value: String)
/**
 * Represents a folder full of text files, without sub-folders, storing
 * the name of each file together with it's text content in a tuple
 */
case class FolderContents(value: Seq[(String, String))
```

While these may seem like plausible ways to model a `URL`, an `EmailAddress`,
and a `FolderContents`, it is possible to construct invalid instances of them:

- `new URL("http:/www.google.com")` is invalid; it needs a double `//`
  after the `http`

- `new EmailAddress("haoyi.com")` as a email address, is invalid, as email
  addresses must contain an `@` somewhere in them

- `new FolderContents(Seq("file.txt" -> "Hello", "file.txt" -> "World"))`, is
  invalid: you cannot have two files of the same name

For example, if someone was trying to make a HTTP request to
`http:/www.google.com`, it would fail as a malformed URL. And since we don't
find out until the URL is used, it could easily be stored in our program for
minutes or hours before blowing up later on when we're not paying attention,
maybe past midnight on Saturday at 1am. Not great!

If they were total we would be sure that if we had a `URL`
object, it would be "well-formed" and not blow up due to its own malformed-ness
when you try to use it. Sure it could still fail due to run-time problems (Wifi
down?) but at least it won't fail due to "internal" problems, or if it does
fail it'll fail early while we're trying to construct it, so we can fix the bug
early and go home worry-free.

Now that definitely sounds like a nice property to have, but how can we achieve
it? It turns out there are two main techniques to make data-types *Total*:

- [Self Checks](#self-checks)
- [Structural Enforcement](#structural-enforcement)


### Self Checks

A simple way of enforcing that the data-types are never *invalid* is to add
assertions to their constructors to ensure that if someone tries to make an
invalid instance that violates whatever rules we have in mind, we throw an
exception:

```scala
case class URL(value: String){
  assert(value.contains("//"))
}
case class EmailAddress(value: String){
  assert(value.contains("@"))
}
/**
 * Represents a folder full of text files, without sub-folders, storing
 * the name of each file together with it's text content in a tuple
 */
class FolderContents(value: Vector[(String, String)){
  assert(value.map(_._1).distinct == value.map(_._1))
}
```

Now if we try to instantiate an invalid instance, it fails before we get our
hands on it:

```scala
@ new EmailAddress("haoyi.com")
java.lang.AssertionError: assertion failed
  scala.Predef$.assert(Predef.scala:156)
```

And we can be sure that if someone passes us `EmailAddress` as a function
argument, or we're reading it from some field on some object, that
`EmailAddress` satisfies at least some basic properties we expect all email
addresses to have: in this case it must contain an `@`. If it didn't contain
an `@`, it would have thrown an exception during construction, and we wouldn't
be able to get our hands on it. That's a nice property to have!

Implementing totality in this way has some benefits:

- It's *really easy*: just assert the things you know *must* be true in the
  body of your class, and you're done

- It requires minimal changes to existing code: everyone can construct
  `EmailAddress`s and use their `.value` as they always did, except now if you
  try to construct a bad one it'll blow up on you.

It also has some problems:

- These self-checks can be expensive to perform each time! For example, the
  self-check on the `FolderContents` class creates *three* brand new `Vector`s
  and compares them before throwing them away.

- They can be incomplete; there are likely other constraints on a `URL` apart
  from the fact that they contain a `//`, but this won't catch them.

- As your checks get more complex, they become both slower and easier to get wrong.

Thus, it's often worth considering the other way we can ensure totality:
[Structural Enforcement](#structural-enforcement).


### Structural Enforcement

Structural enforcement of totality is when you make sure a data-type cannot
contain invalid data purely by how it is defined. For example, you may define
the above cases as

```scala
case class URL(protocol: String, host: String, path: String){
  def value = protocol + "://" + host + "/" + path
}

case class EmailAddress(prefix: String, suffix: String)
  def value = prefix + "@" + suffix
}
/**
 * Represents a folder full of text files, without sub-folders
 */
case class FolderContents(value: Map[String, String])
```

In the case of `URL` and `EmailAddress`, instead of starting with a "raw"
`String` and trying to assert components of it exist, we instead start off
with the components, and then only convert it to a `String` on-demand.

In the case of `FolderContents`, we pick a better data-structure that fits
what we know of the data: a `Map` can only have one value for each key, no
matter what you try to do to it. Here we may not even need to provide an
alternate `def value`, as the `Map[String, String]` is as easy to work with
as `Seq[(String, String)]` was.

Note we may still want to put asserts in here: for example, we may assert that
the `prefix` and `suffix` of an `EmailAddress` does not contain `@`:

```scala
case class EmailAddress(prefix: String, suffix: String)
  assert(!prefix.contains('@'))
  assert(!suffix.contains('@'))
  def value = prefix + "@" + suffix
}
```

This would give us additional checks that we did not have above: that there
is exactly one `@` in an email address. No more, no less. While we could have
added the same check via [Self Checks](#self-checks), doing so makes the
asserts more complicated, and thus slower and more error-prone.

*Structural Enforcement* has many advantages over [Self Checks](#self-checks):

- If anyone wants to construct an instance of your data-type from the base
  components, they can skip the asserts and do so directly, which can save a
  lot of unnecessary computation.

- The constraints of "what it contains" are usually much more clear to future
  maintainers when expressed as the fields of a class, rather than some ad-hoc
  `assert`s in the class body

Even when using Structural Enforcement, it could still be worthwhile having a
"parse from unsafe input" method for creating these data-types. e.g. throwing
exceptions if the input is invalid:

```scala
object EmailAddress{
  def parse(input: String): EmailAddress = {
    input.split('@') match{
      case Array(prefix, suffix) => new EmailAddress(prefix, suffix)
      case _ => throw new IllegalArgumentException("Invalid email address: " + input)
  }
}
case class EmailAddress(prefix: String, suffix: String)
  assert(!prefix.contains('@'))
  assert(!suffix.contains('@'))
  def value = prefix + "@" + suffix
}
```

Or returning `Option`s

```scala
object EmailAddress{
  def parse(input: String): Option[EmailAddress] = {
    input.split('@') match{
      case Array(prefix, suffix) => Some(new EmailAddress(prefix, suffix))
      case _ => None
  }
}
case class EmailAddress(prefix: String, suffix: String)
  assert(!prefix.contains('@'))
  assert(!suffix.contains('@'))
  def value = prefix + "@" + suffix
}
```

After all, at the edges of your program you are definitely going to need to
be interfacing with external APIs which would often use `String`s rather than
your own special `EmailAddress` class.

Even though there's an escape hatch, having structural enforcement of totality
is valuable. Often, using escape hatches like `EmailAddress.parse` are
relegated to the edges of your program. You won't find yourself "accidentally"
parsing email addresses from invalid strings at random spots in your program!.
But if you pass around plain `String`s, or even wrapped
`class EmailAddress(value: String)` as shown above, it's very easy to confuse
an email-address `String` with any other `String`, or even accidentally use
string methods like `.substring` without noticing it might leave you with an
invalid `EmailAddress`.

## Data-Types should be Normalized

Ideally, data-types should be *Normalized*: this means they should not allow
multiple instances with different contents that are "equal". Just like
how "invalid" is defined [above](#data-types-should-be-total), "equal"
is subjective and depends on what you are trying to do. For example:

- The absolute filesystem path `/home/haoyi/./.ssh` is equal to the path
  `/home/haoyi/.ssh`, which is equal to the path `/home/haoyi/.ssh/config/..`

- The relative filesystem path `./.ssh/../..` is equal to the relatieve path
  `..`

- The Ansi-colored string `"\u001b[31mHello\u001b[0mWorld"` is equal to the
  Ansi-colored string `"\u001b[31mHello\u001b[0m\u001b[0mWorld"` when displayed
  at the terminal; the "Red" at the start (`\u001b[31m`) is reverted by the
  "Reset" in the middle (`\u001b[0m`) but it doesn't matter if there's two
  resets in a row or one; it still resets the color the same way and renders
  the same way.

If we did not care about Canonicity, and only about
[Totality](#data-types-should-be-total), we might define the two filesystem
paths as:

```scala
case class AbsPath(value: String){
  assert(value(0) == '/')
}
case class RelPath(value: String){
  assert(value(0) != '/')
}
```

With the `assert` enforcing what we know of the two types (absolute paths start
with `/`, relative paths don't). We can see this take effect if we try to
create invalid relative or absolute paths:

```scala
@ AbsPath("foo/bar/..")
java.lang.AssertionError: assertion failed
 
@ RelPath("/foo/bar/..")
java.lang.AssertionError: assertion failed
```

Although this is great for ensuring our data is *valid*, it does not help us
ensure our data is normalized. For example, if we wanted to compare two paths,
the default comparison wouldn't work:

```scala
@ AbsPath("/foo/bar/..") == AbsPath("/foo")
res3: Boolean = false
```

What do we do about that?

### Manual Normalization

One option is to provide a `.normalize()` or `.canonicalize()` method that
people have to call to make the data-type into it's "normalized form" before
doing comparisons and e.g. `java.io.File` provides it as a [getCanonicalFile]
method, and we could write our own version for `AbsPath`:

[getCanonicalFile]: https://docs.oracle.com/javase/7/docs/api/java/io/File.html#getCanonicalFile()

```scala
case class AbsPath(value: String){
  assert(value(0) == '/')
  def normalized() = {
    val parts = value.split('/')
    val output = collection.mutable.Buffer.empty[String]
    parts.foreach{
      case "." => // do nothing
      case ".." => output.remove(output.length - 1)
      case segment => output.append(segment)
    }
    AbsPath(output.mkString("/"))
  }
}
```

Something similar could be written for `RelPath`, but the idea
is the same: you give the user an option to normalize it, if necessary.

This works, and if you remember to call `.normalized()` before comparing
`AbsPath`s for equality, you get the right answer:

```scala
@ AbsPath("/foo/bar/..") == AbsPath("/foo")
res11: Boolean = false

@ AbsPath("/foo/bar/..").normalized()
res12: AbsPath = AbsPath("/foo")

@ AbsPath("/foo").normalized()
res13: AbsPath = AbsPath("/foo")

@ AbsPath("/foo/bar/..").normalized() == AbsPath("/foo").normalized()
res14: Boolean = true
```

This has been the way things are in programming for decades: Java,
Python, and countless other languages provide filesystem paths as basically
strings with some kind of normalize method. However, it's not
ideal: you can easily forget, and normalizing is expensive so you don't want
to *unnecessarily* normalize things either! You're thus left in a tight spot
where you have to normalize things *exactly* as much as you need them, else
suffer bugs or wasted-performance.

We can do better, and that's via [Automatic
Normalization](#automatic-normalization)

### Automatic Normalization

*Automatic Normalization* is a similar principle to [Structural
Enforcement](#structural-enforcement), and is often used together: you enforce
that a data-type is normalized not by trying to remember when to call the right
method on it, but by ensuring that *every time the data-type is created* it is
in a normalized state. This has several advantages over manually normalizing
things:

- It's easier to find things! If you see a normalized path printed in your logs,
  for example, you can be sure that you can grep for it to find any other times
  it's printed, rather than grepping for a dozen different variants of the same
  path. The same applies to data stored in your databases, caches, in the
  console output, etc.

- You cannot *forget* to normalize something before comparing it with `==`,
  or doing other things that require normalization (putting it into a `Map`,
  `Set`, etc.), which would result in subtle bugs where *how* a path was
  constructed affects the behavior of your program, even for "equal" paths.

- You cannot accidentally *double-normalize* things. If you get an unknown
  `AbsPath` or `java.io.File` from somewhere else, and you're not sure where
  (codebases can be big places...) it is often tempting to normalize it
  again "just in case". This can end up being wasteful if you do it over and
  over unnecessarily, and with *Automatic Normalization* that does not happen.

- It's more efficient! Manipulating data-types when in their normalized form
  is almost always easier than manipulating them "normally". For example,
  comparing two paths to see if one is a sub-path of the other is much easier
  if both paths are already normalized. The cost of keeping
  already-normalized data normalized is usually not more expensive than
  normalizedizing things manually when-necessary, and is often cheap.

For example, rather than defining filesystem paths as a case class with a
normalize method:

```scala
case class AbsPath(value: String){
  assert(value(0) == '/')
  def normalized() = {
    val parts = value.split('/')
    val output = collection.mutable.Buffer.empty[String]
    parts.foreach{
      case "." => // do nothing
      case ".." => output.remove(output.length - 1)
      case segment => output.append(segment)
    }
    AbsPath(output.mkString("/"))
  }
}
case class RelPath(value: String){
  assert(value(0) != '/')
  def normalized() = { ??? }
}
```

We can define them as a case class whose `parse` method performs the
normalization before creating the `case class`es:

```scala
object AbsPath{
  def parse(input: String) = {
    val output = collection.mutable.Buffer.empty[String]
    input.drop(1).split('/').foreach{
      case "." => // do nothing
      case ".." => output.remove(output.length - 1)
      case segment => output.append(segment)
    }
    AbsPath(output)
  }
}
case class AbsPath(segments: Seq[String]){
  assert(!segments.contains("..") && !segments.contains("."))
  def value = "/" + segments.mkString("/")
}
object RelPath{
  def parse(input: String) = {
    var ups = 0
    val output = collection.mutable.Buffer.empty[String]
    input.split('/').foreach{
      case "." => // do nothing
      case ".." =>
        if (output.nonEmpty) output.remove(output.length - 1)
        else ups += 1
      case segment => output.append(segment)
    }
    RelPath(ups, output)
  }
}
case class RelPath(ups: Int, segments: Seq[String]){
  assert(!segments.contains("..") && !segments.contains("."))
  def value = (Array.fill(ups)("..") ++ segments).mkString("/")
}
```

What did we do?

- Move the body of the `normalize` method in each class into their
  respective `parse` functions

- Changed the representation of both paths from a dumb `String` to a
  `Seq[String]`, representing the individual path segments

- Enforced with asserts that the segments cannot be `.` or `..`!

- Made `RelPath` keep a count of `ups: Int`

The first three points should be relatively straighforward: after all, paths
are just sequences of path segments, and in an absolute path the normalized
version of a file never has `.` or `..` segments:

```scala
@ new java.io.File("/foo/../bar/././baz/..").getCanonicalPath
res24: String = "/bar"
```

The last point is a bit subtle: the normalized version of an absolute path *can*
have `..`s, but they can *only be at the start of a path*! For example, the
relative path

- `foo/../../bar/../../././baz`

Can be reduced step by step, removing the `.`s:

- `foo/../../bar/../../baz`

And collapsing the `..`s to the left:

- `../bar/../../baz`
- `../../baz`

Until you have all the `..`s in the left-most segments, and the non-`..`s in
the segments on the right. Thus, in this normalized form, a relative path is
simply a count of `..`s on the left, and a `Seq` of non-`..` segments on the
right! Anyway, we've made the `AbsPath.parse` and `RelPath.parse` methods
in the above defifition perform this normalization, and once everything is
defined, it works, and both `AbsPath`s:

```scala
@ AbsPath.parse("/foo/bar/..")
res19: AbsPath = AbsPath(ArrayBuffer("foo"))

@ AbsPath.parse("/foo")
res20: AbsPath = AbsPath(ArrayBuffer("foo"))

@ AbsPath.parse("/foo/bar/..") == AbsPath.parse("/foo")
res21: Boolean = true
```

And `RelPath`s:

```scala
@ RelPath.parse("../../baz")
res38: RelPath = RelPath(2, ArrayBuffer("baz"))

@ RelPath.parse("foo/../../bar/../../././baz")
res39: RelPath = RelPath(2, ArrayBuffer("baz"))

@ RelPath.parse("foo/../../bar/../../././baz") == RelPath.parse("../../baz")
res40: Boolean = true
```

Are in an always-normalized state.

In a real data-type, there will be more operations that just `.parse` and `==`,
and you will have to ensure that all those operations keep the data-type in its
normalized form. Nevertheless, doing so is often relatively cheap (at runtime)
and easy (to implement), so it's not a terrible cost.

You can see that in the automatically-normalized version, `==` equality
works. We can be free to put things in `Set`s, `Map`s. You get all the good
things mentioned at the [beginning of this section](#automatic-normalization),
without having to worry about it forever more!

## Data-types should have Instant Initialization

Data-types should ideally be totally initialized and ready to use as soon as
the constructor or factory function returns. You should not construct "half
baked" instances and hope people fill them in later. When I call a constructor:

```scala
val myFoo = new Foo(...)
```

`myFoo` should be complete and immediately ready to use. Two common cases where
this is important are when:

- [Initializing Classes](#initializing-classes)
- [Initializing Collections](#initializing-collections)

### Initializing Classes

In Scala, you should use constructor parameters to initialize your classes; if
you need to given them defaults, etc., do so:

```scala
class MyAction(text: String, image: String, tooltip: String = "")

val action = new MyAction("My Action Text", "Some Image")
```

Do not design your data-type so that you instantiate the class, in a half-baked
un-usable state, and "later" come along and fill in all the missing fields.
This is less common in Scala, but very common in Java libraries, e.g. the
following Java definition of `class MyAction`:

```java
public class MyAction {
    private String _text     = "";
    private String _tooltip  = "";
    private String _imageUrl = "";

    public MyAction()
    {
       // nothing to do here.
    }

    public MyAction text(string value)
    {
       this._text = value;
       return this;
    }

    public MyAction tooltip(string value)
    {
       this._tooltip = value;
       return this;
    }

    public MyAction image(string value)
    {
       this._imageUrl = value;
       return this;
    }
}
```

Lets you construct a `MyAction` via

```scala
val action = new MyAction()
action.text("My Action Text")
action.tooltip("My Action Tool tip")
action.image("Some Image");
```

This has a problem: if you accidentally use `action` somewhere between
`new MyAction()` and `action.image`, anywhere in the `NOT READY` area below:

```scala
// Not available...
val action = new MyAction()
// NOT READY
action.text("My Action Text")
// NOT READY
action.tooltip("My Action Tool tip")
// NOT READY
action.image("Some Image");
// ...Ready
```

You will find yourself with a half-constructed `MyAction` with some fields
mysteriously set to `""`. Not good!

Even worse, is the fact that someone may be trying to use your `MyAction`
class and forget one of the initialization steps altogether:

```scala
val action = new MyAction()
action.text("My Action Text")
action.tooltip("My Action Tool tip")
// Oops
```

And find themselves wondering why, sometime much later, somewhere far away in
their program, their `image` is becoming `""`. Quite likely "much later" means
1am past Saturday midnight, and "far away" means in your employer's data-center
where it needs to be fixed ASAP... by you.

These APIs also lend themselves to be called
"[fluently](https://en.wikipedia.org/wiki/Fluent_interface)", e.g.

```scala
val action = new MyAction()
    .text("My Action Text")
    .tooltip("My Action Tool tip")
    .image("Some Image");
```

Which lets you chain the initialization calls one after another. This helps
fix the `NOT READY` problem:

```scala
// Not available...
val action = new MyAction()
    .text("My Action Text")
    .tooltip("My Action Tool tip")
    .image("Some Image");
// ...Ready
```

Where you can see it jumps immediately from `Not available`, where the compiler
won't let you make use of the `val action` yet, and `Ready`, where it's fully
initialized. At no point does it go through the `NOT READY` phase where the
compiler will let you use it but it's not ready, and will either misbehave or
throw an exception at runtime.

That's great, but it doesn't help in case someone forgets one of the
initialization calls:

```scala
// Not available...
val action = new MyAction()
    .text("My Action Text")
    .tooltip("My Action Tool tip")
    // Oops
// NOT READY
```

There are ways to work around this by using a
[Builder Class](https://en.wikipedia.org/wiki/Builder_pattern) and making the
different stages of your fluent constructor a Builder and not a real
`MyAction`. That stops you from accidentally using an incomplete action, but
is a huge amount of boilerplate to write!

Luckily, in Scala, you do not need special fluent-builder-classes; you can
simply use the constructor, possibly with defaults for some of the arguments
(something you can't do in Java, which is why the fluent/Builder patterns
emerged). In Scala, it is much simpler to define your class as:

```scala
class MyAction(text: String, image: String, tooltip: String = "")
```

Called via

```scala
val action = new MyAction("My Action Text", "Some Image")
```

Or

```scala
val action = new MyAction(
  text = "My Action Text",
  image = "Some Image"
)
```

And not worry about anyone mis-using your half-complete class, since there's no
point around the instantiation side where the `action` instance is available but
not ready for use:

```scala
// Not available...
val action = new MyAction(
  text = "My Action Text",
  image = "Some Image"
)
// ...Ready
```

And the compiler helpfully tells you if you forget a required argument:

```scala
val action = new MyAction(
  text = "My Action Text",
  tooltip = "My Action Tool tip"
)
// not enough arguments for constructor MyAction: (text: String, image: String, tooltip: String)ammonite.session.cmd18.MyAction.
// Unspecified value parameter image.
// val action = new MyAction(
//              ^
// Compilation Failed
```

In this case it's simple, but you should try to maintain this style even if
your constructor is getting large an unwieldy. Perhaps your constructor has
11 different arguments:

```scala
class Interpreter(prompt0: Ref[String],
                  frontEnd0: Ref[FrontEnd],
                  width: => Int,
                  height: => Int,
                  colors0: Ref[Colors],
                  printer: Printer,
                  storage: Storage,
                  history: => History,
                  predef: String,
                  wd: Path,
                  replArgs: Seq[Bind[_]]){
  ...
}
```

Even so, it's better to just pass everything in and initialize the instance
all-at-once rather than trying to do it piecemeal and risk someone will make
use of your half-baked instance and end up fighting weird bugs.

### Initializing Collections

In many existing libraries in the Java ecosystem, it is common to create
something, spend time initializing it, and hope that nobody tries to use your
thing until you're done initializing.

One common offender is the Java collections library:

```scala
val listA = new java.util.ArrayList[String]();

listA.add("element 1");
listA.add("element 2");
listA.add("element 3");
```

Although `ArrayList` is a mutable collection, it often isn't used in a mutable
way: people will create it empty, insert content to initialize it, and never
change it again after that. However, if we look at the places where our `listA`
is not ready to use, we can see that there is a lot of space where someone
writing code can get their hands on a not-ready `ArrayList`:


```scala
// Not available...
val listA = new java.util.ArrayList[String]();
// NOT READY
listA.add("element 1");
// NOT READY
listA.add("element 2");
// NOT READY
listA.add("element 3");
// ...Ready
```
While this is a small example and perhaps making a mistake is unlikely, in
larger real-world codebases all these `NOT READY` spots are bugs waiting to
happen when someone adds code to call some initialization method, which finds
`listA` in it's `NOT READY` state and uses it anyway, resulting in odd bugs.

On the other hand, when you consider initializing a list in Scala:

```scala
// Not available...
val listB = List[String](
  "element 1",
  "element 2",
  "element 3"
)
// ...Ready
```

You can see that it jumps immediately from `Not available` to `Ready`, without
any intermediate states being exposed to the outside world. Obviously, *inside*
the `List(...)` constructor it must be taking time assembling the list piece by
piece, but to *outside code*, they cannot see any of that. Again, outside code
can only see `listB` "instantly" jumping from `Not available` to `Ready`, and
there is no chance to inadvertently use a partially-incomplete list in your
other code.

While the Scala collections library is already written, remember this principle
if you ever end up writing your own collection. If you ever find yourself
writing some custom collection data-structure, make sure you can initialize
"instantly" in one go.

## Conclusion

In this post, we've covered the following guidelines:

- [Opaque or Transparent?](#opaque-or-transparent)
- [Data-types should be Total](#data-types-should-be-total)
- [Data-types should be Normalized](#data-types-should-be-normalized)
- [Data-types should have Instant Initialization](#data-types-should-have-instant-initialization)

These are just a small number of guidelines that I follow when deciding how to
represent my data as `class`es or `case class`es in my Scala programs, together
with the reasoning behind them. None of these guidelines are "novel", or of
any academic interest, and in fact may well be "obvious" for people who have
been writing Scala for a while. Nevertheless, hopefully they will be useful to
beginning or intermediate Scala programmers trying to build a foundation for
talking about Scala code and evaluating the plethora of solutions that the
Scala programming language offers to someone designing their own data-types.

What are your favorite tips for how to design the data-types holding your Scala
application together? Let us know in the comments below!
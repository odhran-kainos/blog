[uJson](http://www.lihaoyi.com/upickle/#uJson) is a new JSON library for the
Scala programming language. It serves as the back-end for the
[uPickle](http://www.lihaoyi.com/upickle) serializaiton library, but can be used
standalone to manipulate JSON in a way that is fast, flexible and intuitive, far
more than the existing JSON libraries in the Scala library ecosystem. This post
will go over what makes uJson an improvement over other JSON libraries that are
available, and why you might consider using uJson and uPickle in your next big
project.

-------------------------------------------------------------------------------

## Intuitiveness

There are a lot of existing JSON libraries in Scala:
[Circe](https://github.com/circe/circe),
[Argonaut](https://github.com/argonaut-io/argonaut),
[Play-Json](https://github.com/playframework/play-json), and many others.
However, none of them achieve the heights of intuitiveness that someone coming
from Python, Ruby or Javascript might expect working with JSON data types.

Consider a simple JSON data structure:


```js
{
  "id": "c730433b-082c-4984-9d66-855c243266f0",
  "name": "Foo",
  "counts": [1, 2, 3],
  "values": {
    "bar": true,
    "baz": 100.001,
    "qux": ["a", "b"]
  }
}
```

Trying to update the `name` field, e.g. to reverse the string, would seem like a
trivial operation that should have a trivial solution. In most languages, such
as Python, Ruby or Javascript, it is trivial:

### Python

```python
import json
data = json.loads("...")
data["name"] = reversed(data["name"])
updated = json.dumps(data)  
```

### Ruby

```ruby
require "json"
data = JSON.parse("...")
data["name"] = data["name"].reverse
updated = JSON.generate(data)
```

### Javascript

```js
data = JSON.parse("...")
data["name"] = data["name"].split("").reverse().join("")
updated = JSON.stringify(data)
```

Each of the above examples is trivial; even a beginner learning Python, Ruby or
Javascript would have no problem understanding what each one is doing! A trivial
code snippet is a fitting way of implementing a trivial task.

## uJson

With uJson, manipulating JSON should be just as simple and familiar regardless
of what your programming background is:

```scala
val data = ujson.read(input)
data("name") = data("name").str.reverse
val updated = data.render()
```

Apart from the minor syntactic differences from Python/Ruby/Javascript (e.g.
using `("name")` instead of `["name"]` for dictionary lookup), the only
additional syntax here is the `.str` method call. This is simply a property of
working in a statically-typed language: it's effectively a cast to say "this
JSON value is a `String`" to allow you to work with it as a `String`. There are
similar methods `.num` `.arr` `.obj` for casting to other common JSON types.

Constructing JSON values using uJson is also simple, using the `Js.*`
constructors. Here's an example constructing a JSON dictionary (`Js.Obj`) from
the Ammonite codebase:

```scala
import ujson._
val dict = Js.Obj(
  "tag_name" -> ammoniteVersion,
  "name" -> ammoniteVersion,
  "body" -> s"http://www.lihaoyi.com/Ammonite/#$ammoniteVersion"
)
```

Nested structures are constructed similarly:

```scala
val nested = Js.Arr(
  Js.Obj("myFieldA" -> 1, "myFieldB" -> "g"),
  Js.Obj("myFieldA" -> 2, "myFieldB" -> "k")
)
```

And trivial structures are trivial to construct:

```scala
val nums = Js.Arr(1, 2, 3)
```

Due to uJson's mutable data model, you probably already know how to modify any
of the structures above, including modifying values nested in arrays and
dictionaries:

```scala
nested(0)("myFieldA") = 123
```

With uJson, you have a JSON library that does what *you* want to do, rather than
forcing you to learn how to library wants to do things. uJson makes it both
simple and easy to work with JSON, so you can spend less time thinking about
JSON and more time on things which are actually important to you: your business
logic, application, and product.

## Scala Alternatives

Compare the above uPickle examples to the complexity of manipulating JSON in the
various Scala libraries:

### Argonaut

```scala
import scalaz._, Scalaz._
import argonaut._, Argonaut._

val data = "...".parseOption.get
val data2 = data.withObject(o =>
  JsonObject.fromTraversableOnce(
    o.fields.map{f =>
      f -> (if (f == "name") o(f).get.withString(_.reverse) else o(f).get)
    }
  )
)
val updated = data2.toString 
```

### Circe

```scala
import io.circe._, io.circe.parser._
val data = parse("...").right.get

val data2 = data.hcursor.downField("name").withFocus(_.mapString(_.reverse)).top.get

val updated = data2.toString
```

### Play-Json

```scala
import play.api.libs.json._
val data = Json.parse(input)
val jsonTransformer = (__ \ "name").json.update(
    __.read[JsString].map(x => JsString(x.value.reverse))
)
val data2 = data.transform(jsonTransformer)
val updated = data2.toString
```

None of these examples using Scala libraries are as simple, or easy, as the
Python, Ruby, or Javascript examples, or the uJson example shown above. Some of
the complexity is incidental: e.g. Circe's syntax for doing it is much less
verbose than Argonaut's or Play-Json's. However, some of the complexity is
fundamental: these libraries have a JSON data type that aims to be
immutable. Thus, users can no longer us their familiar mutation operations
(`data["name"] = reversed(data["name"])`) to manipulate the JSON, and are forced
to use less-common tools like [Lenses](http://argonaut.io/doc/zipper/) or
[Cursors](https://circe.github.io/circe/cursors.html#cursors) or Zippers to
transform it.

Fundamentally, nobody *wants* to become an expert at the JSON library they're
using. They want to become experts on AI, financial software, UI-development,
distributed systems, and other things. They need to become experts in the system
they maintain, the proprietary data integrations or algorithms that give their
system value, or the needs & workflows of their users. While there is a place
for sophisticated libraries that require learning & in-depth knowledge to use
well, I think processing JSON is not one of those places.

### What of Immutability?

A lot of the complexity in Argonaut, Circe and Play-Json arise from the issue of
immutability.

Without the ability to mutate values at runtime, transforming JSON blobs becomes
much more cumbersome. Immutability forces you to re-construct the JSON structure
from the ground up, with the changes you want, rather than simply mutating the
original JSON structure. While this is a potential performance issue
(reconstructing trees with changed bits tends to be slower than point-mutations)
the most obvious difference is the amount of code you need to write:
reconstructing-tree-with-changes has a lot more going on than mutating a single
variable!

There are tools that help you reduce the boilerplate of immutability's
reconstruct-tree-with-changes approach to JSON manipulation: Lenses like
Play-Json's `__ \ "name"` or `__.read` save you some boilerplate of writing the
recursive-transformation manually, and fluent APIs like Circe's
`data.hcursor.downField("name").withFocus` saves further boilerplate.
Nevertheless, these are only a partial workaround: even with tools such as
these, Circe's fluent lens-based API:

```scala
val data2 = data.hcursor.downField("name").withFocus(_.mapString(_.reverse)).top.get
```

Still has a lot more boilerplate & new concepts to learn than uJson's mutation
based API:

```scala
data("name") = data("name").str.reverse
```

One question you may ask is what is immutability good for? Obviously it helps
avoid bugs where you accidentally mutate something that isn't meant to be
mutated: re-initializing an already-initialized object, mutating your function's
parameters in a way that accidentally affects other places that parameter is
passed, etc..

However, in my experience these are simply not common failure modes when
mangling JSON data: JSON structures tend to be transient - they are parsed,
manipulated and serialized - without the long lifetimes to make them susceptable
to accidental mutation. While complicated functions mutating a JSON structure
tend to be confusing, complicated sequences of lenses/cursors/zippers tend to be
confusing as well.

For the 99% use case, uJson exposes a dead-simple - but mutable - JSON data
structure that makes it easy for anyone to step up and start manipulating JSON
without any prior experience. However, you may bump into cases where the JSON
processing you are doing is complex enough that the benefits of immutability
outweigh the cost of learning how to deal with cursors/lenses/zippers. uJson has
an answer for that too, by supporting third-party JSON libraries in all it's
JSON processing operations

## JSON Library Interoperability

uJson comes with a rich collection of basic operations to manipulate JSON:

```scala
package object ujson{
  def read(s: Transformable): Js.Value
  
  def copy(t: Js.Value): Js.Value
  
  def write(t: Js.Value, indent: Int = -1): String
  
  def writeTo(t: Js.Value, out: java.io.Writer, indent: Int = -1): Unit
  
  def validate(s: Transformable): Unit
  
  def reformat(s: Transformable, indent: Int = -1): String
  
  def reformatTo(s: Transformable, out: java.io.Writer, indent: Int = -1): Unit
  
  def transform[T](t: Transformable, v: Visitor[_, T]): T
}
```

The uPickle library that uses uJson builds on top of these, exposing similar
operations that work on any type `T` with a provided `Reader` or `Writer`:

```scala
trait Api{
  def read[T: Reader](s: Transformable): T
  
  def readJs[T: Reader](s: Js.Value): T
  
  def reader[T: Reader]: Reader[T]
  
  def write[T: Writer](t: T, indent: Int = -1): String
  
  def writeJs[T: Writer](t: T): Js.Value
  
  def writeTo[T: Writer](t: T, out: java.io.Writer, indent: Int = -1): Unit
  
  def writer[T: Writer]: Writer[T]
  
  def writable[T: Writer](t: T): Transformable
  
  def readwriter[T: ReadWriter]: ReadWriter[T]
  
  case class transform[T: Writer](t: T) extends Transformable{
    def to[V](f: ujson.Visitor[_, V]): V
    def to[V](implicit f: Reader[V]): V
  }
}
```

Apart from the "obvious" operations like `ujson.read` or `Api#write`, the most
interesting APIs in uJson and uPickle are `ujson.transform` and
`Api#transform#to`. These can be used to process any `Transformable` by any
`Visitor`, or to process any type `T` with a `Writer` by either a `Visitor` or
by a `Reader`.

What is interesting about uJson and uPickle is that the generic `Transformable`
and `Visitor` types can be defined for any JSON structure, not just the
"default" one provided by uJson! uJson comes with default integrations with
[Argonaut](http://www.lihaoyi.com/upickle/#Argonaut),
[Circe](http://www.lihaoyi.com/upickle/#Circe),
[Json4s](http://www.lihaoyi.com/upickle/#Json4s) and
[Play-Json](http://www.lihaoyi.com/upickle/#Play-Json): JSON structure from any
of those libraries can take part in any uJson/uPickle operations. This means,
taking Argonaut as an example:

- uJson can parse an Argonaut JSON structure from a String

```scala
import ujson.argonaut.ArgonautJson
val argJson: argonaut.Json = ArgonautJson(
  """["hello", "world"]"""
)
```

- uPickle can de-serialize any Scala data-types from the Argonaut JSON
  structure:

```scala
val items: Seq[String] = ArgonautJson.transform(
  updatedArgJson,
  upickle.default.reader[Seq[String]]
)
```

- uPickle can serialize any Scala data-types to the Argonaut JSON structure:

```scala
val rewritten = upickle.default.transform(items).to(ArgonautJson)
```

- uJson can write out the Argonaut JSON structure to a String:

```scala
val stringified = ArgonautJson.transform(rewritten, StringRenderer()).toString
```

While the above examples are for Argonaut, uJson can perform all the same
operations on the JSON data type of any of the other libraries. uJson can
even convert the JSON data type from one library to another, directly &
without overhead.

The following example shows us parsing JSON into a Circe JSON structure,
manipulating it, converting it to the Play-Json JSON structure, manipulating it
more, and finally writing it out as a String:

```scala
import ujson.circe.CirceJson
val circeJson: io.circe.Json = CirceJson(
  """["hello", "world"]"""
)

val updatedCirceJson =
  circeJson.mapArray(_.map(x => x.mapString(_.toUpperCase)))

import ujson.play.PlayJson
import play.api.libs.json._

val playJson: play.api.libs.json.JsValue = CirceJson.transform(
  updatedCirceJson,
  PlayJson
)

val updatedPlayJson = JsArray(
  for(v <- playJson.as[JsArray].value)
  yield JsString(v.as[String].reverse)
)

val stringified = PlayJson.transform(updatedPlayJson, StringRenderer()).toString

stringified ==> """["OLLEH","DLROW"]"""
```

While uJson provides a simple mutable JSON data type that should be easy for
anyone to pick up, using uJson doesn't bind you to it: any of uJson's or
uPickle's operations can work just as easily with the JSON data type provided by
any other library. uJson's high-performance direct-conversion protocol (the
`.transform` methods you see above) mean it is both easy and efficient to
convert your JSON data into whatever form you prefer manipulating.

uJson and uPickle are not just a convenient JSON manipulation library and
serialization library: they effectively act as a common protocol shared by all
popular JSON libraries in the Scala ecosystem.

uPickle builds on top of uJson and gains the ability to read/write to/from every
JSON library in existance, with high performance & zero overhead. Other
serialization libraries could also be written on top of uJson to receive the
same read/write-anything capability. Similarly, any other JSON library that
chooses to integrate with uJson (which is
[a tiny amount of work](https://github.com/lihaoyi/upickle/blob/003244a25c476a56462f616d6383543c5ecd3c5c/ujson/play/src/ujson/play/PlayJson.scala))
will similarly receive a rich library of JSON operators, compatibility with
uPickle & other serialization libraries, and fast/zero-overhead conversions
to/from every other JSON library out there.

## Zero Overhead Serialization

Most JSON serialization libraries out there follow a familiar pattern:

- Parse input `String`s into a JSON data type
- Convert the JSON data type into Scala data types (`case class`es, `Seq`s, ...)
- Convert the Scala data types into JSON data type
- Serialize the JSON data type into a `String`

Sometimes you care about the JSON data type in between: you want to manipulate
it, inspect it, modify it, etc.. But other times you do not, and just want to go
from `String` to `case class` as quickly as possible. Why should we spend time
building a JSON structure that we're just going to throw away?

Above, I described how uJson allows for zero-overhead conversions between JSON
data types from different libraries. This converts from one JSON type to another
JSON type without any intermediate structures. This zero-overhead-conversion
capability also lets uJson perform zero-overhead conversions between *anything*
that uJson knows how to read or write: uJson can read `String`s, and uPickle can
write `case class`es, and so uJson can do zero-overhead conversion from `String`
to `case class` (and vice versa) without constructing an intermediate AST.

This direct serialization means uJson/uPickle have great performance. The
following (somewhat ad-hoc) benchmark counts how many times uJson/uPickle can
read/write a simple `case class` to JSON `String`s in a fixed span of time
(higher is better)

| Library         |     Reads |     Writes |
|:----------------|----------:|-----------:|
| Jackson Scala   | 2,038,770 | 11,324,495 |
| Play Json       |   987,940 |  1,357,490 |
| Circe           | 2,360,411 |  2,139,692 |
| upickle.default | 3,135,576 |  3,496,939 |

As you can see, uPickle has a significant speed advantage over Circe, which is
impressive given that Circe is already more than twice as fast as Play-Json.
uPickle has had much less effort put into micro-optimizations than Circe, and
there are definitely still a lot of long hanging fruit to be picked just by
spending a bit more time with a profiler.

The difference in speed is even larger in Scala.js:


| Library         |   Reads |    Writes |
|:----------------|--------:|----------:|
| Play Json       | 117,181 |   194,582 |
| Circe           | 132,519 |   441,906 |
| upickle.default | 613,727 | 1,041,798 |

Here, we see uPickle's reads are about 5 times as fast as Play-Json or Circe,
and it's writes are 5x or 2x as fast as each of those libraries respectively.

## Conclusion

uJson is a fast, flexible, and intuitive JSON library for Scala. I have shown
how using uJson to manipulate raw JSON compares favorably to the most concise
scripting languages out there, how uJson allows seamless integration of any
library's JSON data type into all of it's core functions & operations, and how
uJson's zero-overhead-conversion protocol can be leveraged to provide
zero-overhead JSON serialization that parses a `String` directly into a
`case class` (or vice versa) giving you significant performance wins.

I think JSON libraries are a big pain point for Scala: many of the existing
libraries are designed for experts, exposing great power to those sophisticated
enough to wield them. What is missing is a "JSON for dummies" library, that can
handle the 99% of "boring" use cases with minimal required learning or
onboarding.

Next time you find yourself looking for a simple, fast JSON library or
serialization library to use in some application, it's probably worth
giving uJson and uPickle a try!

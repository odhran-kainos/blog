Apart from the old design patterns from the 1990s, the Scala programming 
language in 2016 has a whole new set of design patterns that apply to it. These 
are patterns that you see in Scala code written across different organizations, 
cultures and communities.

This blog post will describe some of these design patterns around the use of
Scala implicits: specifically around the use of implicit parameters. Hopefully
this should help document some of the more fundamental patterns around how 
people use implicit parameters "in the wild", and provide some insight into 
what can easily be a confusing language feature.

-------------------------------------------------------------------------------

"Implicits" - implicit parameters and implicit conversions - are a major 
feature of the Scala language. An implicit parameter is one that can be 
automatically inferred based on its type and the values in 
scope, without you needing to pass in the value of the argument explicitly,
and an implicit conversion function converts one type to another automatically
on-demand, without needing to call the function explicitly.

However, implicits themselves are a pretty low-level feature. You generally 
do not use implicits for the sake of using implicits, neither do you use 
implicits freely in all possible ways. Rather, you most often use implicits as 
a tool to help you implement one of a small number of patterns. This blog post
documents some of those, specifically around the use of implicit parameters:

- [Implicit Contexts](#implicit-contexts)
- [Type-class Implicits](#type-class-implicits)
- [Derived Implicits](#derived-implicits)
- [Type-driving Implicits](#type-driving-implicits)

Since there isn't that much published literature about design patterns in 
Scala, all these are names I just made up off the top of my head, so
hopefully the names will make sense. This is not going to be anywhere near an
exhaustive list of the things you can do with implicits, but should hopefully
provide a foundation that you can use when trying to use them yourself or
understanding other people's code.

## Implicit Contexts
 
The most basic use of implicits is the Implicit Context pattern: using them 
to pass in some "context" object into all your methods. This is something you 
could pass in manually, but is common enough that simply "not having to pass it 
everywhere" is itself a valuable goal. 


For example, the standard library uses it to pass around an implicit 
[ExecutionContext](http://docs.scala-lang.org/overviews/core/futures.html) to 
every piece of code that needs to "run something asynchronously":

```scala
implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
def getEmployee(id: Int)(implicit e: ExecutionContext): Future[Employee] = ???
def getRole(employee :Employee)(implicit e: ExecutionContext): Future[Role] = ???
val bigEmployee: Future[EmployeeWithRole] =
  getEmployee(100).flatMap { e =>
    getRole(e).map { r =>
      EmployeeWithRole(e.id, e.name,r) 
    }
  }
```

`getEmployee` and `getRole` both require an `ExecutionContext` to run their
asynchronous fetches, and the `.flatMap` and `.map` methods also need an
`ExecutionContext` to run their callbacks. Without implicits you would need
to pass it into each of those functions manually:

```scala
val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global
val bigEmployee: Future[EmployeeWithRole] =
  getEmployee(100)(ec).flatMap { e =>
    getRole(e)(ec).map { r =>
      EmployeeWithRole(e.id, e.name,r) 
    }(ec)
  }(ec)
```
 
While there are only 4 copies of `ec` in this short snippet, in a larger file
or codebase you could easily have dozens, hundreds or thousands of redundant
copies. By passing it around as an Implicit Context using `implicit`, it saves 
all that duplication and cleans up the code considerably.
 
Similarly, the [Play framework](https://www.playframework.com/) also uses it to 
pass around the `request` object:

```scala
def index = Action { implicit request =>
  Ok(views.html.index())
}
```

[Akka](http://akka.io/) uses it to pass around `ActorContext`s and 
`ActorSystem`s, and so on. In
all these cases, the goal is to pass around some object that is ubiquitous 
enough that explicitly passing it into each and every function call is tedious
and verbose.

The Implicit Context pattern is just one use of the `implicit` keyword in 
Scala, which is a broadly flexible tool with many uses. Implicit Contexts 
usually have some properties that make them distinct from many other uses of
implicits:

- The implicit parameter usually is not generic type, and does not have any 
  type parameters
  
- The same implicit is being passed to all sorts of different functions with
  different signatures

- Different *values* of the implicit will be passed into the same function when
  called at different times, e.g. every Play Framework HTTP request gets a new
  `Request` value that gets passed around implicitly 
  
- The implicit value might even be mutable! This is certainly the case for
  Akka's `ActorSystem`s, which encapsulate a large pool of Actors and the
  ability to spawn new ones or send them messages.

Essentially, the Implicit Context pattern is the only use of implicit 
parameters which treat them as "a convenient way to pass extra arguments",
which are not much different from any other arguments you might pass except
being ubiquitous enough you pass them everywhere.
 
## Type-class Implicits

The Type-class Implicit pattern is named after a language feature in Haskell 
which provides the same functionality. In short, it is using generic implicits 
which take a type parameter, e.g. `Foo[T]`, and resolving them based on that 
type parameter. That means you an implicit of the same `Foo[T]` type almost 
always resolves to the same, immutable value. This is in contrast to the 
[Implicit Context](#implicit-context) pattern where the implicit `Foo` type 
typically has no type parameter but you provide a different (possibly mutable!) 
value each time.

For example, you may want to define a function that lets you serialize 
an object to JSON:

```scala
sealed trait Json
object Json{
  case class Str(s: String) extends Json
  case class Num(value: Double) extends Json
  // ... many more definitions
}

def convertToJson(x) // converts x to a Json object
```

However, what would the type signature of `convertToJson` be? It should 
probably return `Json` if that's what we want out of it:

```scala
def convertToJson(x): Json
```

But what about it's argument?

It could be `Any`, and we could pattern-match on it to figure out what kind
of `Json` object we want to create:


```scala
def convertToJson(x: Any): Json = {
  x match{
    case s: String => Json.Str(s)
    case d: Double => Json.Num(d)
    case i: Int => Json.Num(i.toDouble)
    // maybe more cases for float, short, etc.
  }
}
```

This works if you pass in the right thing:

```scala
@ convertToJson("hello")
res: Json = Str("hello")
@ convertToJson(1234)
res: Json = Num(1234.0)
```

But if you pass in the wrong thing, it blows up:

```scala
@ convertToJson(new java.io.File("."))
scala.MatchError: . (of class java.io.File)
  $sess.cmd2$.convertToJson(cmd2.sc:5)
  $sess.cmd6$.<init>(cmd6.sc:1)
  $sess.cmd6$.<clinit>(cmd6.sc:-1)
```

This works, but could be improved: what if we could make `convertToJson(x)`
only compile if `x` is of type `String`, `Double` or `Int`? We can't use the
common supertype because that's just `Any`, which also includes things we don't
want like `java.io.File`. And of course, using any of `String`, `Double` or 
`Int` directly prevents you from passing in the other two.

It turns out there is a solution to this problem, using the Type-class Implicit 
pattern:

```scala
trait Jsonable[T]{
  def serialize(t: T): Json
}
object Jsonable{
  implicit object StringJsonable extends Jsonable[String]{
    def serialize(t: String) = Json.Str(t)
  }
  implicit object DoubleJsonable extends Jsonable[Double]{
    def serialize(t: Double) = Json.Num(t)
  }
  implicit object IntJsonable extends Jsonable[Int]{
    def serialize(t: Int) = Json.Num(t.toDouble)
  }
}
```
Now, we can define our `convertToJson` as:


```scala
def convertToJson[T](x: T)(implicit converter: Jsonable[T]): Json = {
  converter.serialize(x)
}
```

And it works for the cases where it should work:

```scala
@ convertToJson("hello")
res: Json = Str("hello")

@ convertToJson(123)
res: Json = Num(123.0)

@ convertToJson(123.56)
res: Json = Num(123.56)
```


And fails for the cases it shouldn't work:

```scala
@ convertToJson(new java.io.File("."))
could not find implicit value for parameter converterJsonable[java.io.File]
convertToJson(new java.io.File("."))
              ^
Compilation Failed
```

This pattern is common enough that Scala provides a shorthand syntax for 
writing the `convertToJson` function:

```scala
def convertToJson[T: Jsonable](x: T): Json = {
  implicitly[Jsonable[T]].serialize(x)
}
```

Thus, using Type-class implicits, we are able to make `convertToJson` take 
any one of an arbitrary set of types, with no common super-type between them,
while still letting the compiler reject cases where you pass in an invalid 
type. While there is some amount of boilerplate setting this up (e.g. all the
`implicit Object FooJsonable...` declarations above) the boilerplate only needs
to be defined once per type (e.g. `Jsonable`) for functions all over a codebase
to make use of.

### Method Overloading 

Someone who has programmed in Java or a similar language may have used method
overloading in the past to get this kind of functionality. e.g. defining
`convertToJson` as:

```scala
def convertToJson(t: String) = Json.Str(t)
def convertToJson(t: Double) = Json.Num(t)
def convertToJson(t: Int) = Json.Num(t.toDouble)
```

This works, allowing multiple different types to be passed to `convertToJson`
while disallowing invalid types at compile time, just as our Type-class 
Implicits version written above:

```scala
@ convertToJson("Hello")
res5: Json.Str = Str("Hello")

@ convertToJson(1.23)
res6: Json.Num = Num(1.23)

@ convertToJson(new java.io.File(","))
cmd7.sc:1: overloaded method value convertToJson with alternatives:
  (t: Int)$sess.cmd0.Json.Num <and>
  (t: Double)$sess.cmd0.Json.Num <and>
  (t: String)$sess.cmd0.Json.Str
 cannot be applied to (java.io.File)
val res7 = convertToJson(new java.io.File(","))
           ^
Compilation Failed
```

However, where this falls down is if you need to use `convertToJson` in another
function. For example, maybe I want to write the following functions:

```scala
def convertToJsonAndPrint(x: T): Unit
def convertMultipleItemsToJson(x: Array[T]): Seq[Json]
def convertFutureToJson(x: Future[T]): Future[Json]
```

Using operator overloading, you have to duplicate each of these methods once
for each type that can be converted to JSON. This results in a lot of 
duplication:

```scala
def convertToJson(t: String) = Json.Str(t)
def convertToJson(t: Double) = Json.Num(t)
def convertToJson(t: Int) = Json.Num(t.toDouble)

def convertToJsonAndPrint(t: String) = println(convertToJson(t))
def convertToJsonAndPrint(t: Double) = println(convertToJson(t))
def convertToJsonAndPrint(t: Int) = println(convertToJson(t))

def convertMultipleItemsToJson(t: Array[String]) = t.map(convertToJson)
def convertMultipleItemsToJson(t: Array[Double]) = t.map(convertToJson)
def convertMultipleItemsToJson(t: Array[Int]) = t.map(convertToJson)
```

This works:

```scala
@ convertToJsonAndPrint(123)
Num(123.0)

@ convertMultipleItemsToJson(Array("Hello", "world"))
res14: Array[Json.Str] = Array(Str("Hello"), Str("world"))
```

But at the cost of duplicating every operation once per-type. Here we only have
three types and three operation, resulting in 9 methods in total, but in a 
larger program you may easily have 10 different types which are convertible to
JSON, which are called by a hundred different methods. While you could still 
use method overloading, you'd need to write 1000 duplicate methods to make it 
work.

Using Type-class implicits, there's some boilerplate in defining the implicits
as we did above, but once that's done each additional operation no longer needs
N duplicate methods in order to work with any `Jsonable` type `T`:
 
```scala
def convertToJson[T: Jsonable](x: T): Json = {
  implicitly[Jsonable[T]].serialize(x)
}
def convertToJsonAndPrint[T: Jsonable](x: T) = println(convertToJson(x))
def convertMultipleItemsToJson[T: Jsonable](t: Array[T]) = t.map(convertToJson(_))
```

And this works just the same as the method-overloaded version above, just with
less duplication:

```scala
@ convertToJsonAndPrint(123)
Num(123.0)

@ convertMultipleItemsToJson(Array("Hello", "world"))
res22: Array[Json] = Array(Str("Hello"), Str("world"))
```

In general, while method overloading works, it is better to use 
Type-class implicits. It is a bit more verbose to set up the `Jsonable` trait 
at the start, but that it avoids having to duplicate methods throughout your 
codebase which use `convertToJson`. Instead, any method in your codebase just 
needs to take a `[T: Jsonable]` type parameter and it will automatically (and 
consistently!) be able to work with the same set of types that the original 
`convertToJson` method could, and work with other `[T: Jsonable]` methods
(e.g. calling into `convertToJson` in their implementation) completely 
seamlessly.

--------------------------------------------------------------------------------

Type-class Implicits are a broadly useful pattern, and are a very different
pattern than the Implicit Contexts we describe above: 

- Implicit Contexts tend to have different values injected in each time, 
  selected by the user of the library. Type-Class Implicits tend
  to always have the same value for each type, e.g. `Jsonable[Int]` or 
  `Jsonable[Seq[String]]`, and often it is selected by the author of the 
  library instead of the programmer using it. While it's possible you may 
  find yourself wanting to serialize an `Int` into something other than a JSON
  number, it's not going to be something you do very often.
  
- Implicit Contexts are usually full of data, and are often even mutable. 
  Type-Class Implicits tend to have none of that: their contribution is usually 
  a single pure function.

Note that although technically Type-class implicits and 
[Implicit Contexts](#implicit-contexts) use the same "implicit parameter" 
language features, they are totally different patterns and should not be mixed.
For example, you should not pass in mutable or stateful Type-class implicits.
Although technically valid Scala, it will definitely confuse most future 
readers of your code.

Many libraries in the wild use Type-class Implicits:

- [Scalatags](https://github.com/lihaoyi/scalatags) provides Type-Class 
  Implicits for `AttrValue[T]` and `StyleValue[T]`, for every `T` that can be 
  used as a HTML attribute or CSS style value
  
- [Spray-Json](https://github.com/spray/spray-json) uses Type-Class Implicits
  almost identically as described here: to control which types are acceptable
  for JSON serialization
  
- [PPrint](http://www.lihaoyi.com/upickle-pprint/pprint/) uses Type-Class
  Implicits to control how things get pretty-printed: there are defaults for
  most built in types and case classes, but you can define your own 
  pretty-printing-style for your own types if you wish.


## Derived Implicits

One neat thing about using Type-class Implicits is that you can perform "deep"
checks. For example, what if apart from `Str` and `Num`, we also wanted to 
support JSON lists?
 
```scala
sealed trait Json
object Json{
  case class Str(s: String) extends Json
  case class Num(value: Double) extends Json
  case class List(items: Json*) extends Json
  // ... many more definitions
}
```

Now, we want to be able to serialize `scala.Seq` into a `Json.List`, but with
a caveat: only `scala.Seq`s which contain serializable things should be 
serializable! For example we want this to work:

```scala
convertToJson(Seq(1, 2, 3))
convertToJson(Seq("baz", "bar", "foo"))
```

But we want this to fail:

```scala
convertToJson(Seq(new java.io.File("."), new java.io.File("/")))
```

To do this, we can define an implicit `Jsonable` just like we did earlier, but
with a catch: this new `SeqJsonable` *itself* takes a type `T`, for which there
must be an implicit `Jsonable` available! This can be seen below in the 
`implicit def SeqJsonable[T: Jsonable]`:

```scala
trait Jsonable[T]{
  def serialize(t: T): Json
}
object Jsonable{
  implicit object StringJsonable extends Jsonable[String]{
    def serialize(t: String) = Json.Str(t)
  }
  implicit object DoubleJsonable extends Jsonable[Double]{
    def serialize(t: Double) = Json.Num(t)
  }
  implicit object IntJsonable extends Jsonable[Int]{
    def serialize(t: Int) = Json.Num(t.toDouble)
  }
  implicit def SeqJsonable[T: Jsonable]: Jsonable[Seq[T]] = new Jsonable[Seq[T]]{
    def serialize(t: Seq[T]) = {
      Json.List(t.map(implicitly[Jsonable[T]].serialize):_*)
    }
  }
}
```

Now, we can convert any `Seq` into a `Json`, as long as it contains something
that itself can be converted, such as an `Int` or `String`:

```scala
@ convertToJson(Seq(1, 2, 3))
res: Json = List(List(Num(1.0), Num(2.0), Num(3.0)))

@ convertToJson(Seq("baz", "bar", "foo"))
res: Json = List(List(Str("baz"), Str("bar"), Str("foo")))
```

But `Seq`s with non-convertable contents, like some `java.io.File`s, are 
rejected by the compiler:

```scala
@ convertToJson(Seq(new java.io.File("."), new java.io.File("/")))
cmd.sc:1: could not find implicit value for evidence parameter of type Jsonable[Seq[java.io.File]]
val res = convertToJson(Seq(new java.io.File("."), new java.io.File("/")))
                         ^
Compilation Failed
@
```

It even works for "deep" types, like if we pass in a `Seq[Seq[Seq[Int]]]`, it
resolves it and serializes it correctly:
 
```scala
@ convertToJson(Seq(Seq(Seq(1, 2, 3))))
res22: Json = List(List(List(List(List(List(Num(1.0), Num(2.0), Num(3.0)))))))
```
Whereas if we pass in a `Seq[Seq[Seq[java.io.File]]]`, it fails to compile:
 
```scala
@ convertToJson(Seq(Seq(Seq(new java.io.File(".")))))
cmd.sc:1: could not find implicit value for evidence parameter of type Jsonable[Seq[Seq[Seq[java.io.File]]]]
val res = convertToJson(Seq(Seq(Seq(new java.io.File(".")))))
                       ^
Compilation Failed
@
```

What we have done is we have set up the implicits such that at compile time,
it first look for something satisfying `Jsonable[Seq[T]]`, and then finding
our definition of `SeqJsonable`, it then tries to look for an implicit 
definition for `Jsonable[T]`. The compiler does this recursively, and hence is
able to, at compile time, decide that `Seq(Seq(Seq(1, 2, 3)))` is fine but 
`Seq(Seq(Seq(new java.io.File("."))))` is unacceptable.

Apart from better controlling what types are acceptable when serializing to 
JSON and rejected bad types at compile-time, and working recursively, using 
Derived Implicits has 
another advantage over a naive `match` statement on an `Any` value: you can 
let a user define implicits for their own types,
e.g. if I decide I want to let `java.io.File`s be serialized to JSON as a 
string containing their fully qualified path, I can do so:

```scala
@ implicit object FileJsonable extends Jsonable[java.io.File]{
    def serialize(t: java.io.File) = Json.Str(t.getAbsolutePath)
  }
defined object FileJsonable

@ convertToJson(Seq(Seq(Seq(new java.io.File(".")))))
res: Json = List(List(List(List(List(List(Str("/Users/lihaoyi/test/.")))))))
```

This opens up your protocol for users of your library to plug into: rather
than allowing just a fixed set of types, they can "register" their own types
by defining their own implicits for whatever they want. This is similar to 
registering handlers for each type in a global dictionary somewhere, but using
Derived Implicits the compiler is always able to ensure you never use a type 
nobody has registered a handler for. With a global dictionary of types, 
mistakes result in runtime errors.

## Type-driving Implicits

One neat feature of implicits is that they do not just depend on types to be
inferred, but they themselves can also affect the types a compiler infers as 
part of an expression. For example, consider the way that you can automatically
"widen" numbers by assigning a number of a smaller type to one of a larger 
type:

```scala
@ val x: Byte = 123
x: Byte = 123

@ val y: Short = x
y: Short = 123

@ val z: Long = y
z: Long = 123L

@ val a: Float = 1.23f
a: Float = 1.23F

@ val b: Double = a
b: Double = 1.2300000190734863
```

This generally does what you want, but sometimes misbehaves. For example:

```scala
@ val bigLong = Long.MaxValue - 1
bigLong: Long = 9223372036854775806L

@ val bigLong: Long = Long.MaxValue - 1
bigLong: Long = 9223372036854775806L

@ val bigFloat: Float = bigLong
bigFloat: Float = 9.223372E18F

@ val bigLong2: Long = bigFloat.toLong
bigLong2: Long = 9223372036854775807L

@ bigLong == bigLong2
res40: Boolean = false
```

Here, we see that `bigLong` is being automatically widened to `bigFloat`,
but when converting it back to `bigLong2`, we end up a different value. That's
probably not what someone would expect from a simple "widening", which is meant
to let you put a smaller value in a "wider" type but leave the value unchanged.

It turns out, you can define a function that does this widening manually:

```scala
@ {
  def widen[T, V](x: T)(implicit widener: Widener[T, V]): V = widener.widen(x)
  class Widener[T, V](val widen: T => V)
  object Widener{
    implicit object FloatWiden extends Widener[Float, Double](_.toDouble)
    implicit object ByteWiden extends Widener[Byte, Short](_.toShort)
    implicit object ShortWiden extends Widener[Short, Int](_.toInt)
    implicit object IntWiden extends Widener[Int, Long](_.toLong)
  }
  }
  
@ widen(1.23f: Float)
res12: Double = 1.2300000190734863

@ val byte: Byte = 123
byte: Byte = 123

@ val smallValue: Byte = 123
smallValue: Byte = 123

@ val shortValue = widen(smallValue)
shortValue: Short = 123

@ val intValue = widen(shortValue)
intValue: Int = 123

@ val longValue = widen(intValue)
longValue: Long = 123L
```

Here, you can see that every call to `widen` returns a different type; in fact, 
the returned types are entirely arbitrary, based on what implicit `Widener` 
objects we defined! In addition, trying to widen things that we didn't define 
`Widener`s for, such as `Long` or `Double`, fails to compile:

```scala
@ widen(longValue) 
cmd20.sc:1: could not find implicit value for parameter widener: $sess.cmd11.Widener[Long,V]
val res20 = widen(longValue)
                 ^
Compilation Failed

@ widen(1.23: Double) 
cmd20.sc:1: could not find implicit value for parameter widener: $sess.cmd11.Widener[Double,V]
val res20 = widen(1.23: Double) 
                 ^
Compilation Failed
```

Which is a nice property that ensures we don't have runtime errors from trying
to `widen` the wrong type.
  
This isn't a totally complete implementation of `widen`: in particular, it 
can't widen things more than one step, e.g. from `Byte` to `Long`, and isn't 
applied implicitly like Scala's default number-widening behavior. Nevertheless, 
it demonstrates an important fact: that you can define implicits that don't 
just depend on the expected types, but also play an active role in deciding 
what type gets inferred by the compiler. 

The same technique could be used to define a function that "generically" 
extends a Tuple into a larger Tuple:

```scala
@ {
  def extend[T, V, R](tuple: T, value: V)(implicit extender: Extender[T, V, R]): R = {
    extender.extend(tuple, value)
  }
  case class Extender[T, V, R](val extend: (T, V) => R)
  object Extender{
    implicit def tuple2[T1, T2, V, R] = Extender[(T1, T2), V, (T1, T2, V)]{
      case ((t1, t2), v) => (t1, t2, v)
    }
    implicit def tuple3[T1, T2, T3, V, R] = Extender[(T1, T2, T3), V, (T1, T2, T3, V)]{
      case ((t1, t2, t3), v) => (t1, t2, t3, v)
    }
    implicit def tuple4[T1, T2, T3, T4, V, R] = 
      Extender[(T1, T2, T3, T4), V, (T1, T2, T3, T4, V)]{
        case ((t1, t2, t3, t4), v) => (t1, t2, t3, t4, v)
      }
    // ... and so on until tuple21 ...
  }
  }
```

Just like in the `widen` example earlier, the return type of `extend` depends 
on what implicit `Extender`s you defined. As a result, although e.g. `Tuple2`
and `Tuple3` have no direct relation to each other in the class hierarchy, we 
can now use `extend` on a `Tuple2` and the compiler automatically figures out 
the result should be a `Tuple3`.

```scala
@ val t = (1, "lol")
t: (Int, String) = (1, "lol")

@ val bigger = extend(t, true)
bigger: (Int, String, Boolean) = (1, "lol", true)

@ val evenBigger = extend(bigger, List())
evenBigger: (Int, String, Boolean, List[Nothing]) = (1, "lol", true, List())
```

The takeaway from this section is that you can use the Type-driving Implicits 
pattern to control how a function's return type gets inferred, depending on
what instances of an implicit parameter are in scope. This is a somewhat clever
trick that you probably shouldn't use lightly: having the return type of a 
function depend on the argument types in completely arbitrary ways can easily 
be extremely confusing! Nevertheless, in the rare cases where you really want
to do this (e.g. my [FastParse](https://github.com/lihaoyi/fastparse) library 
uses a `Extender` implicit similar to this one) this is how you do it.

--------------------------------------------------------------------------------

That ends this quick overview of some of the more fundamental patterns around
using implicit parameters in Scala. This is by no means exhaustive, as there 
are countless others not described here: 

- More advanced ones like the 
  [Aux Pattern](http://gigiigig.github.io/posts/2015/09/13/aux-pattern.html) 
  from [Shapeless](https://www.github.com/milessabin/shapeless). Shapeless
  in-general is full of clever things you can do with implicits, too many to
  even list here, let alone discuss.
  
- Those using Implicit *Conversions* instead of *Parameters*, e.g. to implement
  implicit constructors (similar to those in C#) or extension methods. 
  
- [Derived Implicits](#derived-implicits) that work on case classes, versus 
  just generic collections as those shown above. Shapeless provides these, 
  and I perform a similar (though more primitive and far less principled) sort 
  of derivation in my own 
  [uPickle and PPrint](https://github.com/lihaoyi/upickle-pprint) libraries. 

- Compiler/Macro-powered [Implicit Contexts](#implicit-contexts), such as implicit 
  [ClassTag](http://www.scala-lang.org/api/2.12.x/scala/reflect/ClassTag.html)s 
  or those in my [SourceCode](https://github.com/lihaoyi/sourcecode) library, 
  which let you on-demand access "additional" information from the compiler
  such as line numbers or file names that is not normally available to your 
  program.

What are your own favorite implicit tricks and patterns that you use in your
own code, or you've seen in someone else's? Let us know in the comments below!
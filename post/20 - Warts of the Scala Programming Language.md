Scala is my current favorite general-purpose programming language. However,
it definitely has its share of flaws. While some are deep trade-offs in the 
design of the language, others are trivial, silly issues which cause 
frustration far beyond their level of sophistication: "warts". This post will 
explore some of what, in my opinion, are the warts of the Scala programming 
language, to hopefully raise awareness of their existence as problems and build 
a desire to fix them in the broader community.

-------------------------------------------------------------------------------


I think that Scala has a disproportionate ratio of superficial-warts to 
deep-design-problems. While many languages have a relatively clean superficial
syntax and semantics that hides a crazy and unpredictable core logic, Scala in 
my opinion has a relatively elegant core logic that's overlaid by a gnarly, 
messy superficial syntax and semantics.
 
Not every problem with a programming language is a "wart". Many problems arise
from deep design decisions and trade-offs, where there often isn't a "correct"
solution, or where proposed solutions often come with their own share of 
(often unsolved) problems.

However, many problems are simply incidental. They exist for no particular 
reason, have no particular benefit for being around, and have "obvious" 
solutions that would be non-controversial. They really should have been fixed
years ago, though the second best time to fix them starts today.

This post will cover some things that I don't consider warts, to set the stage
for exploring an incomplete list of things I consider the warts of the Scala programming 
language 


- [Non Warts](#non-warts)    
    - [Universal Equality](#universal-equality)
    - [Running on the JVM](#running-on-the-jvm)
    - [Type-erasure](#type-erasure)
    - [Implicits](#implicits)
- [Warts](#warts)
    - [Weak eta-expansion](#weak-eta-expansion)
    - [Calling zero-parameter methods without parens](#calling-zero-parameter-methods-without-parens)
    - [Needing curlies/case for destructuring anonymous functions](#needing-curliescase-for-destructuring-anonymous-functions)
    - [Extraneous extension methods on Any](#extraneous-extension-methods-on-any)
    - [Convoluted de-sugaring of for-comprehensions](#convoluted-de-sugaring-of-for-comprehensions)
    - [For-comprehensions syntax restrictions](#for-comprehension-syntax-restrictions)
    - [Abstract/non-final case classes](#abstractnon-final-case-classes)
    - [Classes cannot have only implicit parameter lists](#classes-cannot-have-only-implicit-parameter-lists)
    - [Presence of comments affects program logic](#presence-of-comments-affects-program-logic)
    - [Conflating total destructuring with partial pattern-matching](#conflating-total-destructuring-with-partial-pattern-matching)
- [Conclusion](#conclusion)

## Not Warts

Just because people complain about something doesn't mean it's a "wart".

Here are a few of things in the Scala language that I don't consider to be
warts:

- [Universal Equality](#universal-equality)
- [Running on the JVM](#running-on-the-jvm)
- [Type-erasure](#type-erasure)
- [Implicits](#implicits)

### Universal Equality

```scala
@ val x = "hello"
x: String = "hello"

@ val y = 123
y: Int = 123

@ x == y
res68: Boolean = false
```

Scala lets you compare any two values for equality via the `==` operator,
which forwards to the Java `.equals` method under the hood defined a 

```scala
def equals(other: Any): Boolean
```

This allows for all sorts of human mistakes to go uncaught at compile time: the 
above example will *never* return `true`, regardless of the values of `x` and 
`y`, and the compiler should be able to figure that out and tell us. While this
example is short and the mistake is obvious, in larger examples it's much less
obvious, e.g. in this example from
[Lincon Atkinson's blog](http://latkin.org/blog/2017/05/02/when-the-scala-compiler-doesnt-help/):
 
```scala
> "foobar".toList == List('f','o','o','b','a,'r')
false
```

Despite the fact that this seems to be letting through an obvious class of 
errors, it's not clear to me what the "correct" way of fixing it is:

- Most other programming languages follow this exact behavior: Java, C#, etc.
  all let you compare values willy-nilly even if their types mean can never be 
  equal

- Haskell has an [Eq](https://hackage.haskell.org/package/base-4.9.1.0/docs/Data-Eq.html)
  type-class which seems like what we'd want: restricting comparisons only to 
  types which it makes sense.
  
- The Scala people have come up with a similar-but-different idea in their 
  [Multiversal Equality](http://www.scala-lang.org/blog/2016/05/06/multiversal-equality.html)
  proposal.

The unsafe-ness of equality is a problem: making mistakes like this is very 
common in my experience. e.g. when refactoring a code to change `String`s to
an `Id` class, you can fix all the compile errors, but all the equality checks
are now invalid, and without the compiler's help you need to go hunt them all 
down manually. Furthermore, I'm sure there is a solution out there that can
make things better, but the solution isn't "trivial" or "obvious" enough to
make me consider this a Wart.

### Running on the JVM

Scala has traditionally run on the JVM. It now runs on Javascript with 
[Scala.js](http://www.scala-js.org/), and more recently on LLVM with 
[Scala Native](http://www.scala-native.org/), but the heart of the ecosystem
and all the tooling runs on the JVM.

This gives Scala all the benefits of the JVM: good stack traces, 
monitoring/deployment tools, a fast JIT and good garbage collectors, a 
tremendous ecosystem of libraries 

This also gives Scala all the constraints of the JVM: boxing everywhere
resulting in unnecessary GC pressure, a disk-heavy binary format for classfiles
and jars, and a tremendously slow startup time for any non-trivial program. 

For example, the Ammonite script-runner (which
is a medium-sized command-line tool) spends up to half a second *just 
classloading* when running a script, *before* counting any actual program-logic 
being run:

![JvmSlow](warts/JvmSlow.png)

However, the reliance on the JVM is deep enough to be a core feature of the
Scala language. It's debatable whether Scala would have been able to achieve
the success it has without piggy-backing on all the JVM has to offer. While it
would be nice to slowly reduce that reliance, which is happening now with 
Scala.js and Scala-Native, it's a hard trade-off that I can't consider a Wart.

### Type-erasure

```scala
@ val x = List(1, 2, 3)
x: List[Int] = List(1, 2, 3)

@ val y = x.asInstanceOf[List[String]]
y: List[String] = List(1, 2, 3)

@ println(y.last)
java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.String
  $sess.cmd76$.<init>(cmd76.sc:1)
  $sess.cmd76$.<clinit>(cmd76.sc:-1)
```

On the JVM, and now on the Javascript platform using Scala.js, generic types
are erased: that means you can `asInstanceOf` or `isInstanceOf` or pattern-match
against them, and they always are considered equal. That results in the above
behavior, where you can cast a `List[Int]` into a `List[String]` without any
errors (though it gives off a warning) and it only crashes at runtime when
you try to extracta value from the *already casted* list!

This behavior is clearly unsafe, and can result in weird and hard-to-track-down
problems. However, it's not clear to me what the correct behavior is:

- You could not-erase generic types, like in C#, which contains a performance
  cost when compiling to the JVM and Javascript and makes interop with erased 
  JVM libraries less convenient.
  
- You could erase *even more*, like in Scala.js, where even the top-level types
  are (partially) erased at runtime, meaning the above example runs perfectly
  fine without any errors!
  
```scala
// Scala.js
val x = List(1, 2, 3)
val y = x.asInstanceOf[List[String]]
println(y.last) // 3
```

"Full" erasure on Scala.js for Javascript types also allows for several 
interesting features, such as phantom-types and zero-overhead wrapper-types,
that are difficult/impossible to do on the JVM without jumping through hoops.
It also allows much more aggressive optimizations, since the optimizer no 
longer needs to keep adding guards to throw `ClassCastExceptions` if you get
a cast wrong. The cost of this is you get more confusing error messages if 
you write invalid casts. In my experience, this is basically a non-issue in 
practice, since "typical" Scala programs tend not to rely on casts.

Lastly, it could be argued that having "Full" erasure helps enforce 
parametricity/encapsulation: you can no longer check what class something is
when calling a method, and are thus forced to rely on whatever abstract 
interface you are provided with.

Hence, I don't know what the correct answer is here. There are good arguments 
why both "more erasure" and "less erasure" are good things, so without a 
obvious better-way I don't consider this a Wart.
 

### Implicits

Scala allows implicit parameters, which can be passed manually but can also be
automatically passed to functions that require them based on their type. For
example, in the following case, we call the `repeat` function twice: once passing
in `count` manually as 2, an once passing it implicitly via the `implicit val c` 
which is `3`:

```scala
@ def repeat(s: String)(implicit count: Int) = s * count
defined function repeat

@ repeat("hello")(2)
res78: String = "hellohello"

@ repeat("hello")
cmd79.sc:1: could not find implicit value for parameter count: Int
val res79 = repeat("hello")
                  ^
Compilation Failed

@ implicit val c = 3
c: Int = 3

@ repeat("hello")
res80: String = "hellohellohello"
```

Scala also allows implicit conversions, that can be used to define automatic 
conversions from out type to another:

```scala
@ case class Name(s: String){
    require(s.nonEmpty)
  }
defined class Name

@ val n1 = Name("hello")
n1: Name = Name("hello")

@ val n2 = Name("")
java.lang.IllegalArgumentException: requirement failed
  scala.Predef$.require(Predef.scala:264)
  $sess.cmd81$Name.<init>(cmd81.sc:2)
  $sess.cmd83$.<init>(cmd83.sc:1)
  $sess.cmd83$.<clinit>(cmd83.sc:-1)

@ implicit def autoName(s: String): Name = Name(s)
defined function autoName

@ val n3: Name = "hello"
n3: Name = Name("hello")

@ val n3: Name = ""
java.lang.IllegalArgumentException: requirement failed
  scala.Predef$.require(Predef.scala:264)
  $sess.cmd81$Name.<init>(cmd81.sc:2)
  $sess.cmd84$.autoName(cmd84.sc:1)
  $sess.cmd86$.<init>(cmd86.sc:1)
  $sess.cmd86$.<clinit>(cmd86.sc:-1)
```

While this behavior may be confusing, unintuitive and hard-to-debug sometimes,
it also forms the basis for a large number of common 
[Scala design patterns](http://www.lihaoyi.com/post/ImplicitDesignPatternsinScala.html).
Scala wouldn't be Scala without implicits.

The actual implementation of implicits contains many strange corner cases, 
in addition to out-right bugs, but implicits themselves are so core to Scala 
that I don't think they could be considered a "wart"


## Warts

Warts are somewhere on the spectrum between design issues and outright bugs.
These are things that, I think, have obvious solutions that would be both 
easy and relatively uncontroversial to fix, that nonetheless cause annoyance
and frustration disproportionate given how trivial the issue is.

The warts I'm going to discuss are:

- [Weak eta-expansion](#weak-eta-expansion)
- [Calling zero-parameter methods without parens](#calling-zero-parameter-methods-without-parens)
- [Needing curlies/case for destructuring anonymous functions](#needing-curliescase-for-destructuring-anonymous-functions)
- [Extraneous extension methods on Any](#extraneous-extension-methods-on-any)
- [Convoluted de-sugaring of for-comprehensions](#convoluted-de-sugaring-of-for-comprehensions)
- [For-comprehensions syntax restrictions](#for-comprehension-syntax-restrictions)
- [Abstract/non-final case classes](#abstractnon-final-case-classes)
- [Classes cannot have only implicit parameter lists](#classes-cannot-have-only-implicit-parameter-lists)
- [Presence of comments affects program logic](#presence-of-comments-affects-program-logic)
- [Conflating total destructuring with partial pattern-matching](#conflating-total-destructuring-with-partial-pattern-matching)

### Weak eta-expansion

Scala maintains a distinction between "functions" and "methods": in general,
methods are things you call on an object, whereas functions are objects 
themselves. However, since they're so similar ("things you can call"), it gives 
you a way to easily wrap a method in a function object called "eta expansion"

```scala
@ def repeat(s: String, i: Int) = s * i
defined function repeat

@ repeat("hello", 2)
res89: String = "hellohello"

@ val func = repeat _
func: (String, Int) => String = $sess.cmd90$$$Lambda$2796/1082786554@2a3983b9

@ func("hello", 3)
res91: String = "hellohellohello"
```

Above, we use the underscore `_` to assign `repeat _` to a value `func`, which 
is then a function object we can call. This can happen automatically, without the
`_`, based on the "expected type" of the place the method is being used. For 
example, if we expect `func` to be a `(String, Int) => String`, we can assign
`repeat` to it without the `_`:

```scala
@ val func: (String, Int) => String = repeat
func: (String, Int) => String = $sess.cmd92$$$Lambda$2803/669946146@46226d53

@ func("hello", 3)
res92: String = "hellohellohello"
```

Or by stubbing out the arguments with `_` individually:

```scala
@ val func = repeat(_, _)
func: (String, Int) => String = $sess.cmd98$$$Lambda$2832/1025548997@358b1f86
```

This works, but has a bunch of annoying limitations. Firstly, even though you 
can fully convert the method `repeat` into a `(String, Int) => String` value 
using `_`, you cannot *partially* convert it:

```scala
@ val func = repeat("hello", _)
cmd4.sc:1: missing parameter type for expanded function 
((x$1: <error>) => repeat("hello", x$1))
val func = repeat("hello", _)
                           ^
Compilation Failed
```

*Unless* you know the the "expected type" of `func`, in which case you *can*
partially convert it:

```scala
@ val func: Int => String = repeat("hello", _)
func: Int => String = $sess.cmd93$$$Lambda$2808/1138545802@2c229ed2
```

Or you provide the type to the partially-applied-function-argument `_` manually:
```scala

@ repeat("hello", _: Int)
res4: Int => String = $sess.cmd4$$$Lambda$1988/1407003104@5eadc347
```

This is a bit strange to me. If I can easily convert the entire `repeat` method
into a function without specifying any types, why can I not convert it into a 
function if I already know one of the arguments? After all, I have provided
strictly *more* information in the `repeat("hello", _)` case than I have in the
`repeat(_, _)` case, and yet somehow type inference got worse! 

Furthermore, there's a more fundamental issue: if I know that `repeat` is a 
method that takes two arguments, why can't I just do this?

```scala
@ val func = repeat
cmd99.sc:1: missing argument list for method repeat in object cmd88
Unapplied methods are only converted to functions when a function type is expected.
You can make this conversion explicit by writing `repeat _` or `repeat(_,_)` instead of `repeat`.
val func = repeat
           ^
Compilation Failed
```

After all, since the compiler already knows that `repeat` is a method, and that 
it doesn't have it's arguments provided, why not convert it for me? Why force me
to go through the `_` or `(_, _)` dance, or why ask me to provide an expected type
for `func` if it already knows the type of `repeat`? 

In other languages with first-class functions, like Python, this works fine:

```python
>>> def repeat(s, i):
...     return s * i
...

>>> func = repeat

>>> func("hello", 3)
'hellohellohello'
```


The lack of automatic eta-expansion results in people writing weird code to work
around it, such as this example from [ScalaMock](http://scalamock.org/):

```scala
"drawLine" should "interact with Turtle" in {
  // Create mock Turtle object
  val mockedTurtle = mock[Turtle]
 
  // Set expectations
  (mockedTurtle.setPosition _).expects(10.0, 10.0)
  (mockedTurtle.forward _).expects(5.0)
  (mockedTurtle.getPosition _).expects().returning(15.0, 10.0)
 
  // Exercise System Under Test
  drawLine(mockedTurtle, (10.0, 10.0), (15.0, 10.0))
}
```

Here, the weird `(foo _)` dance is something that they have to do purely 
because of this restriction in eta-expansion.

While I'm sure there are good implementation-reasons why this doesn't work, I
don't see any reason this shouldn't work from a language-semantics point of 
view. From a user's point of view, methods and functions are just "things you 
call", and Scala is generally successful and not asking you to think about the
distinction between them.

However, in cases like this, I think there isn't a good reason the compiler 
shouldn't try a bit harder to figure out what I want before giving up and 
asking me to pepper `_`s or expected types all over the place. The compiler
*already has* all the information it needs - after all, it works if you put 
an `_` after the method - and it just needs to use that information when
the `_` isn't present.


### Calling zero-parameter methods without parens

Scala lets you leave off empty-parens lists when calling functions. This
looks kind of cute when calling getters:
```scala
@ def getFoo() = 1337
defined function foo

@ getFoo()
res8: Int = 1337

@ getFoo
res9: Int = 1337
```

However, it doesn't really make sense when you consider how this works in
most other languages, such as Python:

```python
>>> def getFoo():
...     return 1337
...
>>> getFoo()
1337
>>> func = getFoo
>>> func()
1337
```

After all, if `getFoo()` is a `Int`, why shouldn't `getFoo` without the 
parens be a `() => Int`? After all, calling a `() => Int` with parens
give you an `Int`. However, in Scala methods are "special", as shown above,
and methods with empty parens lists are treated even more specially.

Furthermore, this feature really doesn't make sense when you start pushing it:

```scala
@ def bar()()()()() = 2
defined function bar

@ bar
res11: Int = 2

@ bar()
res12: Int = 2

@ bar()()
res13: Int = 2

@ bar()()()
res14: Int = 2

@ bar()()()()
res15: Int = 2

@ bar()()()()()
res16: Int = 2

@ bar()()()()()()
cmd17.sc:1: Int does not take parameters
val res17 = bar()()()()()()
                         ^
Compilation Failed
```

Is this really the behavior we expect in a statically-typed language, that you
can call this method with any number of argument lists `0 < n <= 5` and 
it'll do the same thing regardless? What on earth is the type of `bar`? The 
Scala community likes to think that it's "definition-side variance" is better 
than Java's "use-site variance", but here we have Scala providing 
definition-site parens where every caller of `bar` can pick and choose how many 
parens they want to pass.

The only reason I've heard for this feature is to "let you call Java `getFoo`
methods without the parens", which seems like an exceedingly weak justification
for a language feature that so thoroughly breaks the expectations of a 
statically-typed language. This causes a significant amount of confusion for 
[newbies trying to learn the language](https://stackoverflow.com/questions/8303817/nine-ways-to-define-a-method-in-scala),
and I think really should be removed: methods should be called with as many 
sets of parentheses as they are defined with (excluding implicits), and any 
method call missing parens should be eta-expanded into the appropriate
function value.


### Needing curlies/case for destructuring anonymous functions

Scala lets you create anonymous functions with a `x => x + 1` syntax:

```scala
@ Seq(1, 2, 3).map(x => x + 1)
res44: Seq[Int] = List(2, 3, 4)
```

But if you want to have the function work on e.g. Tuples, that doesn't work:
```scala
@ Seq((1, 2), (3, 4), (5, 6)).map((x, y) => x + y + 1)
cmd45.sc:1: missing parameter type
Note: The expected type requires a one-argument function accepting a 2-Tuple.
      Consider a pattern matching anonymous function, `{ case (x, y) =>  ... }`
val res45 = Seq((1, 2), (3, 4), (5, 6)).map((x, y) => x + y + 1)
                                             ^
cmd45.sc:1: missing parameter type
val res45 = Seq((1, 2), (3, 4), (5, 6)).map((x, y) => x + y + 1)
                                                ^
Compilation Failed
```

And you need to then swap over to a similar-but-annoyingly-different
`{ case ... => ...}` syntax:

```scala
@ Seq((1, 2), (3, 4), (5, 6)).map{case (x, y) => x + y + 1}
res45: Seq[Int] = List(4, 8, 12)
```

There are two changes here:

- Needing the strange `case` just to destructure a tuple. Can't the compiler 
  see that it's getting a `(A, B) => C` function, needs a `(Tuple[A, B]) => C`
  function, and convert it automatically?

- Needing the curly brackets, rather than parens. After all, even if we demanded
  the `case` keyword, there's no reason that `Seq(1, 2, 3).map(case x => x + 1)`
  shouldn't work
  
Happily, both these limitations are 
[slated to go away](https://github.com/lampepfl/dotty/pull/898) in a future 
version. However, right now they are definitely an unnecessary, 
trivial annoyance when writing Scala. 

### Extraneous extension methods on Any

Scala adds a bunch of extension methods on every value in your codebase:

```scala
@ 1.ensuring(_ > 2)
java.lang.AssertionError: assertion failed
  scala.Predef$.assert(Predef.scala:204)
  scala.Predef$Ensuring$.ensuring$extension2(Predef.scala:316)
  $sess.cmd25$.<init>(cmd25.sc:1)
  $sess.cmd25$.<clinit>(cmd25.sc:-1)

@ 1.formatted("hello %s")
res26: String = "hello 1"

@ 1.synchronized(println("yo"))
yo
```

It really shouldn't. In my experience, there extension methods are rarely
ever used. If someone wants to use them, they can import the functions
themselves or write their own extensions. It doesn't make any sense to pollute 
the method-namespace of every value in existance to add some unused 
functionality.

### Convoluted de-sugaring of for-comprehensions

Scala lets you write for-comprehensions, which are converted into a chain
of `flatMap`s an `map`s as shown below:

```scala
@ val (x, y, z) = (Some(1), Some(2), Some(3))
x: Some[Int] = Some(1)
y: Some[Int] = Some(2)
z: Some[Int] = Some(3)
@ for{
    i <- x
    j <- y
    k <- z
  } yield i + j + k
res40: Option[Int] = Some(6)
@ desugar{
    for{
      i <- x
      j <- y
      k <- z
    } yield i + j + k
  }
res41: Desugared = x.flatMap{ i => 
  y.flatMap{ j => 
    z.map{ k => 
      i + j + k
    }
  }
}
```

I have nicely formatted the desugared code for you, but you can try this 
yourself in the [Ammonite Scala REPL](http://www.lihaoyi.com/Ammonite/) to 
verify that this is what the for-comprehension gets transformed into. 

This is a convenient way of implementing nested loops over lists, and happily
works with things that aren't lists: `Option`s (as shown above), `Future`s, 
and many other things.

You can also assign local values within the for-comprehension, e.g.

```scala
@ for{
    i <- x
    j <- y
    foo = 5
    k <- z
  } yield i + j + k + foo
res42: Option[Int] = Some(11)
```

The syntax is a bit wonky (you don't need a `val`, you can't define `def`s or
`class`es or run imperative commands without `_ = println("debug")`) but for 
simple local assignments it works. You may expect the above code to be 
transformed into something like this

```scala
res43: Desugared = x.flatMap{ i => 
  y.flatMap{ j =>
    val foo = 5
    z.map{ k => 
      i + j + k
    }
  }
}
```

But it turns out it instead gets converted into something like this:

```scala
@ desugar{
    for{
      i <- x
      j <- y
      foo = 5
      k <- z
    } yield i + j + k + foo
  }
res43: Desugared = x.flatMap(i => 
  y.map{ j =>
    val foo = 5
    scala.Tuple2(j, foo)
  }.flatMap((x$1: (Int, Int)) => 
    (x$1: @scala.unchecked) match {
    case Tuple2(j, foo) => z.map(k => i + j + k + foo)
    }
  )
)
```

Although it is roughly equivalent, and ends up with the same result in most 
cases, this output format is tremendously convoluted and wastefully inefficient
(e.g. creating and taking-apart unnecessary `Tuple2`s). As far as I can tell,
there is no reason at all not to generated the simpler version of the code 
shown above.

### For-comprehensions syntax restrictions

As mentioned above, you cannot have `def`s, `class`es, or imperative
statements in the generators of a for-comprehension:

```scala
scala> for{
     |   i <- Seq(1)
     |   println(i)
     |   j <- Seq(2)
<console>:4: error: '<-' expected but ';' found.
  j <- Seq(2)
^
```

This is a rather arbitrary restriction, and as far as I can tell doesn't serve 
any purpose, and forces you to put random `_ =` prefixes on your statements to
make things compile:

```scala
scala> for{
     |   i <- Seq(1)
     |   _ = println(i)
     |   j <- Seq(2)
     | } yield j
1
res0: Seq[Int] = List(2)
```

There really isn't any reason that this shouldn't work out-of-the-box, and 
convert say:

```scala
for{
  i <- Seq(1)
  def debug(s: Any) = println("Debug " + s)
  debug(i)
  j <- Seq(2)
  debug(j)
  k <- Seq(3)
} yield i + j + k
```

Into

```scala
Seq(1).flatMap{ i => 
  def debug(s: Any) = println("Debug " + s)
  debug(i)
  Seq(2).flatMap{ j =>
    debug(j)
    Seq(3).map{ k => 
      i + j + k
    }
  }
}
```


### Abstract/non-final case classes

You can inherit from case classes and extend them with new functionality:

```scala
@ case class Foo(i: Int)
defined class Foo

@ Foo(1)
res18: Foo = Foo(1)

@ Foo(1).i
res19: Int = 1

@ class Bar extends Foo(1)
defined class Bar

@ (new Bar).i
res21: Int = 1
```

You can even declare it an `abstract case class` to force someone to inherit
from it rather than instantiating it directly. If you want your
case class to not allow inheritance you should label it `final`

As far as I can tell, "nobody" does any of this: people don't inherit from case 
classes, declare their case classes `abstract`, or rememebr to mark them 
`final`. Literally the only programmer I've ever seen making good use of 
`abstract` case classes and inheritance is probably Martin Odersky himself. I 
think we should disallow it, and just force people to use normal classes if 
they want to inherit from them.

### Classes cannot have only implicit parameter lists

This doesn't work:

```scala
@ class Foo(i: Int)
defined class Foo

@ new Foo(1)
res50: Foo = $sess.cmd49$Foo@7230510

@ class Bar(implicit i: Int)
defined class Bar

@ new Bar(1)
cmd52.sc:1: no arguments allowed for nullary constructor Bar: ()(implicit i: Int)$sess.cmd51.Bar
val res52 = new Bar(1)
                    ^
Compilation Failed
```

But this does:

```scala
@ new Bar()(1)
res52: Bar = $sess.cmd51$Bar@467de021
```

This one straddles the line between "Wart" and "Bug", but definitely should
be fixed so that a class defined with one argument list doesn't magically 
sprout two.

## Presence of multiple-newlines/comments affects program logic

Did you know the presence or absence of comments can affect the logic of your
program?

```scala
@ object Foo{
    def bar(x: Any) = println("Foo#bar(x) " + x)
    def bar = println("Foo#bar")
  }
defined class Foo

@ val baz = 1
baz: Int = 1

@ {
  Foo bar
  baz
  }
Foo#bar(x) 1

@ {
  Foo bar

  baz
  }
Foo#bar

@ {
  Foo bar
  // Wooo!
  baz
  }
Foo#bar(x) 1

@ {
  Foo bar
  // Wooo!

  baz
  }
Foo#bar


@ {
  Foo bar
  // Wooo!
  // Hoo!
  baz
  }
Foo#bar(x) 1
```

As you can see, this code behaves differently if we have a line between
the `Foo bar` and the `baz`, *unless* that line has a line comment on it!
When there's no newlines or the newline is filled by a comment, 
`Foo.bar(x: Any)` gets called, and when there's a newline *not* filled by a
comment then the other overload `Foo.bar` gets called.

There are other places in the language syntax where this is the case:

```scala
@ {
  class X(x: Int)(y: Int)

  new X(1)(2)

  class Y(x: Int)
         (y: Int)

  new Y(1)(2)
  }
defined class X
res103_1: X = $sess.cmd103$X@6a58eda9
defined class Y
res103_3: Y = $sess.cmd103$Y@136ca935

@ {
  class Z(x: Int)

         (y: Int)
  }
cmd105.sc:3: not found: value y
       val res105_1 = (y: Int)
                       ^
Compilation Failed

@ {
  class W(x: Int)
  // Woohoo
         (y: Int)
  }
defined class W
```

A full listing of the places where this "comment can change behavior of 
newlines" can be found in the 
[OneNLMax](https://github.com/lihaoyi/fastparse/search?utf8=%E2%9C%93&q=onenlmax&type=)
rule of the ScalaParse grammar. I don't have an immediate answer for what the
correct solution is here, but I'm 99.9% sure we should make it so comments 
like this don't affect the semantics of a Scala program!

### Conflating total destructuring with partial pattern-matching

The following example, also from 
[Lincon Atkinson's blog](http://latkin.org/blog/2017/05/02/when-the-scala-compiler-doesnt-help/), 
compiles without warning and fails with an exception at runtime:

```scala
@ val (a, b, c) = if (true) "bar" else Some(10)
scala.MatchError: bar (of class java.lang.String)
  $sess.cmd105$.<init>(cmd105.sc:1)
  $sess.cmd105$.<clinit>(cmd105.sc:-1)
```

The basic problem here is that when Scala sees `val (a, b, c) = ...`, it doesn't
mean which of two things you mean:

1. Help me extract the values from `...`, and help me check that it's a tuple
2. Help me extract the values from `...`, and fail at runtime if it is not a tuple

Currently, it assumes the latter, in all cases. 

That makes any sort of "destructuring assignment" unchecked, and thus extremely 
unsafe.

The above example at least happily fails with an exception, but the following 
exhibits the same problem, but instead truncates your data silently, losing the
`5`:
 
```scala
@ for((a, b) <- Seq(1 -> 2, 3 -> 4, 5)) yield a + " " +  b
res107: Seq[String] = List("1 2", "3 4")
```

Though the following also fails with an exception:
```scala
@ Seq(1 -> 2, 3 -> 4, 5).map{case (a, b) => a + " " + b}
scala.MatchError: 5 (of class java.lang.Integer)
  $sess.cmd108$.$anonfun$res108$1(cmd108.sc:1)
  scala.collection.TraversableLike.$anonfun$map$1(TraversableLike.scala:234)
  scala.collection.immutable.List.foreach(List.scala:389)
  scala.collection.TraversableLike.map(TraversableLike.scala:234)
  scala.collection.TraversableLike.map$(TraversableLike.scala:227)
  scala.collection.immutable.List.map(List.scala:295)
  $sess.cmd108$.<init>(cmd108.sc:1)
  $sess.cmd108$.<clinit>(cmd108.sc:-1)
```

While interpretation #2 makes sense in `match` blocks and partial-functions,
where you expect to "fall through" to the next handler if it doesn't match, it
doesn't make much sense in cases like this where there *is nowhere to fall
through to*.

The correct solution would look something like this:

- By default, assume the user wants 1. "Help me extract the values from `...`, 
  and help me check that it's a tuple"

- Require a special keyword if the user wants 2. "Help me extract the values 
  from `...`, and fail at runtime if it is not a tuple"


A possible syntax might be using `case`, which Scala developers already 
associate with partial functions and pattern matches: 

```scala
for(case (a, b) <- Seq(1 -> 2, 3 -> 4, 5)) yield a + " " +  b

case val (a, b, c) = if (true) "bar" else Some(10)
```

This would indicate that you want to perform an "partial fail at runtime" 
match, and the earlier non-`case` examples:

```scala
for((a, b) <- Seq(1 -> 2, 3 -> 4, 5)) yield a + " " +  b

val (a, b, c) = if (true) "bar" else Some(10)
```

Could then verify that the pattern match is complete, otherwise fail at
compile time.

## Conclusion

Most of the warts listed here are not inherent to the "core" of the Scala 
language: types, values, classes, traits, functions, and implicits. None of 
them are particularly deep, nor should they be very controversial. This list is 
obviously neither objective nor comprehensive.

Nevertheless, these warts are annoying far beyond their level of sophistication, and 
especially pose a barrier to newbies (such as 
[myself, six years ago](https://stackoverflow.com/questions/8303817/nine-ways-to-define-a-method-in-scala))
who haven't learned to "tune out the noise" and "jump through the hoops" to
be able to work with Scala's elegant core.

I don't have the capability to contribute fixes to all of these myself, but
hopefully by publishing this I'll be able to raise awareness in the community
about such problems, and add pressure so that some day all these sharp corners
can be sanded down to reveal Scala's true elegance.

If you have your own favorite warts in the Scala language, let us know in the 
comments below!
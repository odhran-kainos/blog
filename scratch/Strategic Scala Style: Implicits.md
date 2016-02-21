
## Extension Methods, Implicit Conversions, Implicit Parameters

The rules around these are simple:

- Don't use an extension method unless it's going to be triggered many times
  in your program
- Don't use an implicit conversion unless it's going to be triggered many times
  in your program
- Don't use an implicit parameter unless it's going to be triggered many times
  in your program

All of these features perform the same function: making boilerplate disappear.
Extension methods let you call `foo.bar` instead of `bar(foo)`, implicit
parameters let you call `bar` instead of `bar(foo)` and implicit conversions
let you call `bar(flooo)` instead of `bar(convertToFoo(flooo))`

As described in the section on [Philosophy](#Philosophy), it is worth getting
rid of the boilerplate, *if and only if* you think the future maintainer
already knows what that boilerplate is going to tell them! And the only way
they would already know what the boilerplate was going to tell them is if
they're used to the semantic after seeing the same "mysterious" implicit
action working many times within their program.

### Quantity is Quality

What does "triggered many times" mean? It's hard to give a concrete answer, but
for example this is definitely wrong:

```scala
class Foo
implicit def foo = new Foo
def foo(x: Int)(implicit f: Foo) = ...

foo(1) // only usage of implicit Foo in entire codebase
```

Similarly, for implicit conversions

```scala
class Foo(x: Int)
implicit def convert(f: Int) = new Foo(f)
def foo(f: Foo) = ...

foo(1) // only usage of implicit def convert in entire codebase
```

Or for extension methods

```scala
implicit class FooOps(f: Int){
  def foo() = ...
}

1.foo() // only usage of implicit class FooOps in entire codebase
```

So that's one data-point: using an implicit only once, ever, is definitely
wrong. We can play out some scenarios on paper, without actually writing code,
to see what people think about varying amount of use for their implicits
varying numbers of times in the codebase:

- 1 time in codebase? BAD
- 5 times?
- 20?
- 100?
- 200?
- 500?

I'd say that if your implicit is so critical it's used 500 times throughout
your codebase, it's probably ok. As an arbitrary threshold, about ~20 times is
when an implicit *something* (whether conversion, parameter, or extension
method) starts to pay for its own weight:

- 1? BAD
- 5? BAD
- 20? Ok
- 100? Good
- 200? Good
- 500? Good

You may disagree with the numbers, but hopefully the message isn't too
disagreeable. Implicits have to be used to be valuable, and at small numbers
of usages are actively harmful! Like with operators or short-names, if you're
not working the implicits enough you aren't getting the readability benefits
that their added-conciseness gives, and also are paying a much larger confusion
cost since future maintainers aren't going to be sufficiently familiar with
these implicits and will be surprised when they kick in.

###
### Advanced Implicit Techniques

Extension methods, implicit parameters, and implicit conversions have all sorts
of use cases. You can use them enrich libraries, you can use them to write
automatic adapters, you can implement type-classes, or even do automatic
"generic" typeclass derivation. However, in all these use cases, it is always
possible to write the equivalent code more "normally": using good old function
calls and objects.

Even when you're using these language features to do these cool techniques, if
the implicits aren't being used enough, they're not pulling their weight and
you should replace them with something more straightforward. For example,
performing automatic JSON-serialization of case classes is cool! But if you're
using it to serialize just *three* simple case classes, it's probably not worth
the complexity and you should just write the serializers manually.



### Case Study: Extension Methods

Should you or should you not use extension methods as part of your library's
API? You certainly always *can*, but this section will compare some different
usages and analyze why some are better than others.

#### Positive: Scalatest

Scalatest uses extension methods on a lot of things, including `should` as
a way of defining your test labels (`"A Stack"...` below) and `should be(...)`
as a way of defining asserts:

```scala
import collection.mutable.Stack
import org.scalatest._

class ExampleSpec extends FlatSpec with Matchers {

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new Stack[Int]
    stack.push(1)
    stack.push(2)
    stack.pop() should be (2)
    stack.pop() should be (1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new Stack[Int]
    a [NoSuchElementException] should be thrownBy {
      emptyStack.pop()
    }
  }
}
```

Although you may not agree with the
[BDD-style](https://en.wikipedia.org/wiki/Behavior-driven_development) test
specifications, or the english-like syntax for the `should be` asserts (I
don't), *if we assume that this kind of syntax is what we wanted to achieve*,
this kind of heavy usage is what extension methods are good for. When
you're working with ScalaTest code, you will see these few extensions regularly
enough that you cannot possibly forget them, and they're common enough to
materially affect the verbosity of the code v.s. a non-extension-method
plain-old-function-calls way of doing things.

#### Negative: Spray-Json & Pickling
There are a lot of libraries which use extension methods for no real purpose.
For example, [Spray-Json](https://github.com/spray/spray-json) has its JSON
serializer be an extension method:

```scala
val jsonAst = List(1, 2, 3).toJson
```

So does [Scala-Pickling](https://github.com/scala/pickling#scalapickling):

```scala
scala> import scala.pickling.Defaults._, scala.pickling.json._
scala> case class Person(name: String, age: Int)

scala> val pkl = Person("foo", 20).pickle
pkl: pickling.json.pickleFormat.PickleType =
JSONPickle({
  "$type": "Person",
  "name": "foo",
  "age": 20
})

scala> val person = pkl.unpickle[Person]
person: Person = Person(foo,20)
```

While there's no question that this works, I do not think that this is good
usage of extension methods. You simple don't serialize things often enough,
or densely enough, for there to be any real gain in terms of verbosity.
Furthermore, the callsites will be sufficiently scattered throughout your
program that it's always going to be slightly-mysterious where the calls come
from and how they're being dispatched (through the implicit extension method).

There's a second set of implicits here, e.g. in spray-json it automatically
implicitly instantiates the JSON serializer for `List[Int]` based on the
serializer for `List[T]` and the serializer for `Int`. Unlike the
extension-method implicits, these implicit serializers are triggered many
times per-callsite, e.g. serializing `List[List[Map[String, Int]]]` would
result in the implicit instantiation happening 5 times in one call. That easily
reaches hundreds of calls in a moderately-sized program, and those implicits
thus justify their existence.

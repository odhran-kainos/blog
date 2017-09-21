[uTest](https://github.com/lihaoyi/utest) is a testing framework for the Scala
programming language. Unlike other frameworks, uTest aims to be both simple and
convenient to use, to allow you to focus on what's most important: your tests
and your code. This post will explore what makes uTest interesting, and why you
should consider using it to build the test suite in your next Scala project.

[ScalaTest]: https://www.scalatest.org
[Specs2]: https://etorreborre.github.io/specs2/

-------------------------------------------------------------------------------

[Scalatest Suites]: http://www.scalatest.org/user_guide/selecting_a_style
[Specs2 Asserts]: https://etorreborre.github.io/specs2/guide/SPECS2-3.9.5/org.specs2.guide.Matchers.html

> **essential**
>
> 1. absolutely necessary; indispensable
> 2. pertaining to or constituting the essence of a thing
>
> - http://www.dictionary.com/browse/essential

uTest isn't "essential" in the sense that you *have* to use it: other testing
frameworks like [ScalaTest] or [Specs2] are always an option. Rather, uTest is
*essential* in that it contains just the features that are *absolutely
necessary* for testing your code, and trims them down to their core *essence*,
without unnecessary fluff.

uTest supports the following tasks:

- Writing snippets of code to run ("tests"), labeling them and organizing them
  neatly

- Sharing initialization and other code between your various tests

- `assert`-ing things to make sure they're true

- Selecting which tests you want to run, and seeing if they blow up

Anything else: fluent matchers, conventions about how tests should be labelled
using "should" or "must" or "given - when - then", mocking, whether you're
writing
[unit or integration tests](http://www.lihaoyi.com/post/PrinciplesofAutomatedTesting.html#unit-vs-integration-tests)
or
[example vs bulk tests](http://www.lihaoyi.com/post/PrinciplesofAutomatedTesting.html#example-vs-bulk-tests),
all that is left up to the user. uTest doesn't need to know, or care: it will
happily run your tests and report their results no matter how you choose to
write them.

Traditional Scala testing libraries force you to make many arbitrary decisions:
[choosing what syntax you'd like to use to structure your tests][Scalatest Suites],
or [what syntax you'd like to use when doing asserts][Specs2 Asserts]. In many
cases, the difference is superficial, but nonetheless adds to the mental effort
needed to use the library:

- You have to choose (and hopefully standardize!) on one of the half-dozen
  styles of defining tests,

- Choose between a half-dozen styles each time you want to assert something,

- And read code written by collaborators or colleagues who happened to make a
  different choice!

When I myself have used [ScalaTest] or [Specs2] in the past, there has always
been the feeling at the back of my mind that I don't fully understand how the
test framework works, that I'm not properly using the tools the test framework
is giving me, that my test seems to be working but I don't understand why. All
this even as I'm already struggling to understand the complexities of *my own*
code and test suite!

uTest chooses a different path: it strips these features down to the bare
essentials. A [single, simple way of defining tests](#test-definitions) and a
[single, simple syntax for smart asserts](#smart-asserts): in most things that
uTest provides, there is one - and only one - way to do things. That one way is
made powerful and flexible enough to accommodate a wide range of use cases, and
if that isn't enough, you can build your own tools and helpers specialized for
your own specific need.

uTest is simple enough you will never bump into a dark corner of the library you
aren't familiar with. This simplicity lets you focus on what's you should be
focusing on - your own code and test suite - with full confidence that the test
framework will yield no surprises.

## Getting started with uTest

While you can run uTest
[standalone](https://github.com/lihaoyi/utest#running-utest-standalone), most
people will probably be using it [as
part of an SBT project](https://github.com/lihaoyi/utest#getting-started). For
that, you need to add uTest to your `build.sbt` file:

```scala
libraryDependencies += "com.lihaoyi" %% "utest" % "0.5.3" % "test"

testFrameworks += new TestFramework("utest.runner.Framework")
```

Define a test file in your `src/test/scala` folder:

```scala
package test.utest.examples

import utest._

object HelloTests extends TestSuite{
  val tests = Tests{
    'test1 - {
      throw new Exception("test1")
    }
    'test2 - {
      1
    }
    'test3 - {
      val a = List[Byte](1, 2)
      a(10)
    }
  }
}
```

Here, the `'test1 - { ... }` syntax marks out a block of code as a test, which
will then get run and reported separately from every other block.

You then run it via:

```text
sbt myproject/test
```

And see the results:

```text
-------------------------------- Running Tests --------------------------------
Setting up CustomFramework
X test.utest.examples.HelloTests.test1 4ms
  java.lang.Exception: test1
    test.utest.examples.HelloTests$.$anonfun$tests$2(HelloTests.scala:7)
+ test.utest.examples.HelloTests.test2.inner 0ms  1
X test.utest.examples.HelloTests.test3 0ms
  java.lang.IndexOutOfBoundsException: 10
    scala.collection.LinearSeqOptimized.apply(LinearSeqOptimized.scala:63)
    scala.collection.LinearSeqOptimized.apply$(LinearSeqOptimized.scala:61)
    scala.collection.immutable.List.apply(List.scala:86)
    test.utest.examples.HelloTests$.$anonfun$tests$5(HelloTests.scala:16)
Tearing down CustomFramework
Tests: 3, Passed: 1, Failed: 2
```

And that's all there is to it! There's a lot more documentation in the
[uTest readme](https://github.com/lihaoyi/utest) about how to use the project:
nesting tests to group them, selecting which tests to run, how the assertions
work, how to configure the framework. Feel free to spend more time digging
through those docs if you want to learn more about how to use the library. The
remainder of this blog post will focus less on how to use uTest, and more on
what makes uTest interesting and different from traditional testing libraries
like [ScalaTest] or [Specs2].

## Test Definitions

The first thing that uTest does differently is that it only provides a single
syntax for defining tests and groups of tests. Whether it's the simple, flat
test suite shown above:

```scala
'test1 - {
  throw new Exception("test1")
}
'test2 - {
  1
}
'test3 - {
  val a = List[Byte](1, 2)
  a(10)
}
```

Or a more complex test suite with tests grouped together in blocks, with some
values/fixtures shared between the various tests in a block (`x`, `y` and `z`
below):

```scala
val x = 1
'outer1 - {
  val y = x + 1

  'inner1 - {
    assert(x == 1, y == 2)
    (x, y)
  }
  'inner2 - {
    val z = y + 1
    assert(z == 3)
  }
}
'outer2 - {
  'inner3 - {
    assert(x > 1)
  }
}
```

In all cases, it's a simple `'foo - {...}` (or `"foo bar" - {...}` if the label
has spaces or other special characters in it) to define a test or block of
tests, with the inner-most blocks automatically being treated as "tests" to be
run and their results reported.

This is in contrast with libraries like ScalaTest, which defines no less that
[eight different styles of test definitions][Scalatest Suites]:

```scala
test("foo bar"){
  ...
}
"foo" should "bar" in {
  ...
}
describe("foo bar"){
  ...
}
"foo bar" - {
  ...
}
property("foo bar"){
  ...
}
scenario("foo bar"){
  ...
}
def `foo bar` {
  ...
} 
```

While ScalaTest provides lots of options of how to define your test suite, *in
essence* it doesn't really matter whether you call something a `property` or a
`scenario`, or whether you use the words `should` `in` `describe` or just `-`.
All you want to do when defining your tests is to write labelled snippets of
code and group them together to keep things neat. The label just needs to be
something descriptive enough that it's clear (together with the context) what
the snippet is intended to test.


Hence uTest just takes the `-`-based syntax from ScalaTest's FreeSpec:

```scala
// FooTests.scala
"Should respond appropriately when action event happens" - {
  ...
}
```
and extends it to work with `'symbol`s:


```scala
// FooTests.scala
'actionEventHandler - {
  ...
}
```
As well as anonymous bullets:

```scala
// FooTests.scala
* - {
  ...
}
```

While it may initially seem strange to be able to define anonymous test cases
without any label at all, it comes in useful when sometimes what the test is
testing is completely obvious from the code. For example, these few test cases
from the [Ammonite](http://ammonite.io) repo which exercise Ammonite's parser:

```scala
// ParserTests.scala

def assertResult(x: String, pred: Option[fastparse.all.Parsed[_]] => Boolean) = {
  val res = ammonite.interp.Parsers.split(x)
  assert(pred(res))
}
def assertComplete(x: String) = assertResult(x, _.isDefined)

'endOfCommand{
  * - assertComplete("{}")
  * - assertComplete("foo.bar")
  * - assertComplete("foo.bar // line comment")
  * - assertComplete("foo.bar /* block comment */")
  * - assertComplete("va va") // postfix
  * - assertComplete("")
  // ...
}
```

Here, it is relatively clear from the surrounding `endOfCommand` label (in this
`ParserTests` suite) that this is testing to make sure that these inputs parse
to a complete result. And what sorts of inputs each test is exercising is also
relatively clear from the string being passed to `assertComplete`.

uTest leaves it up to the developer to decide how much verbosity is needed. I
personally find this encourages you to write helper methods and bang out
relatively large numbers of simple test cases, which makes it easier to hit edge
cases than with a small number of verbosely-labeled, complex test cases. Apart
from that, uTest only provides a single syntax for defining test cases and
groups of test cases, since that's all you really need to label snippets of code
and keep things neat.

## Test Running

Both ScalaTest and Specs2 have syntaxes for running individual tests within a
suite:

```text
# ScalaTest
sbt> testOnly mypackage.MySuite -- -z testCaseNumberTwo
```
```text
# Specs2
sbt> testOnly mypackage.ExampleSpec -- -ex testCaseNumberTwo
```

uTest removes the distinction between "test suite" and "test case":

```text
sbt> testOnly -- mypackage.ExampleTests.testCaseNumberTwo
```

in uTest, there are only tests and groups of tests. It doesn't matter whether
you are running a package full of test suites:

```text
sbt> testOnly -- mypackage
```

All the tests in a test suite:

```text
sbt> testOnly -- mypackage.ExampleTests
```

A group of tests within the test suite:

```text
sbt> testOnly -- mypackage.ExampleTests.fooRelatedTests
```

Or a specific test case:

```text
sbt> testOnly -- mypackage.ExampleTests.fooRelatedTests.testCaseNumberTwo
```

To uTest, you can run any grouping of tests, no matter how small or large, with
the same uniform syntax. That uniform syntax also applies when running multiple
tests or groups:

```text
sbt> testOnly -- {mypackage,yourpackage}
sbt> testOnly -- mypackage.{ExampleTests,HelloTests}
sbt> testOnly -- mypackage.HelloTests.{fooRelatedTests,barRelatedTests}
sbt> testOnly -- mypackage.HelloTests.fooRelatedTests.{testCaseOne,testCaseNumberTwo}
```

While in Scala you are forced to wrap code in an `object` within a source file,
that wrapper object is not core to the *essence* of testing. The real goal is
just to organize things hierarchially: whether multiple tests within a single
file, or multiple files within a folder.

With uTest, it doesn't matter whether you are running tests within a Scala
package, a single `TestSuite` object, a specific group of tests within a
`TestSuite`, or even an individual test. You have a single hierarchy of "tests"
organized in a tree, and can choose which bits of the tree to run to see if it
blows up. And that is the essential part of what running tests is about.

## Smart Asserts

Both ScalaTest and Specs2 have a large library of "matchers" to help you perform
asserts of various sorts on your data. This ranges from simple equality checks:


```scala
// ScalaTest
foo should equal (bar)
foo should === (bar)  
foo should be (bar)   
foo shouldEqual bar   
foo shouldBe bar      
```

```scala
// Specs2
foo must beEqualTo(bar)
foo must be_==(bar)
foo must_== bar
foo mustEqual bar
foo should_== bar
foo === bar
foo must be equalTo(bar)
```

To more specific matchers, e.g. to assert properties on Strings:

```scala
// ScalaTest
string should startWith ("Hello")
string should endWith ("world")
string should include ("seven")
string should startWith regex "Hel*o"
string should endWith regex "wo.ld"
string should include regex "wo.ld"
```
```scala
// Specs2
beMatching or be matching       // check if a string matches a regular expression
=~(s)       // shortcut for beMatching("(.|\\s)*"+s+"(.|\\s)*")
find(exp).withGroups(a, b, c)   // check if some groups are found in a string
have length // check the length of a string
have size   // check the size of a string (seen as an Iterable[Char])
be empty    // check if a string is empty
beEqualTo(b).ignoreCase	    // check if 2 strings are equal regardless of casing
beEqualTo(b).ignoreSpace    // check if 2 strings are equal when you replaceAll("\\s", "")
beEqualTo(b).trimmed        // check if 2 strings are equal when trimmed
beEqualTo(b).ignoreSpace.ignoreCase	    // you can compose them
contain(b)      // check if a string contains another one
startWith(b)    // check if a string starts with another one
endWith(b)      // check if a string ends with another one
```

With similar matchers for `Seq`s, `Array`s, numbers, `case class`es, and a
hodge-podge of other supported types.

ScalaTest also provides useful "Smart" `assert` methods that automatically
print out the values of variables involved in the assertion:

```scala
val a = 1
val b = 2
val c = 3
val d = 4
assert(a == b || c >= d)
// Error message: 1 did not equal 2, and 3 was not greater than or equal to 4
```

To me, the implementation of "matchers" in these libraries is flawed for a
couple of reasons:

- They are extremely redundant! I don't need 7 different ways to check `assert(a
  == b)`, just one will do

- They use a strange, english-like syntax: I can use `a should equal (b)` and `a
  shouldEqual b` but not `a should equal b`, when in my mind I'm just thinking
  `assert(a == b)`

- They pollute your namespace! By flooding your namespace with names like
  `include`, `endWith`, `empty`, `length`, `size`, this ends up causing name
  clashes e.g.
  [here](https://etorreborre.github.io/specs2/guide/SPECS2-3.9.5/org.specs2.guide.MutableSpecSyntax.html)
  where Specs2's matcher `should` syntax collides with it's test-definition
  `should` syntax

Overall, these matchers are not *essential* to what tests are about. While they
*do* provide nicer error messages of plain-old `assert(a == b)` asserts,
ScalaTest's smart asserts are already able to give you good error messages,
without needing an entire matcher framework.

Hence, uTest does without matchers entirely, and offers a relatively small
number of built-in assertions. The main ones are:

**Smart Asserts**, similar to ScalaTest's, which automatically print out the
code that failed, together with the values and types of the values involved in
the assertion:

```scala
val x = 1
val y = "2"
assert(
  x > 0,
  x == y
)

// utest.AssertionError: x == y
// x: Int = 1
// y: String = 2
```

**Intercept**, which asserts that the given code throws an exception, capturing
it and returning it so you can perform checks on its properties:

```scala
val e = intercept[MatchError]{
  (0: Any) match { case _: String => }
}
println(e)

// scala.MatchError: 0 (of class java.lang.Integer)
```

**assertMatch**, which lets you check if the given value matches a pattern. This
is similar to `assert(a == b)` but a lot more flexible, since the pattern can
have `_` holes in it for parts which you do not care about asserting:

```scala
assertMatch(Seq(1, 2, 3)){case Seq(1, _) =>}
// AssertionError: Matching failed Seq(1, 2, 3)
```

**compileError**, which lets you assert that a given expression doesn't compile,
and assert further that it produces a particular error message. It's a good
practice to test runtime error cases and make sure the error messages are what
you expect, and there's no reason this shouldn't apply to compile-time error
cases too:

```scala
compileError("true * false")
// CompileError.Type("value * is not a member of Boolean")

compileError("(}")
// CompileError.Parse("')' expected but '}' found.")
```

I think that these few core asserts cut to the essence of what people want from
their asserts when they write tests: they want to be able to write code, check
some property, and if the check fails (or the code blows up) they want to see
what values likely caused the code to fail or blow up.

On top of that, people inevitably want more customized asserts, more customized
comparisons, with their own customized error messages and diagnostics when
things go wrong. While uTest's `assert` doesn't let you provide a custom error
message, `Predef.assert` is still available if you want to do an assert with a
custom error message. After that, it is then up to the user of uTest to write
their own "check" functions to perform whatever more custom checks they want.

For example, [Fansi](http://github.com/lihaoyi/fansi)'s test suite has a simple
`check` function that round-trips a `fansi.Str` through rendering and parsing
and makes sure that the output is the same as the input.

```scala
'parsing - {
  def check(frag: fansi.Str) = {
    val parsed = fansi.Str(frag.render)
    assert(parsed == frag)
    parsed
  }
  * - check(fansi.Color.True(255, 0, 0)("lol"))
  * - check(fansi.Color.True(1, 234, 56)("lol"))
  * - check(fansi.Color.True(255, 255, 255)("lol"))
  * - check(fansi.Color.True(10000)("lol"))

  * - check(square(for(i <- 0 to 255) yield fansi.Color.True(i,i,i)))
}
```

[PPrint](http://github.com/lihaoyi/pprint) uses a custom `check` function to
make sure escaped ASCII characters match the expected output string:


```scala
'escapeChar{
  def check(c: Char, expected: String) = {
    val escaped = pprint.Util.escapeChar(c, new StringBuilder).toString
    assert(escaped == expected)
  }
  check('a', "a")
  check('-', "-")
  check('\n', "\\n")
  check('\\', "\\\\")
  check('\t', "\\t")
}
```

uTest just provides the basic tools that "everyone" wants: `assert`s that print
out the values involved when they go wrong. Past that, test suites are just
Scala code: it's easy for the developer using uTest to define their own
helpers to perform whatever checks they want, and so uTest leaves it up to them.

## Shared Fixtures

When writing code in Scala, you are always aware of lexical scope: that things
defined in broader scopes are available to code running in narrower, enclosed
scopes.

When you have a test suite with nested blocks of tests, it is then natural to
want to put shared "fixture" definitions in the outer blocks, so the inner
blocks can access them. It doesn't matter whether those fixtures are `val`s,
`def`s or something else, although test fixtures have a tendency to fail just
like any other code in your test suite.

It turns out that this causes problems in many traditional frameworks like
ScalaTest. Consider the following ScalaTest suite:

```scala
import org.scalatest.FreeSpec

class SetSpec extends FreeSpec {

  "outer1" - {
    throw new Exception("Outer One")
    "inner1" in {
      throw new Exception("Inner One")
    }
    "inner2" in {
      throw new Exception("Inner Two")
    }
  }
  "outer2" - {
    throw new Exception("Outer Two")
    "inner3" in {
      throw new Exception("Inner Three")
    }
  }
}
```

Here, we're using `throw new Exception` as a stand-in for code that might fail.
While this test suite is of course nonsensical, the way it can throw exceptions
in various places is not unlike how many perfectly sensible test suites work.

Running this suite gives the following error:

```text
SetSpec:
SetSpec *** ABORTED ***
java.lang.Exception was thrown inside "outer1" -, 
construction cannot continue: "Outer One" (SetSpec.scala:5)
```

Here, we can see that in ScalaTest, any failure in the "outer" blocks causes the
entire test suite to error out without running properly.

On the other hand, if you run a set of nested uTest tests like this:

```scala
import utest._
object MyTests extends TestSuite{
  val tests = Tests{
    'outer1 - {
      throw new Exception("Outer One")
      'inner1 - {
        throw new Exception("Inner One")
      }
      'inner2 - {
        throw new Exception("Inner One")
      }
    }
    'outer2 - {
      throw new Exception("Outer Two")
      'inner3 - {
        throw new Exception("Inner Two")
      }
    }
  }
}
```

You will get the following output:

```text
----------------------------------- Results -----------------------------------
- MyTests 0ms
  - outer1 0ms
    X inner1 0ms
      java.lang.Exception: Outer One
        mypackage.MyTests.$anonfun$tests$2(MyTests.sc:3)
    X inner2 0ms
      java.lang.Exception: Outer One
        mypackage.MyTests.$anonfun$tests$2(MyTests.sc:3)
  - outer2 0ms
    X inner3 0ms
      java.lang.Exception: Outer Two
        mypackage.MyTests.$anonfun$tests$5(MyTests.sc:12)
```

Note how despite the fact that `outer1` and `outer2` failed with exceptions
`Outer One` and `Outer Two`, uTest was still aware of all the tests in the suite
and able to report their failure.

Another case worth looking at is what happens to mutable variables defined in
the enclosing scopes. In ScalaTest, these mutable variables are shared between
the various individual tests:

```scala
import org.scalatest.FreeSpec

class SetSpec extends FreeSpec {
  var x = 0
  "outer1" - {
    x += 1
    "inner1" in {
      x += 2
      println(x) // 7, from 0 + 1 + 4 + 2
    }
    "inner2" in {
      x += 3
      println(x) // 10, from 7 + 3
    }
  }
  "outer2" - {
    x += 4
    "inner3" in {
      x += 5
      println(x) // 15, from 10 + 5
    }
  }
}
```

This can cause confusing issues, e.g. your test suite may pass or fail
differently depending on how you re-order the tests within it.

However, in the equivalent uTest test suite, each test gets its own copy of any
fixtures initialized in the `Tests{ ... }` block. This means that each test is
only affected by code in their surrounding blocks, and won't see interference
from other tests next to it:

```scala
import utest._

object SeparateSetupTests extends TestSuite{
  val tests = Tests{
    var x = 0
    'outer1 - {
      x += 1
      'inner1 - {
        x += 2
        assert(x == 3) // 0 + 1 + 2
        x
      }
      'inner2 - {
        x += 3
        assert(x == 4) // 0 + 1 + 3
        x
      }
    }
    'outer2 - {
      x += 4
      'inner3 - {
        x += 5
        assert(x == 9) // 0 + 4 + 5
        x
      }
    }
  }
}
```

Lexical scope is a convenient, well-understood way of sharing code: whatever you
put in outer blocks is available to code in inner blocks. However, with a
framework like ScalaTest, you are unable to use this fact to define shared
fixtures that can be shared by all your tests: if defining the fixture throws an
exception, that causes the suite to error out without running properly, and any
mutable fixtures are shared which can lead to tests interfering with each other
in confusing ways.

In uTest, on the other hand, these problems are solved: you can define shared
initialization code in outer blocks, whether `val`s of `def`s, just like in
normal Scala code. Furthermore, these definitions are by-default not shared
between different tests in the suite, to avoid accidental interference. Thus,
defining shared fixtures is as simple as defining them in the block enclosing
the tests that need to access it: an intuitive approach that really gets to the
core essence of what shared test fixtures are all about.

At some point, your test initialization code will grow too large and messy to
include in the outer blocks of a test suite. The solution is to extract it into
a separate file, the same way you deal with any too-long block in normal code.

## Missing Features from uTest

uTest is no where near as feature-rich as traditional frameworks like ScalaTest
or Specs2. Apart from a lot of missing features I consider "unessential", there
are some things which I think are pretty useful and should eventually end up in
uTest in some shape or form:

- **Tagging tests**: uTest only allows you to organize your tests
  hierarchically. If you wanted to conveniently "only run integration tests",
  you would need to put all those tests in a separate `integration` package.
  This works for many cases, but there are cases where there are multiple
  different ways you may want to select parts of your test suite, and a
  convenient way to tag tests would help achieve that.

- **More complex test queries**: using globs (`foo.*.bar`), negation ("all test
  in `foo.bar` except those in `foo.bar.baz`), partial matches
  (`foo.*BarTests`). While these tend to be sloppy and should not be the default
  way of picking individual tests, it would be great to have them available.

- **Better SBT integration**: things like
  [taking test configuration from SBT](https://github.com/lihaoyi/utest/issues/42),
  or [test arguments from SBT](https://github.com/lihaoyi/utest/issues/87).

- **Fine-grained teardown hooks**: while uTest allows you to put "setup" code in
  a variety of places -the `object` bodies, or in various scopes within the
  `Test{}` block - it only lets you put teardown code in one global "teardown"
  handler that runs at the end of each test run.

- **Customizable reporting**: currently, uTest only outputs to the console and
  to a `junit.xml` results file via it's SBT integration. It would be nice to
  expose hooks such that uTest's results could be easily post-processed after a
  test run is complete, to generate custom reports e.g.
  [showing the slowing tests](https://github.com/lihaoyi/utest/pull/93) in the
  test suite.

All these are are useful things that eventually should find their way into
uTest. None of them are in-principle difficult to implement, just that I have
never found the need for them yet in my own projects, and so they simply haven't
been implemented. While uTest is likely to grow in breadth and functionality
over time, it is unlikely to ever become as large and complex as the other test
frameworks out there.

## Conclusion

While there are endless possibilities for what a test framework *could* do, I
believe that only a few of those things are really *essential*:

- Writing snippets of code to run ("tests"), labeling them and organizing them
  neatly

- Sharing initialization and other code between your various tests

- `assert`-ing things to make sure they're true

- Selecting which tests you want to run, and seeing if they blow up

uTest is a test framework optimized for these features - making them as simple,
intuitive and powerful as possible.

As a project grows, it is natural to want more things: perhaps a custom
organization of your test suite and test data, your own libraries of shared test
setup fixtures, or asserts that check properties specific to your codebase.
Rather than trying to be all things to all people, uTest provides the basics and
then stays out of the way, to let you focus on building out your own test
infrastructure specialized to your specific need.

uTest is not a new project. The initial code was written in 2014, as it was
written first to be a test framework to support Scala.js, back when no other
libraries supported it. However, apart from it's Scala.js support (which is no
longer unique, with ScalaTest and other libraries now offering it) I believe
there are still many good reasons why you may want to consider using uTest over
the alternatives.

The recent [0.5.3](https://github.com/lihaoyi/utest#053) release has fixed a
large number of long-standing issues that historically made uTest uncomfortable
to use. What remains should is, I hope, a tight and polished package that
provides the core tools needed to build your test suites. An essential test
framework for the Scala programming language.
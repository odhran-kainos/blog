Automated testing is a core part of writing reliable software; there's only so 
much you can test manually, and there's no way you can test things as 
thoroughly or as conscientiously as a machine. As someone who has spent an 
inordinate amount of time working on automated testing systems, for both work 
and open-source projects, this post covers how *I* think of them. Which 
distinctions are meaningful and which aren't, which practices make a difference 
and which don't, building up to a coherent set of principles of how to think 
about the world of automated testing in any software project.

-------------------------------------------------------------------------------

I probably care more about automated testing than most software engineers. At
a previous job, I agitated-for and rolled-out Selenium integration tests as 
part of the development process across engineering, developed static-analysis 
"tests" to block silly mistakes and code quality issues, and led projects to 
fight the flaky test scourge to help Make The CI Build Green Again. In my open 
source work, e.g. in projects like [Ammonite](http://ammonite.io/) or 
[FastParse](http://www.lihaoyi.com/fastparse/), my ratio of test code
to "main" application code often is about 1 to 1.

A lot has been written of the practice of automated testing: about 
[Unit Testing](https://en.wikipedia.org/wiki/Unit_testing),
[Property-based Testing](http://blog.jessitron.com/2013/04/property-based-testing-what-is-it.html),
[Integration Testing](https://en.wikipedia.org/wiki/Integration_testing),
and other topics. Unsurprisingly, much of the information you can find on the 
internet is incomplete, at odds to one another, or only applies narrowly to
certain kinds of projects or scenarios. 

Rather than talking about individual tools or techniques, this post attempts to 
define a way of thinking about automated testing that should apply broadly 
regardless of what software project you are working on. Hopefully this should 
form a foundation that will come in useful when you end up having to lift your 
gaze from the daily grind of software development and start thinking about the 
broader strategy of automated testing in your project or organization.

- [The Purpose of Automated Tests](#the-purpose-of-automated-tests)
- [Unit vs Integration tests](#unit-vs-integration-tests)
- [How to Prioritize Tests](#how-to-prioritize-tests)
- [Tests are code](#tests-are-code)
- [Example vs Bulk tests](#example-vs-bulk-tests)
- [Cost of tests](#cost-of-tests)
- [Conclusion](#conclusion)


## The Purpose of Automated Tests

The purpose of automated tests is to try and verify your software does what you
expect it to do, now and into the future.

This is a very broad definition, and reflects how there are very many different 
ways to try and verify your software does what you expect: 

- Calling a function on known inputs and `assert`-ing on the expected result

- Setting up a staging website and checking that the web pages, together with 
  all the systems behind it, can perform simple operations correctly
  
- [Fuzzing](https://en.wikipedia.org/wiki/Fuzzing) your system with large 
  amounts of random input and just seeing if it crashes
  
- Comparing the behavior of your program against a known-good reference
  implementation and ensuring it behaves identically

Note how the stated goal doesn't say a word about "unit" or "integration"
testing. That is because those are almost never the end goal: you want tests
that automatically check that your software does what you want, by whatever 
means necessary. "unit" or "integration" tests are only one distinction out of
many different ways of approaching automated testing, several of which we will 
cover in this post.

Now that we've defined the high-level goal, the rest of this post will go into 
much more detail about the intricacies and trade-offs inherent to the different
ways we can try and achieve it.

## Unit vs Integration tests

When you are working on automated tests, some arguments always come up: 

- Are we writing unit tests or integration tests?
 
- *Should* we be writing unit tests or integration tests?
 
- How do we define "unit tests" or "integration tests"? 

There is an endless number of "one true way"s of distinguishing between unit 
and integration tests, all of them different. Rules like:

- Unit tests must run entirely in a single process

- Unit tests are not allowed to execute code in more than one file: all imports 
  must be mocked

- Unit tests are any test that don't cross the client-server boundary

However, I think such discussion often lacks perspective. In reality,
the exact point where you draw the line is arbitrary. Every piece of code or 
system is a unit integrating smaller units:

- A cluster that integrates multiple physical or virtual machines

- A machine (virtual of physical) that integrates multiple processes

- A process that integrates multiple other subprocesses (e.g. databases, 
  workers, etc.)

- A subprocess that integrates multiple modules

- A module or package integrating multiple smaller modules

- A module integrating individual functions

- A function integrating primitives like `Int`s using basic arithmetic

Every piece of code or system could be thought of as a "unit" to be tested, and 
every piece of code or system could be thought of as an "integration" of other
smaller units. Basically all software ever written is broken down 
hierarchically in this way.
```text
                            _________ 
                           |         |
                           | Machine |
                           |_________|
                           /         \
                _________ /           \ _________
               |         |             |         |
               | Process |             | Process |
               |_________|             |_________|
               /                       /         \
              /              ________ /           \ ________              
            ...             |        |             |        |             
                            | Module |             | Module |             
                            |________|             |________|
                            /        \                      \
                __________ /          \ __________           \ __________ 
               |          |            |          |           |          |
               | Function |            | Function |           | Function |
               |__________|            |__________|           |__________|
               /          \            /          \           /          \
              /            \          /            \         /            \
            ...            ...      ...            ...     ...            ...
```


Earlier we defined that the purpose of automated testing is "to try and verify 
your software does what you expect", and in any piece of software you'll have
code at every level of this hierarchy. All of that code is your 
responsibility to test and verify. 

For consistency with existing terminology, I will call tests for code low in 
the hierarchy (e.g. functions integrating primitives) "unit" tests, and
tests for code high in the hierarchy (e.g. a cluster integrating virtual 
machines) "integration" tests. But those labels are simply directions on a
spectrum, and there isn't a bright line you can draw between the "unit" and 
"integration" labels that applies to every project. 

```text
                      Most tests somewhere in between
  Unit <------------------------------------------------------------> Integration
              |           |           |            |          |
          Functions    Modules    Processes    Machines    Clusters
```

 
What really matters is that you are conscious of the way your software is 
broken down hierarchically, and that automated tests can live at any level in
the code hierarchy and any point in the unit-integration spectrum.

Being on a spectrum doesn't mean that the distinction between "unit" or 
"integration" tests is meaningless. While there is no bright line between the
two extremes, tests towards each end of the spectrum do have different 
properties:


| Unit                      | Integration              |
|---------------------------|--------------------------|
| Low in the hierarchy      | High in the hierarchy    |
| Fast                      | Slow                     |
| More Reliable             | More Flaky               |
| Little setup required     | Lots of setup required   |
| Few dependencies          | Many dependencies        | 
| Few failure modes         | Many failure Modes       |
| Specific failure messages | Generic failure messages |

- Unit tests tend to be faster, since they exercise less code that needs to run.

- Unit tests tend to be more reliable, since they exercise less code that may
  have non-deterministic failures

- Unit tests need less set up beforehand, since they have fewer dependencies

- Unit tests tend to fail in relatively specific ways ("function was meant to
  return 1, returned 2") with few possible causes, whereas 
  integration tests tend to fail with broad, meaningless errors ("could not 
  reach website") with many different possible causes

What does this mean to you, as a test writer?

### The distinction between "unit" and "integration" tests is up to you to define
  
A library of algorithms is likely to have a different definition of "unit" and 
"integration" tests than a website, which may have a different definition of 
"unit" and "integration" tests than a cluster deployment system. 

- The library-of-algorithms may define "unit" tests as tests which run one 
  function on tiny inputs (e.g. sorting a list of zero, one, two or three 
  numbers) while "integration" tests use multiple functions to construct 
  common algorithms
  
- The website may define "unit" tests as anything that doesn't touch the HTTP 
  API, and "integration" tests as anything that does. 
  
- Alternately, a website it may define "unit" tests as anything that doesn't 
  spin up a browser (up-to-and-including API interactions) 
  and "integration" tests as those that spin up a browser using Selenium to
  interact with the server through the UI/Javascript
   
- The cluster-deployment-system may define "unit" tests as anything that 
  doesn't physically create virtual machines, up to and including tests using
  HTTP APIs or database-access, while "integration" tests are those that spin 
  up a real cluster in a staging environment
  
While there are differences (e.g. an algorithm-library's integration tests may 
run faster than a cluster-deployment-system's unit tests) in the end all these
systems have tests that range from the "more unit" to "more integration" ends
of the spectrum.

Thus it is up to the project owner to draw the line between them, and then 
build up practices around that line. The bullets above should give you some 
an idea of where the line could be drawn in various projects, and practices 
around that line could be things like:

- Unit tests must be run before-commit while only integration tests are only 
  run once a day during a nightly-build

- Integration tests run on a separate CI machine/cluster from Unit tests due 
  to their different setup requirements

There could be value in splitting up the spectrum of tests into more 
fine-grained partitions. Again, it is again up to the project owner to 
decide how many lines to draw, where to draw them, what each group of tests is
called (e.g. "unit", "integration", "end to end", "functional"?) and how they
are treated within your project.

There is no universal classification of "unit" and "integration" tests
that is meaningful across the huge range of software projects that people
work on, but that does not mean the distinction is meaningless. It simply means 
that it is up to each individual project to draw the line in a way that is 
meaningful and useful.
  
### Tests at every level of the hierarchy

Every piece of software is written hierarchically, as units integrating smaller
units. And at every level, it is possible for the programmer to make a mistake:
ideally a mistake that our automated tests would catch.

Hence, rules like "only write unit tests, not integration tests" or "only write
integration tests, not unit tests" are overly restrictive. 

- **You cannot just write unit tests**. It doesn't matter if 
  every individual function is heavily tested but your module combines your 
  functions in an incorrect way, or if every individual module is heavily tested 
  but the application-process is using the modules incorrectly. While it is great
  to have a test suite which runs really fast, it's useless if it can't catch
  bugs introduced at upper layers of your program hierarchy.

- **You shouldn't just write integration tests**. In theory it works,
  code at a upper layer in the hierarchy exercises code at layers beneath it,
  but you would need a large number of integration tests to sufficiently
  exercise various cases in the low-level code. e.g. if you want to check a 
  single function's behavior with 10 different sets of primitive arguments, using
  integration tests to test it may mean you end up setting-up and tearing-down an 
  application process 10 times: a slow and and wasteful use of your 
  compute resources.

Instead, the structure your tests should roughly mirror the structure of your 
software. You want tests at all levels, proportionate to the amount of
code at that level and how likely/serious it is to be wrong. This guards 
against the possibility for errors to be introduced at any level in the 
hierarchy of your piece of software. 

## How to Prioritize Tests

Automated tests serve two main purposes: making sure your code isn't already 
broken (perhaps in some way that's hard to catch via manual testing) and making 
sure that working code doesn't *become* broken at some point in the future
(regressions). The former may be caused by an incomplete implementation, 
and the latter due to mistakes as the codebase evolves over time.

Thus, it doesn't make sense to have automated tests for code that isn't likely 
to be broken, code whose breakage isn't important, or code which is likely to
disappear entirely before someone causes it to break.

It's more an art than science to decide how much testing a system or 
piece-of-code needs, but some guidelines may be:

- Important things need more testing! Your password/authentication system definitely
  should be heavily tested to ensure 
  [bad passwords don't let people log in anyway](https://techcrunch.com/2011/06/20/dropbox-security-bug-made-passwords-optional-for-four-hours/),
  more so than other random logic in your application.

- Less important things, may need less testing or no testing at all. Maybe it
  doesn't matter if your web-upsell-modal-thingy doesn't appear for a few 
  days until the next deploy, and maybe the only way to properly test it is via
  expensive/slow/flaky Selenium integration tests. If so, the 
  [Cost of Tests](#cost-of-tests) may dictate that you simply should not have 
  automated tests for it.

- Code under active development needs more tests, while code not 
  under development need less. If a scary piece of code has been untouched for
  years, it is unlikely to become broken if it wasn't before. Now, you may want 
  tests to make sure it is not [already broken](https://en.wikipedia.org/wiki/Heartbleed), 
  but you won't need tests to guard against regressions and "new" breakage.

- APIs that aren't going to disappear should be tested more than APIs that 
  might. You should focus more effort in testing the stable interfaces within
  your application, rather than testing unstable code that may be gone
  entirely in a week. Combined with the above guideline, the code that deserves
  the most testing has a stable API but has internals undergoing heavy
  development.

- If the complexity in your code is in awkward places (inter-process, 
  browser-server, with database interop, ...) you should make sure you test
  that logic, no matter how awkward. Do not just test the "easy" things: it 
  doesn't matter how well individual functions are tested if the gnarly/fragile 
  code tying them together ends up broken.

Many of these points are subjective, and cannot be determined purely from the 
code itself. Nevertheless, these are judgements you have to make when 
prioritizing where to focus your efforts writing automated tests for your 
codebase. 

## Tests are code

Tests are code like any other: your test suite is a piece of software that 
checks that your "main" software behaves in certain ways. Thus, your test
code should be treated like any other proper piece of software:

- Common test logic should be refactored out to helpers. If there's a bug 
  in some common test logic, it's great to be able to fix it in one place 
  rather than digging through copy-paste boilerplate over the test suite to 
  apply fixes.

- Tests should have the same code-quality standards applied as normal code:
  proper naming, formatting, comments, inline-docs, code organization, coding 
  style and conventions.

- Tests need to be refactored to maintain code quality. Any code gets messy and
  hard to handle as it grows, requiring refactoring to keep things neat and DRY 
  and well-organized. Test code is no different, and as it grows and changes to
  support testing the growing/changing feature-set of the main application, it
  needs periodic refactoring to keep things DRY and maintain code quality.

- Your test suite should be agile and flexible. If the API being tested
  changes, it should be quick and easy to change your tests. If a chunk of code 
  is deleted, you should feel free to delete the corresponding tests, and if 
  the code is re-written it should not be hard to re-write the tests to match. 
  Proper abstractions/helpers/fixtures help make sure that 
  modifying and re-writing parts of your test suite isn’t burdensome.

- If your test abstractions/helpers/fixtures grow complex, they themselves 
  should be tested, at least minimally.

Not everyone agrees with these guidelines. I have seen people who argue that
tests are different from normal code. That copy-paste test code is not just
acceptable, but *preferable* to setting up test abstractions and helpers to
keep things DRY. The argument being it's simpler to see if there's a 
mistake in the tests when there's no abstraction. I do not agree with that 
point of view.

My view is that tests are code like any other, and should be treated as such.

### DRY data-driven tests

Tests are code, and code should be DRY and factored such that only the 
necessary logic is visible and you don't have repeated boilerplate. One good
example of this is defining test-helpers to let you easily shove lots of test
cases through your test suite, and at-a-glance be able to see exactly what 
inputs your test suite is testing. For example, given the following test code:

```scala
// Sanity check the logic that runs when you press ENTER in the REPL and
// detects whether a set of input lines is...
//
// - Complete, and can be submitted without needing additional input
// - Incomplete, and thus needs additional lines of input from the user
    
def test1 = {
  val res = ammonite.interp.Parsers.split("{}")
  assert(res.isDefined)
}
def test2 = {
  val res = ammonite.interp.Parsers.split("foo.bar")
  assert(res.isDefined)
}
def test3 = {
  val res = ammonite.interp.Parsers.split("foo.bar // line comment")
  assert(res.isDefined)
}
def test4 = {
  val res = ammonite.interp.Parsers.split("foo.bar /* block comment */")
  assert(res.isDefined)
}
def test5 = {
  val res = ammonite.interp.Parsers.split(
    "val r = (1 until 1000).view.filter(n => n % 3 == 0 || n % 5 == 0).sum"
  )
  assert(res.isDefined)
}
def test6 = {
  val res = ammonite.interp.Parsers.split("{")
  assert(res.isEmpty)
}
def test7 = {
  val res = ammonite.interp.Parsers.split("foo.bar /* incomplete block comment")
  assert(res.isEmpty)
}
def test8 = {
  val res = ammonite.interp.Parsers.split(
    "val r = (1 until 1000.view.filter(n => n % 3 == 0 || n % 5 == 0)"
  )
  assert(res.isEmpty)
}
def test9 = {
  val res = ammonite.interp.Parsers.split(
    "val r = (1 until 1000).view.filter(n => n % 3 == 0 || n % 5 == 0"
  )
  assert(res.isEmpty)
}
```

You can see that it's doing the same thing over and over. It really should be 
written as:

```scala
// Sanity check the logic that runs when you press ENTER in the REPL and
// detects whether a set of input lines is...
//
// - Complete, and can be submitted without needing additional input
// - Incomplete, and thus needs additional lines of input from the user

def checkDefined(s: String) = {
  val res = ammonite.interp.Parsers.split(s)
  assert(res.isDefined)
}
def checkEmpty(s: String) = {
  val res = ammonite.interp.Parsers.split(s)
  assert(res.isEmpty)
}
def testDefined = {
  checkDefined("{}")
  checkDefined("foo.bar")
  checkDefined("foo.bar // line comment")
  checkDefined("foo.bar /* block comment */")
  checkDefined("val r = (1 until 1000).view.filter(n => n % 3 == 0 || n % 5 == 0).sum")
}
def testEmpty = {
  checkEmpty("{")
  checkEmpty("foo.bar /* incomplete block comment")
  checkEmpty("val r = (1 until 1000.view.filter(n => n % 3 == 0 || n % 5 == 0)")
  checkEmpty("val r = (1 until 1000).view.filter(n => n % 3 == 0 || n % 5 == 0")
}
```

This is just a normal refactoring that you would perform on any code in any
programming language. Nevertheless, it immediately turns the boilerplate-heavy
copy-paste test methods into elegant, DRY code which makes it 
obvious-at-a-glance exactly what inputs you are testing and what their expected
output is. There are other ways you could do this, you could e.g. define all 
the `Defined` cases in an `Array`, all the `Empty` cases in an `Array`, and 
loop over them with asserts:


```scala
def definedCases = Seq(
  "{}",
  "foo.bar",
  "foo.bar // line comment",
  "foo.bar /* block comment */",
  "val r = (1 until 1000).view.filter(n => n % 3 == 0 || n % 5 == 0).sum"
)

for(s <- definedCases){
  val res = ammonite.interp.Parsers.split(s)
  assert(res.isDefined)
}

def emptyCases = Seq(
  "{",
  "foo.bar /* incomplete block comment",
  "val r = (1 until 1000.view.filter(n => n % 3 == 0 || n % 5 == 0)",
  "val r = (1 until 1000).view.filter(n => n % 3 == 0 || n % 5 == 0"
)

for(s <- emptyCases){
  val res = ammonite.interp.Parsers.split(s)
  assert(res.isEmpty)
}
```

Both refactorings achieve the same goal, and there are countless other ways of
DRYing up this code. Which style you prefer is up to you.

There are a lot of fancy tools/terminology around this idea: "table-driven 
tests", "data-driven tests", etc.. But fundamentally, all you want is for your
test cases to be concise and the expected/asserted behavior 
obvious-at-a-glance. This is something that normal code-refactoring techniques 
are capable of helping you achieve without any fancy tooling. Only after you've 
tried to do this manually, and found it lacking in some way, then is it worth
starting to look at more specialized tools and techniques.

### Testing DSLs

There are a variety of testing DSLs that let you write tests in a very 
different way from normal code. I find general-purpose testing DSLs generally
unhelpful, though there are use cases for DSLs specialized to a particular 
narrow use case.

#### General-Purpose Testing DSLs

These include external DSLs like the [Cucumber](https://cucumber.io/) family, 
which provide a whole new syntax to write your tests in:

```
Scenario: Eric wants to withdraw money from his bank account at an ATM
    Given Eric has a valid Credit or Debit card
    And his account balance is $100
    When he inserts his card
    And withdraws $45
    Then the ATM should return $45
    And his account balance is $55
```
```
Scenario Outline: A user withdraws money from an ATM
    Given <Name> has a valid Credit or Debit card
    And their account balance is <OriginalBalance>
    When they insert their card
    And withdraw <WithdrawalAmount>
    Then the ATM should return <WithdrawalAmount>
    And their account balance is <NewBalance>

    Examples:
      | Name   | OriginalBalance | WithdrawalAmount | NewBalance |
      | Eric   | 100             | 45               | 55         |
      | Pranav | 100             | 40               | 60         |
      | Ed     | 1000            | 200              | 800        |
```

To internal/embedded DSLs like [Scalatest](http://www.scalatest.org/), which 
twist the host language's syntax into something english-like, to let you write 
tests:

```scala
"An empty Set" should "have size 0" in {
  assert(Set.empty.size == 0)
}

"A Set" can {
  "empty" should { 
    "have size 0" in {
      assert(Set.empty.size == 0)
    }
    "produce NoSuchElementException when head is invoked" in { 
      intercept[NoSuchElementException] {
        Set.empty.head
      }
    }
    "should be empty" ignore { 
      assert(Set.empty.isEmpty)
    }
  }
}
```
```scala
val result = 8
result should equal (3) // By default, calls left == right, except for arrays
result should be (3)    // Calls left == right, except for arrays
result should === (3)   // By default, calls left == right, except for arrays

val one = 1
one should be < 7       // works for any T when an implicit Ordered[T] exists
one should be <= 7
one should be >= 0

result shouldEqual 3    // Alternate forms for equal and be
result shouldBe 3       // that don't require parentheses
```
  
My view of such DSLs is that they are generally not worth the effort. They 
provide an added level of indirection & complexity, whether through a special 
syntax/parser/interpreter in the case of Cucumber, or through special extension
methods/syntax in the case of Scalatest. Both of these make it *harder* for me 
to figure out what a test is testing.

I see such syntaxes as generally inferior to just 
using `assert`s and normal helper-methods/for-loops/etc. to write tests. While
they often provide additional features like nice error messages, these days 
test frameworks like [PyTest](https://docs.pytest.org/en/latest/) or 
[uTest](https://github.com/lihaoyi/utest) are also able to provide such "nice"
errors using plain-old-`assert`s:

```python
$ cat test_foo.py
def test_simple():
    result = 8
    assert result == 3

$ py.test test_foo.py
=================================== FAILURES ===================================
_________________________________ test_simple __________________________________

    def test_simple():
        result = 8
>       assert result == 3
E       assert 8 == 3

test_foo.py:3: AssertionError
=========================== 1 failed in 0.03 seconds ===========================
```

As mentioned earlier, I think that [Tests are code](#tests-are-code), and thus
the normal code-writing-tools like functions, objects and abstractions you use 
when writing normal code works just fine for writing tests. If you aren't using 
Cucumber-like external DSLs or Scalatest-like embedded-english-like DSLs
to write your main project, you should not be using such things to write your
test suite.

#### Specialized Testing DSLs

While I think *general purpose* testing DSLs like Scalatest or Cucumber are not
a good idea, *specialized* testing DSLs (e.g. narrowly defining the 
inputs/outputs of a test case) do have a purpose. 

For example the MyPy project uses a special syntax to define the input/output 
of test cases for it's python type checker: 

```python
[case testNewSyntaxBasics]
# flags: --python-version 3.6
x: int
x = 5
y: int = 5

a: str
a = 5  # E: Incompatible types in assignment (expression has type "int", variable has type "str")
b: str = 5  # E: Incompatible types in assignment (expression has type "int", variable has type "str")

zzz: int
zzz: str  # E: Name 'zzz' already defined
```

Where the `# E:` comments are asserts that the typechecker will raise specific
errors at specific locations when checking this file.

My own Ammonite project has its own special syntax to assert the 
behavior of REPL sessions: 

```scala
@ val x = 1
x: Int = 1

@ /* trigger compiler crash */ trait Bar { super[Object].hashCode }
error: java.lang.AssertionError: assertion failed

@ 1 + x
res1: Int = 2
```

In both of these cases, the DSL is narrowly scoped, to the extent where it is
"obvious" what it is testing. Furthermore, these DSLs are only necessary when
the "noise" of normal code becomes too great. For example, defining the above
Ammonite test case in "normal code" looks something like

```scala
checker.run("val x = 1")
checker.assertSuccess("x: Int = 1")

checker.run("/* trigger compiler crash */ trait Bar { super[Object].hashCode }")
checker.assertError("java.lang.AssertionError: assertion failed")

checker.run("1 + x")
checker.assertSuccess("res1: Int = 2")
```

Here, you can see that the Ammonite REPL-test-case DSL is a clear improvement
in readability compared to writing the tests in "normal" code! It is in these
cases, where a DSL actually reduces the amount of noise/ceremony beyond what 
normal code can do, where you should reach towards a specialized DSL. In all
other cases, and certainly as a default, your tests should be written in the
same style of code as the main codebase it is testing.

## Example vs Bulk tests

Example tests are those which walk your code through a single (or small number 
of) example, with careful asserts along the way to make sure it's doing exactly
the right thing. Bulk tests, on the other hand, are those which shove large
amounts of examples through your code, with a less thorough examination of how
each case behaves: just making sure it's not crashing, with perhaps a rough
check to make sure it's not completely misbehaving. [Fuzz Testing](https://en.wikipedia.org/wiki/Fuzzing) 
or [Property-based Testing](http://blog.jessitron.com/2013/04/property-based-testing-what-is-it.html)
are two common approaches within this category.

Like the distinction between Unit vs Integration tests, Example vs Bulk tests
are a spectrum, with most tests falling somewhere in the middle. The 
[DRY data-driven tests](#dry-data-driven-tests) above, for example lie 
somewhere in the middle: covering more than one set of input data with the same
set of checks, but not the hundreds or thousands of different inputs normally 
associated with fuzz tests or property-based tests.

The Example vs Bulk spectrum is orthogonal to the Unit vs Integration 
spectrum, and you can easily find examples towards every extreme in the two
spectrums:

|----------|---------|-------------|
|          | Unit    | Integration |
| Example  | Feeding `[1, 0]` into a sorting algorithm and ensuring it becomes `[0, 1]` | Clicking through a single flow on a website and making sure a particular flow works |
| Bulk     | Feeding large amounts of random numbers into a sorting algorithm and ensuring it ends up sorted | Clicking around a website randomly overnight to make sure no 500 errors appear |

### Example Tests

Example tests are often what people first think of when they hear "automated
testing": tests that use an APIs in a certain way and check the results. Here's
one such test from my FastParse library, which tests a trivial parser that 
parses a single character `a`:


```scala
import fastparse.all._
val parseA = P( "a" )

val Parsed.Success(value, successIndex) = parseA.parse("a")
assert(
  value == (), 
  successIndex == 1
)

val failure = parseA.parse("b").asInstanceOf[Parsed.Failure]
assert(
  failure.lastParser == ("a": P0),
  failure.index == 0,
  failure.extra.traced.trace == """parseA:1:1 / "a":1:1 ..."b""""
)
```

As you can see, this takes takes multiple steps: 

- Defining a parser
- Using it to parse different strings
- Checking that it succeeds and fails when it should succeed or fail
- Checking that the contents of each success and failure are what we expect

This is not unlike what you would do poking around in the REPL, except in a 
REPL we would simply eyeball the values returned by the library while here we 
use `assert`s.

Often, example tests are paired with manual testing: you poke around in a
REPL or run the main method during development to make sure the feature works.
Then you add a test that does basically the same thing to the test suite to 
ensure the feature keeps working and avoids regressions. If you do TDD, you may 
write the test first, but everything else remains the same.

Example tests are good documentation: often, just from reading a few examples,
it's relatively clear what a module does and how it is expected to be used.
Example tests are great for covering the "expected" success and failure 
cases, those that you probably already tested manually. However, they are not 
enough to cover "unexpected" cases. You can make it easier to cover a bunch
of input/output test cases via 
[DRY data-driven tests](#dry-data-driven-tests), but in the
end you are still limited by what examples you can imagine, which are only a
subset of all the possible inputs. That is where [Bulk tests](#bulk-tests) come 
in.

### Bulk tests

Bulk tests are those that check many more cases than you can cover via manual 
testing: rather than running a piece of code once and checking what it does,
bulk tests run the code with 100s of 1000s of different inputs. This lets you 
cover unexpected cases you never thought to test manually, or add
to your [Example tests](#example-tests).

There are well-known approaches like 
[Fuzz Testing](https://en.wikipedia.org/wiki/Fuzzing) or 
[Property-based Testing](http://blog.jessitron.com/2013/04/property-based-testing-what-is-it.html)
that are ways of performing bulk tests, and frameworks like 
frameworks like [QuickCheck](https://en.wikipedia.org/wiki/QuickCheck) or 
[ScalaCheck](https://www.scalacheck.org/) that help with this, and 
and provide a lot of bells and whistles, but in the end bulk testing boils down 
to something like this:

```python
for i in range(0, 9999):
    for j in range(0, 9999):
        result = func(i, j)
        assert(sanity_check(result))
```

Here, we're calling `func` with a hundred million different inputs, with a
simple `sanity_check` function that doesn't know all the expected outputs for
each input, but can check basic things like "output is not negative". At the
same time, we are checking that `func` doesn't throw an exception or loop 
forever on some input.

#### What to Bulk Test
 
Bulk test are slower than single example tests, due to the number of inputs 
they test. Thus their [Cost of tests](#cost-of-tests) is much higher, and they 
should be used with care. Nevertheless, for functionality where the range of
possible inputs is large and it's hard to manually pick example tests to cover
all edge cases, they can be worth the cost. Examples include:

- Mathy algorithms with lots of `while`-loops, if implemented incorrectly, tend 
  to end up in infinite loops when certain combinations of numbers are input

- Programming language analyzers, where the range of possible input programs is 
  large and often includes code patterns you didn't expect
   
- Log file parsers, where often due to the messy-and-unstructured nature of 
  logs it's hard to know exactly what kind of patterns you need to accept or 
  reject

In such cases, feeding in large amounts of varied test-data helps suss out edge
cases you may not have thought of yourself. The test data could be a wide range
of random numbers, sample programs sourced from the internet, or a days worth 
of logs pulled from your production environment.

#### How to Bulk Test

When dealing with such large sets of inputs, "correct" isn't defined by an 
equally big set of expected outputs. Rather, "correct" is usually defined by a
relationship between the input and output that you expect to to be true
regardless of what the input is:

- "any input does not cause program to throw an exception or loop forever". 

- "the output list of a sorting function must contain all values in the input 
  list exactly once, and the output list must be sorted"

- "all lines parsed from my sample log file must contain a date between X and 
  Y, and no other lines in that log file must contain the flag USER_CREATED"

Usually, the checks you do in these bulk tests are simpler and much 
less precise than the checks you would do in example tests. It's not practical
to run your log-file parser against a log dump and try and assert the thousands 
of values returned precisely match a thousands-long list of expected values:
you are just as likely to make an error in entering your expected-output as you
are in the logic of the parser itself! Nevertheless, we know that *some* 
properties should always hold true, regardless of exactly what values come out 
of your program. Those properties are what bulk tests are meant to test for. 

Apart from generating a huge pile of input data using for-loops, you can often 
find lots of real-world input-data to feed into your code. If we are testing a 
program meant to process Python source code, for example, such a bulk-test may 
look like

```python
repos = [
    "dropbox/changes",
    "django/django",
    "mitsuhiko/flask",
    "zulip/zulip",
    "ansible/ansible",
    "kennethresitz/requests"
]
for repo in repos:
    clone_repo("https://github.com/" + repo)
    for file in os.walk(repo):
        if file.endswith(".py"):  
            result = process_python_source(file)
            assert(sanity_check(result))
```

Bulk tests are often much slower than example tests: perhaps taking seconds or 
minutes to run, instead of milliseconds. Furthermore, bulk tests tend to be 
opaque and unreadable: when you're generating thousands of test values or 
loading thousands of test inputs from the internet, it's not clear which 
inputs are the actual edge cases and which inputs are common and uninteresting. 

#### Minimizing Bulk Tests to Example Tests

Thus it is often worth minimizing the bulk test cases that cause bugs and 
adding them to your example test suite. This means your example tests end up 
containing a good selection of the edge cases that occur in the bulk test data.
This serves as good documentation for edge cases that someone modifying the 
program code should pay attention to, and lets them can quickly run tests for 
the "most important" edge cases to check for basic correctness in milliseconds 
rather than waiting seconds or minutes for bulk tests to run.

My own [FastParse library](https://github.com/lihaoyi/fastparse) has a test 
suite in this fashion: with an expansive (and expensive!) [bulk tests](https://github.com/lihaoyi/fastparse/blob/master/scalaparse/jvm/src/test/scala/scalaparse/ProjectTests.scala) 
suite that spends several minutes pulling in thousands of source files from the 
internet, parsing them, and performing basic checks ("any file that the 
existing parser can successfully parse, we can parse too"). This is paired
with a large collection of 
[DRY data-driven example tests](https://github.com/lihaoyi/fastparse/blob/master/scalaparse/shared/src/test/scala/scalaparse/unit/SuccessTests.scala).
These contain minimized examples of all the issues the bulk tests have found,
and run in less than a second.

Again, there are
[Property-based Testing](http://blog.jessitron.com/2013/04/property-based-testing-what-is-it.html)
tools like [QuickCheck](https://en.wikipedia.org/wiki/QuickCheck) or 
[ScalaCheck](https://www.scalacheck.org/) that help with writing this kind of
bulk test. They make it easy to generate large quantities of "representative"
data to feed into your functions, to automatically find "small" failing inputs, 
that are easier to debug, and have many other nice things. However, they aren't 
strictly necessary: sometimes, a few for-loops, a dump of production data, or a 
few large inputs found "in the wild" are enough to serve this purpose. If you
are finding the quick-n-dirty methods of performing bulk tests lacking, only 
then should you start looking for more sophisticated tools.

## Cost of tests

Tests are not free: after all, someone has to write them! Even after they're 
already written, tests are still not free! Every test imposes an ongoing cost 
on your test suite. Each test:
 
- Makes your test suite slower
- Makes your test suite flakier
- Will need to be maintained: updated when the main code changes,
  grepped through during refactorings, etc.

These aren't theoretical concerns:

- I've worked with codebases with tens of thousands of tests we ran hundreds of 
  times a day: even a 1 in a million chance of flakiness per-test was enough 
  to create several false-positives each day, confusing and frustrating our 
  engineers. Test runs were costing 6-figure sums every month, and the 
  millions of lines of code we maintained actively slowed us down. 

- In my open source work, some projects take a half-hour to run tests in CI
  (across 5 parallel workers!), and I've been frustrated with flakiness causing 
  false red-builds.

Parallelizing your tests over multiple machines can speed up slow tests, but 
costs $$$, more than just running the tests on a single machine due to the 
per-machine setup overhead.

Automated tests can be "not worth it" if they take forever to run, are not 
reliable, are difficult to maintain and/or cover things which are of low 
[priority to test](#how-to-prioritize-tests). Such tests are actively 
harmful: they should not be written, and if already written should be deleted. 
I have personally deleted many such tests, e.g. selenium tests for an web 
upsell experiment that:
 
- Added 15 minutes to our test suite: selenium tests easily take a minute each,
  and this tested many cases through selenium

- Flaked a several every day

- Tested a feature that would have been noticed immediately if it wasn't 
  behaving correctly: the experiment already has all the logging for AB testing 
  and tracking users' engagement with it
  
- Wouldn't really have mattered if it broke for a day: no data lost, no 
  functionality broken, no user would have even noticed

- Even if they did matter, they weren't likely to catch any bugs or regressions
  in the 2-3 weeks before the experiment was going to be discarded anyway

In such cases, you should thank the authors for trying their best to be good
engineers and testing their code, but nevertheless delete those tests if they
are not pulling their weight. 

In my open source work on [Ammonite](http://ammonite.io/), I similarly ended up 
deleting many entries from my {Scala-Version x JVM-Version} test matrix that 
were adding tens of minutes to the test suite but were unlikely to catch any 
bugs that weren't already caught by other entries in the matrix. While it would 
be "nice" to run tests on the product of every Scala version and every JVM 
version, in practice it was costing enough time and catching sufficiently few 
bugs that it was not worth it.

### Refactoring to reduce the cost of tests

Apart from not-writing or deleting tests whose cost is too high, you can also
put in effort to try and reduce the cost of the tests you already have. For 
example, refactoring/modularizing code often lets you push tests away from big 
"integration" tests towards small "unit" tests, which are faster and more 
reliable:

- Global variables often force you to spawn new subprocesses to test 
  application logic. If you refactor the logic to remove the dependence on 
  globals, you can often test the logic within the same process,
  which can be much faster. This is especially important on slow-booting 
  platforms like the JVM, but even snappy interpreters like Python easily take
  10s to 100s of milliseconds to load the necessary modules before running.
  
- Database access is slower than in-memory logic; for example, rather than 
  loading from the database in bits and pieces throughout your code, perhaps 
  load the necessary data up-front then feed it into your "core business 
  logic". That way you can run tests of your database-free core business logic 
  in-memory thousands of times faster than if they had to keep interacting with 
  the database.

Essentially, this involves taking a monolithic application which looks like:



```text
                     ____________________
                    |                    |
                    |                    |
                    |    Application     | <-- Lots of Integration Tests
                    |                    |
                    |____________________|
```

And breaking it down to look something like this:


```text
                         __________
                        |          |
                        |   Main   |  <------- Few Integration Tests
                        |__________|
                        /   |  |   \ 
             __________/    |  |    \__________
            /               /  \               \
 __________/     __________/    \__________     \__________                        
|          |    |          |    |          |    |          |                        
|  Module  |    |  Module  |    |  Module  |    |  Module  | <-- Lots of Unit Tests
|__________|    |__________|    |__________|    |__________|                        
```

Now that your monolith has been broken down into smaller units, you can then
start shifting from the "integration" towards the "unit" ends of the spectrum: 
many integration tests previously testing logic within the monolithic 
`Application` can now be shifted to unit tests for individual `Module`s, 
following the guideline of having
[Tests at every level of the hierarchy](#tests-at-every-level-of-the-hierarchy).

In these cases, you usually want to leave a few "integration" tests running
the `Main` module to exercise the full flow and making sure the various
`Module`s work together. Even so, the exercise of breaking apart your monolith
into modules, and updating your tests to match, should make your test suite run 
much faster and more reliably, without much of a loss in 
bug-catching-capability.

Again, this strategy applies at every level of your 
[code hierarchy](#unit-vs-integration-tests), whether you are breaking apart
a monolithic cluster, monolithic application process, or a monolithic module.

If your test suite is growing big/slow/unreliable, and you are reluctant
to delete tests or pay money to parallelize them over different machines,
trying to refactor code to convert integration tests to unit tests is one
possible way forward.

--------------------------------------------------------------------------------

It is surprisingly easy to write tests with negative value. Tests have an 
ongoing cost: in runtime, flakiness, and maintenance. This is something 
that engineers should definitely keep in mind, and actively manage, to 
maximize their return-on-investment for writing and maintaining their suite of 
automated tests.

## Conclusion

This post has gone over a number of considerations I keep in mind when writing
automated tests:

- [The Purpose of Automated Tests](#the-purpose-of-automated-tests)
- [Unit vs Integration tests](#unit-vs-integration-tests)
- [How to Prioritize Tests](#how-to-prioritize-tests)
- [Tests are code](#tests-are-code)
- [Example vs Bulk tests](#example-vs-bulk-tests)
- [Cost of tests](#cost-of-tests)
- [Conclusion](#conclusion)

This post is intentionally silent about a whole host of test-writing topics:
[Test-Driven Development](https://en.wikipedia.org/wiki/Test-driven_development), 
[code coverage](https://en.wikipedia.org/wiki/Code_coverage),
UI testing, and many other things.
More than specific tools you should use or techniques you can apply, this post
is meant to have painted a coherent set of principles for how to think about 
automated testing in any software project.

This hopefully gives you a framework you can use to think about various 
approaches to automated testing, and to help you be conscious of the various 
spectrums and the trade-offs involved in each one. This should form a solid 
foundation for any discussion of tools, techniques or best practices, and apply 
regardless of what language, platform or environment you are working with.
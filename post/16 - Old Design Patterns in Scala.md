A [Design Pattern] is something you do over and over when building software, 
but isn't concrete enough to be made into a helper method, class or other 
abstraction. The "Gang of Four" [Design Patterns book] popularized the idea, 
and discussed in detail many of the design patterns which are common in C++,
Java and similar "Object Oriented" languages.
 
The Scala programming language in 2016 is different from languages common 22 
years ago. While some of the traditional design patterns still apply, others 
have changed significantly, and yet others have been entirely superseded by 
new language features. This post will explore how some of these old design 
patterns apply to the Scala programming language.

[Design Patterns book]: https://en.wikipedia.org/wiki/Design_Patterns
[Design Pattern]: https://en.wikipedia.org/wiki/Software_design_pattern

-------------------------------------------------------------------------------

- [What is a Design Pattern?](#what-is-a-design-pattern)
- [Builder](#builder)
- [Singleton](#singleton)
- [Adapter](#adapter)
- [Chain of Responsibility](#chain-of-responsibility)
- [Interpreter](#interpreter)
- [Observer](#observer)
- [Conclusion](#conclusion)

## What is a Design Pattern?

A "Design Pattern" is a common solution to a common problem that isn't concrete
enough to be packaged up as a helper method, class or module.

Implementing a design pattern isn't as simple as using a language feature,
calling a method or instantiating a class; rather than concrete code, a design 
pattern is a high-level sketch of a solution, and you still have to write all 
the code yourself in order to make something useful. If the 
"commonality" is concrete enough to make into a helper method, then you make it
into a helper method and it is no longer called a design pattern! Hence the
idea of "design patterns" will always have a bit of vagueness to it.

However, that doesn't mean design patterns are without value. Knowing these 
patterns helps you quickly identify common techniques in other peoples' code, 
provides a toolbox of common ways you can approach common problems, and 
provides a common vocabulary to explain or discuss implementations (real or
imagined) with other developers. For example, "This is a builder object" tells
you what it's for much faster and more effectively than reading through dozens
of method signatures or hundreds of lines of code.

Some say that design patterns are missing language features: this may be true, 
but that fact is uninteresting. No matter how many 
features a language has or how flexible they are, they will never be able to
satisfy the ever-growing set of things that people will want to do with a 
programming language! As programming languages gets more flexible and powerful, 
people will use them for ever more complex things, and there will be always be
"missing language features" at the margin.

The Design Patterns book describes a dozen or two different patterns:

- [Abstract Factory](https://en.wikipedia.org/wiki/Abstract_factory_pattern)
- [Builder](https://en.wikipedia.org/wiki/Builder_pattern)
- [Factory method](https://en.wikipedia.org/wiki/Factory_method_pattern)
- [Prototype](https://en.wikipedia.org/wiki/Prototype_pattern)
- [Singleton](https://en.wikipedia.org/wiki/Singleton_pattern)

- [Adapter](https://en.wikipedia.org/wiki/Adapter_pattern)
- [Bridge](https://en.wikipedia.org/wiki/Bridge_pattern)
- [Composite](https://en.wikipedia.org/wiki/Composite_pattern)
- [Decorator](https://en.wikipedia.org/wiki/Decorator_pattern)
- [Facade](https://en.wikipedia.org/wiki/Facade_pattern)
- [Flyweight](https://en.wikipedia.org/wiki/Flyweight_pattern)
- [Proxy](https://en.wikipedia.org/wiki/Proxy_pattern)

- [Chain of Responsibility](https://en.wikipedia.org/wiki/Chain-of-responsibility_pattern)
- [Command](https://en.wikipedia.org/wiki/Command_pattern)
- [Interpreter](https://en.wikipedia.org/wiki/Interpreter_pattern)
- [Iterator](https://en.wikipedia.org/wiki/Iterator_pattern)
- [Mediator](https://en.wikipedia.org/wiki/Mediator_pattern)
- [Memento](https://en.wikipedia.org/wiki/Memento_pattern)
- [Observer](https://en.wikipedia.org/wiki/Observer_pattern)
- [State](https://en.wikipedia.org/wiki/State_pattern)
- [Strategy](https://en.wikipedia.org/wiki/Strategy_pattern)
- [Template](https://en.wikipedia.org/wiki/Template_method_pattern)
- [Visitor](https://en.wikipedia.org/wiki/Visitor_pattern)

There are many other books discussing design patterns, and countless other
patterns have been described and analysed. Some of these patterns are broadly
applicable to most large codebases, while others are specialized and only
relevant within a particular field. 

There are too many design patterns to discuss them all in this post. Instead I 
will bring up just a few to highlight how those design patterns change when 
applied to Scala, or if they apply at all.

- [Builder](#builder)
- [Singleton](#singleton)
- [Adapter](#adapter)
- [Chain of Responsibility](#chain-of-responsibility)
- [Interpreter](#interpreter)
- [Observer](#observer)

## Builder
 
> The intent of the Builder design pattern is to separate the construction of 
a complex object from its representation. - Wikipedia

The Builder pattern takes a class e.g. `Car` and defines a matching 
`CarBuilder` class that you can use to build a `Car`. For example, in Java
syntax:

```java
CarBuilder carBuilder = new CarBuilder()
carBuilder.setSeats(2)
carBuilder.setSportsCar(true)
carBuilder.setTripComputer(true)
carBuilder.setGPS(false)
Car car = carBuilder.build()
```

This requires a bit of ceremony to define a whole new class with all the
`set` methods, but has distinct advantages over using a constructor:

```java
Car car = new Car(2, true, true, false)
```

The builder code lets you to associate names with each of the "constructor 
arguments" you are setting via `setXXX` methods, and lets you to provide 
defaults for each argument. In a language like Java, you cannot name the
constructor arguments while passing them, making it easy to mix up which `true`
or `false` is going into which aregument. Furthermore, in Java you also cannot
provide arguments with default values.
That means that if the `Car` had a few more arguments that were always false,
we'd still need to pass them all in every time, leading to code like:

```java
Car car = new Car(2, true, true, false, false, false, false, false, false)
Car car = new Car(2, true, true, false, false, false, true, false, false)
```

The builder pattern improves on this by making it clear which arguments you are 
setting to what values, and letting you ignore the arguments you don't care 
about. While verbose to use and even more verbose to implement, it's 
sufficiently more readable than the sea of `true, false, false, false, false`s
that in many cases it's worth it.


In a language like Scala which lets you name arguments while passing them in,
the builder pattern is mostly obsolete:

```scala
val car = new Car(
  seats = 2, 
  isSportsCar = true, 
  hasTripComputer = true, 
  hasGPS = false
  // no need to pass in arguments with default values
)
```

For the vast majority of cases, passing in arguments by name and setting 
defaults is enough: you can see what each argument is being set to which value,
and you don't need to pass in the uninteresting arguments which have a default
value. For the majority of cases, having a builder doesn't give you any 
additional value.

There are still cases where you want a builder, but those are relatively 
uncommon. For example, the Akka-Streams library uses a mutable builder to 
construct it's data-flow graphs before they are "materialized" and executed:

```scala
val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
  import GraphDSL.Implicits._
  val in = Source(1 to 10)
  val out = Sink.ignore
 
  val bcast = builder.add(Broadcast[Int](2))
  val merge = builder.add(Merge[Int](2))
 
  val f1, f2, f3, f4 = Flow[Int].map(_ + 10)
 
  in ~> f1 ~> bcast ~> f2 ~> merge ~> f3 ~> out
  bcast ~> f4 ~> merge
  ClosedShape
})
```

In this case the builder is used to construct a relatively complex data 
structure that can't easily be passed as constructor arguments. These cases
still exist, but are relatively few and far between. In the rare case a builder
class will save you complexity (e.g. above, separating the messy 
graph-construction phase from the equally-messy execution phase) you can still
use one, but mostly you should not need to.

## Singleton

> In software engineering, the singleton pattern is a design pattern that 
restricts the instantiation of a class to one object. This is useful when 
exactly one object is needed to coordinate actions across the 
system. - Wikipedia

The Singleton pattern lets you define a static object with only one instance:

```java
public final class MyFoo {
    private static final MyFoo INSTANCE = new MyFoo();
    private Singleton() {}

    public static MyFoo getInstance() {
        return INSTANCE;
    }
    // ... implementation of Singleton ...
}
```

This has the advantage over using `static` methods directly on `MyFoo` 
because the singleton, as a "real" object, can implement interfaces, inherit
functionality from other classes, and be passed around as any other object 
would: 

```java
public final class MyFoo extends MySuperclass implements Fooable{
    private static final MyFoo INSTANCE = new MyFoo();
    private Singleton() {}

    public static MyFoo getInstance() {
        return INSTANCE;
    }
    // ... implementation of Singleton class ...
}
// ... in other code ...
FooHandler.processFoo(MyFoo.getInstance())
```

A class with `static` methods can't do any of that.

```java
class MyFoo{
    // bunch of static methods
}

FooHandler.processFoo(MyFoo)// doesn't work
```

The singleton pattern is baked into the Scala language, using the `object`
keyword:

```scala
object MyFoo extends MySuperclass with Fooable{
    // ... implementation of Singleton ...
}

FooHandler.processFoo(MyFoo) // works
```

Thus in Scala, "Singleton" is obsolete as a design pattern: you no longer need
to think about doing the `INSTANCE`/`getInstance`/private-constructor dance 
throughout your codebase. You just use `object` and get everything that 
Singleton gives you for free.

## Adapter

> In software engineering, the adapter pattern is a software design pattern 
that allows the interface of an existing class to be used as another interface.
It is often used to make existing classes work with others without modifying 
their source code. - Wikipedia
  

The Adapter pattern is when you wrap a class that doesn't implement an 
interface in a wrapper-class which does. For example, if you have a `ClassA`
which you need to make implement the `StringProvider` interface, but you can't
change either of them (maybe `ClassA` comes from third-party library? And
`StringProvider` from another third-party library?) you can
wrap it in an adapter:

```java
public interface StringProvider {
    public String getStringData();
}

public class ClassAFormatAdapter implements StringProvider {
    private ClassA myA = null;

    public ClassAFormatAdapter1(final ClassA A) {
        myA = A;
    }

    public String getStringData() {
        return Helpers.formatNicely(classA);
    }
}

// ... in other code ...
ClassA thing = ...
StringHandler.handleStringProvider(new ClassAFormatAdapter(thing))
```

The adapter pattern is basically identical in Scala, just much less verbose:

```scala
class ClassAFormatAdapter(myA: ClassA) extends StringProvider{
  def getStringData() = Helpers.formatNicely(myA)
}
```

This is basically identical to the Java example above, just using Scala syntax.
Just like in Java, you can `new ClassAFormatAdapter(myClassA)` to wrap it in something
you can pass to some library that only takes `StringProvider`s:

```scala
// ... in other code ...
val thing: ClassA = ...
StringHandler.handleStringProvider(new ClassAFormatAdapter(thing))
```

But in Scala, you can go one step further and make the adapter `implicit`:
 
```scala
implicit class ClassAFormatAdapter(myA: ClassA) extends StringProvider{
  def getStringData() = Helpers.formatNicely(myA)
}
// ... in other code ...
val thing: ClassA = ...
StringHandler.handleStringProvider(thing)
```

With an implicit adapter, the instantiation of the adapter-wrapper happens 
entirely automatically depending on the expected types. While this may feel 
magical and confusing if overused, in many cases the 
`new ClassAFormatAdapter(...)` calls are just uninteresting boilerplate, and
making the instantiation `implicit` can cut through the boilerplate and let you
focus on the underlying logic that actually matters. 

In general you use Adapters much more in Scala than you do in Java, largely due
to the reduction in verbosity. Many libraries will have adapters to turn 
built-in types into library-specific types:

- [Scalatags](https://github.com/lihaoyi/scalatags) has adapters to turn 
  `java.lang.String` into Scalatags HTML `Frag`s for rendering
- [FastParse](https://github.com/lihaoyi/fastparse) has adapters to turn
  `java.lang.String` into `Parser` objects, to use as part of a parser 
  expression

While Java requires you to define an adapter in a whole new file, with many 
lines of boilerplate and more boilerplate at the use-site, Scala lets you
define an adapter in just 3 lines of code and the use-site is boilerplate free.
Thus if you find yourself needing to define a small adapter class to make 
a value fit into some parameter, it's easy enough you should just do it without
much thought.


## Chain of Responsibility

> In object-oriented design, the chain-of-responsibility pattern is a design 
pattern consisting of a source of command objects and a series of processing 
objects. Each processing object contains logic that defines the types of 
command objects that it can handle; the rest are passed to the next processing 
object in the chain. A mechanism also exists for adding new processing objects 
to the end of this chain. - Wikipedia

Using an [example from Wikipedia](https://en.wikipedia.org/wiki/Chain-of-responsibility_pattern#Example),
which models a chain of individuals in an organization who can approve a 
purchase (depending on price), you first define a bunch of `PurchasePower` 
classes:

```java
class ManagerPPower extends PurchasePower {
    
    protected double getAllowable(){
        return BASE*10;
    }

    protected String getRole(){
        return "Manager";
    }
}
```

Each one has a `getAllowable()` method or similar, which defines what kind
of input it is allowed to process. The `PurchasePower` class then has logic
to take an input, process it if it's allowed to, and otherwise delegate the
processing to the next processor in the chain:

```java
public void processRequest(PurchaseRequest request){
    if (request.getAmount() < this.getAllowable()) {
        System.out.println(this.getRole() + " will approve $" + request.getAmount());
    } else if (successor != null) {
        successor.processRequest(request);
    }
}
```

To use this, you instantiate a bunch of `PurchasePower` processors, link them
into a chain, and then ask the chain to process some input:

```java
ManagerPPower manager = new ManagerPPower();
DirectorPPower director = new DirectorPPower();
VicePresidentPPower vp = new VicePresidentPPower();
PresidentPPower president = new PresidentPPower();
manager.setSuccessor(director);
director.setSuccessor(vp);
vp.setSuccessor(president);

System.out.println("Enter the amount to check who should approve your expenditure.");
System.out.print(">");
double d = Double.parseDouble(new BufferedReader(new InputStreamReader(System.in)).readLine());
manager.processRequest(new PurchaseRequest(d, "General"));
```

The Chain of Responsibility pattern is still present and still common in Scala,
but with two tweaks:

- [Pattern Matching Partial Functions](#pattern-matching-partial-functions)
  satisfy many of the simpler use cases of Chain of Command

- [Immutability](#immutability) means you would construct the chain as a single
  large expression, rather than piece things together using `setSuccessor` calls

### Pattern Matching Partial Functions

Scala already has a basic implementation of the Chain of Command pattern built
into the language: `PartialFunction[T, V]`. For example, here is some code from 
the Ammonite project which defines an error-handling partial function, checking
the input `Throwable` against a list of possible conditions and handling each
one separately:

```scala
val userCodeExceptionHandler: PartialFunction[Throwable, Res.Failing] = {
  // Exit
  case Ex(_: InvEx, _: InitEx, ReplExit(value))  => Res.Exit(value)

  // Interrupted during pretty-printing
  case Ex(e: ThreadDeath)                 =>  interrupted(e)

  // Interrupted during evaluation
  case Ex(_: InvEx, e: ThreadDeath)       =>  interrupted(e)

  case Ex(_: InvEx, _: InitEx, userEx@_*) => Res.Exception(userEx(0), "")
  case Ex(_: InvEx, userEx@_*)            => Res.Exception(userEx(0), "")
  case Ex(userEx@_*)                      => Res.Exception(userEx(0), "")
}
```
 
While a pattern-match is similar to a `switch` statement or chain of 
`if-else`s, by assigning it to a partial function you can then manipulate it
in some limited ways. For example, you can combine `PartialFunction`s using
`orElse`, meaning the above could be written as:

```scala
val exitHandler: PartialFunction[Throwable, Res.Failing] = {
  case Ex(_: InvEx, _: InitEx, ReplExit(value))  => Res.Exit(value)
}
val prettyPrintFailedHandler: PartialFunction[Throwable, Res.Failing] = {
  case Ex(e: ThreadDeath)                 =>  interrupted(e)

}
val simpleFailureHandler: PartialFunction[Throwable, Res.Failing] = {
  case Ex(_: InvEx, e: ThreadDeath)       =>  interrupted(e)

  case Ex(_: InvEx, _: InitEx, userEx@_*) => Res.Exception(userEx(0), "")
  case Ex(_: InvEx, userEx@_*)            => Res.Exception(userEx(0), "")
  case Ex(userEx@_*)                      => Res.Exception(userEx(0), "")
}
val userCodeExceptionHandler = {
  exitHandler.orElse(prettyPrintFailedHandler).orElse(simpleFailureHandler)
} 
```

What you can't do with `PartialFunction`s is inspect them in any way, as they 
are entirely opaque: 

- You won't be able to store any metadata on each handler
- You can't remove handlers from the chain
- You can't inspect the handler to display or pretty-print it

If you do not need to do these things, pattern-matching `PartialFunction`s 
works great. For example, the 
[Play Framework's "SIRD" Router](https://www.playframework.com/documentation/2.5.x/ScalaSirdRouter)
uses partial functions to define routes:

```scala
val router = Router.from {
  case GET(p"/hello/$to") => Action {
    Results.Ok(s"Hello $to")
  }
}
```
```scala
val server = NettyServer.fromRouter() {
 case GET(p"/posts/") => Action {
    Results.Ok(”All posts")
  }
  case GET(p"/posts/$id") => Action {
    Results.Ok(“Post:" + id ) 
  }
}
```

For simple use cases, it works great. But if you *do* want to do any of the
things that partial functions don't support, you will have to use a 
normal Chain of Command with structured objects as handlers.

### Immutability

The Scala language encourages immutability, and thus instead of using 
`.setSuccessor` methods, Chain of Command chains tend to be constructed 
in-place as one large expression. Examples in the wild include 
[Akka-HTTP's routes](http://doc.akka.io/docs/akka-http/current/scala/http/routing-dsl/overview.html)
chains together the handlers using the `~` operator:

```scala
val route = get {
  pathSingleSlash {
    complete(HttpEntity(ContentTypes.`text/html(UTF-8)`,"<html><body>Hello world!</body></html>"))
  } ~
  path("ping") {
    complete("PONG!")
  } ~
  path("crash") {
    sys.error("BOOM!")
  }
}
```

Each `path(string){body}` call defines the URL that that handler can accept,
as well as the logic that handler will actually perform when it accepts 
something. The above example defines three route-handlers that will be tried
one after the other when a HTTP request comes in. Unlike the partial functions
shown earlier, these `Route` data structures aren't totally opaque, and in 
theory you could take advantage of that to optimize or transform them after 
construction.

## Interpreter

> The basic idea is to have a class for each symbol (terminal or nonterminal) 
in a specialized computer language. The syntax tree of a sentence in the 
language is an instance of the composite pattern and is used to evaluate 
(interpret) the sentence for a client. - Wikipedia

The Interpreter pattern centers around having a base interface with a single
abstract method. To use an example from 
[springframework.guru](https://springframework.guru/gang-of-four-design-patterns/interpreter-pattern/),
here is an interface representing an arithmetic expression, with an `interpret`
method:

```java
public interface Expression {
    int interpret();
}
```

From there you define multiple subclasses of `Expression`: one or more terminal 
expression types, e.g.

```java
 
public class NumberExpression implements Expression{
    private int number;
    public NumberExpression(int number){
        this.number=number;
    }
    @Override
    public int interpret(){
        return this.number;
    }
}
```

As well as one or more "compound" expression types, such as:

```java
public class AdditionExpression implements Expression {
    private Expression firstExpression, secondExpression;
    public AdditionExpression(Expression firstExpression, Expression secondExpression){
        this.firstExpression = firstExpression;
        this.secondExpression = secondExpression;
    }
    @Override
    public int interpret(){
        return this.firstExpression.interpret()+this.secondExpression.interpret();
    }
}
```

Using this, you construct a tree of expressions, call `interpret`, which 
recursively calls the equivalent `interpret` method on all its children and
grandchildren in the tree, eventually returning the final result. Here the 
result is an `int` that represents the sum of all the expression objects, 
representing an arithmetic expression, but there can be endless variations: 

- The `interpret` method make be called something else, e.g. `parse` if your 
  tree represents a grammar-definition and the goal is to parse some input
  
- It may take arguments, e.g. a `Context` parameter, if you are interpreting 
  something like a programming language AST or bytecode where you have to keep
  track of scoping

Often, you end up writing a parser to construct your expression objects from a
`String`. While not strictly part of the Interpreter pattern, almost all 
examples of it include a parser because constructing everything as nested `new`
invocations gets verbose:

```java
new AdditionExpression(
    new NumberExpression(1),
    new AdditionExpression(
        new NumberExpression(2),
        new NumberExpression(3)
    )
)
```

Instead, you take a string as input:

```java
"1+(2+3)"
```

And parse it into the `Expression` objects before calling `interpret`. The 
parser logic isn't shown here due to verbosity but the 
[Wikipedia example](https://en.wikipedia.org/wiki/Interpreter_pattern#Java)
shows one possible implementation.

In Scala, the Interpreter pattern still exists mostly unchanged, and is used in
libraries like:

- [FastParse](https://github.com/lihaoyi/fastparse): you construct a compound
  `Parser` object from simpler `Parser`s, and call `.parse(string)` to parse 
  some input 
 
- [Scalatags](https://github.com/lihaoyi/scalatags): you construct a compound
  html `Frag` from simpler `Frag`s, and call `.render` to render HTML

However, there are some tweaks that are distinct to using Interpreter in Scala:

### Defining the `Expression` classes is much simpler:

```scala
 
class NumberExpression(number: Int) extends Expression{
  def interpret = number
}
class AdditionExpression(firstExpression: Expression, 
                         secondExpression: Expression) extends Expression {
  def interpret() = firstExpression.interpret() + secondExpression.interpret()
}
```

No need to deal with private variables and constructors initializing those 
variables and all that. This is purely a syntactic change (the underlying 
model is exactly the same) but makes it much quicker to get started defining
your `Expression`s so you can start using them

### You often do not use a parser

While initializing nested `Expression`s is verbose and annoying in Java or C++,
in Scala it is possible to make the syntax much more concise. For example, 
Scalatags lets you define your nested `Frag` objects as such:
 
```scala
val frag = html(
  body(
    h1(id := "my-title")(
      "Hello"
    ),
    p(backgroundColor := "red")(
      "World"
    )
  )
)

frag.render
```

Which in Java would be something like


```scala
Frag frag = new HtmlFrag({},
  new HtmlFrag(
    new Header1Frag(new Attr[]{new Attr("id", "my-title")},
      new StringFrag("Hello")
    ),
    new ParagraphFrag(new Attr[]{new Attr("background-color", "red")},
      new StringFrag("Hello")
    )
  )
)

frag.render()
```

Similarly, FastParse lets you define a parser as:

```scala
val myParser = P( "a".rep ~ ("b" | "c" | "d") ~ End)

myParser.parse("aaaab")
```

Using custom operators like `~` and `|` to create the `Sequence` and `Either`
parsers for you, extension methods to add the `.rep` method on the string 
literal `"a"`, and macros to easily capture the name `"myParser"` to use in
the debugging output. 

The naive equivalent in Java would be
 
```java
Parser myParser = new NamedParser("parser",
    new SequenceParser(
        new RepeatParser(
            new StringParser("a")
        ),
        new EitherParser(
            new StringParser("a"),
            new StringParser("b"),
            new StringParser("c")
        ),
        EndParser.INSTANCE  
    ) 
)

myParser.parse("aaaab")
```

This is a relatively "naive" approach to instantiating the `Frag` and `Parser`
objects in code, and it could be cleaned up with some cleverness. Nonetheless,
the "in-code" instantiations of complex `Expression` objects is generally 
always many times more verbose in Java or C++ than in Scala, and hence while
the Interpreter pattern traditionally is almost always paired with a parser to
build the `Expression` from a string, in Scala people prefer to create their
`Expression`s in code using Scala's lightweight syntax

Apart from the reduction in verbosity, and the fact that people tend to prefer
creating their expression-trees in-code rather than parsing it from a string,
the interpreter pattern is still heavily used in Scala libraries and projects.

## Observer

> The observer pattern is a software design pattern in which an object, called 
the subject, maintains a list of its dependents, called observers, and notifies 
them automatically of any state changes, usually by calling one of their 
methods. - Wikipedia

The Observer pattern is another pattern that is still prevalant in Scala. It's
common in UI programming, such as in Swing or using the HTML DOM (using 
Scala.js) where you have inputs such as text-boxes or select-dropdowns whose
values can change, and you want to perform some action when they change.

Typical usage code in Java would look like:

```java
EventSource eventSource = ...

// New java 8 style with lambda expressions
eventSource.addObserver( (Observable obj, Object arg) -> { 
    System.out.println("Received response: " + arg);
    ...
});
```

Where `eventSource` usually comes from elsewhere, and you want to do something
whenever it updates. In Scala (Scala.js), the equivalent code may look like

```scala
val element: html.Element =   ...
element.addEventListener("mouseenter", (e: dom.Event) => {
  jQuery(popover).popover("show")
  currentlyShown = true
  ...  
}
```

Which is more or less identical.

While the "raw" Observer pattern is still common in Scala, there are a number 
of wrappers around the Observer pattern that are commonly used in particular 
use cases

- [Futures](#futures)
- [Streams](#streams)

### Futures

In the case where an event can only trigger once, it is usually best to 
encapsulate the observer pattern using a `scala.concurrent.Future`:

```scala
val myPromise = Promise[Int]()
val myFuture: Future[Int] = p.future


myFuture.foreach{
  case x => println(x)
}

myPromise.success(123) // prints 123
```

A `Promise[T]` is essentially a generic one-shot event source, and it's 
`.future: Future[T]` is interface that external observers can register 
themselves. The `.success(...)` method fires the event, causing all 
observers to be notified.

Futures tend to be much easier to work with than raw callback/observer-style 
interfaces: you can easily `.map` them to transform the output, `.zip` them if 
you want the results when two Futures are both complete, `Future.sequence`
if you want to wait on a whole list of Futures, or `.flatMap` if you want to
kick off a new Future depending on the result of the previous one. 

Under the hood, `Future[T]` and `Promise[T]` is all still implemented using 
Observers and callbacks, but most of the gnarly and error-prone parts are
nicely encapsulate in the Future so you don't trip up over them. Things like:

- Logic to maintain a list of observers, and register additional ones
- Thread-safety while all this mutable state is changing
- Control over "who runs" each callback, using an implicit `ExecutionContext`

Not every callback/observer-style API can be replaced by Futures: apart from
only allowing a single event to trigger, Futures also don't provide any 
flexibility over managing/de-registering observers after they've subscribed:
once you've called `.foreach` on a callback, there's no turning back. 
Nevertheless, in Scala you tend to use `scala.concurrent.Future` over raw
Observers whenever possible.

### Streams

Futures only allow for a single event; that is how they are defined. However,
there are other related projects that provide a similar style API that works
for long-lasting, multi-event streams. Some of those are:

- [RxScala](https://github.com/ReactiveX/RxScala)
- [Monix](https://github.com/monix/monix)
- [Scala.Rx](https://github.com/lihaoyi/scala.rx)

Each of these has a different design, style and tradeoffs, and none of them are
as "standard" as `scala.concurrent.Future` that is in the standard library.
Nonetheless, they are all easily available on Maven Central, and can be used in
your own projects just by adding the dependency in your build file. All of them
provide a nice layer over raw Observers, and are worth looking at if you find
your observer/callback-style code is getting out of hand.

## Conclusion

While you shouldn't obsess over design patterns, neither should you ignore them
complete and re-invent the world from first principles. Design patterns are a
common vocabulary of common solutions to common problems, and being able to
identify them helps you analyze programs and discuss them in a higher-level way 
without getting lost in the weeds. "you need to register an Observer" or "X is 
a builder for Y" is enough for someone to understand the high-level shape of a
piece of code, even if he hasn't dug through all the method names, signatures,
and thousands of lines of code.

While many of the design patterns as originally described in 1994 do not 
directly apply to the Scala language, many of them still apply with some tweaks
or modifications. Some, like [Singleton](#singleton), have been folded into the
language so well you no longer think of them as a "pattern". Others, like 
[Builder](#builder) or [Chain of Responsibility](#chain-of-responsibility),
have had much of their use-cases replaced by core language features, but are
still used as design-patterns in more complex cases. Then there are those
like [Adapter](#adapter) or [Interpreter](#interpreter) which apart from a
significant reduction in verbosity, work about the same as they always have.

This post only looks at how historical "Design Patterns" apply to the Scala
programming language, and does not look at what new design patterns have 
emerged that are unique to the Scala language. That would be a topic for a 
future post!

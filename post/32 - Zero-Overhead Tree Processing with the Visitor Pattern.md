The Visitor Pattern is one of the most mis-understood of the classic design
patterns. While it has a reputation as a slightly roundabout technique for doing
simple processing on simple trees, it is actually an advanced tool for a
specific use case: flexible, streaming, zero-overhead processing of complex data
structures. This blog post will dive into what makes the Visitor Pattern
special, and why it has a unique place in your toolkit regardless of what
language or environment you are programming in.

-------------------------------------------------------------------------------

## Json: a Strawman Data Structure

The Visitor Pattern operates on structured data: often, but not always,
hierarchical or tree-like data structures. For the sake of this article, we will
base our discussion on a subset of the JSON data format with only three kinds of
value:

- Strings: `"hello"`, without escapes, and without inner quotes (for simplicity)
- Integers: `12345`
- Dictionaries: `{"cow": "moo"}`, mapping String to Json values

This should serve to illustrate the usage of the Visitor Pattern, without the
overwhelming complexity of real data formats, and allow us to discuss the
techniques and approaches in the limited confines of a blog post. The techniques
described on this "Json-lite" format all apply to dealing with more complex
real-world formats.

Modeling such a JSON tree structure in Scala code, we might write something like
this:

```scala
abstract class Json
case class Str(value: String) extends Json
case class Num(value: Int) extends Json
case class Dict(pairs: (String, Json)*) extends Json
```

Below are some example Json data structures, expressed in both their textual
JSON format, as well as using the corresponding Scala data types defined above:

```json
"hello"
```
```scala
Str("hello")
```
```json
12345
```
```scala
Num(12345)
```
```json
{"cow": "moo"}
```
```scala
Dict("cow" -> Str("moo"))
```
```json
{
    "hello": {
        "i am": {"cow": 1},
        "you are": {"cow": 2}
    },
    "world": 31337
}
```

```scala
Dict(
  "hello" -> Dict(
    "i am" -> Dict("cow" -> Num(1)),
    "you are" -> Dict("cow" -> Num(2))
  ),
  "world" -> Num(31337)
)
```

## What is the Visitor Pattern

To begin with, let's assume that we have already parsed our Json from its
textual input format into the Json data types defined above:

```scala
val tree = Dict(
  "hello" -> Dict(
    "i am" -> Dict("cow" -> Num(1)),
    "you are" -> Dict("cow" -> Num(2))
  ),
  "world" -> Num(31337),
  "bye" -> Str("314")
)
```

The most trivial use of the Visitor Pattern involves writing two things:

- A `Visitor` class exposing methods that operate on each element in the
  Json tree

- A `dispatch` function that recursively walks over the Json tree and calls the
  relevant method on `Visitor`

A Visitor for our JSON tree may look something like this:

```scala
abstract class Visitor[T]{
  def visitStr(value: String): T
  def visitNum(value: Int): T
  def visitDict(): DictVisitor[T]
}
abstract class DictVisitor[T]{
  def visitKey(key: String): Unit
  def visitValue(): Visitor[T]
  def visitValue(value: T): Unit
  def done(): T
}
```

The contract of `Visitor` and `DictVisitor` is as follows:

- For each JSON string, `visitStr` is called
- For each JSON number, `visitNum` is called
- For each JSON dictionary, `visitDict` is called
  - Within that JSON dictionary, `visitKey` is called for each key
  - `visitValue()` is called before each dictionary value, and the `Visitor` it
    returns is used when visiting that value's JSON nodes
  - `visitValue(value: T)` is called on the result of the visiting that value

Exactly how "for each JSON {string, number, dictionary}" is implemented, is left
to a separate `dispatch` function.

For now, I am assuming that `Visitor` is generic: its methods return a type `T`,
representing the "output" of this `Visitor`. `T` will vary depending on what the
concrete Visitor implementation is trying to do:

- If it's meant to serialize the Json tree then `T` might be a `String`

- If it's meant to redact sensitive keys from the input data, or convert
  number-like strings into proper numbers, then `T` might be `Json`.

- If it's meant to perform some summary statistics on the Json tree, e.g.
  summing up all the numbers within it, then `T` might be an `Int`

Next, we have to write the `dispatch` function that takes both a Json tree,
and a `Visitor` object, and calls the various methods on the `Visitor` depending
on what it sees in the tree. For example:

```scala
def dispatch[T](input: Json, visitor: Visitor[T]): T = {
  input match{
    case Str(value) => visitor.visitStr(value)
    case Num(value) => visitor.visitNum(value)
    case Dict(pairs @ _*) => 
      val dictVisitor = visitor.visitDict()
      for((k, v) <- pairs){
        dictVisitor.visitKey(k)
        val subVisitor = dictVisitor.visitValue()
        dictVisitor.visitValue(dispatch(v, subVisitor))
      }
      dictVisitor.done()
  }
}
```

Let's look at some concrete implementations for `Visitor` that accomplish the
three things mentioned above:

### StringifyVisitor

A simple `Visitor` that renders the Json structure to a `String`. Not as
efficient as it could be - a production-quality serializer may want to build up
the output using a `StringBuilder` or render directly to an output stream - but
as an example it works well enough.

```scala
class StringifyVisitor extends Visitor[String]{
  def visitStr(value: String) = "\"" + value + "\""
  def visitNum(value: Int) = value.toString
  def visitDict() = new StringifyDictVisitor
}
class StringifyDictVisitor extends DictVisitor[String]{
  val tokens = collection.mutable.Buffer("{")
  def visitKey(key: String) = {
    if (tokens.length > 1) tokens.append(",")
    tokens.append("\"" + key + "\"")
    tokens.append(":")
  }
  def visitValue() = new StringifyVisitor
  def visitValue(value: String) = {
    tokens.append(value)
  }
  def done() = {
    tokens.append("}")
    tokens.mkString
  }
}

println(dispatch(tree, new StringifyVisitor))
// {"hello":{"i am":{"cow":1},"you are":{"cow":2}},"world":31337}
```

### RedactTreeVisitor

A simple `Visitor` that takes your Json structure and returns a new Json
structure with the value of any dictionary-key named `"hello"` removed.

```scala
class RedactTreeVisitor extends Visitor[Json]{
  def visitStr(value: String) = Str(value)
  def visitNum(value: Int) = Num(value)
  def visitDict() = new RedactTreeDictVisitor
}
class RedactTreeDictVisitor extends DictVisitor[Json]{
  val pairs = collection.mutable.Buffer.empty[(String, Json)]
  var lastKey = ""
  def visitKey(key: String) = {
    lastKey = key
  }
  def visitValue() = new RedactTreeVisitor
  def visitValue(value: Json) = {
    if (lastKey != "hello") pairs.append(lastKey -> value)
  }
  def done() = Dict(pairs:_*)
}

println(dispatch(tree, new RedactTreeVisitor)) 
// Dict("hello" -> Str("redacted"), "world" -> Num(31337))
```
### ToIntTreeVisitor

A simple `Visitor` that takes your Json structure and returns a new Json
structure with any number-like strings converted into proper Json numbers

```scala
class ToIntTreeVisitor extends Visitor[Json]{
  def visitStr(value: String) = {
    if (value.forall(_.isDigit)) Num(value.toInt)
    else Str(value)
  }
  def visitNum(value: Int) = Num(value)
  def visitDict() = new ToIntTreeDictVisitor
}
class ToIntTreeDictVisitor extends DictVisitor[Json]{
  val pairs = collection.mutable.Buffer.empty[(String, Json)]
  var lastKey = ""
  def visitKey(key: String) = {
    lastKey = key
  }
  def visitValue() = new ToIntTreeVisitor
  def visitValue(value: Json) = {
    pairs.append(lastKey -> value)
  }
  def done() = Dict(pairs:_*)
}

println(dispatch(tree, new ToIntTreeVisitor)) 
// Dict("hello" -> Str("redacted"), "world" -> Num(31337))
```

### SummationVisitor

A simple `Visitor` that adds up all the numbers within a Json tree and
returns the sum:

```scala
class SummationVisitor extends Visitor[Int]{
  def visitStr(value: String) = 0
  def visitNum(value: Int) = value
  def visitDict() = new SummationDictVisitor
}
class SummationDictVisitor extends DictVisitor[Int]{
  var sum = 0
  def visitKey(key: String) = {} // do nothing
  def visitValue() = new SummationVisitor
  def visitValue(value: Int) = {
    sum += value
  }
  def done() = sum
}

println(dispatch(tree, new SummationVisitor)) 
// 31340
```


## Visitor Pattern vs Recursive Transformations

So far, we have seen some ways of using the Visitor Pattern to process our Json
trees. Nevertheless, in these simple cases, it seems like a very roundabout way
of doing something really simple: I could just as easily have written simple
functions that recurse over the Json tree and do what I want.

Here are sample implementations of `stringify`, `redact`, `toInt` and
`summation` as simple recursive functions:

```scala
def stringify(input: Json): String = {
  input match{
    case Str(value) => "\"" + value + "\""
    case Num(value) => value.toString
    case Dict(pairs@_*) =>
      val tokens = collection.mutable.Buffer("{")
      for((key, value) <- pairs){
        if (tokens.length > 1) tokens.append(",")
        tokens.append("\"" + key + "\"")
        tokens.append(":")
        tokens.append(stringify(value))
      }
      tokens.append("}")
      tokens.mkString
  }
}
println(stringify(tree))
// {"hello":{"i am":{"cow":1},"you are":{"cow":2}},"world":31337}
```
```scala
def redact(input: Json): Json = {
  input match{
    case Str(value) => Str(value)
    case Num(value) => Num(value)
    case Dict(pairs@_*) =>
      val newPairs = collection.mutable.Buffer.empty[(String, Json)]
      for((key, value) <- pairs){
        if (key != "hello") newPairs.append(key -> redact(value))
      }
      Dict(newPairs:_*)
  }
}
println(redact(tree))
// Dict("world" -> Num(31337))
```
```scala
def toInt(input: Json): Json = {
  input match{
    case Str(value) => 
      if (value.forall(_.isDigit)) Num(value.toInt)
      else Str(value)
    case Num(value) => Num(value)
    case Dict(pairs@_*) =>
      val newPairs = collection.mutable.Buffer.empty[(String, Json)]
      for((key, value) <- pairs){
        newPairs.append(key -> toInt(value))
      }
      Dict(newPairs:_*)
  }
}
println(toInt(tree))
// Dict("world" -> Num(31337))
```
```scala
def summation(input: Json): Int = {
  input match{
    case Str(value) => 0
    case Num(value) => value
    case Dict(pairs@_*) =>
      var total = 0
      for((key, value) <- pairs) total += summation(value)
      total
  }
}
println(summation(tree))
// 31340
```

In doing so, I have accomplished the same outcome as we did earlier using the
Visitor Pattern, but with a single function rather than one function and two
classes! Here were are using Scala's `match` pattern matching syntax, but it
could just as easily be done using Java's `instanceof` and `(Str)` casts, or
Python's `isinstance`. What, then, is the point of all this `Visitor` stuff?

## Chaining Recursive Functions

The recursive transformation functions written above can be chained: as long as
the types line up, the output of one can be trivially fed into another, for
example performing a summation after redaction, or stringifying the redacted
Json trees:

```scala
println(summation(redact(tree)))
// 31337

println(stringify(redact(tree)))
// {"world":31337}

println(summation(toInt(redact(tree))))
// 31651
```

However, there is a downside to this approach: each recursive transformation you
chain in this way creates an entire intermediate `Json` tree structure to
pass to the next transform in the chain. In the above examples there's only one
intermediate tree - the output of `redact` - but you can easily imagine chains
of transformations with dozens of stages: redact some sensitive data, convert
all strings to lowercase, sort the keys of the dictionaries, etc. In such cases,
creating and throwing away dozens of intermediate trees is wasteful and can be a
performance bottleneck.

There is a solution to this problem: to fuse the different recursive
transformations together manually. For example, you may fuse `summation` and
`toInt` and `redact` into a single `redactToIntSum` function as follows:

```scala
def redactToIntSum(input: Json): Int = {
  input match{
    case Str(value) => if (value.forall(_.isDigit)) value.toInt else 0
    case Num(value) => value
    case Dict(pairs@_*) =>
      var total = 0
      for((key, value) <- pairs) {
        if (key != "hello"){
          total += summation(value)
        }
      }
      total
  }
}
println(redactToIntSum(tree))
// 31337
```

This gets us the efficiency we want - we no longer are constructing an
intermediate tree in `redact` just to pass it to `toInt`, and construction and
intermediate tree in `toInt` just to pass to `summation` - but at a cost of
flexibility. We have to manually fuse the recursive transformation we want into
a single function and can no longer mix-and-match the different transforms as
we'd like. Manually fusing all possible combinations would require O(n!)
different fused functions, which quickly becomes unfeasible.

## Chaining Visitors

Like recursive functions, Visitors can also be chained. A slight modification is
necessary to the `RedactTreeVisitor` above to make this possible:

```scala
class RedactVisitor[T](downstream: Visitor[T]) extends Visitor[T]{
  def visitStr(value: String) = downstream.visitStr(value)
  def visitNum(value: Int) = downstream.visitNum(value)
  def visitDict() = new RedactDictVisitor(downstream.visitDict())
}
class RedactDictVisitor[T](downstream: DictVisitor[T]) extends DictVisitor[T]{
  var lastKey = ""
  def visitKey(key: String) = {
    lastKey = key
    if (lastKey != "hello") downstream.visitKey(key)
  }
  def visitValue() = new RedactVisitor[T](downstream.visitValue())
  def visitValue(value: T) = {
    if (lastKey != "hello") downstream.visitValue(value)
  }
  def done() = downstream.done()
}
```

Which can then be chained with e.g. `StringifyVisitor` or `SummationVisitor` as
follows:

```scala
println(dispatch(tree, new RedactVisitor(new StringifyVisitor)))
// {"world":31337}

println(dispatch(tree, new RedactVisitor(new SummationVisitor)))
// 31337
```

Note how the above code is slightly different from the `RedactVisitor` we saw
earlier: rather than immediately constructing a `Json` value to return, it
simply forwards to a `downstream: Visitor[T]` and returns whatever `T` the
`downstream`'s methods return.

A similar change could be made to `ToIntTreeVisitor`:

```scala
class ToIntVisitor[T](downstream: Visitor[T]) extends Visitor[T]{
  def visitStr(value: String) = {
    if (value.forall(_.isDigit)) downstream.visitNum(value.toInt)
    else downstream.visitStr(value)
  }
  def visitNum(value: Int) = downstream.visitNum(value)
  def visitDict() = new ToIntDictVisitor(downstream.visitDict())
}
class ToIntDictVisitor[T](downstream: DictVisitor[T]) extends DictVisitor[T]{
  def visitKey(key: String) = downstream.visitKey(key)
  def visitValue() = new ToIntVisitor[T](downstream.visitValue())
  def visitValue(value: T) = downstream.visitValue(value)
  def done() = downstream.done()
}
```

Which allows us to chain both `ToIntVisitor` and `RedactVisitor` together:

```scala
println(dispatch(tree, new RedactVisitor(new ToIntVisitor(new SummationVisitor))))
// 31337
```

Like recursive transformations, Visitors can be chained in arbitrary ways using
the `downstream` argument: this makes it just as easy to compose whatever
computation you want out of smaller, independent parts.

Unlike chaining recursive transformations like `redact`/`toInt`/`summation`,
chaining Visitors does not produce any intermediate trees: you simply feed in
the original `tree`, and in one pass it computes the redaction/toInt/summation
and produces a result. You do not need to manually fuse the computations into a
single function if you want it to be efficient!

## Streaming Sources

Above, we have already seen how `Visitor`s are much more verbose than manually
writing recursive transformation functions, but with an upside: you can chain
`Visitor`s together to combine their transformations without needing to
construct intermediate data structures, and without needing to manually write
code to fuse the transformations you want into a single, big function.

However, one assumption we have made so far is that we are starting from the
already-parsed, structured data in-memory: the `Json` classes defined above.
This is often not the case in reality! In reality, often you are starting from
some kind of serialized data format: text files, binary data coming over a
network, etc.. For now, let's just consider the textual form of our Json format:

```json
{
    "hello": {
        "i am": {"cow": 1},
        "you are": {"cow": 2},
    },
    "world": 31337
}
```

To convert this into our `Json` data structure, we will need a parser. A
simple parser might look something like this:

```scala
class Parser{
  var offset: Int = 0
  val DOUBLE_QUOTE = 34.toChar
  def whitespace(input: String) = {
    while(input(offset).isWhitespace) offset += 1
  }
  def parse(input: String): Json = {
    if(input(offset) == DOUBLE_QUOTE) Str(parseStr(input))
    else if(input(offset).isDigit) Num(parseNum(input))
    else if(input(offset) == '{') parseDict(input)
    else ???
  }
  def parseNum(input: String) = {
    val start = offset
    while(input(offset).isDigit) offset += 1
    input.slice(start, offset).toInt
  }
  def parseStr(input: String) = {
    val start = offset
    offset += 1
    while(input(offset) != DOUBLE_QUOTE) offset += 1
    offset += 1
    input.slice(start + 1, offset - 1)
  }
  def parseDict(input: String): Dict = {
    val pairs = collection.mutable.Buffer.empty[(String, Json)]
    offset += 1
    var done = false
    while(!done){
      whitespace(input)
      val key = parseStr(input)
      whitespace(input)
      assert(input(offset) == ':', input(offset) -> offset)
      offset += 1
      whitespace(input)
      val value = parse(input)
      pairs.append(key -> value)
      whitespace(input)
      if (input(offset) == '}') done = true
      offset += 1
    }
    
    Dict(pairs:_*)
  }
}
def parse(input: String) = new Parser().parse(input)
```

Used as follows:

```scala
val input = """{
    "hello": {
        "i am": {"cow": 1},
        "you are": {"cow": 2}
    },
    "world": 31337
}"""

val data = parse(input)
// Dict(
//   "hello" -> Dict(
//     "i am" -> Dict("cow" -> Num(1)),
//     "you are" -> Dict("cow" -> Num(2))
//   ),
//   "world" -> Num(31337)
// )

println(summation(redact(tree)))
// 31337

println(stringify(redact(tree)))
// {"world":31337}
```

It might seem obvious that a `Parser` parses an input string into some kind of
tree structure for further processing, but there is another alternative: the
`Parser` could instead take a `Visitor` to dispatch to! This looks like the
following:

```scala
class DispatchParser{
  var offset: Int = 0
  val DOUBLE_QUOTE = 34.toChar
  def whitespace(input: String) = {
    while(input(offset).isWhitespace) offset += 1
  }
  def parse[T](input: String, visitor: Visitor[T]): T = {
    if(input(offset) == DOUBLE_QUOTE) visitor.visitStr(parseStr(input))
    else if(input(offset).isDigit) visitor.visitNum(parseNum(input))
    else if(input(offset) == '{') parseDict(input, visitor.visitDict())
    else ???
  }
  def parseNum(input: String) = {
    val start = offset
    while(input(offset).isDigit) offset += 1
    input.slice(start, offset).toInt
  }
  def parseStr(input: String) = {
    val start = offset
    offset += 1
    while(input(offset) != DOUBLE_QUOTE) offset += 1
    offset += 1
    input.slice(start + 1, offset - 1)
  }
  def parseDict[T](input: String, dictVisitor: DictVisitor[T]): T = {
    offset += 1
    var done = false
    while(!done){
      whitespace(input)
      val key = parseStr(input)
      dictVisitor.visitKey(key)
      whitespace(input)
      assert(input(offset) == ':', input(offset) -> offset)
      offset += 1
      whitespace(input)
      val value = parse(input, dictVisitor.visitValue())
      dictVisitor.visitValue(value)
      whitespace(input)
      if (input(offset) == '}') done = true
      offset += 1
    }
    
    dictVisitor.done()
  }
}

def dispatchParse[T](input: String, visitor: Visitor[T]) = {
  new DispatchParser().parse(input, visitor)
}
```


Effectively, rather than having a `dispatch` function recurse over a structured
`Json` tree and call the `Visitor`'s methods, we have a `dispatchParse`
function than parses over the un-structured Json text and dispatches calls
to the `Visitor`s methods. The `Visitor`s themselves do not care who is calling
their methods as long as they are called in the same order, so they should
behave the same - and produce the same result - either way.

You can use this `DispatchParser` as follows:

```scala
println(dispatchParse(res32, new StringifyVisitor()))
// {"hello":{"i am":{"cow":1},"you are":{"cow":2}},"world":31337}

println(dispatchParse(res32, new SummationVisitor()))
// 31340

println(dispatchParse(res32, new RedactVisitor(new SummationVisitor())))
// 31337
```

What's happening here is worth calling out explicitly: we are processing the
Json data without ever parsing it into a full tree structure!

- In the first case, we are constructing the minified output string while the
  input string is still being parsed.

- In the second case, we are summing up the numbers in the Json during the
  parse.

- In the third, we are feeding the Json through the redactor, then
  summing up the remaining numbers

All three computations above happen without constructing any Json tree
structure: by paying the verbosity cost of using the Visitor pattern instead of
writing recursive computations on the Json tree, in exchange we get the ability
to run our computations on raw input data without needing to parse/store the
entire data structure in memory.

## Tree Construction Visitors

`ToIntVisitor` and `RedactVisitor` above can be chained into downstream
visitors, to combine their computations but without constructing intermediate
data structures. Sometimes this is what you want, but sometimes it isn't:
sometimes you actually *do* want to spit out a `Json` structure. Apart from
maintaining two versions of `ToIntVisitor` and `RedactVisitor` - one chainable
and one not - what can we do?

It turns out the solution is simple: define a `Visitor` that does nothing but
constructs the `Json` tree structure! It looks something like this:

```scala
class ConstructionVisitor extends Visitor[Json]{
  def visitStr(value: String) = Str(value)
  def visitNum(value: Int) = Num(value)
  def visitDict() = new ConstructionDictVisitor
}
class ConstructionDictVisitor extends DictVisitor[Json]{
  val pairs = collection.mutable.Buffer.empty[(String, Json)]
  var lastKey = ""
  def visitKey(key: String) = {
    lastKey = key
  }
  def visitValue() = new ConstructionVisitor
  def visitValue(value: Json) = {
    pairs.append(lastKey -> value)
  }
  def done() = Dict(pairs:_*)
}
```

Now, if you want a version of `RedactVisitor` or `ToIntVisitor` that
constructs a `Json` tree, simply chain it to `ConstructionVisitor`:

```scala
dispatchParse(res32, new RedactVisitor(new ConstructionVisitor()))

dispatchParseres32, new ToIntVisitor(new ConstructionVisitor()))
// Dict("world" -> Num(31337))
```

Furthermore, you can now trivially convert the `Visitor`-based `DispatchParser`
into a `Json` tree-building `Parser`, just by providing it a
`ConstructionVisitor`:

```scala
dispatchParse(res32, new ConstructionVisitor())
// Dict(
//   "hello" -> Dict(
//     "i am" -> Dict("cow" -> Num(1)),
//     "you are" -> Dict("cow" -> Num(2))
//   ),
//   "world" -> Num(31337)
// )
```

Previously, defining `Visitor`-based transformations rather than a
recursive-transformation-function based API was a tradeoff: you could chain them
to other `Visitor`s for zero-overhead streaming processing, but you lost the
ability to construct concrete structures in the case that is what you wanted.
With `ConstructionVisitor`, the tradeoff is gone: your `Visitor`-based
transforms can now be trivially converted into tree-building transformations
simply by chaining the together with `ConstructionVisitor`!

## Composability

So far we have defined two dispatchers:

- `dispatchParse("...", _)`
- `dispatch(tree, _)`

Two chainable visitors:

- `RedactVisitor`
- `ToIntVisitor`

And three non-chainable "terminal" Visitors:

- `StringifyVisitor`
- `SummationVisitor`
- `ConstructionVisitor`

What makes the Visitor Pattern so flexible is the way you can plug & play these
simple components any way you like, performing operations on trees:

- `dispatch(tree, new SummationVisitor)` performs a summation over the tree

Serializing trees to raw text:

- `dispatch(tree, new StringifyVisitor)`

Parsing raw text into trees::

- `dispatchParse(text, new ConstructionVisitor)`

Zero-overhead chaining of multiple operations without intermediate trees:

- `dispatch(tree, new RedactVisitor(new StringifyVisitor))` serializes a tree to
  a minified string with redacted keys/values removed

- `dispatch(tree, new ToIntVisitor(new SummationVisitor))` performs a
  summation over the tree after converting number-like strings to numbers

- `dispatch(tree, new RedactVisitor(new ToIntVisitor(new SummationVisitor))) `
  performs a summation over the tree after converting number-like strings to
  numbers *and* removing all redacted keys/values

Directly performing operations on raw text, without constructing a tree:

- `dispatchParse(text, new StringifyVisitor)` performs a streaming re-formatting
  of raw input text into minified text

- `dispatchParse(text, new SummationVisitor)` performs a streaming summation
  over raw input text

Perform chained, zero-overhead operations directly on raw input text:

- `dispatchParse(text, new RedactVisitor(new StringifyVisitor))` performs a
  streaming redaction directly on the raw input text

- `dispatchParse(text, new ToIntVisitor(new SummationVisitor))` performs a
  streaming summation over the raw input text after converting number-like
  strings to numbers

- `dispatchParse(text, new RedactVisitor(new ToIntVisitor(new
  SummationVisitor)))` performs a summation over the raw input text after
  converting number-like strings to numbers *and* removing all redacted
  keys/values

We have paid a cost in complexity: rather than writing e.g. a single recursive
`def redact(input: Json): Json` function, we had to write a `dispatch`
functions and pair of `RedactorVisitor` classes.

In exchange we get the flexibility of chaining different
recursive-transformation functions one after the other, but with zero-overhead
as if we had manually merged those transformations into a single function.
Furthermore, all our `Visitor` machinery can work regardless of dispatcher, so
we can implement `dispatchParse` and immediately have the ability to do
streaming computations directly on raw input text, without ever parsing it into
a complete tree structure, with all of our existing
`Redact`/`ToInt`/`Stringify`/`Summation` operations immediately available for
us to use for free!

## Further work using the Visitor Pattern

There are further interesting things you can do with the Visitor pattern that I
will touch on but not go into too deeply:

### Syntax Validation

You can define a new terminal visitor that does nothing:

```scala
class NoOpVisitor extends Visitor[Unit]{
  def visitStr(value: String) = ()
  def visitNum(value: Int) = ()
  def visitDict() = new NoOpDictVisitor
}
class NoOpDictVisitor extends DictVisitor[Unit]{
  def visitKey(key: String) = ()
  def visitValue() = new NoOpVisitor()
  def visitValue(value: Unit) = ()
  def done() = ()
}
```

And combine it with `dispatchParse` to allow zero-overhead validation of raw
input text without constructing a throw-away JSON tree:

```scala
val text = """{
  "hello": "cow",
  "world": {
    "foo": "bar",
    "bar": 123
  }
}"""

dispatchParse(text, new NoOpVisitor) 
// no result!


val brokenText = """{
  "hello": "cow",
  "world": {
    "foo": "bar",
    "bar" 123
  }
}"""

dispatchParse(brokenText, new NoOpVisitor)
// java.lang.AssertionError: assertion failed: (1,62)
```

Validating that serialized data follows a particular format is a common task.
Most people would use a parser to parse the input and report any errors, and
simple throw away the data structure the parser generates: a workable but
somewhat wasteful solution. Using the Visitor Pattern, we can simply combine our
existing `DispatchParser` that we used for streaming computations with a
`NoOpVisitor` and get a zero-overhead syntax validator entirely for free

### Data Mapping

Another common thing to do is to parse JSON into instances of classes to use in
your application. perhaps you want to convert:

```json
{
  "hello": "cow", 
  "world": {
    "foo": "bar",
    "bar": 123
  }
}

```

Into an instance of the class:

```scala
case class Thingy(hello: String, world: Inner)
case class Inner(foo: String, bar: Int)
```

You can use this a set of Visitors to create `Thingy` and `Inner`:

```scala

class LiteralVisitor extends Visitor[Any]{
  def visitStr(value: String) = value
  def visitNum(value: Int) = value
  def visitDict() = ???
}

abstract class InstantiatorVisitor extends Visitor[Any]{
  def visitStr(value: String) = ???
  def visitNum(value: Int) = ???
  def done(values: Seq[Any]): Any
  def visitDict() = new InstantiatorDictVisitor(done, visitors)
  def visitors: Map[String, Visitor[Any]]
}
class InstantiatorDictVisitor(outerDone: Seq[Any] => Any,
                              visitors: Map[String, Visitor[Any]]) 
                             extends DictVisitor[Any]{
  var lastKey = ""
  val values = collection.mutable.Buffer.empty[Any]
  def visitKey(key: String) = {
    lastKey = key
  }
  def visitValue() = visitors(lastKey)
  def visitValue(value: Any) = {
    values.append(value)
  }
  def done(): Any = outerDone(values)
}
class ThingVisitor extends InstantiatorVisitor{
  def visitors = Map(
    "hello" -> new LiteralVisitor,
    "world" -> new InnerVisitor
  )
  def done(values: Seq[Any]) = new Thingy(
    values(0).asInstanceOf[String],
    values(1).asInstanceOf[Inner]
  )
}
class InnerVisitor extends InstantiatorVisitor{
  def visitors = Map(
    "foo" -> new LiteralVisitor,
    "bar" -> new LiteralVisitor
  )
  def done(values: Seq[Any]) = new Inner(
    values(0).asInstanceOf[String],
    values(1).asInstanceOf[Int]
  )
}
```

Which can be used both to construct `Thingy`s from `Json` trees:

```scala
val tree = parse("""{
  "hello": "cow", 
  "world": {
    "foo": "bar",
    "bar": 123
  }
}""")
dispatch(tree, new ThingVisitor).asInstanceOf[Thingy]
// Thingy("cow", Inner("bar", 123))
```

And to construct `Thingy`s from raw Json text, without the intermediate
tree:

```scala
val text = """{
  "hello": "cow", 
  "world": {
    "foo": "bar",
    "bar": 123
  }
}"""
dispatchParse(text, new ThingVisitor).asInstanceOf[Thingy]
// Thingy("cow", Inner("bar", 123))
```

Doing your data mapping straight from raw input text to your class instances can
easily double the throughput compared to first parsing your text into a
`Json` tree.

You can similarly re-serialize your class instances back to Json using the
Visitor Pattern as well. Simply define a dispatcher:

```scala
def dispatchInstance[T](value: Thingy, visitor: Visitor[T]): T
```

And you can immediately start using it to serialize your class instances either
to Json trees:

```scala
dispatchInstance(Thingy("cow", Inner("bar", 123)), new ConstructionVisitor)
// Dict(
//   "hello" -> Str("cow"),
//   "world" -> Dict("foo" -> Str("bar"), "bar" -> Num(123))
// )
```

Or directly to an output string:

```scala
dispatchInstance(Thingy("cow", Inner("bar", 123)), new StringifyVisitor)
// {"hello":"cow","world":{"foo":"bar","bar":123}}
```

Again with zero-overhead from intermediate trees. `dispatchInstance` itself can
be implemented in a variety of different ways: using reflection, using
type-classes, etc. but for now is left as an exercise to the reader.

### True Streaming IO

Above, our `dispatchParse` function starts dispatching to the visitor from an
in-memory `String`:

```scala
def dispatchParse[T](input: String, visitor: Visitor[T]): T
```

It is not difficult to make an equivalent `dispatchParseStream` that can work on
JSON coming from arbitrary `java.io.InputStream`s:

```scala
def dispatchParseStream[T](input: java.io.InputStream, visitor: Visitor[T]): T
```

Similarly, while the `StringifyVisitor` above accumulates the result in-memory
as a `String`, you could easily define a `OutputStreamVisitor` that streams the
output directly to a `java.lang.OutputStream:

```scala
class OutputStreamVisitor(output: java.io.OutputStream) extends Visitor[Unit]{
  ...
}
```

This would allow you to perform streaming processing parses on Json data
that may be too large to fit in memory: you could perform a summation,
validation or directly off a file on disk without ever loading the whole file
into memory:

```scala
// Validate JSON on disk
dispatchParseStream(new java.io.FileInputStream("big.json"), new NoOpVisitor)

// Sum up numbers from JSON on disk
dispatchParseStream(new java.io.FileInputStream("big.json"), new SummationVisitor)

// Redact JSON from disk to another file on disk
dispatchParseStream(
  new java.io.FileInputStream("big.json"), 
  new RedactVisitor(
    new OutputStreamVisitor(new java.io.FileOutputStream("big-redacted.json"))
  )
)
```

All of these capabilities come entirely free: our previous `RedactVisitor`,
`SummationVisitor` and `NoOpVisitor` are entirely unchanged, but thanks to the
Visitor Pattern are now able to perform their computations in a 100% streaming
fashion.

And of course, once you're working with `InputStream`s and `OutputStream`s we
can now perform streaming JSON processing directly over the network either as
well!

The exact implementations of `dispatchParseStream` and `OutputStreamVisitor` are
left as an exercise for the reader.

## What's the Visitor Pattern All About?

The Visitor Pattern gives you flexible, streaming, zero-overhead processing of
complex data structures. While composable tree-transforming functions give you
the flexibility but without the efficiency, and manually-fusing operations in
one big function gives you the efficiency without the flexibility. The Visitor
Pattern gives you the best of both worlds, while allowing your computations to
happen in a streaming fashion where there isn't a concrete data-structure at
all!

Libraries like the [ASM Bytecode Engineering Library](http://asm.ow2.io/) and
the [uPickle JSON serialization library](https://github.com/lihaoyi/upickle)
make heavy use of the visitor pattern to implement their complex, performant
bytecode and JSON transformations. While this blog post uses JSON-lite
processing as an example, the same principles apply to any sort of processing of
complex data structures

If you are just doing a simple computation using a concrete data structure, it
does not make sense to use the Visitor Pattern: just define a recursive function
using `isinstanceof` and do what you want directly.

Where the Visitor Pattern shines is when your computation is neither simple, nor
on a concrete data structure:

- If you want to break up a complex transformation on a complex data structure
  into multiple smaller computations, but don't want to wastefully generate a
  bunch of intermediate data structures

- If you want to perform computations on data that doesn't have any concrete
  data structure at all: performing your computations in a streaming fashion,
  dispatched directly from the parser reading a file or over the network, and
  never having the entire input or output dataset in memory

If you have either of these requirements, the Visitor Pattern is for you.

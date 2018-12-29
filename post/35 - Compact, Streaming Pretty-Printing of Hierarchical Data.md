Pretty-printing hierarchical data into a human-readable form is a common thing
to do. While a computer doesn't care exactly how you format the same textual
data, a human would want to view data that is nicely laid out and indented, yet
compact enough to make full use of the width of the output window and avoid
wasting horizontal screen space. This post presents an algorithm which achieves
optimal usage of horizontal space, predictable layout, and good runtime
characteristics: peak heap usage linear in the width of the output window, the
ability to start and stop the pretty-printing to print any portion of it, with
total runtime linear in the portion of the structure printed.

-------------------------------------------------------------------------------

Many data formats are hierarchical: whether textual formats like JSON or YAML,
binary formats like MessagePack or Protobuf, or even program source code in
common languages like Java or Python. However, the same textual data can
be formatted a variety of ways: perhaps it was written without strong style
conventions, or minified to send over the wire. Binary data, of course, needs to
be converted to textual data for human readability. While some data formats have
interactive GUIs to let you explore them, in most cases that job falls to the
pretty-printer to convert the data structure into a plain-text string that is
sufficiently nicely formatted that someone can skim over it and find what they
want without undue difficulty.

## Requirements


### Indented Output

Let us consider two samples of hierarchical data, formatted to fit within a 50
character wide screen. A JSON blob:

```json
{
    "person1": {
        "name": "Alice", 
        "favoriteColors": ["red", "green", "blue"]
    },
    "person2": {
        "name": "Bob",
        "favoriteColors": [
            "cyan", 
            "magenta", 
            "yellow", 
            "black"
        ]
    }
}
```

And a Python code snippet:


```python
model = Sequential(
    Dense(512, activation=relu),
    Dense(10, activation=softmax)
)
```

These two examples (both simplified from real code) are both roughly formatted
to fit nicely within a 50 character wide output. Note how both examples have a
mix of horizontally and vertically laid out structures: e.g. `["red", "green",
"blue"]` is horizontally laid out because it can fit within our 50 character max
width, while `["cyan", "magenta", "yellow", "black"]` is vertically laid out
because if laid out horizontally it would overshoot. This layout max maximal use
of the horizontal space available while also formatting things vertically where
necessary.

While there are some variety in exactly how things should be formatted - e.g.
some people prefer closing braces on the same line as the enclosing statement -
this post will walk through the algorithm for the choice of pretty-printing
given above. Adapting it to other styles is straightforward.

### Configurable Width

The way you pretty-print a structure depends on how wide you want it to be: for
example, above we assumed a target width of 50 characters. If it was narrower,
we would likely want to spread things out more vertically to avoid overshooting
our target width. Here is the Javascript example formatted for a target width of
30 characters:


```json
{
    "person1": {
        "name": "Alice", 
        "favoriteColors": [
            "red", 
            "green", 
            "blue"
        ]
    },
    "person2": {
        "name": "Bob",
        "favoriteColors": [
            "cyan", 
            "magenta", 
            "yellow", 
            "black"
        ]
    }
}
```

Here, we can see the `["red", "green", "blue"]` array that was previously laid
out horizontally now is laid out vertically to avoid hitting the line limit. And
the Python code snippet:


```python
model = Sequential(
    Dense(
        512, 
        activation=relu
    ),
    Dense(
        10, 
        activation=softmax
    )
)
```

Here, we see the `Dense` expressions are laid out vertically, to fit within the
30 character width limit.

Output widths can also be wider, rather than narrower. If we expand our output
to 80 characters, we would expect to see more things laid out horizontally to
take advantage of the added space to the right:

```json
{
    "person1": {"name": "Alice", "favoriteColors": ["red", "green", "blue"]},
    "person2": {
        "name": "Bob",
        "favoriteColors": ["cyan", "magenta", "yellow", "black"]
    }
}
```
```python
model = Sequential(Dense(512, activation=relu), Dense(10, activation=softmax))
```

Here, we see more items laid out horizontally: the entire `"person1"` in the
JSON example fits on one line, as does the entire `model` in the Python example,
while `"person2"` is still too long to fit within the 80 character limit without
wrapping.

### Efficiency

The last requirement that is often seen in pretty-printing is that of
efficiency:

1. **Constant heap usage**: I would want to be able to pretty print large data
   structures without needing to load it all into memory at the same time for
   manipulation.

2. **Linear execution time**: pretty-printing should take time proportional to
   the thing you are trying to print, and shouldn't scale quadratically or
   exponentially when that thing gets bigger

3. **Laziness**: I should be able to print only the part of the structure that
   I want, and then stop. For large structures, you often only need to see part
   of it (e.g. first 100 lines) to figure out what you need, and you shouldn't
   need to pay to pretty-print the entire structure.

Note that most common pretty-printers fail these requirements. Specifically,
even something like a `.toString` function fails (1) because it has to
materialize the whole thing thing in memory, and (3) because it has to construct
the entire output string before you can see any output at all. This post will
show you how to do better.

## The Algorithm

I will present the algorithm written in Scala, but it should be easy to
understand for anyone with familiarity with programming and trivially
convertible to any language of your choice.

### Interface

Now that we know what our requirements are, let us define the interface of our
pretty-print function:

```scala
def prettyprint(t: T, maxWidth: Int, indent: Int): Iterator[String]
```

In detail:

- `t` is the thing being pretty-printed, of type `T`. That would be either our
  JSON data structure, our Python syntax tree, or something else

- `maxWidth` is the width the pretty-printer will try to avoid breaching.

- `indent` is how far to indent nested parts of the pretty-printed output. In
  the above examples, this would be 4 spaces.

- The `Iterator[String]` being returned represents the chunks of the
  pretty-printed output, that the caller of the function can stream on-demand
  and handle individually (writing them to a file, writing to stdout, ...) with
  the ability to stop early at any time and without ever materializing the whole
  output in memory.

One comon use case that we leave out here is a `maxHeight: Int` flag: while it
is very common to want to see the first N lines of the pretty-printed output
without evaluating the whole thing, this is trivially implementable on top of
the `Iterator[String]` that `prettyprint` returns, and so implementing a
`maxHeight` flag is left as an exercise to the reader.

### Trees

To begin with, I will define a `Tree` data structure: this will be used to
represent the thing we are trying to print in a hierarchical fashion:

```scala
sealed trait Tree

// Foo(aa, bbb, cccc)
case class Nested(prefix: String,
                  children: Iterator[Tree],
                  sep: String,
                  suffix: String) extends Tree

// xyz
case class Literal(body: String) extends Tree
```

This defines a type `Tree` with two subclasses: a `Tree` is either an `Nested`
node representing something with a prefix/children/separator/suffix such as
`Foo(aa, bbb, cccc)` or `(123 456 789)`, or a `Literal` node representing a
simple string. Note that the `children` of an `Nested` node is a one-shot
`Iterator[Tree]`, rather than a concrete `Array[Tree]`: we can define a `Tree` to
mirror our data structure without actually materializing the whole thing in
memory, as long as we only need to iterate over the tree once.

This is a relatively minimal representation, and is simplified for the sake of
this blog post: it does not have handling for infix operators `LHS op RHS`, any
sort of terminal nodes beyond a literal string, or anything else. Nevertheless,
it is enough to handle many common formats, including the examples above. the
Python example:

```python
model = Sequential(
    Dense(512, activation=relu),
    Dense(10, activation=softmax)
)
```


Can be represented as:
```scala
def python = Nested(
  "model = Sequential(",
  Iterator(
    Nested(
      "Dense(",
      Iterator(Literal("512"), Literal("activation=relu")), 
      ",", 
      ")"
    ),
    Nested(
      "Dense(",
      Iterator(Literal("100"), Literal("activation=softmax")), 
      ",", 
      ")"
    ),
  ),
  ",",
  ")"
)
```
This is a bit of a mouthful, but it represents the entirety of the `Tree` that
can be constructed from your Python syntax tree. Note that in real code, this would
be constructed from an existing structure, rather than laid out literally as
above.

Similarly the JSON snippet:
```json
{
    "person1": {
        "name": "Alice", 
        "favoriteColors": ["red", "green", "blue"]
    },
    "person2": {
        "name": "Bob",
        "favoriteColors": [
            "cyan", 
            "magenta", 
            "yellow", 
            "black"
        ]
    }
}
```

Can be represented as:

```scala
def json = Nested(
  "{",
  Iterator(
    Nested(
      "\"person1\": {",
      Iterator(
        Literal("\"name\": \"Alive\""),
        Nested(
          "\"favoriteColors\": [",
          Iterator(
            Literal("\"red\""),
            Literal("\"green\""),
            Literal("\"blue\"")
          ),
          ",",
          "]"
        )
      ),
      ",",
      "}"
    ),
    Nested(
      "\"person2\": {",
      Iterator(
        Literal("\"name\": \"Bob\""),
        Nested(
          "\"favoriteColors\": [",
          Iterator(
            Literal("\"cyan\""),
            Literal("\"magenta\""),
            Literal("\"yellow\""),
            Literal("\"black\"")
          ),
          ",",
          "]"
        )
      ),
      ",",
      "}"
    )
  ), 
  ",",  
  "}"
)
```


Again, this would typically be constructed from your JSON data structure
programmatically, and because `Nested`'s `children` is an `Iterator` we do not need
to materialize the entire `Tree` in memory at the same time.

Now that we have an iterator-based `Tree` representation, let's change the
signature of our pretty-printing function slightly to take `Tree`s instead of
`T`s:

```scala
def prettyprint(t: Tree, maxWidth: Int, indent: Int): Iterator[String]
```

We leave out the code to go from `JSON => Tree`, or from `Python Syntax Tree =>
Tree`, since that would depend on the exact API of the data structure you are
trying to pretty-print. For now, we will simple assume that such `* => Tree`
functions exist.

### The Implementation

The basic approach we will take with `prettyprint` is:

- Recurse over the `Tree`, keeping track of the current left offset at every
  node

- For each node, return a `multiLine: Boolean` of whether the current node's
  pretty-printing is multiple lines long, and an `chunks: Iterator[String]` of
  the chunks of pretty-printed output

- For `Literal` nodes, this is trivial: if the body contains a newline, it is
  multiple lines long, and the chunks is an iterator whose sole contents is the
  `body`

- For `Nested` nodes, this is more involved: to decide whether something is
  `multiLine` or not, we buffer up the pretty-printed chunks of its children and
  use those chunks to decide.

    - If we exhaust all the children, then return `multiLine = false` and an
      iterator over all the buffered chunks

    - If we fail to exhaust the children, either due to a child returning
      `multiLine = true` or due to hitting the `maxWidth` limit, return
      `multiLine = true` and a combined iterator of the buffered chunks and the
      iterator of remaining not-yet-buffered chunks

In code, this looks like:

```scala
import collection.mutable.Buffer
def prettyprint(t: Tree, maxWidth: Int, indent: Int): Iterator[String] = {
  def recurse(current: Tree, leftOffset: Int, enclosingSepWidth: Int): (Boolean, Iterator[String]) = {
    current match{
      case Literal(body) =>
        val multiLine = body.contains('\n')
        val chunks = Iterator(body)
        (multiLine, chunks)

      case Nested(prefix, children, sep, suffix) =>
        var usedWidth = leftOffset + prefix.length + suffix.length + enclosingSepWidth
        var multiLine = usedWidth > maxWidth
        val allChildChunks = Buffer[Iterator[String]]()

        // Prepare all child iterators, but do not actually consume them

        for(child <- children){
          val (childMultiLine, childChunks) = recurse(
            child,
            leftOffset + indent,
            if (children.hasNext) sep.trim.length else 0
          )
          if (childMultiLine) multiLine = true
          allChildChunks += childChunks
        }
        val bufferedChunks = Buffer[Buffer[String]]()

        val outChunkIterator = allChildChunks.iterator

        var remainingIterator: Iterator[String] = Iterator.empty

        // Buffer child node chunks, until they run out or we become multiline
        while(outChunkIterator.hasNext && !multiLine){
          bufferedChunks.append(Buffer())
          val childIterator = outChunkIterator.next()

          if (outChunkIterator.hasNext) usedWidth += sep.length

          while (childIterator.hasNext && !multiLine){
            val chunk = childIterator.next()
            bufferedChunks.last.append(chunk)
            usedWidth += chunk.length
            if (usedWidth > maxWidth) {
              remainingIterator = childIterator
              multiLine = true
            }
          }
        }

        def joinIterators(separated: Iterator[TraversableOnce[String]],
                          sepChunks: Seq[String]) = {
          separated.flatMap(sepChunks ++ _).drop(1)
        }

        val middleChunks =
          if (!multiLine) {
            // If not multiline, just join all child chunks by the separator
            joinIterators(bufferedChunks.iterator, Seq(sep))
          } else{
            // If multiline, piece back together the last half-consumed iterator
            // of the last child we half-buffered before we stopped buffering.
            val middleChildChunks = bufferedChunks.lastOption.map(_.iterator ++ remainingIterator)

            // Splice it in between the chunks of the fully-buffered children
            // and the not-at-all buffered children, joined with separators
            joinIterators(
              separated = bufferedChunks.dropRight(1).iterator ++
                          middleChildChunks ++
                          outChunkIterator,
              sepChunks = Seq(sep.trim, "\n", " " * (leftOffset + indent))
            ) ++ Iterator("\n", " " * leftOffset)
          }

        val chunks = Iterator(prefix) ++ middleChunks ++ Iterator(suffix)
        (multiLine, chunks)
    }
  }

  val (_, chunks) = recurse(t, 0, 0)
  chunks
}
```

There's a bit of messiness in keeping track of the `usedWidth`, joining child
iterators by the separator, and mangling the half-buffered-half-not-buffered
`middleChildChunks`, but otherwise it should be relatively clear what this code
is doing.

We can run this on the example JSON and Python `Tree`s above:

```scala
var last = ""
for(i <- 0 until 200){
  val current = prettyprint(json, maxWidth = i, indent = 4).mkString
  if (current != last){
    println("width: " + i)
    println(current)
    last = current
  }
}
for(i <- 0 until 100){
  val current = prettyprint(python, maxWidth = i, indent = 4).mkString
  if (current != last){
    println("width: " + i)
    println(current)
    last = current
  }
}
```

Here's the output for pretty-printing the Python source code:

```python
width: 0
model = Sequential(
    Dense(
        512,
        activation=relu
    ),
    Dense(
        100,
        activation=softmax
    )
)
width: 32
model = Sequential(
    Dense(512, activation=relu),
    Dense(
        100,
        activation=softmax
    )
)
width: 34
model = Sequential(
    Dense(512, activation=relu),
    Dense(100, activation=softmax)
)
width: 79
model = Sequential(Dense(512, activation=relu), Dense(100, activation=softmax))
```

Here, we can see every width at which the pretty-printing changes:

- At width 0, the printer tries to flatten it out as much as possible (though
  obviously it is uable to reach an actual width of 0)
- At width 32, the first `Dense` call can be one-lined
- At width 34, the first `Dense` call can be one-lined
- At width 79, the entire statement can be put on one line.

We can also see an identical sort of progression in the pretty-printed JSON,
with it starting off totally vertically expanded but taking advantage of the
horizontal space to one-line parts of the JSON as we provide it a wider and
wider acceptable width:

```json
width: 0
{
    "person1": {
        "name": "Alive",
        "favoriteColors": [
            red,
            green,
            blue
        ]
    },
    "person2": {
        "name": "Bob",
        "favoriteColors": [
            cyan,
            magenta,
            yellow,
            black
        ]
    }
}
width: 44
{
    "person1": {
        "name": "Alive",
        "favoriteColors": [red, green, blue]
    },
    "person2": {
        "name": "Bob",
        "favoriteColors": [
            cyan,
            magenta,
            yellow,
            black
        ]
    }
}
width: 56
{
    "person1": {
        "name": "Alive",
        "favoriteColors": [red, green, blue]
    },
    "person2": {
        "name": "Bob",
        "favoriteColors": [cyan, magenta, yellow, black]
    }
}
width: 71
{
    "person1": {"name": "Alive", "favoriteColors": [red, green, blue]},
    "person2": {
        "name": "Bob",
        "favoriteColors": [cyan, magenta, yellow, black]
    }
}
width: 80
{
    "person1": {"name": "Alive", "favoriteColors": [red, green, blue]},
    "person2": {"name": "Bob", "favoriteColors": [cyan, magenta, yellow, black]}
}
width: 146
{"person1": {"name": "Alive", "favoriteColors": [red, green, blue]}, "person2": {"name": "Bob", "favoriteColors": [cyan, magenta, yellow, black]}}
```

You can copy-paste the code snippets above into any Scala program, or
[Scala REPL](http://ammonite.io/), and should see the output as shown above.

## Analysis

Earlier, we claimed the following properties of our pretty-printing algorithm:

- Peak heap usage linear in the width of the output window
- The ability to start and stop the pretty-printing to print any portion of it,
  with total runtime linear in the portion of the structure printed.

Let us look at these in turn.

### Heap Usage

Our `recurse` function walks over the `Tree` structure in order to return the
pretty-printed `Iterator[String]`. One thing of note is that the `Tree` nodes
are "lazy": `Nested` contains an iterator of children, not a concrete array of
children, and so at no point is the entire tree materialized at the same time.

At any point in time, there are a number of `recurse` calls on the stack linear
in the depth of the tree we're printing (if it's roughly balanced, that means
roughly `O(log n)` the total tree size). Within each of those `recurse` calls,
we buffer up some number of chunks: however, the total number of buffered chunks
by all calls in the call-stack cannot exceed the `maxWidth` value, since each
call subtracts the width of it's prefix whenever it recurses into a child.

Note that *siblings* in the `Tree` each have separate buffers, which each can be
up to `maxWidth` in size. However, as siblings they are never active on the call
stack at the same time: the first sibling's returned `Iterator[String]` must be
exhausted before the second sibling starts evaluating, so the peak heap usage is
still limited by `maxWidth`.

Strictly speaking, the total heap usage is `O(max-width + tree-depth +
biggest-literal)`. This is much better than algorithms that involve
materializing the entire `O(size-of-tree)` data structure in memory to
manipulate.


### Start-Stop, Linear Runtime

Since the output of `prettyprint` is an `Iterator[String]`, we can choose to
consume as much or as little of the output as we want, resulting in a
corresponding amount of computation happening. We do not need to wait for the
entire pretty-printing to complete before we start receiving chunks.

Because we do not construct the entire pretty-printed output up-front, we also
do not need to pay for the portions of the `Tree` structure that we did *not*
print! This means that `prettyprint`ing the first few lines of a huge data
structure is very fast, where-as calling a `.toString` that materializes the
whole output in memory could easily take a long time.

## Conclusion

The describe `prettyprint` algorithm is currently being used in my Scala
[PPrint](http://www.lihaoyi.com/PPrint/) library, though extended in a few
incidental ways (colored output, infix nodes, etc.). It is also used to display
values in the [Ammonite Scala REPL](http://ammonite.io/), and I have used it to
prettyprint huge data-structures. Both the quality of pretty-printed output and
convenient runtime characteristics behave exactly as described.

This blog post extracts the core pretty-printing algorithm, separates it out
from all the incidental concerns and documents it it for posterity. While the
initial goal was to pretty-print sourcecode representations of Scala data
structures, this blog post demonstrates how the core pretty-printing algorithm
can be applied to any hierarchical data formats comprised of (1) terminal nodes
with a plain string representation and (2) nested nodes, with a prefix, suffix,
and a list of children with separators. This means simple formats like JSON or
the subset of Python shown are trivially supported, while the core algorithm can
be easily extended with additional `Tree` subclasses to support additional
syntax such as infix operators, bin-packed lists, or other constructs.
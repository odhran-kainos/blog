This post will dive into the runtime characteristics of the Scala collections
library, from an empirical point of view. While a lot has been written about
the Scala collections from an implementation point of view ([inheritance
hierarchies], [CanBuildFrom], [etc...])
surprisingly little has been written about
how these collections actually behave under use.

[inheritance hierarchies]: http://docs.scala-lang.org/tutorials/FAQ/collections
[CanBuildFrom]: http://docs.scala-lang.org/tutorials/FAQ/breakout.html
[etc...]: http://docs.scala-lang.org/overviews/core/architecture-of-scala-collections.html

Are `List`s faster than `Vector`s for what you're doing, or are `Vector`s
faster than `List`s? How much memory can you save by using un-boxed `Array`s
to store primitives? When you do performance tricks like pre-allocating arrays
or using a `while`-loop instead of a `foreach` call, how much does it *really*
matter? `var l: List` or `val b: mutable.Buffer`? This post will tell you the
answers.

-------------------------------------------------------------------------------

- [Memory Usage](#memory-usage)
    - [Memory use of Immutable Collections](#memory-use-of-immutable-collections)
    - [Memory use of Arrays](#memory-use-of-arrays)
- [Performance](#performance)
    - [Construction Performance](#construction-performance)
    - [Deconstruction Performance](#deconstruction-performance)
    - [Concatenation Performance](#concatenation-performance)
    - [Foreach Performance](#foreach-performance)
    - [Lookup Performance](#lookup-performance)
- [Take Aways](#take-aways)
    - [Arrays are great](#arrays-are-great)
    - [Sets and Maps are slow](#sets-and-maps-are-slow)
    - [Lists vs Vectors](#lists-vs-vectors)
    - [Lists vs mutable.Buffer](#lists-vs-mutablebuffer)
    - [Vectors are... ok](#vectors-are-ok)
- [Conclusion](#conclusion)
- [Reference](#reference)
    - [Performance Data with Standard Deviations](#performance-data-with-standard-deviations)
    - [Raw Benchmark Data](#raw-benchmark-data)
    - [Benchmark Code](#benchmark-code)

The Scala programming language has a rich set of built-in collections: `List`s,
`Vector`s, `Array`s, `Set`s, `Map`s, and so on. There is a lot of "common
knowledge" around these: that `List`s has a fast prepend but slow indexing, how
`Vector`s are a "good general purpose collection", but surprisingly little
concrete data around how these collections actually perform, in practice.

For example, how much more memory does a `Vector` take than an `Array`? How
about a `List`? How much faster is iterating using a `while`-loop instead of
a `.foreach` call? How does using `Map`s and `Set`s fit into all this?

The closest thing that's currently available describing the runtime
characteristics of Scala collections is the table below, available on
[docs.scala-lang.org](http://docs.scala-lang.org/overviews/collections/performance-characteristics.html):

| operation | head  | tail  | apply | update | prepend| append | insert |
|-----------|-------|-------|-------|--------|--------|--------|--------|
| List      | C     | C     | L     | L      | C      | L      |        |
| Stream    | C     | C     | L     | L      | C      | L      |        |
| Vector    | eC    | eC    | eC    | eC     | eC     | eC     |        |
| Stack     | C     | C     | L     | L      | C      | C      | L      |
| Queue     | aC    | aC    | L     | L      | L      | C      |        |
| Range     | C     | C     | C     |        |        |        |        |
| String    | C     | L     | C     | L      | L      | L      |        |

With the following legend

| Key   | Meaning                                                                                                                                                                                       |
|-------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| C     | The operation takes (fast) constant time.                                                                                                                                                     |
| eC    | The operation takes effectively constant time, but this might depend on some assumptions such as maximum length of a vector or distribution of hash keys.                                     |
| aC    | The operation takes amortized constant time. Some invocations of the operation might take longer, but if many operations are performed on average only constant time per operation is taken.  |
| Log   | The operation takes time proportional to the logarithm of the collection size.                                                                                                                |
| L     | The operation is linear, that is it takes time proportional to the collection size.                                                                                                           |
|       | The operation is not supported.                                                                                                                                                               |


This lacks concrete numbers and is a purely theoretical analysis. Worst, it
uses weird terminology like "Effectively Constant time, assuming maximum
length", which is confusing and doesn't match what the rest of the world thinks
when they discuss performance characteristics or asymptotic complexity (If
you're wondering why you've never heard the term "Effectively Constant Time"
before, it's because everyone else calls it "Logarithmic time", and it's the
same as the "Log" category above).

This post will thus go into detail with benchmarking both the memory and
performance characteristics of various Scala collections, from an empirical
point of view. By using runtime benchmarks rather than theoretical analysis, we
will gain an understanding of the behavior and nuances of the various Scala
collections, far more than what you'd gain from theoretical analysis or
blind-leading-the-blind in-person discussions.

## Memory Usage

The first thing we will analyze in this post is the memory usage of various
collections. This is easier to analyze than performance, as it's deterministic:
you do not require multiple benchmarks to average together to reduce
randomness. While it's not commonly done, you can relatively straightforwardly
write a program that uses reflection and the [Java Instrumentation] API to
analyze the memory usage of any object.

[Java Instrumentation]: https://docs.oracle.com/javase/8/docs/api/java/lang/instrument/package-summary.html

There are many posts online about how to to do this, for example:

- [JavaWorld: Sizeof for Java](http://www.javaworld.com/article/2077408/core-java/sizeof-for-java.html)
- [Actual Memory Consumption of Java Objects](http://www.aligelenler.com/2015/01/actual-memory-consumption-of-java.html)

And it's relatively straightforward to write your own. I ended up with the
following implementation in Scala, that crawls the object-graph using Java
reflection, and uses a `getObjectSize` method provided by a
[Java Instrumentation] agent:

```scala
package bench

import java.lang.reflect.Modifier
import java.util

import scala.collection.mutable

object DeepSize {
  private val SKIP_POOLED_OBJECTS: Boolean = false

  private def isPooled(paramObject: AnyRef): Boolean = {
    paramObject match{
      case e: java.lang.Enum[_]   => true
      case s: java.lang.String    => s eq s.intern()
      case b: java.lang.Boolean   => (b eq java.lang.Boolean.TRUE) || (b eq java.lang.Boolean.FALSE)
      case i: java.lang.Integer   => i eq java.lang.Integer.valueOf(i)
      case s: java.lang.Short     => s eq java.lang.Short.valueOf(s)
      case b: java.lang.Byte      => b eq java.lang.Byte.valueOf(b)
      case l: java.lang.Long      => l eq java.lang.Long.valueOf(l)
      case c: java.lang.Character => c eq java.lang.Character.valueOf(c)
      case _ => false
    }
  }

  /**
    * Calculates deep size
    *
    * @param obj
    * object to calculate size of
    * @return object deep size
    */
  def apply(obj: AnyRef): Long = {
    deepSizeOf(obj)
  }

  private def skipObject(obj: AnyRef, previouslyVisited: util.Map[AnyRef, AnyRef]): Boolean = {
    if (SKIP_POOLED_OBJECTS && isPooled(obj)) return true
    (obj == null) || previouslyVisited.containsKey(obj)
  }

  private def deepSizeOf(obj0: AnyRef): Long = {
    val previouslyVisited = new util.IdentityHashMap[AnyRef, AnyRef]
    val objectQueue = mutable.Queue(obj0)
    var current = 0L
    while(objectQueue.nonEmpty){
      val obj = objectQueue.dequeue()
      if (!skipObject(obj, previouslyVisited)){
        previouslyVisited.put(obj, null)
        val thisSize = agent.Agent.getObjectSize(obj)

        // get size of object + primitive variables + member pointers
        // for array header + len + if primitive total value for primitives
        obj.getClass match{
          case a if a.isArray =>
            current += thisSize
            // primitive type arrays has length two, skip them (they included in the shallow size)
            if (a.getName.length != 2) {
              val lengthOfArray = java.lang.reflect.Array.getLength(obj)
              for (i <- 0 until lengthOfArray) {
                objectQueue.enqueue(java.lang.reflect.Array.get(obj, i))
              }
            }
          case c =>
            current += thisSize
            var currentClass: Class[_] = c
            do {
              val objFields = currentClass.getDeclaredFields
              for(field <- objFields) {
                if (
                  !Modifier.isStatic(field.getModifiers) &&
                    !field.getType.isPrimitive
                ) {
                  field.setAccessible(true)
                  var tempObject: AnyRef = null
                  tempObject = field.get(obj)
                  if (tempObject != null) objectQueue.enqueue(tempObject)
                }
              }
              currentClass = currentClass.getSuperclass
            } while (currentClass != null)

        }

      }
    }
    current
  }
}
```

While this probably does not account for every edge case (32/64 bit JVMs,
compressed pointers, ...), and may have some subtle bugs, but empirically I
have run it on a bunch of objects and it seems to match up with the numbers
reported by profilers such a `JProfiler`, so I'm inclined to trust that it's
more or less correct. If you wish to try running the code yourself, or
modifying the memory measurer to see what happens, feel free to try running the
benchmark code yourself:

- [Benchmark Code](#benchmark-code)


From that point, it is relatively straightforward to run this on a bunch of
different collections of different sizes, and see what it spits out. Each
collection was filled up with `new Object`s for consistency, except for
`SortedSet` which was filled with `new java.lang.Integer(...)` objects with a
range of values so they would be sorted.

The table below is the estimated size, in bytes, of the various collections
of zero elements, one element, four elements, and powers of four all the way
up to 1,048,576 elements:

|:--------------------|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| Vector              |           56 |          216 |          264 |          456 |        1,512 |        5,448 |       21,192 |       84,312 |      334,440 |    1,353,192 |    5,412,168 |   21,648,072 |
| Array[Object]       |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,242,896 |   20,971,536 |
| List                |           16 |           56 |          176 |          656 |        2,576 |       10,256 |       40,976 |      162,776 |      647,696 |    2,621,456 |   10,485,776 |   41,943,056 |
| Stream (unforced)   |           16 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |
| Stream (forced)     |           16 |           56 |          176 |          656 |        2,576 |       10,256 |       40,976 |      162,776 |      647,696 |    2,621,456 |   10,485,776 |   41,943,056 |
| Set                 |           16 |           32 |           96 |          880 |        3,720 |       14,248 |       59,288 |      234,648 |      895,000 |    3,904,144 |   14,361,000 |   60,858,616 |
| Map                 |           16 |           56 |          176 |        1,648 |        6,800 |       26,208 |      109,112 |      428,592 |    1,674,568 |    7,055,272 |   26,947,840 |  111,209,368 |
| SortedSet           |           40 |          104 |          248 |          824 |        3,128 |       12,344 |       49,208 |      195,368 |      777,272 |    3,145,784 |   12,582,968 |   50,331,704 |
| Queue               |           40 |           80 |          200 |          680 |        2,600 |       10,280 |       41,000 |      162,800 |      647,720 |    2,621,480 |   10,485,800 |   41,943,080 |
| String              |           40 |           48 |           48 |           72 |          168 |          552 |        2,088 |        8,184 |       32,424 |      131,112 |      524,328 |    2,097,192 |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| m.Buffer            |          104 |          120 |          168 |          360 |        1,320 |        5,160 |       20,520 |       81,528 |      324,648 |    1,310,760 |    5,242,920 |   20,971,560 |
| m.Map               |          120 |          176 |          344 |        1,080 |        4,152 |       16,440 |       65,592 |      260,688 |    1,037,880 |    4,194,360 |   16,777,272 |   67,108,920 |
| m.Set               |          184 |          200 |          248 |          568 |        2,104 |        8,248 |       32,824 |      130,696 |      521,272 |    2,097,208 |    8,388,664 |   33,554,488 |
| m.Queue             |           48 |           88 |          208 |          688 |        2,608 |       10,288 |       41,008 |      162,808 |      647,728 |    2,621,488 |   10,485,808 |   41,943,088 |
| m.PriQueue          |          144 |          160 |          208 |          464 |        1,616 |        6,224 |       24,656 |       81,568 |      324,688 |    1,572,944 |    6,291,536 |   25,165,904 |
| m.Stack             |           32 |           72 |          192 |          672 |        2,592 |       10,272 |       40,992 |      162,792 |      647,712 |    2,621,472 |   10,485,792 |   41,943,072 |
| m.SortedSet         |           80 |          128 |          272 |          848 |        3,152 |       12,368 |       49,232 |      195,392 |      777,296 |    3,145,808 |   12,582,992 |   50,331,728 |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| Array[Boolean]      |           16 |           24 |           24 |           32 |           80 |          272 |        1,040 |        4,088 |       16,208 |       65,552 |      262,160 |    1,048,592 |
| Array[Byte]         |           16 |           24 |           24 |           32 |           80 |          272 |        1,040 |        4,088 |       16,208 |       65,552 |      262,160 |    1,048,592 |
| Array[Short]        |           16 |           24 |           24 |           48 |          144 |          528 |        2,064 |        8,160 |       32,400 |      131,088 |      524,304 |    2,097,168 |
| Array[Int]          |           16 |           24 |           32 |           80 |          272 |        1,040 |        4,112 |       16,296 |       64,784 |      262,160 |    1,048,592 |    4,194,320 |
| Array[Long]         |           16 |           24 |           48 |          144 |          528 |        2,064 |        8,208 |       32,568 |      129,552 |      524,304 |    2,097,168 |    8,388,624 |
| Boxed Array[Boolean]|           16 |           40 |           64 |          112 |          304 |        1,072 |        4,144 |       16,328 |       64,816 |      262,192 |    1,048,624 |    4,194,352 |
| Boxed Array[Byte]   |           16 |           40 |           96 |          336 |        1,296 |        5,136 |        8,208 |       20,392 |       68,880 |      266,256 |    1,052,688 |    4,198,416 |
| Boxed Array[Short]  |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,230,608 |   20,910,096 |
| Boxed Array[Int]    |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,242,896 |   20,971,536 |
| Boxed Array[Long]   |           16 |           48 |          128 |          464 |        1,808 |        7,184 |       28,688 |      113,952 |      453,392 |    1,835,024 |    7,340,048 |   29,360,144 |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| j.List              |          216 |          240 |          312 |          600 |        1,944 |        7,320 |       28,824 |      114,192 |      454,296 |    1,835,160 |    7,340,184 |   29,360,280 |
| j.Map               |          240 |          296 |          464 |        1,200 |        4,272 |       16,560 |       65,712 |      260,808 |    1,038,000 |    4,194,480 |   16,777,392 |   67,109,040 |
| j.Set               |          296 |          312 |          360 |          680 |        2,216 |        8,360 |       32,936 |      130,808 |      521,384 |    2,097,320 |    8,388,776 |   33,554,600 |

You can take your time to browse these values, yourself, but I will highlight
some of them as worth comparing and discussing.


### Memory use of Immutable Collections

|:--------------------|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| Vector              |           56 |          216 |          264 |          456 |        1,512 |        5,448 |       21,192 |       84,312 |      334,440 |    1,353,192 |    5,412,168 |   21,648,072 |
| Array[Object]       |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,242,896 |   20,971,536 |
| List                |           16 |           56 |          176 |          656 |        2,576 |       10,256 |       40,976 |      162,776 |      647,696 |    2,621,456 |   10,485,776 |   41,943,056 |
| Stream (unforced)   |           16 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |
| Stream (forced)     |           16 |           56 |          176 |          656 |        2,576 |       10,256 |       40,976 |      162,776 |      647,696 |    2,621,456 |   10,485,776 |   41,943,056 |
| Set                 |           16 |           32 |           96 |          880 |        3,720 |       14,248 |       59,288 |      234,648 |      895,000 |    3,904,144 |   14,361,000 |   60,858,616 |
| Map                 |           16 |           56 |          176 |        1,648 |        6,800 |       26,208 |      109,112 |      428,592 |    1,674,568 |    7,055,272 |   26,947,840 |  111,209,368 |
| SortedSet           |           40 |          104 |          248 |          824 |        3,128 |       12,344 |       49,208 |      195,368 |      777,272 |    3,145,784 |   12,582,968 |   50,331,704 |
| Queue               |           40 |           80 |          200 |          680 |        2,600 |       10,280 |       41,000 |      162,800 |      647,720 |    2,621,480 |   10,485,800 |   41,943,080 |
| String              |           40 |           48 |           48 |           72 |          168 |          552 |        2,088 |        8,184 |       32,424 |      131,112 |      524,328 |    2,097,192 |

These are the common Scala collections. Mostly immutable, with
`java.lang.String` thrown in, `scala.Stream` included despite being mutable (e.g.
you can force it, which can cause surprising side effects) and `Array[Object]` included
because they're so common in any code running on the JVM

Points of interest:

|:--------------------|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| Vector              |           56 |          216 |          264 |          456 |        1,512 |        5,448 |       21,192 |       84,312 |      334,440 |    1,353,192 |    5,412,168 |   21,648,072 |
| Array[Object]       |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,242,896 |   20,971,536 |

Small `Vector`s have 3-5x as much memory overhead as small `Array`s: a 1-element
vector takes a 1/5th of a kilobyte of memory!

For size 16 the overhead shrinks to ~30%, and for size 1,048,576 the overhead
is down to about 5%. While the internal machinery means small `Vector`s use
memory wastefully, for larger `Vector`s the overhead compared to a (boxed)
`Array` is negligible.

|:--------------------|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| Array[Object]       |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,242,896 |   20,971,536 |
| List                |           16 |           56 |          176 |          656 |        2,576 |       10,256 |       40,976 |      162,776 |      647,696 |    2,621,456 |   10,485,776 |   41,943,056 |

`List`s take up about twice the memory of `Array[Object]`s. This is the case
all the way from small 4-item lists all the way to large 1,048,576 item lists.
This isn't surprising when you consider `List` is a linked list with a
wrapper-node for each actual element it's storing

|:--------------------|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| List                |           16 |           56 |          176 |          656 |        2,576 |       10,256 |       40,976 |      162,776 |      647,696 |    2,621,456 |   10,485,776 |   41,943,056 |
| Stream (unforced)   |           16 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |          160 |
| Stream (forced)     |           16 |           56 |          176 |          656 |        2,576 |       10,256 |       40,976 |      162,776 |      647,696 |    2,621,456 |   10,485,776 |   41,943,056 |

`Stream (forced)` takes up as much memory as a `List`, while a
`Stream (unforced)` takes up hardly anything since it hasn't been populated
yet.


|:--------------------|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| Array[Object]       |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,242,896 |   20,971,536 |
| Set                 |           16 |           32 |           96 |          880 |        3,720 |       14,248 |       59,288 |      234,648 |      895,000 |    3,904,144 |   14,361,000 |   60,858,616 |
| Map                 |           16 |           56 |          176 |        1,648 |        6,800 |       26,208 |      109,112 |      428,592 |    1,674,568 |    7,055,272 |   26,947,840 |  111,209,368 |

Tiny `Set`s take up as much memory as arrays, while large `Set`s take up
three times as much memory as `Array`s. This makes sense when you consider
small `Set`s are specialized as single-objects containing all the elements,
while larger `Set`s are stored as trees. It's slightly surprising to me that
the overhead was so much: I was expecting somewhere between 50% and 100%
overhead for the internal `Set` machinery, rather than the 200% we measured.

The same applies to small `Map`s and large `Map`s, except small ones start
off taking 2x as much memory as `Array`s while large ones take up 6x as much.
These are also specialized for small collections.

|:--------------------|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| Array[Object]       |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,242,896 |   20,971,536 |
| String              |           40 |           48 |           48 |           72 |          168 |          552 |        2,088 |        8,184 |       32,424 |      131,112 |      524,328 |    2,097,192 |

`String`s store 2-byte `Char`s rather than 4-byte pointers to 16-byte
objects, and so it's not surprising they take 10x less memory to store than
`Array[Object]`s of the equivalent size

### Memory use of Arrays


|:--------------------|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|-------------:|
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| Array[Object]       |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,242,896 |   20,971,536 |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| **Size**            |        **0** |        **1** |        **4** |       **16** |       **64** |      **256** |    **1,024** |    **4,069** |   **16,192** |   **65,536** |  **262,144** |**1,048,576** |
|                     |              |              |              |              |              |              |              |              |              |              |              |              |
| Array[Boolean]      |           16 |           24 |           24 |           32 |           80 |          272 |        1,040 |        4,088 |       16,208 |       65,552 |      262,160 |    1,048,592 |
| Array[Byte]         |           16 |           24 |           24 |           32 |           80 |          272 |        1,040 |        4,088 |       16,208 |       65,552 |      262,160 |    1,048,592 |
| Array[Short]        |           16 |           24 |           24 |           48 |          144 |          528 |        2,064 |        8,160 |       32,400 |      131,088 |      524,304 |    2,097,168 |
| Array[Int]          |           16 |           24 |           32 |           80 |          272 |        1,040 |        4,112 |       16,296 |       64,784 |      262,160 |    1,048,592 |    4,194,320 |
| Array[Long]         |           16 |           24 |           48 |          144 |          528 |        2,064 |        8,208 |       32,568 |      129,552 |      524,304 |    2,097,168 |    8,388,624 |
| Boxed Array[Boolean]|           16 |           40 |           64 |          112 |          304 |        1,072 |        4,144 |       16,328 |       64,816 |      262,192 |    1,048,624 |    4,194,352 |
| Boxed Array[Byte]   |           16 |           40 |           96 |          336 |        1,296 |        5,136 |        8,208 |       20,392 |       68,880 |      266,256 |    1,052,688 |    4,198,416 |
| Boxed Array[Short]  |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,230,608 |   20,910,096 |
| Boxed Array[Int]    |           16 |           40 |           96 |          336 |        1,296 |        5,136 |       20,496 |       81,400 |      323,856 |    1,310,736 |    5,242,896 |   20,971,536 |
| Boxed Array[Long]   |           16 |           48 |          128 |          464 |        1,808 |        7,184 |       28,688 |      113,952 |      453,392 |    1,835,024 |    7,340,048 |   29,360,144 |

The memory usage of the various unboxed `Array`s of primitives is an order of
magnitude less than that of the boxed `Array[Object]`. This makes sense, as
a `Array[Object]` needs to store a 4-byte pointer to each entry, which itself
is an object with a 16-byte object header ([8 bytes for mark word, 8 bytes for
class-pointer](http://stackoverflow.com/questions/26357186/what-is-in-java-object-header)).
In comparison, a `Byte`, `Short`, `Int` or `Long` takes up 1, 2, 4, or 8 bytes
respectively. You see this exact ratio in the memory footprint of these arrays,
with a few bytes of overhead for the object header of the array.

Notable, `Array[Boolean]`s take up as much memory as `Array[Byte]`s. While they
could theoretically be stored as packed bit-fields with one boolean per bit,
they are not. If you want more efficient storage of flags, you should use some
kind of [bit set](https://docs.oracle.com/javase/7/docs/api/java/util/BitSet.html)
that *would* store your booleans as one boolean per bit.

The behavior of boxed arrays is interesting. Naively, you may think that a
boxed array of primitives has to take up the same memory footprint of an
`Array[Object]`s. After all, a boxed `Int` or `Byte` ends up being a
`java.lang.Integer` or `java.lang.Byte` that is a subclass of `Object`.
However, the small values for these constants are interned, so there are only
two boxed `Boolean`s and 256 boxed `Byte`s that are shared throughout the
program. Thus, the Boxed `Array[Byte]` ends up taking only as much memory as a
`Array[Int]`, as it's filled with 4-byte pointers to shared objects.

Boxed `Int`s and `Long`s are only interned for small values, and not for most
values within their range. Thus those end up taking as much (or more) memory
than the same-size `Array[Object]`.

--------------------------------------------------------------------------------

You can probably find more interesting comparisons in the raw table above, but
this should give you a feel for how "large" various data-structures are. While
it's true that today's computers have "lots of memory", it's still useful to
know how much space things take:

- Using more efficient data-structures could mean reducing memory usage, and
  saving money by using smaller machines on [Amazon EC2](https://aws.amazon.com/ec2/pricing/)
  or other hosting providers. For example, at-time-of-writing the only
  difference between an `m3-large` EC2 machine and an `r3-large` is that the
  latter has twice as much memory, and ends up costing about 1/3 more per
  hour. Dollars and cents that add up.

- Using less memory per-element means you can store more elements in memory
  rather than putting them in a database or on-disk, and working with in-memory
  data is orders of magnitude faster than constantly reading files or making
  database calls.

Even if you ignore memory usage at first and come back and optimize it later,
when you finally do come back you will still want to know what the relative
trade-offs are between the memory overhead of different collections. And
hopefully this section will help you make your decisions about how to make your
code more efficient.

## Performance

The next thing we will look at is how long it takes to perform common
operations using various collections. While memory footprint is something
you can analyze statically (and shouldn't change for a given object) the
runtime performance tends to be noisy and random: whether the JIT compiler
has kicked in, whether a garbage-collection is happening, etc.

However, even if we can't get exact numbers, we can definitely get numbers
close enough to be interesting. Below, are rough benchmarks run for various
"representative" collections operations:

- Each benchmark was run 7 times for 2 seconds at a time

- Each run was interleaved with every other benchmark: e.g. every benchmark
  is run through once, then every benchmark run through again, etc. 7 times.

- After that, the five middle-values for each benchmark were taken (the outer
  values discarded as potential outliers) and averaged together to provide a
  mean and standard deviation

While it's not super precise, it's good enough for the purposes of this
benchmark, and the [Benchmark Code](#benchmark-code) already takes 4-and-a-half
hours to run so I'm not inclined to try to make it much more rigorous.

The individual benchmarks are:

- **Construct**: building up a data structure of size n, one element at time, from
  the empty data structure (`Nil`, `Vector.empty`, etc.). Using
  `::` for `List`s, `:+` for `Vector`s, `+` for `Set`s, `.append` for
  `mutable.Buffer`, etc.. For `Array`s the benchmark is run both using `:+`,
  and by pre-allocating the array to the correct size.

- **Concat**: taking two collections of size n, and concatenating them. `++` for
  immutable collections, `.appendAll` or `++=` for the mutable collections to
  combine them "in place"

- **Deconstruct**: starting with a data structure of size n, removing elements one
  at a time, until it is empty

- **Foreach**: time taken to iterate over the elements of a collection

- **Lookup**: time taken to look up *every* element of the collection, using
  the `collection(i)` syntax. Note that this isn't looking up a single element,
  but *every* element, one at a time in a while-loop.

The **Foreach** and **Lookup** benchmarks were run 10 or 100x longer than the
others, and the total time subsequently divided. This was in order to try and
amplify the relatively short runtimes, so that the difference in time taken
could be seen above the random noise and variation in runtimes.

The summarized data is shown below. You can browse this data yourself, raw:
each benchmark (lookup, concat, ...) has a section of the table, where each row
is one collection's benchmarks for that operation (some collections have more
than one benchmark) and each column is the mean time taken to perform the
benchmark, as described above.

Note that the standard deviation is *not* shown here, for conciseness, though
if you want to see it you can skip to the
[Performance Data with Standard Deviations](performance-data-with-standard-deviations)
section below.

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array-prealloc  |             17 |             10 |             14 |             41 |            186 |            710 |          2,710 |         11,000 |         45,100 |        183,000 |        730,000 |      3,100,000 |
| Array:+         |              2 |             12 |             58 |            270 |          1,460 |         19,800 |        260,000 |      3,170,000 |     60,000,000 |  1,020,000,000 |                |                |
| List::          |              1 |              4 |             12 |             69 |            301 |          1,220 |          4,900 |         19,800 |         79,000 |        329,000 |      1,510,000 |      9,100,000 |
| Vector:+        |              2 |             15 |             99 |            410 |          1,730 |          7,000 |         28,600 |        324,000 |      1,498,000 |      7,140,000 |     31,700,000 |    131,000,000 |
| Set+            |              1 |             12 |             58 |          1,860 |          8,530 |         37,400 |        166,000 |        783,000 |      3,600,000 |     18,100,000 |     94,000,000 |    473,000,000 |
| Map+            |              1 |              6 |             95 |          2,100 |          9,010 |         38,900 |        171,000 |        810,000 |      3,710,000 |     18,400,000 |     96,000,000 |    499,000,000 |
| Array.toSet     |             73 |             75 |            187 |          2,140 |          9,220 |         40,000 |        174,000 |        833,000 |      3,800,000 |     19,300,000 |    101,000,000 |    506,000,000 |
| Array.toMap     |             21 |             31 |            104 |          2,100 |          9,200 |         39,500 |        173,000 |        820,000 |      3,790,000 |     19,500,000 |    104,000,000 |    540,000,000 |
| Array.toVector  |             95 |            109 |            143 |            287 |            903 |          3,310 |         12,850 |         51,100 |        203,800 |        821,000 |      3,270,000 |     13,300,000 |
| m.Buffer        |             19 |             30 |             58 |            174 |            691 |          2,690 |         10,840 |         43,000 |        169,800 |        687,000 |      2,770,000 |     11,790,000 |
| m.Map.put       |              6 |             79 |            297 |          1,420 |          6,200 |         25,500 |        103,000 |        414,000 |      1,820,000 |      8,100,000 |     57,000,000 |    348,000,000 |
| m.Set.add       |             13 |             76 |            276 |          1,430 |          6,700 |         27,900 |        113,000 |        455,000 |      1,840,000 |      7,900,000 |     39,000,000 |    267,000,000 |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| **deconstruct** |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array.tail      |              7 |             26 |            114 |            582 |          4,517 |         55,500 |        821,000 |     12,140,000 |    188,000,000 |  3,100,000,000 |                |                |
| List.tail       |              2 |              2 |              7 |             21 |            100 |            420 |          2,100 |         10,000 |         35,000 |        120,000 |        540,000 |      1,500,000 |
| Vector.tail     |              3 |              6 |             90 |            425 |          1,970 |         11,800 |         58,400 |        500,000 |      2,390,000 |     11,000,000 |     50,200,000 |    211,000,000 |
| Vector.init     |              2 |              5 |            103 |            483 |          2,490 |         12,800 |         64,000 |        543,000 |      2,470,000 |     11,900,000 |     52,600,000 |    218,000,000 |
| Set.-           |              8 |             30 |            162 |          1,480 |          7,700 |         34,200 |        164,000 |        770,000 |      3,660,000 |     20,300,000 |     94,000,000 |    420,000,000 |
| Map.-           |             12 |             52 |            201 |          1,430 |          7,660 |         34,900 |        169,000 |        810,000 |      3,990,000 |     24,000,000 |    103,000,000 |    470,000,000 |
| m.Buffer        |              6 |              8 |             14 |             43 |            166 |            630 |          2,510 |         10,000 |         40,600 |        167,000 |        660,000 |      2,490,000 |
| m.Set           |              5 |             28 |            130 |            671 |          4,900 |         54,000 |        770,000 |     11,990,000 |    189,000,000 |  3,040,000,000 |                |                |
| m.Map           |              7 |             44 |            172 |            670 |          3,650 |         26,400 |        282,000 |      3,970,000 |     62,600,000 |  1,000,000,000 |                |                |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| **concat**      |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array++         |             89 |             83 |             85 |             91 |            144 |            330 |            970 |          4,100 |         17,000 |         70,000 |        380,000 |      1,700,000 |
| arraycopy       |             23 |             18 |             20 |             27 |             48 |            280 |          1,000 |          4,000 |         16,000 |         65,000 |        360,000 |      1,400,000 |
| List            |              7 |             81 |            162 |            434 |          1,490 |          5,790 |         23,200 |         92,500 |        370,000 |      1,510,000 |      6,300,000 |     30,000,000 |
| Vector          |              5 |             48 |            188 |            327 |            940 |          3,240 |         12,700 |         52,000 |        210,000 |        810,000 |      3,370,000 |     14,500,000 |
| Set             |             91 |             95 |            877 |          1,130 |          5,900 |         26,900 |        149,000 |        680,000 |      3,600,000 |     23,000,000 |    100,000,000 |    280,000,000 |
| Map             |             54 |             53 |            967 |          1,480 |          6,900 |         31,500 |        166,000 |        760,000 |      4,100,000 |     27,000,000 |    118,000,000 |    450,000,000 |
| m.Buffer        |             11 |             32 |             32 |             38 |             70 |            250 |            700 |          3,900 |         20,000 |         40,000 |        400,000 |      1,500,000 |
| m.Set           |             58 |             81 |            142 |          1,080 |          4,200 |         16,000 |         69,000 |        263,000 |      1,160,000 |      6,300,000 |     43,000,000 |    310,000,000 |
| m.Map           |             47 |             69 |            181 |            990 |          3,700 |         15,000 |         62,000 |        290,000 |      1,500,000 |     16,000,000 |    103,000,000 |    493,000,000 |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| **foreach**     |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array           |              2 |              5 |             15 |             57 |            230 |            900 |          3,580 |         14,200 |         55,600 |        228,000 |        910,000 |      3,610,000 |
| Array-while     |              0 |              1 |              0 |              1 |              0 |              0 |              0 |             -4 |             10 |             70 |              0 |            500 |
| List            |              0 |              3 |             13 |             50 |            209 |            800 |          3,500 |         14,100 |         55,000 |        231,000 |        920,000 |      3,800,000 |
| List-while      |              4 |              5 |             13 |             49 |            211 |            812 |          3,400 |         14,200 |         57,000 |        226,000 |        930,000 |      3,700,000 |
| Vector          |             15 |             19 |             30 |             74 |            268 |          1,000 |          3,960 |         16,200 |         62,000 |        256,000 |      1,030,000 |      4,300,000 |
| Set             |              4 |              5 |             10 |             99 |            420 |          1,560 |         10,200 |         51,000 |        217,000 |      2,200,000 |     10,800,000 |     48,600,000 |
| Map             |             19 |              7 |             20 |            140 |            610 |          2,500 |         13,900 |         72,800 |        360,000 |      3,700,000 |     20,700,000 |     75,000,000 |
| m.Buffer        |              0 |              1 |              1 |              1 |              1 |              0 |              1 |              2 |             -1 |            -10 |              0 |           -200 |
| m.Set           |             19 |             26 |             50 |            130 |            508 |          2,190 |         11,900 |         56,600 |        235,000 |        940,000 |      3,800,000 |     14,700,000 |
| m.Map           |              8 |             16 |             48 |            146 |            528 |          2,210 |         10,300 |         54,100 |        255,000 |      1,140,000 |      6,800,000 |     30,000,000 |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| **lookup**      |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array           |              0 |              1 |              1 |              1 |              0 |              0 |              1 |             -1 |              4 |              0 |            100 |           -200 |
| List            |              0 |              1 |              8 |            103 |          2,390 |         47,200 |        870,000 |     16,900,000 |                |                |                |                |
| Vector          |              0 |              1 |              5 |             17 |            104 |            440 |          1,780 |          8,940 |         38,000 |        198,000 |        930,000 |      4,260,000 |
| Set             |              0 |             18 |             81 |            507 |          1,980 |          7,800 |         39,800 |        203,000 |      1,040,000 |      8,300,000 |                |                |
| Map             |              0 |             12 |             97 |            578 |          2,250 |          9,400 |         46,000 |        233,000 |      1,150,000 |     11,400,000 |                |                |
| m.Buffer        |              0 |              1 |              1 |              1 |              1 |              1 |              1 |              0 |              6 |            -10 |              0 |              0 |
| m.Set           |              0 |              5 |             22 |             97 |            410 |          1,690 |          7,100 |         31,300 |        148,000 |        690,000 |      4,800,000 |                |
| m.Map           |              0 |              6 |             25 |            112 |            454 |          1,910 |          9,400 |         52,500 |        243,000 |      1,760,000 |      9,900,000 |                |



The raw data with standard deviations is available below:

- [Performance Data with Standard Deviations](#performance-data-with-standard-deviation)

As is the actual time taken for each benchmark run:

- [Raw Benchmark Data](#raw-benchmark-data)

As well as the code for the benchmark, if you want to try running
and reproducing these results yourself.

- [Benchmark Code](#benchmark-code)

Take a look at these if you want to dig a bit deeper into these results. For
this post, I will now discuss some of the more interesting insights I noticed
when going through this data myself.

### Construction Performance

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array-prealloc  |             17 |             10 |             14 |             41 |            186 |            710 |          2,710 |         11,000 |         45,100 |        183,000 |        730,000 |      3,100,000 |
| Array:+         |              2 |             12 |             58 |            270 |          1,460 |         19,800 |        260,000 |      3,170,000 |     60,000,000 |  1,020,000,000 |                |                |
| List::          |              1 |              4 |             12 |             69 |            301 |          1,220 |          4,900 |         19,800 |         79,000 |        329,000 |      1,510,000 |      9,100,000 |
| Vector:+        |              2 |             15 |             99 |            410 |          1,730 |          7,000 |         28,600 |        324,000 |      1,498,000 |      7,140,000 |     31,700,000 |    131,000,000 |
| Set+            |              1 |             12 |             58 |          1,860 |          8,530 |         37,400 |        166,000 |        783,000 |      3,600,000 |     18,100,000 |     94,000,000 |    473,000,000 |
| Map+            |              1 |              6 |             95 |          2,100 |          9,010 |         38,900 |        171,000 |        810,000 |      3,710,000 |     18,400,000 |     96,000,000 |    499,000,000 |
| Array.toSet     |             73 |             75 |            187 |          2,140 |          9,220 |         40,000 |        174,000 |        833,000 |      3,800,000 |     19,300,000 |    101,000,000 |    506,000,000 |
| Array.toMap     |             21 |             31 |            104 |          2,100 |          9,200 |         39,500 |        173,000 |        820,000 |      3,790,000 |     19,500,000 |    104,000,000 |    540,000,000 |
| Array.toVector  |             95 |            109 |            143 |            287 |            903 |          3,310 |         12,850 |         51,100 |        203,800 |        821,000 |      3,270,000 |     13,300,000 |
| m.Buffer        |             19 |             30 |             58 |            174 |            691 |          2,690 |         10,840 |         43,000 |        169,800 |        687,000 |      2,770,000 |     11,790,000 |
| m.Map.put       |              6 |             79 |            297 |          1,420 |          6,200 |         25,500 |        103,000 |        414,000 |      1,820,000 |      8,100,000 |     57,000,000 |    348,000,000 |
| m.Set.add       |             13 |             76 |            276 |          1,430 |          6,700 |         27,900 |        113,000 |        455,000 |      1,840,000 |      7,900,000 |     39,000,000 |    267,000,000 |

This is the time taken to construct a data-structure one element at a time:
using `::` for `List`s, `:+` for `Vector`s, `.add` or `.append` or `.put`
for the mutable collections. `Array`s have two benchmarks: one using `:+`,
and one pre-allocating the array via `new Array[Object](n)` and then filling
it in with a while-loop.

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| List::          |              1 |              4 |             12 |             69 |            301 |          1,220 |          4,900 |         19,800 |         79,000 |        329,000 |      1,510,000 |      9,100,000 |
| Vector:+        |              2 |             15 |             99 |            410 |          1,730 |          7,000 |         28,600 |        324,000 |      1,498,000 |      7,140,000 |     31,700,000 |    131,000,000 |

It turns out that constructing a `Vector` one-element-at-a-time is 5-15x
slower than constructing a `List`, depending on the size.

This is perhaps not surprising - adding things to a linked list is about as
simple as you can get - but the magnitude of the difference surprised me.
If you are constructing things one by one and iterating over them, using `List`
would be faster than using a `Vector`. nevertheless, it's slightly surprising
to me how large the multiplier is. If building up a `Vector` turns out to be
bottle-necking your code, it's probably worth considering replacing it with
a `List` or `Buffer`, as shown below.

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| List::          |              1 |              4 |             12 |             69 |            301 |          1,220 |          4,900 |         19,800 |         79,000 |        329,000 |      1,510,000 |      9,100,000 |
| m.Buffer        |             19 |             30 |             58 |            174 |            691 |          2,690 |         10,840 |         43,000 |        169,800 |        687,000 |      2,770,000 |     11,790,000 |


Constructing a `mutable.Buffer` with `.append` seems to be about 2-3x as slow
as constructing a `List` with `::`, though with large lists the difference seems
to drop down to a 1.5x difference. I find this a bit surprising, but what it means
is that if you have an accumulator that needs to be fast, you using a `List` is 
possibly the better option.

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array-prealloc  |             17 |             10 |             14 |             41 |            186 |            710 |          2,710 |         11,000 |         45,100 |        183,000 |        730,000 |      3,100,000 |
| List::          |              1 |              4 |             12 |             69 |            301 |          1,220 |          4,900 |         19,800 |         79,000 |        329,000 |      1,510,000 |      9,100,000 |
| m.Buffer        |             19 |             30 |             58 |            174 |            691 |          2,690 |         10,840 |         43,000 |        169,800 |        687,000 |      2,770,000 |     11,790,000 |


The fastest is pre-allocating an `Array` of the right size and filling that in;
that is about 4x faster than constructing a `List`, 5x faster than constructing
a `mutable.Buffer`, and 15x faster than constructing a `Vector`.

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array-prealloc  |             17 |             10 |             14 |             41 |            186 |            710 |          2,710 |         11,000 |         45,100 |        183,000 |        730,000 |      3,100,000 |
| Array:+         |              2 |             12 |             58 |            270 |          1,460 |         19,800 |        260,000 |      3,170,000 |     60,000,000 |  1,020,000,000 |                |                |


Constructing an array bit-by-bit using `:+` is quadratic time, as it copies the
entire array each time. This shows in the benchmarks: while for small arrays
it's fine, it very quickly grows large and becomes infeasible for arrays even
a few tens of thousands of elements in size.

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array-prealloc  |             17 |             10 |             14 |             41 |            186 |            710 |          2,710 |         11,000 |         45,100 |        183,000 |        730,000 |      3,100,000 |
| Set+            |              1 |             12 |             58 |          1,860 |          8,530 |         37,400 |        166,000 |        783,000 |      3,600,000 |     18,100,000 |     94,000,000 |    473,000,000 |
| Map+            |              1 |              6 |             95 |          2,100 |          9,010 |         38,900 |        171,000 |        810,000 |      3,710,000 |     18,400,000 |     96,000,000 |    499,000,000 |
| Array.toSet     |             73 |             75 |            187 |          2,140 |          9,220 |         40,000 |        174,000 |        833,000 |      3,800,000 |     19,300,000 |    101,000,000 |    506,000,000 |
| Array.toMap     |             21 |             31 |            104 |          2,100 |          9,200 |         39,500 |        173,000 |        820,000 |      3,790,000 |     19,500,000 |    104,000,000 |    540,000,000 |

Constructing `Set`s and `Map`s bit by bit is really slow: 30x slower
than constructing a `List`, 150x slower than pre-allocating and filling an
`Array` of the same size. This is presumably due to `Set`s and `Map`s needing
to do constant hashing/equality checks in order to maintain uniqueness.

While it's not surprising that `Set`s and `Map`s are slower, just *how much*
slower is surprising. It means that if you want some kind of accumulator
collection to put stuff into, you should not use a `Set` or `Map` unless you
*really* need the uniqueness guarantees they provide. Otherwise, chucking
everything into a `List` or `mutable.Buffer` is much faster.

Pre-allocating an array and then calling `.toSet` or `.toMap` on it isn't
faster than building up the `Set` or `Map` bit by bit using `+`. This is in
contrast to calling `.toVector`, which *is* faster than building up the
`Vector` incrementally...

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array-prealloc  |             17 |             10 |             14 |             41 |            186 |            710 |          2,710 |         11,000 |         45,100 |        183,000 |        730,000 |      3,100,000 |
| List::          |              1 |              4 |             12 |             69 |            301 |          1,220 |          4,900 |         19,800 |         79,000 |        329,000 |      1,510,000 |      9,100,000 |
| Vector:+        |              2 |             15 |             99 |            410 |          1,730 |          7,000 |         28,600 |        324,000 |      1,498,000 |      7,140,000 |     31,700,000 |    131,000,000 |
| Array.toVector  |             95 |            109 |            143 |            287 |            903 |          3,310 |         12,850 |         51,100 |        203,800 |        821,000 |      3,270,000 |     13,300,000 |


It turns out, if constructing a `Vector` by pre-allocating/filling an `Array`
of all items and then calling `.toVector` on it is 10x faster than constructing
the `Vector` element by element. While it wasn't benchmarked here, putting
everything into a `mutable.Buffer` and then calling `.toVector` is also
probably going to be much faster than building up the `Vector` incrementally.

### Deconstruction Performance

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **deconstruct** |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array.tail      |              7 |             26 |            114 |            582 |          4,517 |         55,500 |        821,000 |     12,140,000 |    188,000,000 |  3,100,000,000 |                |                |
| List.tail       |              2 |              2 |              7 |             21 |            100 |            420 |          2,100 |         10,000 |         35,000 |        120,000 |        540,000 |      1,500,000 |
| Vector.tail     |              3 |              6 |             90 |            425 |          1,970 |         11,800 |         58,400 |        500,000 |      2,390,000 |     11,000,000 |     50,200,000 |    211,000,000 |
| Vector.init     |              2 |              5 |            103 |            483 |          2,490 |         12,800 |         64,000 |        543,000 |      2,470,000 |     11,900,000 |     52,600,000 |    218,000,000 |
| Set.-           |              8 |             30 |            162 |          1,480 |          7,700 |         34,200 |        164,000 |        770,000 |      3,660,000 |     20,300,000 |     94,000,000 |    420,000,000 |
| Map.-           |             12 |             52 |            201 |          1,430 |          7,660 |         34,900 |        169,000 |        810,000 |      3,990,000 |     24,000,000 |    103,000,000 |    470,000,000 |
| m.Buffer        |              6 |              8 |             14 |             43 |            166 |            630 |          2,510 |         10,000 |         40,600 |        167,000 |        660,000 |      2,490,000 |
| m.Set           |              5 |             28 |            130 |            671 |          4,900 |         54,000 |        770,000 |     11,990,000 |    189,000,000 |  3,040,000,000 |                |                |
| m.Map           |              7 |             44 |            172 |            670 |          3,650 |         26,400 |        282,000 |      3,970,000 |     62,600,000 |  1,000,000,000 |                |                |

`mutable.Buffer` and `List` win as the fastest collections to de-construct.
That makes sense, since removing things from a `mutable.Buffer` is just
changing the `size` field, and removing the head of a `List` is just following
the `.tail` pointer. Neither needs to make changes to the data-structure.

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Vector:+        |              2 |             15 |             99 |            410 |          1,730 |          7,000 |         28,600 |        324,000 |      1,498,000 |      7,140,000 |     31,700,000 |    131,000,000 |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| **deconstruct** |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Vector.tail     |              3 |              6 |             90 |            425 |          1,970 |         11,800 |         58,400 |        500,000 |      2,390,000 |     11,000,000 |     50,200,000 |    211,000,000 |
| Vector.init     |              2 |              5 |            103 |            483 |          2,490 |         12,800 |         64,000 |        543,000 |      2,470,000 |     11,900,000 |     52,600,000 |    218,000,000 |

Deconstructing `Vector`s by `.tail` or `.init` is much slower: about 50% slower
than appending to one via `:+`.

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **deconstruct** |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| List.tail       |              2 |              2 |              7 |             21 |            100 |            420 |          2,100 |         10,000 |         35,000 |        120,000 |        540,000 |      1,500,000 |
| Vector.tail     |              3 |              6 |             90 |            425 |          1,970 |         11,800 |         58,400 |        500,000 |      2,390,000 |     11,000,000 |     50,200,000 |    211,000,000 |
| Set.-           |              8 |             30 |            162 |          1,480 |          7,700 |         34,200 |        164,000 |        770,000 |      3,660,000 |     20,300,000 |     94,000,000 |    420,000,000 |
| Map.-           |             12 |             52 |            201 |          1,430 |          7,660 |         34,900 |        169,000 |        810,000 |      3,990,000 |     24,000,000 |    103,000,000 |    470,000,000 |
| m.Set           |              5 |             28 |            130 |            671 |          4,900 |         54,000 |        770,000 |     11,990,000 |    189,000,000 |  3,040,000,000 |                |                |
| m.Map           |              7 |             44 |            172 |            670 |          3,650 |         26,400 |        282,000 |      3,970,000 |     62,600,000 |  1,000,000,000 |                |                |

Also, for some reason repeatedly removing the
`.head` from immutable `Map`s and `Set`s is also slow, though removing them
from `mutable.Map` an `mutable.Set`s is even slower. I'm not sure why this is
the case.

### Concatenation Performance

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **concat**      |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array++         |             89 |             83 |             85 |             91 |            144 |            330 |            970 |          4,100 |         17,000 |         70,000 |        380,000 |      1,700,000 |
| arraycopy       |             23 |             18 |             20 |             27 |             48 |            280 |          1,000 |          4,000 |         16,000 |         65,000 |        360,000 |      1,400,000 |
| List            |              7 |             81 |            162 |            434 |          1,490 |          5,790 |         23,200 |         92,500 |        370,000 |      1,510,000 |      6,300,000 |     30,000,000 |
| Vector          |              5 |             48 |            188 |            327 |            940 |          3,240 |         12,700 |         52,000 |        210,000 |        810,000 |      3,370,000 |     14,500,000 |
| Set             |             91 |             95 |            877 |          1,130 |          5,900 |         26,900 |        149,000 |        680,000 |      3,600,000 |     23,000,000 |    100,000,000 |    280,000,000 |
| Map             |             54 |             53 |            967 |          1,480 |          6,900 |         31,500 |        166,000 |        760,000 |      4,100,000 |     27,000,000 |    118,000,000 |    450,000,000 |
| m.Buffer        |             11 |             32 |             32 |             38 |             70 |            250 |            700 |          3,900 |         20,000 |         40,000 |        400,000 |      1,500,000 |
| m.Set           |             58 |             81 |            142 |          1,080 |          4,200 |         16,000 |         69,000 |        263,000 |      1,160,000 |      6,300,000 |     43,000,000 |    310,000,000 |
| m.Map           |             47 |             69 |            181 |            990 |          3,700 |         15,000 |         62,000 |        290,000 |      1,500,000 |     16,000,000 |    103,000,000 |    493,000,000 |

The fastest collection to concatenate are `mutable.Buffer`s,
and plain `Array`s. Both of these basically involve copying the contents to a
new array; `mutable.Buffer` keeps an internal array it would need to re-allocate to
make space to copy in the new data, while `Array` needs to copy both input
`Array`s into a new, larger `Array`. Whether you use `Array ++ Array` or
`System.arraycopy` doesn't seem to matter.

It turns out that while the
clever algorithms and structural-sharing and what not that go into Scala's
immutable `Vector`s and `Set`s make it faster to build things up incrementally
element-by-element (as seen in the
[Construction Performance](#construction-performance) benchmark), for this kind
of bulk-concatenation it's still faster just to copy everything manually into a
new array and skip all the fancy data-structure stuff.

Though `Vector` and `List` concatenation is much slower than concatenating
`mutable.Buffer`s or `Array`s, `Vector` concatenation is twice as fast
as `List` concatenation.

`Set` and `Map` again are surprisingly slow, with concatenation being 10x
slower than for `Vector`s or `List`s, and 100x slower than `Array`s or
`mutable.Buffer`s.

### Foreach Performance

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **foreach**     |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array           |              2 |              5 |             15 |             57 |            230 |            900 |          3,580 |         14,200 |         55,600 |        228,000 |        910,000 |      3,610,000 |
| Array-while     |              0 |              1 |              0 |              1 |              0 |              0 |              0 |             -4 |             10 |             70 |              0 |            500 |
| List            |              0 |              3 |             13 |             50 |            209 |            800 |          3,500 |         14,100 |         55,000 |        231,000 |        920,000 |      3,800,000 |
| List-while      |              4 |              5 |             13 |             49 |            211 |            812 |          3,400 |         14,200 |         57,000 |        226,000 |        930,000 |      3,700,000 |
| Vector          |             15 |             19 |             30 |             74 |            268 |          1,000 |          3,960 |         16,200 |         62,000 |        256,000 |      1,030,000 |      4,300,000 |
| Set             |              4 |              5 |             10 |             99 |            420 |          1,560 |         10,200 |         51,000 |        217,000 |      2,200,000 |     10,800,000 |     48,600,000 |
| Map             |             19 |              7 |             20 |            140 |            610 |          2,500 |         13,900 |         72,800 |        360,000 |      3,700,000 |     20,700,000 |     75,000,000 |
| m.Buffer        |              0 |              1 |              1 |              1 |              1 |              0 |              1 |              2 |             -1 |            -10 |              0 |           -200 |
| m.Set           |             19 |             26 |             50 |            130 |            508 |          2,190 |         11,900 |         56,600 |        235,000 |        940,000 |      3,800,000 |     14,700,000 |
| m.Map           |              8 |             16 |             48 |            146 |            528 |          2,210 |         10,300 |         54,100 |        255,000 |      1,140,000 |      6,800,000 |     30,000,000 |


`foreach`ing over most "common" collections is about equally fast; whether it's
a `List` or `Vector` or an `Array`. For that matter, iterating over a `List`
using a `while`-loop and `head`/`tail` is the same speed too, so if you've been
thinking of hand-writing the `head`/`tail` logic to iterate over a `List` for
performance, know that it probably doesn't make any difference.

On the other hand, iterating over immutable `Set`s and `Map`s is about 10-15x
slower. Mutable `Set`s and `Map`s fare better than their immutable
counterparts, at only 3-8x slower than iterating over an `Array` or `Vector`.

Iterating using an `Array` and a `while`-loop is the fastest, with the time
here basically un-measurable over the noise. For some reason, iterating over
the `mutable.Buffer` also did not give meaningful results. It's unclear to me
why this is the case.

### Lookup Performance

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **lookup**      |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array           |              0 |              1 |              1 |              1 |              0 |              0 |              1 |             -1 |              4 |              0 |            100 |           -200 |
| List            |              0 |              1 |              8 |            103 |          2,390 |         47,200 |        870,000 |     16,900,000 |                |                |                |                |
| Vector          |              0 |              1 |              5 |             17 |            104 |            440 |          1,780 |          8,940 |         38,000 |        198,000 |        930,000 |      4,260,000 |
| Set             |              0 |             18 |             81 |            507 |          1,980 |          7,800 |         39,800 |        203,000 |      1,040,000 |      8,300,000 |                |                |
| Map             |              0 |             12 |             97 |            578 |          2,250 |          9,400 |         46,000 |        233,000 |      1,150,000 |     11,400,000 |                |                |
| m.Buffer        |              0 |              1 |              1 |              1 |              1 |              1 |              1 |              0 |              6 |            -10 |              0 |              0 |
| m.Set           |              0 |              5 |             22 |             97 |            410 |          1,690 |          7,100 |         31,300 |        148,000 |        690,000 |      4,800,000 |                |
| m.Map           |              0 |              6 |             25 |            112 |            454 |          1,910 |          9,400 |         52,500 |        243,000 |      1,760,000 |      9,900,000 |                |

Here, we can see that `Array` and `mutable.Buffer` lookups are so fast as to be
basically unmeasurable above the noise. The next fastest is indexed `Vector`
lookups, which take an appreciable amount of time. e.g. looking up every item
in a million-element vector takes 4 milliseconds: this might not be noticeable
in most situations, but could definitely add up if done repeatedly, or if done
during the 16 milliseconds available in a real-time animation/game loop.

Note that this time is measuring the time taken to look up *every element
in the collection*, rather than just looking up a single element. Thus we'd
expect larger collections to take more time to complete the benchmark, even
if the time taken for each lookup is constant.

Immutable `Set`s and `Map`s take far, far longer to look up items than
`Vector`s:  Looking up something in an immutable `Set` takes 10-20x as long
as looking up something in a `Vector`, while looking things up in an immutable
`Map` takes 10-40x as long.

Notably, *mutable* `Set`s and `Map`s perform much better than the immutable
ones: mutable `Set` lookup being 4-5x slower than `Vector`, and mutable `Map`
lookup being 5-10x slower than `Vector`. In both cases, we're looking at 2-4x
faster lookups using the mutable `Map`s and Set`s over the immutable ones. This
is presumably because the mutable versions of these collections use hash-tables
rather than trees.

`List` lookups by index across the entire list is, as expected, quadratic in
time: Each individual lookup takes time linear in the size of the collection,
and there are a linear number of lookups to perform in this benchmark.
While it's reasonable up to about 16 elements, it quickly blows up after
that.

## Take Aways

By this point, we've seen a lot of numbers, and gone over some of the
non-obvious insights that we can see from the data. However, what does this
mean to a working programmer, using Scala day to day? Here are some of the
more interesting take-aways:

### Arrays are great

An un-boxed `Array`s of primitives take 1/4th to 1/5th as much memory as their
boxed counterparts, e.g. `Array[Int]` vs `Array[java.lang.Integer]`. This is
a non-trivial cost; if you're dealing with large amounts of primitive data,
keeping them in un-boxed `Array`s will save you tons of memory.

Apart from primitive `Array`s, even boxed `Array`s of objects still have some
surprisingly nice performance characteristics. Concatenating two `Array`s is
faster than concatenating any other data-structure, even immutable `List`s and
`Vector`s which are [Persistent Data Structure] and supposed to have clever
structural sharing to reduce the
need to copy-everything. This holds even for with a million elements, and is
a 10x improvement that's definitely non-trivial. There's an open issue
[SI-4442](https://issues.scala-lang.org/browse/SI-4442) for someone to fix this,
but for now this is the state of the world.

Indexing into the `Array`, and iterating over it with a `while`-loop, are also
so fast that the time taken is not measurable given these benchmarks. Even
using `:+` to build an `Array` from individual elements, ostentiably "O(n^2)"
and "slow", turns out to be faster than building a `Vector` for collections of
up to ~64 elements.

It is surprising to me how much faster `Array` concatenation is than
everything else, even "fancy" [Persistent Data Structure]s like `List` and
`Vector` with structural sharing to avoid copying the whole thing; it turns out
copying the whole thing is actually faster than trying to combine the fancy
persistent data structures! Thus, even if you have an immutable collection you
a passing around, and sometimes splitting into pieces or concatenating with
other *similarly-sized* collections, it is actually faster to use an `Array`
(perhaps boxed in a `WrappedArray` if you want it to be immutable) as long as
you avoid the pathological build-up-element-by-element use case.

[Persistent Data Structure]: https://en.wikipedia.org/wiki/Persistent_data_structure

### Sets and Maps are slow

Looking up things in an immutable `Vector` takes 1/10th to 1/20th the time
looking things up in an immutable `Set`, and 1/10th to 1/40th the time to
look things up in an immutable `Set`. Even if you convert them to mutable
data-structures for speed, there's still a large potential speedup if you can
use a `Vector` instead. Using a raw `Array` would be even faster.

It makes sense, since a `Set` or `Map` lookup involves tons of hashing and
equality checks, and even for simple keys like `new Object` (which has
identity-based hashes and identify-equality) this ends up being a significant
cost. In comparison, looking up a `Vector` is just a bit of integer math,
and looking an `Array` is a single pointer-addition and memory-dereference.

It's not just looking things up that's slow: constructing them item-by-item
is slow, removing things item-by-item is slow, concatenating them is slow. Even
operations which should not need to perform hashing/equality at all, like
iteration, is 10 times slower than iterating over a `Vector`.

Thus, while it makes sense to use `Set`s to represent collections which cannot
have duplicates, and `Map`s to hold key-value pairings, keep in mind that
they're likely sources of slowness. If your set of keys is relatively small,
and performance is an issue, you could assign integer IDs to each key
and replace `Set`s with `Vector[Boolean]`s and `Map`s with `Vector[T]`s,
looking them up by integer ID. Sometimes, even if you know the collection's
items are all unique, it may be worth giving up the automatic-enforcement that
a `Set` gives you in order to get the raw performance of an `Array` or
`Vector` or `List`.

Mutable `Set`s and `Map`s are faster and smaller: they take up 1/2
the memory, have 2-4x faster lookups, 2x faster `foreach`s,
and are 2x faster to construct element-by-element using `.add` or `.put`. Even
so, working with them is still a lot slower than working with `Array`s or
`Vector`s or `List`s.


### Lists vs Vectors

It's often a bit ambigious whether you should use a singly-linked `List` or a
tree-shaped `Vector` as the immutable collection of choice in your code.
The whole thing about "effectively constant
time" operations does nothing to resolve the ambiguity. However, given the
numbers, we can make a better judgement:

- `List`s have twice as much memory overhead as `Vector`s, the latter of which
  are comparable with raw `Array`s in overhead.

- Constructing a `List` item-by-item is 5-15x (!) faster than constructing a `Vector`
  item-by-item.

- *De*-constructing a `List` item-by-item using `.tail` is 50-60x (!) faster
  than de-constructing a `Vector` item-by-item using `.tail`

- Looking things up by index in a `Vector` works; looking things up by index
  in a `List` is O(n), and expensive for non-trivial lists.

- Iteration is about the same speed on both.

If you find yourself creating and de-constructing a collection bit by bit, and
iterating over it once in a while, using a `List` is best. However, if you want
to look up things by index, using a `Vector` is necessary. Using a `Vector` and
using `:+` to add items or `.tail` to remove items won't kill you, but it's
an order of magnitude slower than the equivalent operations on a `List`.

### Lists vs mutable.Buffer

Apart from being an immutable collection, `List`s are often used as `var`s to
act as a mutable bucket to put things. `mutable.Buffer` serves the same
purpose. Which one should you use?

It turns out, using a `List` is actually substantially faster than using
a `mutable.Buffer` if you are accumulating a collection item-by-item:
2-3x for smaller collections, 1.5-2x for larger collections. A non-trivial
difference!

Apart from performance, there are other differences between them as well:

- `mutable.Buffer` allows fast indexed lookup, while `List` doesn't

- `List` is persistent, so you can add/remove things from your copy of a `List`
  without affecting other people holding references to it. `mutable.Buffer`
  isn't, so anyone who mutates it will affect anyone else using the same buffer

- `List` is constructed "backwards": the last thing added is first in the
  iteration order. This might not be what you want, and you may find yourself
  needing to reverse a `List` before using it. `mutable.Buffer` on the other
  hand is constructed "forward", first thing added is first thing iterated over

- `mutable.Buffer` has about half the memory overhead of a `List`

For accumulating elements one at a time, `List`s are faster, and end up
having more memory overhead. But if one of 
these other factors matters to you, that factor may end up deciding on your
behalf whether to use a `List` or a `mutable.Buffer`.

### Vectors are... ok

Although `Vector` is often thought of as a good "general purpose" data
structure, it turns out they're not that great:

- Small `Vector`s, containing just a single item, have 1/5 of a kilobyte of
  overhead. While for larger `Vector`s the overhead is negligible, if you have
  lots of small collections, this could be a big source of wasted memory.

- `Vector` item-by-item construction is 5-15x slower than `List` or
  `mutable.Buffer` construction, and even 40x slower than pre-allocating an
  `Array` in the cases where that's possible.

- `Vector` indexed lookup is acceptable, but much slower than `Array` or
  `mutable.Buffer` lookup (though we didn't manage to measure how much, since
  those are too fast for these benchmarks)

- `Vector` concatenation of roughly equal sized inputs, while twice as fast
   as `List` concatenation, is 10x slower than concatenating `Array`s: this is
   despite the `Array`s needing a full copy while `Vector`s have some degree
   of structural sharing

Overall, they are an "acceptable" general purpose collection:

- On one hand, you don't see any pathological behavior, like indexed lookup in
  `List`s or item-by-item construction of `Array`s: most of the `Vector`
  benchmarks are relatively middle-of-the-pack. That means you can use them to
  do more or less whatever and be sure you performance won't blow up
  unexpectedly with O(n^2) behavior.

- On the other hand, many common operations are an order of magnitude slower
  than the "ideal" data structure: incremental building `Vector`s is 5-15x
  slower than for `List`s, indexed-lookup is much slower than for `Array`s,
  even concatenation of similarly-sized `Vector`s is 10x slower than
  concatenating `Array`s.

`Vector`s are a useful "default" data structure to reach for, but if it's
at all possible, working directly with `List`s or `Array`s or `mutable.Buffer`s
might have an order-of-magnitude less performance overhead. This might not
matter, but it very well might be worth it in places where performance matters.
A 10x performance difference is a lot!

## Conclusion

This post provides a concrete baseline which you can use to help decide how
to use the Scala collections. Theoretical analyses often miss lots of important
factors, since naturally you'll only analyze factors you think are important to
begin with. This
empirical benchmark provides a sense of how the collections behave, from a
perspective of actually using them.

For example, it's surprising to me how
removing an element from a `Vector` is so much slower than appending one.
It's surprising to me how much slower `Set` and `Map` operations are when
compared to `Vector`s or `List`s, even simple things like iteration which
aren't affected by the hashing/equality-checks that slow down other operations.
It's surprising to me how fast concatenating large `Array`s is, especially
compared to things like `Vector`s and `List`s which are supposed to use
structural sharing to reduce the work required.

Just as important as highlighting the differences between the collections, this
set of benchmarks highlights a lot of things that *don't* matter:

- Calling `.foreach` on a `List` vs an `Array` vs a `Vector`?
- Memory usage of a large `Vector` vs an `Array`?
- Constructing a `Set` via `+`, or allocating an `Array` and calling `.toSet`?

While the code may look different, and internally is structured very
differently, in practice it probably doesn't matter which one you pick in this
cases. Just choose one and get one with your life.

Next time you are trying to choose a collection to use
for a particular situation, or you are discussing with a colleague what
collection would be appropriate, feel free to check back to this post to ground
the decision in the empirical reality of how the collections perform.

## Reference

### Performance Data with Standard Deviations

Here is the performance data, with all the standard deviations included. They
were not shown earlier for conciseness, but if you wish to cross-reference
things to see how stable the values we're discussing are, this table can help
you. Note that these are the standard deviations of the middle 5 runs out of 7
for each benchmark, each run lasting 2 seconds

|:----------------|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|---------------:|
| **construct**   |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array-prealloc  |        17 ± 0% |        10 ± 0% |        14 ± 0% |        41 ± 0% |     186 ± 0.5% |     710 ± 2.1% |   2,710 ± 2.6% |  11,000 ± 2.7% |  45,100 ± 2.1% | 183,000 ± 2.0% | 730,000 ± 2.0% |3,100,000 ± 1.6% |
| Array:+         |         2 ± 0% |        12 ± 0% |        58 ± 0% |     270 ± 0.4% |   1,460 ± 2.4% |  19,800 ± 1.5% | 260,000 ± 1.7% |3,170,000 ± 1.5% |60,000,000 ± 1.8% |1,020,000,000 ± 1.2% |                |                |
| Vector          |         2 ± 0% |      15 ± 6.7% |      99 ± 1.0% |     410 ± 2.7% |   1,730 ± 2.3% |   7,000 ± 2.7% |  28,600 ± 3.4% | 324,000 ± 0.5% |1,498,000 ± 0.5% |7,140,000 ± 0.4% |31,700,000 ± 0.4% |131,000,000 ± 1.0% |
| List            |         1 ± 0% |         4 ± 0% |        12 ± 0% |      69 ± 1.4% |     301 ± 2.7% |   1,220 ± 2.3% |   4,900 ± 2.2% |  19,800 ± 2.8% |  79,000 ± 2.5% | 329,000 ± 1.6% |1,510,000 ± 1.2% |9,100,000 ± 8.3% |
| Set             |         1 ± 0% |        12 ± 0% |        58 ± 0% |   1,860 ± 0.5% |   8,530 ± 0.4% |  37,400 ± 0.8% | 166,000 ± 1.2% | 783,000 ± 0.5% |3,600,000 ± 1.6% |18,100,000 ± 0.7% |94,000,000 ± 1.4% |473,000,000 ± 1.5% |
| Map             |         1 ± 0% |         6 ± 0% |        95 ± 0% |   2,100 ± 1.9% |   9,010 ± 0.7% |  38,900 ± 0.5% | 171,000 ± 1.0% | 810,000 ± 1.3% |3,710,000 ± 1.3% |18,400,000 ± 1.6% |96,000,000 ± 1.1% |499,000,000 ± 1.4% |
| Array.toVector  |      95 ± 1.1% |       109 ± 0% |       143 ± 0% |     287 ± 0.3% |     903 ± 0.9% |   3,310 ± 0.2% |  12,850 ± 0.5% |  51,100 ± 0.8% | 203,800 ± 0.5% | 821,000 ± 0.5% |3,270,000 ± 1.3% |13,300,000 ± 1.4% |
| Array.toSet     |        73 ± 0% |        75 ± 0% |     187 ± 0.5% |   2,140 ± 1.4% |   9,220 ± 0.9% |  40,000 ± 1.2% | 174,000 ± 0.9% | 833,000 ± 0.6% |3,800,000 ± 1.2% |19,300,000 ± 0.6% |101,000,000 ± 1.4% |506,000,000 ± 1.7% |
| Array.toMap     |        21 ± 0% |        31 ± 0% |     104 ± 1.0% |   2,100 ± 0.5% |   9,200 ± 1.5% |  39,500 ± 1.6% | 173,000 ± 1.7% | 820,000 ± 1.7% |3,790,000 ± 2.1% |19,500,000 ± 1.6% |104,000,000 ± 2.5% |540,000,000 ± 2.2% |
| m.Buffer        |        19 ± 0% |        30 ± 0% |        58 ± 0% |     174 ± 1.1% |     691 ± 0.7% |   2,690 ± 1.0% |  10,840 ± 0.7% |  43,000 ± 0.7% | 169,800 ± 0.4% | 687,000 ± 0.7% |2,770,000 ± 0.6% |11,790,000 ± 0.7% |
| m.Set           |        13 ± 0% |        76 ± 0% |     276 ± 0.4% |   1,430 ± 1.1% |   6,700 ± 0.9% |  27,900 ± 1.2% | 113,000 ± 1.6% | 455,000 ± 1.4% |1,840,000 ± 1.2% |7,900,000 ± 1.4% |39,000,000 ± 3.0% |267,000,000 ± 3.2% |
| m.Map           |         6 ± 0% |      79 ± 1.3% |     297 ± 0.3% |   1,420 ± 1.0% |   6,200 ± 0.7% |  25,500 ± 1.0% | 103,000 ± 1.9% | 414,000 ± 2.0% |1,820,000 ± 2.0% |8,100,000 ± 3.3% |57,000,000 ± 4.6% |348,000,000 ± 2.4% |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| **deconstruct** |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array.tail      |         7 ± 0% |        26 ± 0% |     114 ± 0.9% |     582 ± 0.5% |   4,517 ± 0.1% |  55,500 ± 0.9% | 821,000 ± 0.3% |12,140,000 ± 0.6% |188,000,000 ± 1.0% |3,100,000,000 ± 0.4% |                |                |
| List.tail       |         2 ± 0% |         2 ± 0% |         7 ± 0% |      21 ± 4.8% |    100 ± 10.6% |     420 ± 3.7% |   2,100 ± 5.9% | 10,000 ± 10.4% |  35,000 ± 4.6% | 120,000 ± 9.4% | 540,000 ± 9.2% |1,500,000 ± 53.5% |
| Vector.tail     |         3 ± 0% |         6 ± 0% |      90 ± 1.1% |     425 ± 2.1% |   1,970 ± 1.7% |  11,800 ± 2.6% |  58,400 ± 1.1% | 500,000 ± 2.2% |2,390,000 ± 1.3% |11,000,000 ± 1.2% |50,200,000 ± 0.5% |211,000,000 ± 1.3% |
| Vector.init     |         2 ± 0% |         5 ± 0% |     103 ± 1.0% |     483 ± 1.9% |   2,490 ± 1.8% |  12,800 ± 2.0% |  64,000 ± 2.8% | 543,000 ± 0.8% |2,470,000 ± 1.7% |11,900,000 ± 1.8% |52,600,000 ± 1.5% |218,000,000 ± 1.5% |
| Set.-           |      8 ± 12.5% |      30 ± 3.3% |     162 ± 1.2% |   1,480 ± 3.9% |   7,700 ± 3.0% |  34,200 ± 1.2% | 164,000 ± 1.5% | 770,000 ± 1.4% |3,660,000 ± 2.6% |20,300,000 ± 0.7% |94,000,000 ± 1.3% |420,000,000 ± 1.8% |
| Map.-           |      12 ± 8.3% |        52 ± 0% |     201 ± 0.5% |   1,430 ± 1.3% |   7,660 ± 0.5% |  34,900 ± 0.9% | 169,000 ± 0.7% | 810,000 ± 2.1% |3,990,000 ± 0.3% |24,000,000 ± 3.4% |103,000,000 ± 5.1% |470,000,000 ± 3.4% |
| m.Buffer        |         6 ± 0% |      8 ± 12.5% |      14 ± 7.1% |      43 ± 2.3% |     166 ± 0.6% |     630 ± 2.8% |   2,510 ± 2.9% |  10,000 ± 3.0% |  40,600 ± 1.7% | 167,000 ± 2.8% | 660,000 ± 4.2% |2,490,000 ± 3.0% |
| m.Set           |         5 ± 0% |      28 ± 7.1% |     130 ± 1.5% |     671 ± 1.0% |   4,900 ± 2.9% |  54,000 ± 1.6% | 770,000 ± 1.1% |11,990,000 ± 0.8% |189,000,000 ± 1.1% |3,040,000,000 ± 0.5% |                |                |
| m.Map           |      7 ± 14.3% |      44 ± 2.3% |     172 ± 3.5% |     670 ± 4.6% |   3,650 ± 2.6% |  26,400 ± 1.8% | 282,000 ± 1.3% |3,970,000 ± 0.4% |62,600,000 ± 1.0% |1,000,000,000 ± 1.1% |                |                |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| **concat**      |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| arraycopy       |        23 ± 0% |      18 ± 5.6% |      20 ± 5.0% |      27 ± 3.7% |     48 ± 16.7% |    280 ± 15.5% |  1,000 ± 12.0% |   4,000 ± 7.2% | 16,000 ± 11.0% |  65,000 ± 6.7% |360,000 ± 14.4% |1,400,000 ± 23.1% |
| Array++         |      89 ± 1.1% |      83 ± 1.2% |      85 ± 1.2% |      91 ± 1.1% |     144 ± 5.6% |    330 ± 14.7% |     970 ± 5.6% |   4,100 ± 5.5% | 17,000 ± 17.0% | 70,000 ± 14.9% |380,000 ± 11.5% |1,700,000 ± 7.7% |
| List            |         7 ± 0% |      81 ± 1.2% |     162 ± 1.2% |     434 ± 0.5% |   1,490 ± 2.5% |   5,790 ± 0.8% |  23,200 ± 1.3% |  92,500 ± 0.4% | 370,000 ± 1.1% |1,510,000 ± 1.7% |6,300,000 ± 1.8% |30,000,000 ± 6.5% |
| Vector          |      5 ± 20.0% |      48 ± 2.1% |     188 ± 1.6% |     327 ± 0.3% |     940 ± 2.2% |   3,240 ± 2.0% |  12,700 ± 2.4% |  52,000 ± 4.1% | 210,000 ± 2.2% | 810,000 ± 1.6% |3,370,000 ± 1.3% |14,500,000 ± 2.8% |
| Set             |      91 ± 1.1% |      95 ± 3.2% |     877 ± 0.7% |   1,130 ± 3.4% |   5,900 ± 3.1% |  26,900 ± 2.5% | 149,000 ± 2.7% | 680,000 ± 2.0% |3,600,000 ± 3.3% |23,000,000 ± 2.0% |100,000,000 ± 6.9% |280,000,000 ± 12.6% |
| Map             |      54 ± 1.9% |      53 ± 1.9% |     967 ± 0.9% |   1,480 ± 5.4% |   6,900 ± 2.2% |  31,500 ± 1.0% | 166,000 ± 1.4% | 760,000 ± 2.9% |4,100,000 ± 2.9% |27,000,000 ± 3.5% |118,000,000 ± 4.6% |450,000,000 ± 11.7% |
| m.Buffer        |        11 ± 0% |      32 ± 9.4% |     32 ± 18.8% |      38 ± 2.6% |     70 ± 19.2% |    250 ± 13.7% |    700 ± 29.1% |  3,900 ± 10.0% | 20,000 ± 41.6% | 40,000 ± 34.9% |400,000 ± 14.7% |1,500,000 ± 19.5% |
| m.Set           |      58 ± 3.4% |      81 ± 6.2% |     142 ± 4.9% |   1,080 ± 3.1% |   4,200 ± 3.3% |  16,000 ± 6.7% |  69,000 ± 5.3% | 263,000 ± 2.1% |1,160,000 ± 4.8% |6,300,000 ± 3.7% |43,000,000 ± 5.6% |310,000,000 ± 8.1% |
| m.Map           |      47 ± 2.1% |      69 ± 2.9% |     181 ± 3.3% |     990 ± 1.1% |   3,700 ± 3.0% |  15,000 ± 2.9% |  62,000 ± 5.6% | 290,000 ± 5.2% |1,500,000 ± 16.2% |16,000,000 ± 6.8% |103,000,000 ± 4.3% |493,000,000 ± 1.2% |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| **foreach**     |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array           |         2 ± 0% |         5 ± 0% |        15 ± 0% |      57 ± 1.8% |     230 ± 1.7% |     900 ± 2.0% |   3,580 ± 1.4% |  14,200 ± 1.7% |  55,600 ± 0.8% | 228,000 ± 1.6% | 910,000 ± 1.9% |3,610,000 ± 0.7% |
| Array-while     |         0 ± 0% |         1 ± 0% |         0 ± 0% |         1 ± 0% |         0 ± 0% |         0 ± 0% |         0 ± 0% |    -4 ± 100.0% |    10 ± 166.7% |     70 ± 97.1% |     0 ± 507.0% |   500 ± 153.3% |
| List            |         0 ± 0% |         3 ± 0% |        13 ± 0% |      50 ± 2.0% |     209 ± 1.9% |     800 ± 2.1% |   3,500 ± 3.5% |  14,100 ± 3.8% |  55,000 ± 4.6% | 231,000 ± 2.8% | 920,000 ± 9.7% |3,800,000 ± 6.4% |
| List-while      |         4 ± 0% |         5 ± 0% |        13 ± 0% |      49 ± 2.0% |     211 ± 0.9% |     812 ± 1.1% |   3,400 ± 2.9% |  14,200 ± 4.5% |  57,000 ± 6.9% | 226,000 ± 2.2% | 930,000 ± 6.5% |3,700,000 ± 4.4% |
| Vector          |        15 ± 0% |        19 ± 0% |        30 ± 0% |      74 ± 1.4% |     268 ± 2.2% |   1,000 ± 2.5% |   3,960 ± 1.9% |  16,200 ± 4.1% |  62,000 ± 2.4% | 256,000 ± 1.5% |1,030,000 ± 1.5% |4,300,000 ± 3.2% |
| Set             |         4 ± 0% |         5 ± 0% |        10 ± 0% |      99 ± 1.0% |     420 ± 2.6% |   1,560 ± 2.4% |  10,200 ± 4.1% |  51,000 ± 1.7% | 217,000 ± 2.2% |2,200,000 ± 5.3% |10,800,000 ± 1.7% |48,600,000 ± 1.8% |
| Map             |        19 ± 0% |         7 ± 0% |        20 ± 0% |     140 ± 2.1% |     610 ± 4.0% |   2,500 ± 3.9% |  13,900 ± 3.9% |  72,800 ± 0.9% | 360,000 ± 3.3% |3,700,000 ± 8.2% |20,700,000 ± 1.6% |75,000,000 ± 3.6% |
| m.Buffer        |         0 ± 0% |         1 ± 0% |         1 ± 0% |         1 ± 0% |         1 ± 0% |         0 ± 0% |         1 ± 0% |     2 ± 100.0% |    -1 ± 800.0% |   -10 ± 423.5% |     0 ± 259.4% |  -200 ± 185.1% |
| m.Set           |        19 ± 0% |        26 ± 0% |      50 ± 2.0% |     130 ± 1.5% |     508 ± 1.4% |   2,190 ± 0.5% |  11,900 ± 2.1% |  56,600 ± 1.4% | 235,000 ± 2.6% | 940,000 ± 2.2% |3,800,000 ± 5.5% |14,700,000 ± 5.0% |
| m.Map           |         8 ± 0% |        16 ± 0% |      48 ± 2.1% |     146 ± 0.7% |     528 ± 1.1% |   2,210 ± 1.7% |  10,300 ± 2.8% |  54,100 ± 0.4% | 255,000 ± 2.0% |1,140,000 ± 5.4% |6,800,000 ± 5.4% |30,000,000 ± 6.6% |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| **lookup**      |          **0** |          **1** |          **4** |         **16** |         **64** |        **256** |      **1,024** |      **4,096** |     **16,192** |     **65,536** |    **262,144** |  **1,048,576** |
|                 |                |                |                |                |                |                |                |                |                |                |                |                |
| Array           |         0 ± 0% |         1 ± 0% |         1 ± 0% |         1 ± 0% |         0 ± 0% |         0 ± 0% |         1 ± 0% |    -1 ± 200.0% |     4 ± 200.0% |     0 ± 675.0% |    100 ± 71.6% |  -200 ± 215.5% |
| List            |         0 ± 0% |         1 ± 0% |         8 ± 0% |     103 ± 1.0% |   2,390 ± 0.5% |  47,200 ± 1.0% | 870,000 ± 2.6% |16,900,000 ± 2.8% |                |                |                |                |
| Vector          |         0 ± 0% |         1 ± 0% |         5 ± 0% |        17 ± 0% |     104 ± 2.9% |     440 ± 2.7% |   1,780 ± 2.6% |   8,940 ± 1.1% |  38,000 ± 1.1% | 198,000 ± 1.3% | 930,000 ± 1.6% |4,260,000 ± 1.7% |
| Set             |         0 ± 0% |        18 ± 0% |      81 ± 1.2% |     507 ± 0.6% |   1,980 ± 0.8% |   7,800 ± 1.8% |  39,800 ± 1.8% | 203,000 ± 1.1% |1,040,000 ± 2.3% |8,300,000 ± 2.8% |                |                |
| Map             |         0 ± 0% |        12 ± 0% |      97 ± 1.0% |     578 ± 1.6% |   2,250 ± 2.8% |   9,400 ± 1.5% |  46,000 ± 2.2% | 233,000 ± 1.7% |1,150,000 ± 2.9% |11,400,000 ± 2.6% |                |                |
| m.Buffer        |         0 ± 0% |         1 ± 0% |         1 ± 0% |         1 ± 0% |         1 ± 0% |         1 ± 0% |     1 ± 100.0% |         0 ± ∞% |     6 ± 133.3% |   -10 ± 200.0% |     0 ± 415.4% |     0 ± 970.0% |
| m.Set           |         0 ± 0% |         5 ± 0% |        22 ± 0% |      97 ± 1.0% |     410 ± 2.7% |   1,690 ± 1.4% |   7,100 ± 2.2% |  31,300 ± 1.8% | 148,000 ± 1.4% | 690,000 ± 2.1% |4,800,000 ± 1.6% |                |
| m.Map           |         0 ± 0% |         6 ± 0% |        25 ± 0% |     112 ± 1.8% |     454 ± 1.3% |   1,910 ± 0.7% |   9,400 ± 0.5% |  52,500 ± 0.7% | 243,000 ± 2.7% |1,760,000 ± 1.2% |9,900,000 ± 5.3% |                |

### Raw Benchmark Data

This is the "raw" data that was recorded by the benchmarking code, including
every run of each benchmark separately, rather than coalescing them into
mean and standard deviation:

- [results.json](BenchmarkingScalaCollections/results.json)

### Benchmark Code

If you want to see the raw code used for these benchmarks, you can browse it at

- [https://github.com/lihaoyi/scala-bench](https://github.com/lihaoyi/scala-bench)

Or download the bundle:

- [scala-bench.bundle](BenchmarkingScalaCollections/scala-bench.bundle)

And `git clone scala-bench.bundle` on the downloaded file to get your own personal
checkout of the Fansi repository. From there, you can use

- sbt "~bench/runMain bench.MemoryMain" to do the memory benchmarks

- sbt "~bench/runMain bench.PerfMain" to run the performance tests and dump
  the results into `bench/target/results.json`. Note that this takes 4 *hours*
  to run on my laptop, so best to kick it off and come back the next day.

- sbt "~bench/runMain bench.AnalyzeMain" to read the `results.json` file,
  calculate means and standard deviations, and dump the results into a markdown
  table

While the benchmark code is definitely not as rigorous as using something like
[JMH](http://java-performance.info/jmh/), JMH tends to be pretty slow, and
at 4-and-a-half hours this benchmark suite is already slow enough!

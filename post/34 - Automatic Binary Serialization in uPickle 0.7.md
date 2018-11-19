The latest version 0.7 of the [uPickle](http://www.lihaoyi.com/upickle) Scala
serialization library lets you easily serialize your Scala values to the binary
MessagePack format, in addition to the existing JSON serialization format. This
gives you the option of compact, high-performance, binary serialization entirely
for free, for any value you were previously JSON serializing. This blog post
will explore the benefits of binary serialization, and what uPickle brings to
the table that's special.

-------------------------------------------------------------------------------

uPickle has always provided an intuitive, performant, boilerplate-free way of
serializing your Scala values to JSON strings:

```scala
import upickle.default._

write(1)                          ==> "1"

write(Seq(1, 2, 3))               ==> "[1,2,3]"

read[Seq[Int]]("[1,2,3]")         ==> List(1, 2, 3)

write((1, "omg", true))           ==> """[1,"omg",true]"""

read[(Int, String, Boolean)]("""[1,"omg",true]""") ==> (1, "omg", true)
```

uPickle provides support for the most common data structures that people want to
serialize: primitive types (integers, strings, booleans, ...), collections
(lists, sets, maps, ...) and user-defined case classes. These should support the
vast majority of tree-like data structures that you would want to serialize to
the tree-like JSON format. uPickle is among the [fastest of the common Scala
JSON serialization libraries](http://www.lihaoyi.com/upickle/#Performance), and
works cross-platform on the JVM, Scala-JS and (soon) Scala-Native.

The latest 0.7.x release of uPickle provides drop-in binary serialization:
simply swap out your `read`s and `write`s with `readBinary`s and `writeBinary`s,
and you can serialize all the same data structures to
[MessagePack formatted](https://msgpack.org/index.html) byte arrays:

```scala
import upickle.default._

writeBinary(1)                          ==> Array(1)

writeBinary(Seq(1, 2, 3))               ==> Array(0x93.toByte, 1, 2, 3)

readBinary[Seq[Int]](Array[Byte](0x93.toByte, 1, 2, 3))  ==> List(1, 2, 3)

val serializedTuple = Array[Byte](0x93.toByte, 1, 0xa3.toByte, 111, 109, 103, 0xc3.toByte)

writeBinary((1, "omg", true))           ==> serializedTuple

readBinary[(Int, String, Boolean)](serializedTuple) ==> (1, "omg", true)
```

## Why Binary Serialization

There are three big wins for binary serialization:

- Data size: in the above example, the tuple `(1, "omg", true)` serializes to 14
  bytes in JSON (`[1,"omg",true]`) but only 8 bytes in messagepack (`0x93 0x01
  0xa3 0x6f 0x6d 0x67 0xc3`).

- Performance: MessagePack binary serialization with uPickle is anywhere from
  50-100% faster than JSON serialization, for both reads and writes.

- Binary blobs support: if you want to send some arbitrary bunch-of-bytes (e.g.
  an image file) using JSON, you are forced to hex/base64 encode it to be sent
  as a string, or to send it as a list of JSON numbers. This *further* bloats the
  data-size and reduces performance

The performance difference between uPickle's binary serialization and it's JSON
serialization can be seen in benchmarks (higher number is better):

| Library       | Reads   | Write   |
|:--------------|--------:|--------:|
|Jackson-Scala	|2,080,682|8,905,996|
|Play Json	    |1,123,923|1,518,832|
|Circe	        |2,172,638|2,057,883|
|uPickle	    |3,078,442|4,018,176|
|uPickle binary	|4,907,232|6,812,322|

While the difference in size between MessagePack and JSON is less dramatic after
compression (e.g. GZip), MessagePack's small uncompressed size reduces the need
for spending CPU cycles on compressing/de-compressing messages. And if you do
choose to compress them, the smaller MessagePack binaries will take less time to
compress than large JSON strings. Both of these are on top of MessagePack's
inherent performance advantage.

## A Drop-In Replacement

One big benefit of uPickle is that the binary MessagePack format is a drop-in
replacement for the JSON format: you can serialize all of the same things you
used to be able to serialize with uPickle, just to compact binary data instead
of JSON. This includes primitives:

```scala
write(1)       ==> "1"
writeBinary(1) ==> Array(1)
```

Collections:

```scala
write(Seq(1, 2, 3))       ==> "[1,2,3]"
read[Seq[Int]]("[1,2,3]") ==> List(1, 2, 3)

writeBinary(Seq(1, 2, 3))                               ==> Array(0x93.toByte, 1, 2, 3)
readBinary[Seq[Int]](Array[Byte](0x93.toByte, 1, 2, 3)) ==> List(1, 2, 3)
```

Tuples:

```scala
write((1, "omg", true))                             ==> """[1,"omg",true]"""
read[(Int, String, Boolean)]("""[1,"omg",true]""")  ==> (1, "omg", true)

val serializedTuple = Array[Byte](0x93.toByte, 1, 0xa3.toByte, 111, 109, 103, 0xc3.toByte)
writeBinary((1, "omg", true))                       ==> serializedTuple
readBinary[(Int, String, Boolean)](serializedTuple) ==> (1, "omg", true)
```

Or case classes:

```scala
case class Thing(a: Int, b: String)
object Thing{
  implicit val rw: ReadWriter[Thing] = macroRW
}

write(Thing(1, "gg")) ==> """{"a":1,"b":"gg"}"""

writeBinary(Thing(1, "gg")) ==> Array[Byte](-126, -95, 97, 1, -95, 98, -94, 103, 103)
```

There are many subtleties to how serialization works: streaming reading/writing
to/from files, handling of defaults, custom serializers, and so forth.
With most other libraries, changing to a different serialization format involves
swapping out the entire library; this means different syntax for
reading/writing, different implicits you need to define, different sets of
things that you can and cannot serialize. Changing serialization from JSON to
binary has traditionally been a non-trivial endeavor.

With uPickle, it's just a matter of swapping out `read`/`write` with
`readBinary`/`writeBinary`, and everything else can keep working as before!

## MessagePack

uPickle's binary serialization uses the standard
[MessagePack](https://msgpack.org/index.html) format. This has several
advantages compared to rolling your own ad-hoc binary serialization scheme:

- It has a thorough
  [specification](https://github.com/msgpack/msgpack/blob/master/spec.md); you
  aren't the only one who knows how MessagePack works! And if someone doesn't
  know, it's easy to find out.

- It has broad support, in almost every language:
  [Java](https://github.com/msgpack/msgpack-java),
  [Python](https://github.com/msgpack/msgpack-python),
  [C](https://github.com/ludocode/mpack), the list goes on. Since uPickle
  serializes to standard MessagePack, you can inter-operate with programs
  written in all these other languages for free.

- Semantically it is basically just JSON: lists, dictionaries, and primitives.
  This means the ways things are serialized in uPickle's MessagePack format is
  almost identical to the way they are serialized in uPickle's JSON format, and
  you can inspect MessagePack binaries by converting them to JSON for easy
  viewing.

- uPickle's MessagePack blobs have the same schema-evolution capabilities as
  it's JSON strings: you can add new fields to your Scala case classes, as long
  as you provide a default if the field is not present. You can delete old
  fields from your case class and uPickle will happily skip over those fields in
  the MessagePack blobs. This makes storing or exchanging MessagePack binary
  blobs much less fragile than, say, Java-serialization blobs

While MessagePack isn't perfect, it is a generally reasonable way of serializing
JSON-like binary data: quick to encode/decode, space-efficient, and avoids the
opacity, confusion, and fragility that tends to befall many hand-crafted binary
formats. While not optimized for your specific use case, as a general-purpose
binary format it is very likely "good enough".

uPickle's architecture is such that MessagePack isn't blessed: if needed, we
could add support for other JSON-like serialization formats such as
[CBOR](http://cbor.io/), [BSON](http://bsonspec.org/), and so on. Nevertheless,
MessagePack is a fine format to come bundled with uPickle as it's default binary
serializer.

## Conclusion

uPickle's original contribution to the Scala ecosystem was that it made it very,
very easy to serialize typical data-structures to simple, predictable JSON: no
fancy configuration, no confusing imports, just `upickle.default.write` and
`read` and you're done.

With uPickle 0.7, this capability has been extended to binary serialization: it
is now trivial to serialize any of the common data structures to the common
MessagePack binary format. Whether for speeding up your existing JSON RPCs,
inter-operating with third party MessagePack-based services, or efficiently
storing your Scala binary data on disk or in a database, uPickle's MessagePack
backend makes it quick, efficient and predictable. While for more specialized
tasks it may still make sense to use a hand-crafted serialization format, for
"most" boring use-cases where you need binary serialization you can just use
uPickle.

uPickle 0.7.1 has been published to Maven Central, and is already used in the
latest versions of [Ammonite](http://ammonite.io) and
[Mill](http://www.lihaoyi.com/mill/). Try it out!


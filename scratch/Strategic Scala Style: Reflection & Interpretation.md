- [Reflection](#Reflection)
    1. *Don't use reflection if at all possible*
    2. *If not, use macros, a.k.a. compile-time reflection*
    3. *Else, use Java runtime reflection*
    4. *Else, use Scala runtime reflection*
    5. *Else, use classloaders and code-generation*
- [Domain-Specific Languages](#DomainSpecificLanguages)
    1. *Your DSL is a bunch of method-calls to do what you want*
    2. *Your DSL constructs a data-structure, which is then interpreted*
    3. *Your DSL parses the data-structure from some external textual syntax*



### Reflection

1. *Don't use reflection if at all possible*
2. *If not, use macros, a.k.a. compile-time reflection*
3. *Else, use Java runtime reflection*
4. *Else, use Scala runtime reflection*
5. *Else, use classloaders and code-generation*

Reflection is when you write code that makes use of parts of your program that
are normally transparent to the system: variable names, class-names,
package-structure, all these are things that are normally don't matter to the
code at run-time. Nevertheless, it is often useful to reduce boilerplate: e.g.
when converting classes to JSON, you want the field-names to become the JSON
keys without having to manually map them to- and fro- each time.

Apart from not using reflection, there are different amounts of reflection you
can use. You should use as little as possible.

#### No Reflection

If you can get by without using reflection, e.g. you're only '

#### Macros
#### Java Runtime Reflection
#### Scala Runtime Reflection
#### ClassLoaders

### Domain-Specific Languages

1. *Your DSL is a bunch of method-calls to do what you want*
2. *Your DSL constructs a data-structure, which is then interpreted*
3. *Your DSL parses the data-structure from some external textual syntax*


#### Method Calls
#### Construct & Interpret a Data-Structure
#### External Textual Syntax
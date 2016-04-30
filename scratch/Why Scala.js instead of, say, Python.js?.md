The Scala.js compiler lets you convert Scala source code - normally run on the Java Virtual Machine as bytecode - into Javascript instead, letting you run it in any web browser. In the three years since it was created, it has seen great adoption in the Scala community, with vast swathes of the ecosystem being compatible to Scala.js, and many people using it in production.

There are many other X-to-Javascript compilers out in the wild: converting Java, Python, Ruby and all sorts of languages. Many are older than Scala.js, and have had just as much work gone into them. Why haven't any of them taken off like Scala.js has? This post will use one of the Python-to-JS compilers as a case study, and explore the fundamental reasons why Scala.js is different.

---------------------------------------------

- Why Compile to Javascript?
- Overview of X-to-JS compilers
- The cost of compatibility
    - Python to JS
    - Opal Ruby
- Case study using Python to JS
- Compatibility Hell
- Chains of Dependencies
- The Shape of the Universe

## Why Compile to Javascript?

- No longer unusual
- Typescript, Babel, Coffeescript, ...

- "Better" language to write code in
- Share code with non-JS server

## Overview of X-to-JS Compilers

- Js-to-Js: Typescript, Babel, Flow
- Java: GWT
- Ruby: Opal
- Python: PyJS, Pyjamas, Rapydscript, Brython, PyPyJS, Skulpt, Transcrypt, PythonJS
- C: Emscripten
- C#: Bridge.NET, DuoCode, Salterelle, SharpKit
- F#: Funscript, Websharper

## The Cost of Compatibility

- Performance/Code-Size vs Compatiblity
- Ruby: Opal compatibility switches
- Python: slow/huge/compatible PyPyJS vs fast/tiny/incomplete Brython

## Case Study: PyJS

- Writing string_snippet in PyJS
- Why unicode doesnt work

## Compatibility Hell



## Chains if Dependencies
## The Shape of the Universe
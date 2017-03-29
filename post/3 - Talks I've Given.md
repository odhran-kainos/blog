I've given a bunch of talks at meetup groups, industry conferences and academic
workshops. Most of them are about my work in the Scala programming language.
The actual recordings for these are slightly scattered, over a mix of Youtube
videos, Vimeo, and some conference sites.

Here's a consolidated list of their abstracts and videos, most recent first.
I'll keep this updated as time goes on.

-------------------------------------------------------------------------------

## Intro to Scala.js

- **Singapore Scala Meetup** *28 Mar 2017*
- **Singapore JS Meetup** *16 Mar 2017*
- [Slides](../post/slides/Intro-to-Scala.js.pdf)

A short introduction to the Scala.js Scala to Javascript compiler.

## Scala Scripting

- **Scala Exchange** *8 Dec 2016* [Video](https://vimeo.com/194959852)
- **Scala by the Bay** *10 Nov 2016* [Video](https://vimeo.com/191328477)
- [Slides](../post/slides/Scala-Scripting.pdf)

This talk will demonstrate a new script-file format for writing your Scala code. 

Unlike traditional Scala projects, which are built with SBT or Maven or Ant and 
edited inside your IDE, Scala Scripts do not need a "project" or "build tool" 
in order to run. You simply write your code in a single file, and run it. Need 
code in another script? Simply import it. Need a third-party library? You can 
import it too. 

This greatly reduces the barrier to entry of getting started writing Scala code, 
and allows Scala to be used for common housekeeping work at the command-line, 
much like Python or Ruby is used today. I will demonstrate this Scala Scripting 
file format, explain how it works, and what place it could find in the Scala 
ecosystem today.

## Client-side web dev without Javascript, with Scala.js
**ForwardJS Summit** *10 Feb 2016*

- [Video](https://vimeo.com/154932681)
- [Slides](../post/slides/Isomorphic-Client-side-web-dev-without-Javascript.pdf)

Functional programming is all the rage now: promising concise, simple code with
fewer bugs. Isomorphic web development, on the other hand, has a different set
of promises: letting you share logic between client and server without
duplication of code, ensurign that they will always stay in sync.

Scala.js is a new take on these ideas: rather than bringing your front-end code
to the server, we bring your back-end code to the browser! In this talk, I'll
demonstrate how to get started with Scala.js to build client-side web
applications. We'll see how Scala.js gives you functional programming and
isomorphic web development almost for free, and brings with it additional
bonuses like tooling and safety far beyond anything available in Javascript
land.



## Shell-scripting in a Typed, OO Language
**New Object Oriented Languages, SPLASH** *28 Oct 2015*

- [Video](https://vimeo.com/143819744)
- *Presented without slides*

Talk given 27 October 2015 at the Zeroth Workshop for New Object Oriented
Languages.

What if instead of Bash, you could use a real, modern programming language to
use as your systems shell? I'll spend 30 minutes talking about why people use
Bash over other contemporary languages, how the Ammonite Scala REPL plays
catch-up, and highlight various ways in which the experience benefits from
using a statically-typed, object-oriented language running on the JVM.


## Fast, Modern, OO Parser Combinators
**Parsing@SLE, SPLASH** *24 Oct 2015*

- [Video](https://vimeo.com/143572750)
- [Slides](../post/slides/Parsing-SLE-FastParse.pdf)

A quick, 30 minute overview of the features and usage of the FastParse parser
combinator library lihaoyi.github.io/fastparse, allowing you to quickly write
high-performance, debuggable, fail-friendly parsers



## FastParse: Fast, Modern Parser Combinators
**SF Scala Meetup** *13 Oct 2015*

- [Video](https://vimeo.com/142341803)
- [Slides](../post/slides/FastParse.pdf)

Parsing text is typically difficult. As a programmer you have tools ranging
from String#split (convenient and fast but inflexible) to Lex/Yacc/Antlr (fast
and flexible but inconvenient) and parser combinators (convenient, flexible
but very slow!)

This talk introduces FastParse, a parser-combinator library for the Scala
programming language, that aims to find a middle ground between all these
alternatives. Convenient, flexible and fast, I'll show how code using FastParse
looks almost the same as code using the in-built parser-combinators, but comes
with an 85x (8500%!) speedup at run-time. I'll talk about how FastParse
provides unprecedentedly good, structured error reporting for you to use to
debug your parser (or help your users debug their input) and finish off with
some demo live-coding of a language of the audience's choice.


## Beyond Bash
**Scala by the Bay** *12 Aug 2015*

- [Video](https://www.youtube.com/watch?v=dP5tkmWAhjg)
- [Slides](../post/slides/Beyond-Bash.pdf)

The Scala REPL has been often touted as an advantage for the language: an
interactive, exploratory experience very different from the static,
often-IDE-based experience that for many is the bulk of their experience
using Scala. Nevertheless, in comparison, the Scala REPL really sucks: buggy
& unfriendly, it is not a place you want to spend most of your time.

What if the Scala REPL had the same autocomplete as you'd get in Eclipse or
IntelliJ? What if it had syntax-highlighting for everything? What if you could
load libraries like Shapeless or Akka-HTTP to try out, without needing to muck
with SBT? What if your Scala REPL was as versatile, usable and configurable as
Bash or Zsh, and could be used as your home on the command line?


## Why (You might like) Scala.js
**Scaladays SF** *17 Mar 2015*

- [Video](https://vimeo.com/122611959)
- [Slides](../post/slides/Why-You-Might-Like-Scala.js.pdf)

Scala.js compiles Scala to Javascript. Why should you, as an individual, care?
This talk discusses the things you can get out of Scala.js, starting from
three main archetypes: Scala web developer, Scala non-web developer, and
compiler-writer.


## Scala.js - Safety & Sanity in the wild west of the web
**PhillyETE** *8 Mar 2015*

- [Video](https://vimeo.com/124702603)
- [Slides](../post/slides/PhillyETE-Scala.js.pdf)

Developing for the web platform has historically been a slow, painful,
fragile experience. You write code in multiple different languages, work
with undocumented APIs, and are forced to implement things twice to have
them work on both client and server. Lastly, you had better be the
meticulous sort, because a single typo will bring down your site: at
runtime, in production!

Scala.js is an attempt to fix this problem. Like other compile-to-JS languages,
it provides a concise, expressive language to do your work. Unlike others, it
also promises seamless interop with Javascript, a ready-to-go ecosystem, tool
support, and a smooth development experience. Above all, Scala.js provides
Safety: an extreme level of safety that goes far beyond any competitor. Not
only is your Scala.js code checked, but any use of Javascript APIs is also
checked, and so are your Ajax calls between client and server!


## Bootstrapping the Scala.js Ecosystem
**Scala Exchange** *7 Dec 2014*

- [Video](https://vimeo.com/113967983)
- [Slides](../post/slides/Bootstrapping-the-Scala.js-Ecosystem.pdf)

What does it take to go from a working compiler to a workable, productive
platform? This presentation explores the range of libraries that needed to
be built in order to turn Scala.js from a prototype to a product


## Hands-On Scala.js
**Pacific-North-West Scala** *14 Nov 2014*

- [Video](https://vimeo.com/111978847)
- [Slides](../post/slides/Hands-on-Scala.js.pdf)

A talk I gave at pnwscala.org/2014. This is a hands-on tutorial that goes
through what it's like getting started with Scala.js, introducing the project
and walking through the experience of how to do common things on the Scala.js
platform.

## Cross-platform development with Scala.js
**Scala by the Bay** *9 Aug 2014*

- [Video](https://www.youtube.com/watch?v=Ksoi6AG9nbA)
- [Slides](../post/slides/Cross-Platform-Development-in-Scala.js.pdf)

This talk will explore the developer experience of using ScalaJS, from the
boring-but-important cross-JVM/JS libraries, to pure-Scala client-server web
applications, to whiz-bang ScalaJS games and animations. As the person who has
written more ScalaJS code than anyone on the planet (!) I will go through the
ups and downs of ScalaJS development, and demonstrate why you may want to try
it out for your next round of web development.

## Fun Functional-Reactive Programming with Scala.Rx
**Scaladays** *17 Jun 2014*

- [Video](https://vimeo.com/98477272)
- [Slides](../post/slides/Fun-Functional-Reactive-Programming-with-Scala.Rx.pdf)

Scala.Rx is a change propagation library for Scala, that provides reactive
values that save you the hassle of having to keep mutable variables in sync
manually. This goes into the motivation behind the library, shows off some cool
demos, and hopefully persuades you that Scala.Rx is a useful tool in managing
messy, stateful applications.

## Live-Coding Scala.js
**SF Scala Meetup** *28 Feb 2014*

- [Video](https://vimeo.com/87845442)
- [Slides](../post/slides/Live-Coding-Scala.js.pdf)

Scala like you've never seen it before! Live reloading, in the browser, on the
canvas, with the DOM.

## Metascala: a tiny DIY JVM
**Scala Exchange** *2 Dec 2013*

- [Video](https://skillsmatter.com/skillscasts/4916-metascala-a-tiny-diy-jvm)
- [Slides](../post/slides/Metascala-a-Tiny-DIY-JVM.pdf)

Metascala is a tiny metacircular Java Virtual Machine (JVM) written in the
Scala programming language. Metascala is barely 3000 lines of Scala, and is
complete enough that it is able to interpret itself metacircularly. Being
written in Scala and compiled to Java bytecode, the Metascala JVM requires a
host JVM in order to run.

The goal of Metascala is to create a platform to experiment with the JVM: a
3000 line JVM written in Scala is probably much more approachable than the
1,000,000 lines of C/C++ which make up HotSpot, the standard implementation,
and more amenable to implementing fun features like continuations, isolates or
value classes. The 3000 lines of code gives you:

- The bytecode interpreter, together with all the run-time data structures
- A stack-machine to SSA register-machine bytecode translator
- A custom heap, complete with a stop-the-world, copying garbage collector
- Implementations of parts of the JVM's native interface

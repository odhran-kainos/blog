Programmers usually view old, legacy code with a mix of fear and respect.
As a professional software engineer, you often have to deal with systems
written long before you turned up. You have to dive into them,
understand them, review them, and improve them. You often wonder...

> Why did they do this

or

> What were they thinking?

Sometimes to realize later that the *they* is in fact, yourself!

You learn things to get the job done: the code's purpose, it's constraints and
abilities. Often though, you learn more than that: you feel the thoughts,
feelings and characters of the people who wrote it. You feel their
frustration leaking through their comments and workarounds. Their style,
their imprint on the code that is hard to quantify but often uniquely
identifies the code as "theirs". While figuring out how code works, you
often walk the same path walked by someone months or years ago, with all
the same triumphs and failures.

Old code is literally the crystallized logic and thoughts of someone long ago.
In understanding the code, you are re-constructing that person's thoughts.
Even when that someone is yourself, *you* as an individual change over the
months and years, and best practices evolve, so even when you yourself wrote
the code it always feels slightly foreign.

With that in mind, I went to take a look at some of my own earliest work, over
a decade old, to give it a modern, professional review given what I know now
as a software engineer. I will contrast what 2004-me did with what 2016-me
would want, and consider the 12 years of progress in experience, tools, and
the overall maturity of the field of software engineering that lead to
these differences!

## 2004

The earliest working code I could find dates back from 2004.

I was 15 years old, and had just learned programming in secondary-school.
I would eventually pick up a huge number of various programming-in-Java books,
write a 3D combat flight simulator, learn C#, pick up Python, PHP & Javascript
in MIT, become involved in the [Open-Source Scala] community, and get a job at Dropbox.
However, at the time, there I was with 1 year of Java lessons in school, and
probably having worked through a single book, *Java in Easy Steps*:

![JavaInEasySteps.jpg](JavaInEasySteps.jpg)

It wasn't just myself which was younger in 2004, the entire field was younger.
In 2004, Java was on version 1.4. [The Scala language barely existed].
[Facebook had just started] writing their world-changing website in PHP,
possibly because at the time web frameworks like [Ruby on Rails] and [Django]
did not exist.

In this case, the 2004 code is a 2D Asteroids clone, written in Java.
I had written earlier programs: for class, for my own amusement. Those include
some pretty elaborate console applications: I had a complete 2D-grid
console-based Dungeons-&-Dragons game implemented at one point. But none
of those earlier works survive in an intact state today.

Here's how it begins:

```java
class Asteroids extends JFrame implements KeyListener, ActionListener{

    final int X = GUI.X;
    final int Y = GUI.Y;


    class Projectile{
        int SHOTS = 100;
        double POSY;
        double POSX;
        ...
```

The full ~300 lines of code can be found on Github:

- [Full Source Code](https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L99-L401)

I did not know what git *was* back then! I more recently uploaded it in case
anyone wants to take a look. You can download the contents from github and
run it with Java today. This game, and a few others in the same file, can
also be played online:

- [Online Demo](http://lihaoyi.github.io/scala-js-games/)

As I had ported them to [Scala.js] as an exercise a year or two ago to try
out the capabilities of the then-very-nascent Scala.js project. As far as I
can tell, the games play exactly identically in the browser as they used to
on the desktop, except for minor differences in anti-aliasing and the lack of
sound.

## Running it

The first thing you try to do with legacy code is run it. Whether through
automated tests, manual fiddling, running the code and seeing it work should
give you a quick sense of what it is the application does. Broad questions like:


- Is it a long-running program or a batch program?
- GUI program, CLI program, background job, or library?
- How do I interact with it? What are its inputs and outputs?

When you see all the
disjoint snippets of code later during review, you'll be better able to place
them in the overall scope of the application.

You should be able to try out this code right now! The compiled class-files
are checked into the git repository mixed in with all the sources (best
practices? What're those?)

```
haoyi-mbp:~ haoyi$ git clone https://github.com/lihaoyi/Java-Games
Cloning into 'Java-Games'...
remote: Counting objects: 680, done.
remote: Total 680 (delta 0), reused 0 (delta 0), pack-reused 680
Receiving objects: 100% (680/680), 16.95 MiB | 2.37 MiB/s, done.
Resolving deltas: 100% (228/228), done.
Checking connectivity... done.

haoyi-mbp:~ haoyi$ cd Java-Games/GameLibrary/

haoyi-mbp:GameLibrary haoyi$ java GameLibrary
```

This should pop up the game picker:

![GamePicker.png](GamePicker.png)

Clicking on `Asteroids` should open up the game we're looking at:

![Asteroids.png](Asteroids.png)

And you control your ship with Left, Right Up, Down arrows, and spacebar to
shoot. Destroy all the asteroids and don't get hit. That's all there is!


Now that we've got the code running - the first step in dealing with any
legacy code - let's review it!

## Code Review

Overall, it's a relatively simple ~300 lines of code: I have Vectors of
objects representing the [asteroids and bullets] moving around (because
there are more than one), each one with some variables representing their
speed and size and whatnot. I have some variables representing [the player]
(only one). I listen to key events and update the controls
[LEFT RIGHT FORWARD BACK] when you press or release something. I have
[a timer] that calls the [actionPerformed] method, which then does all the
erasing of images, moving of objects, re-drawing, and collision-detection.

Let's go into individual facets of this codebase.

### File Layout

The first thing you've probably already noticed is the fact that the code for
this game is in one huge, 12 thousand line file, together with the code for
all sorts of other games and utilities. In total there only are a few `.java`
files in the `GUI/` folder. Many seem very random:

- `3D.java`: A simple 3D math library built around [Euler angles].
- `3DM.java`: A 3D math library built around [Rotation Matrices].
- `FileHandler.java`: helpers, wrapping the verbose java.io APIs,
  to read and write semi-structured data from files
- `Counter.java`: Encapsulates a double with current, min and max values
- `GUI.java`: All sorts of random helper functions used in various GUI programs.
- `GameLibrary.java`: 12 thousand lines of graphical games.
- `JCanvas.java`: Some kind of wrapper around JPanel with some helper methods
- `NumberQueue.java`: No idea what this is

There are also two folders, one containing more code, on containing some
`.wav` files.

In general, most of these files can be ignored for now: they're only used
in later games (*also* in `GameLibrary.java`!) that make use of 3D graphics,
files, etc., and Asteroids doesn't need any of it.  The `GUI/` folder itself
was presumably named because this is where all the graphical interfaced,
non-terminal programs I ever wrote lived. The code we're interested in all
lives in the first few hundred lines of `GameLibrary.java`.

Suffice to say, directory/file organization would not pass code review in my
workplace today. In 2004, though, I was not competent at working with
multiple files at a time! I was performing a lot of work using Notepad and
running `javac` and `java` from the Windows Command Prompt, or I was using
Netbeans as Notepad with syntax highlighting, ignoring the rest of its
features. Having everything in one file meant `Ctrl-F` to find things was
easy, as was finding things via their position in the scroll-bar. Given
my skill level at the time, this "everything in one file" approach may well
have been optimal! With a modern text editor, window manager and easy
navigation between files, it makes more sense to quickly start breaking it
up into separate files.

### Class Structure

We've already gone through the "macro-level" layout of the entire folder,
let's take a look at the structure of the `Asteroids` class itself.

```java
class Asteroids extends JFrame implements KeyListener, ActionListener
```

To begin with, we see that it extends `JFrame`. This is done because the
program is a [Java Swing] application, and I wanted the Asteroids game to fit
in as a Swing component. In theory, the logic for the game could easily be
separated from the the GUI toolkit logic: that would follow the best practice
of [Composition over Inheritance]. However, in 2004, 15-year-old-me did not
know anything about best practices, so here we have it.

The `Asteroids` class also implements `KeyListener` and `ActionListener`.
As far as I can tell, that was the best practice at the time: if you wanted
access to an event-handler in some code, you have the surrounding class
implement the event handler and add yourself as a listener, e.g.
[further down](https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L246)

```java
this.addKeyListener(this);
```

If I was programming in Python, C#, Javascript, or Scala today, I would not do
that. I would pass in a callback function to `addKeyListener`. However,
in Java you did not have callback functions, so this inheritence dancewas
how you received the callback. In theory I could pass in a clunky [Anonymous
Inner Class], but I would not learn I could use those as event handlers for
another 4 years (halfway through college). Even in Java today, Java 8 provides
inline [Function Literals], so I wouldn't be surprised if the convention
gradually changed. At the time though, in Java, this was the way you did
things.

The insides of the `Asteroids` class is laid out roughly as follows:

```java
class Asteroids...{
    <constants for X and Y>
    <game variables>
    <class constructor>
    <event handlers>
    <canvas plumbing code>
}
```

This isn't a precise description: there are some helper methods scattered
around various places such as
[createAsteroids](https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L222),
or [splitAsteroid](https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L365),
but it's pretty close. The rest of the games in `GameLibrary.java` follow
the same layout convention.

It turns out, that even back then I had used Java's [Nested Classes] without
really ever having thought about it, or all the subtleties related to them.
To 15-year-old me, a `class` was where I put all my stuff related to
something: whether they were methods, fields, or other classes, it never
occurred to me that they would be treated differently. Only later did I
realize how you couldn't put a method inside another method, or that
inner-classes defined inside *methods* had odd limitations such as [only
being able to access final enclosing fields].

But now, in 2016, I'm programming in Scala and am again free to put "anything
inside anything", regardless of whether those "anything"s are classes, methods
or variables. And that's the way it should be!

### Naming Conventions

Almost immediately, anyone looking at this code would notice it is rather odd:
All variables are ALLCAPS, except some which are arbitrarily PascalCased! For
example:


```java
class Projectile{
    int SHOTS = 100;
    double POSY;
    double POSX;
    double YMOMENTUM;
    double XMOMENTUM;
    double SPEED;
    int FACINGANGLE;
    boolean CONTROLABLE;
    boolean EXITED = false;
    int IMAGE;
    int TYPE;
    int SIZE = 3;
    Shape Image;
    ...
}
```

That does not fit into any naming convention I've ever seen, and I have no idea
where I picked it up. Later on, if you look at the code towards the bottom
of this file, I dropped the ALLCAPS naming and kept variables PascalCased.

It wasn't until I was in college, years later in 2010, that I realized that
the rest of the world was using camelCase for their variables! I remember
flipping through all my old books for validation that I, in fact, picked up
the PascalCased-variables convention from somewhere. Alas, all of them had
"standard" camelCased variables. It turns out I must have made it all up
myself!

Nowadays, professionally, I am extremely conscious of this sort of style
choice or convention, trying hard to ensure it's consistent across the team,
organization, or better yet with the rest of the broader community. At the
time, though, I was programming basically alone, and idiosyncrasies, like
this mistaken naming convention, could persist for years.

### Don't Repeat Yourself

In general, the code is relatively repetitive. For example, a lot of code
is [duplicated for X and Y]

```java
ASTEROID.XMOMENTUM += XMOD;
ASTEROID.YMOMENTUM += YMOD;
XMOD = SPEED * Math.cos(Math.toRadians(ANGLE - 30));
YMOD = SPEED * Math.sin(Math.toRadians(ANGLE - 30));
NEWASTEROID.XMOMENTUM += XMOD;
NEWASTEROID.YMOMENTUM += YMOD;
NEWASTEROID.XMOMENTUM += SHOT.XMOMENTUM / (NEWASTEROID.SIZE * 2 + 3);
NEWASTEROID.YMOMENTUM += SHOT.YMOMENTUM / (NEWASTEROID.SIZE * 2 + 3);
ASTEROID.XMOMENTUM += SHOT.XMOMENTUM / (ASTEROID.SIZE * 2 + 3);
ASTEROID.YMOMENTUM += SHOT.YMOMENTUM / (ASTEROID.SIZE * 2 + 3);
```

Because I did not have a proper 2D point class
with the relevant point/vector operations (this is fixed in the next game,
which uses `java.awt.Point2D.Float`). By this point I was already capable of
defining objects to represent common structures I find in my data, and had
done so in earlier programs, but it mustn't have occured to me to do so here.
In general, I was a lot less averse to duplicate code in 2004 than I am today
in 2016.

Some other code needs [repetitive casting]
because I was on Java 1.3 and 1.4 at the time, which did not have generics: in
Java 5+ we would defined `Shots` and `Asteroids` as

```scala
Vector Asteroids = new Vector<Projectile>();
Vector Shots = new Vector<Projectile>();
```

And not need to do any of those casts.

Nowadays, having this sort of generic container is table-stakes in many
programming languages. [Google Go] gets a lot of flak for not having them
only because they have become part of the expectation of software engineers
working in statically-typed languages. In 2004, though, generics were still
a relatively new and novel concept, and so I just did without.

Lastly, other parts of the
code seemed to have repetitive casting for [no apparent reason].
This would not pass code review today!

### Mutation

The entire program basically works via mutable state: the changing states of
the player, bullets and asteroids is modelled via mutable state, which is
probably what I would do even today. The [initialization of the Swing panel],
setting its size and properties, is also done by mutable state:

```java
super("Window");
this.setSize(X, Y);
this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
this.setResizable(false);
//SET CONTENT PANE
Container contentArea = getContentPane();

//LAYOUT MANAGER
//ADD ITEMS TO CONTENT PANE
Area = new Background();
contentArea.add(Area);
Stopwatch.start();
this.addKeyListener(this);
createAsteroids();

//ADD CONTENT PANE AND PACK
this.setContentPane(contentArea);
this.show();
```
As was the convention in Java and the Swing library (which is builtin and not
something I wrote). Nowadays, I know that this is sub-optimal: this means you
are running lots of code while the window still hasn't finished being
initialized, and accessing the wrong field or method during this initialization
process could return you garbage data and cause weird bugs.

Ideally you would want the window to be in a "ready" state the moment it is
finished constructing: pass in constructor arguments, get working window.
There are some problems with this approach thought:

#### Much of the "configuration" is optional

Java does not have a good way
to handle optional arguments. Apart from using mutator `set` methods to allow
optional-ness, [Telescoping Constructors] are another method which
unfortunately doesn't scale as the number of optional arguments grows more
than 4 or 5. In 2016 many in the community have switched to the
[Builder Pattern], and if I was writing Java today that would hopefully be
what the library exposes to me.

If I was writing in C# or Python or Scala or
some other modern language, I would just use optional constructor arguments
and avoid this whole discussion.

#### Many of these configuration options actually model mutable properties

For example, the size of the window can be changed later after creation, and
listeners can be attached and detached. Given that, it makes sense to have
the configuration itself simply be mutable properties: you don't want to
have to close and re-open the window every time you want to tweak it's size!
That's the "obvious" downside if the window's properties are immutable.

In 2016, we actually *do* have a better way around this. In general, the
[Virtual Dom] concept pioneered by [React.js] has proven itself in many
production environments, and it has shown that you can in fact model GUI
components as immutable data structures. With a Virtual Dom, your code
would indeed throw away the old immutable *virtual* window object and create
a whole new immutable *virtual* window object, but the framework would do
the grunt work of making sure only the bits of the *real* window that differ
are actually changed! That way your code works largely with immutable
copies of the window (which are less prone to bugs involving
accidental/hard-to-track-down mutation) while the user would see and interact
with a single, long-lived, smoothly-updated entity: the best of both worlds

-------------------------------------------------------------------------------

While some of the mutable state is inherent in modelling the mutable game,
much of it isn't. With modern 2016 technology, we now have techniques that
help to reduce the amount of this "incidental" mutation: the [Builder Pattern]
and languages with optional method/constructor arguments would remove the
need for mutability as optional configuration values, and modern techniques
like the [Virtual Dom] even let us remove the mutability from the code
modelling long-lived, mutable GUIs.

But none of this existed in 2004 Java. While Python and C# existed, both of
them were relatively niche languages no where near as popular or widespread
as they are today. The Scala language only appeared in 2004, and has only
become popular in the last few years. [React.js] and its [Virtual Dom]
technique only appeared in 2013.

In 2004 Java Swing, that's just the way it is.

## Modern Comparison

That's the review, so how would it compare to code I would write today?
Luckily we can actually do this comparison, because I ported the code to
Scala two years ago to make it run with Scala.js in the browser:

- [Online Demo](http://lihaoyi.github.io/scala-js-games/)

The source code for this demo is also available online:

- [2013 Scala Source Code](https://github.com/lihaoyi/scala-js-games/blob/master/src/main/scala/example/Asteroids.scala)

It is comparable in scope to the original Java code linked above: while
the Scala version has some helpers e.g. a 2D point class, the Java code
also has such things available through the Java standard library.

Overall, the code looks pretty different. Large nested constructs like the
original [collision detection loop] used to walk over the data structures
and both mutate them and draw on the canvas in-line:

```java
for(int i = 0; i < Shots.size(); i++){
    for(int j = 0; j < Asteroids.size(); j++){
        try{
            if(((Projectile)Shots.elementAt(i)).Image.intersects((Rectangle2D)(((Projectile)Asteroids.elementAt(j)).Image))){
                if(((Projectile)Asteroids.elementAt(j)).SIZE > 1){
                    Projectile ASTEROID = ((Projectile)Asteroids.elementAt(j));
                    Projectile SHOT = ((Projectile)Shots.elementAt(i));
                    splitAsteroid(ASTEROID, SHOT);

                }else{
                    Area.Painter.setColor(Color.black);
                    Area.Painter.fill(((Projectile)Asteroids.elementAt(j)).Image);
                    Area.Painter.draw(((Projectile)Asteroids.elementAt(j)).Image);
                    Asteroids.remove(j);

                }
                Area.Painter.setColor(Color.black);
                Area.Painter.fill(((Projectile)Shots.elementAt(i)).Image);
                Area.Painter.draw(((Projectile)Shots.elementAt(i)).Image);
                Shots.remove(i);
            }
        }catch(Exception n){}
    }
}
```

In the [Scala collision detection code], this gets replaced by a number of
queries to figure out what needs to happen, and then only then doing all
the mutation all at one go at the end

```scala
val changes = for{
  b <- bullets
  a <- asteroids
  if a.contains(b.position)
} yield {
  val newAsteroids =
    if (a.level == 1) Nil
    else {
      Seq(30, -30).map(d =>
        new Asteroid(a.level - 1, a.position, a.momentum.rotate(d*Math.PI/180))
      )
    }
  (Seq(a, b), newAsteroids)
}
val (removed, added) = changes.unzip
val flatRemoved = removed.flatten
asteroids = asteroids.filter(!flatRemoved.contains(_)) ++ added.flatten
bullets =
  bullets
    .filter(!flatRemoved.contains(_))
    .filter(_.position.within(Point(0, 0), bounds))
```

This is hopefully easier to reason about, as the "reading" and the "writing"
get split up into two distinct phases: you no longer need to worry about
your writing somehow messing with the things you are about to read. One
interesting thing is that the old code pain-stakingly blacked out
the shapes which were removed. The modern code, on the other hand, simply
removes the shapes and relies on the per-frame wipe-canvas-redraw-everything
step to draw things back correctly. This is another technique that I had
learned by the games later down that file, but did not know when i wrote
the 2004 version of Asteroids.

The modern Scala version is overall about 40% the size of the original Java
version. To me, that represents progress: that's a significant savings in
code that will need to be written, read, and debugged. Arguably, the
"overhead" in writing this code has dropped by an even larger ratio, since
there is some *intrinsic* complexity to the logic that will always need to
be described! I'd say the modern version is barely above the minimum
complexity needed to implement the game, while the original version has tens
of times as much unnecessary overhead and complexity.

## Verdict

Clearly the old Java code has issues. Compared to the modern Scala version,
it is 2.5x as long, follows no community conventions, uses out of date
techniques, and has poor organization. Not entirely surprising, given that it
was written by some 14-year-old kid, in 2004 Java, with no community, training
or experience! Despite it's flaws, code is never going anywhere.
It was written 12 years ago by a younger, less experienced me, and served it's
purposes as a stepping stone for me to learn more about programming.

As described in the beginning, the point of this exercise is not to criticise
but to learn. To see how the thought process has evolved since the code was
written 12 years ago, what has changed and what has stayed the same. Far from
just calling it "bad code", we've gone through several concrete issues that
exist in that small 300-line codebase, and how they would be resolved by
real software engineers with modern programming languages and best practices.

Furthermore, I think that this investigation reveals things about the progress
of the field of software engineering between 2004 and 2016. For all the
complaints about the plethora of Javascript MVC frameworks, "clever tricks"
like the Virtual Dom really are solving fundamental, decades-old problems.
Programming languages have improved: features like optional arguments, lambdas,
and generics in statically-typed languages are now table stakes, and
demonstrably improve the code from the awkward patterns that were necessary
back in 2004.

Hopefully you've enjoyed the journey, and are as fascinated as I am by the
code and way the field has changed since then. What does the your earliest
surviving code look like? Put it on-line and post a link in the comments!


[Open-Source Scala]: https://github.com/lihaoyi
[Java Swing]: https://en.wikipedia.org/wiki/Swing_(Java)
[Scala.js]: http://www.scala-js.org/
[Euler Angles]: https://en.wikipedia.org/wiki/Euler_angles
[Rotation Matrices]: https://en.wikipedia.org/wiki/Rotation_matrix
[Composition over Inheritance]: https://en.wikipedia.org/wiki/Composition_over_inheritance
[Anonymous Inner Class]: http://stackoverflow.com/questions/355167/how-are-anonymous-inner-classes-used-in-java
[Function Literals]: https://docs.oracle.com/javase/tutorial/java/javaOO/lambdaexpressions.html
[Nested Classes]: https://docs.oracle.com/javase/tutorial/java/javaOO/nested.html
[only being able to access final enclosing fields]: http://stackoverflow.com/questions/4732544/why-are-only-final-variables-accessible-in-anonymous-class
[LEFT RIGHT FORWARD BACK]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L213-L216
[the player]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L209
[asteroids and bullets]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L210-L211
[a timer]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L220
[actionPerformed]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L294
[duplicated for X and Y]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L376-L381
[repetitive casting]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L340-L347
[no apparent reason]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L189-L201
[initialization of the Swing panel]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L232-L253
[Telescoping Constructor]: http://codethataint.com/blog/telescoping-constructor-pattern-java/
[Builder Pattern]: https://en.wikipedia.org/wiki/Builder_pattern
[collision detection loop]: https://github.com/lihaoyi/Java-Games/blob/master/GUI/GameLibrary.java#L329-L352
[Scala collision detection code]: https://github.com/lihaoyi/scala-js-games/blob/master/src/main/scala/example/Asteroids.scala#L36-L56
[Virtual Dom]: http://tonyfreed.com/blog/what_is_virtual_dom
[React.js]: https://facebook.github.io/react/
[Google Go]: https://golang.org/
[Ruby on Rails]: https://en.wikipedia.org/wiki/Ruby_on_Rails
[Django]: https://en.wikipedia.org/wiki/Django_(web_framework)
[Facebook had just started]: https://en.wikipedia.org/wiki/Facebook
[The Scala language barely existed]: https://en.wikipedia.org/wiki/Scala_(programming_language)
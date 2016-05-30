A "Build tool" is a catch-all term that refers to anything that is needed to
get a piece of software set up, but isn't needed after that. Different
programming communities have a wealth of different tools: some use stalwarts
like `make`, some use loose collections of `.sh` scripts, some use XML-based
tools like Maven or Ant, JSON-based tools like Grunt, or code-based tools like
Gulp, Grunt or SBT.

Each of these tools does different things and has a different set of trade-offs
associated with them. Given all these different designs, and different things
each tool does, what are the common features that build tools provide that
people want? How to existing tools stack up against the things that people
want build tools to do?

-------------------------------------------------------------------------------

This post will go through some common use cases for build tools, pick out and
discuss common features, see which boxes existing tools check, and discuss
their strengths and weaknesses. Hopefully in the process, you'll get a good
sense of what the frankenstein "essence" of a build tool really is, and if you
ever decide to go and write your own you'll be well prepared.

I am not an expert in all the tools presented here, so if there are mistakes 
anywhere in this post, feel free to post a correction in the comments and I'll
update the post accordingly!


## At A Glance

- [Use Cases for Build Tools](#use-cases-for-build-tools)
    - [Production Deployment](#production-deployment)
    - [Continuous Integration](#continuous-integration)
    - [Developer Environments](#developer-environments)
- [Common Features In Build Tools](#common-features-in-build-tools)
    - [Running ad-hoc commands][1]
    - [Knowing how to order the execution of commands based on dependencies][2]
    - [Caching command output if its inputs don't change][3]
    - [Parallelize different commands][4]
    - [Watch for file-changes and running commands based on them][5]
    - [Using external processes, including compilers][6]
    - [Being used by external processes][7]
    - [Allowing configuration, re-configuration, re-re-configuration][8]
    - [Download dependencies][9]
- [Analyzing Build Tools](#analyzing-build-tools)
    - [Shell Scripts](#shell-scripts)
    - [Make](#make)
    - [Ant](#ant)
    - [Maven](#maven)
    - [Rake](#rake)
    - [Grunt](#grunt)
    - [Gulp](#gulp)
    - [SBT](#sbt)
- [Round Up](#round-up)

## Use Cases for Build Tools

For the purposes of this post, I'm going to draw an arbitrary line and say that
build tools are used in

- [Production Deployment](#production-deployment): where you want to set up
  the codebase once, stop changing it, and ship the finished product. Whether
  you are shipping it to web servers or shipping it on CDs to install on your
  customers' laptops.

- [Continuous Integration](#continuous-integration): where you are running
  a relatively large test suite on a codebase, perhaps once every several
  minute, with the code only changing between runs of the suite.

- [Developer Environments](#developer-environments): where you are
  interactively working and changing a codebase, several times a second, and
  want it to execute many small actions (whether manually or via unit tests)
  in between modifying the code.

This is arbitrary, but it should encompass a large portion of where people
consider using "build tools". For example

### Production Deployment

Using a build tool in a production deployment is relatively straightforward:
you start off with a clean checkout of the source code, and compile whatever
you need and generate whatever files you need in order to create a complete,
executable environment. You often don't care how responsive things are because
this only happens perhaps once-a-week or once-a-day or once-an-hour, so as long
as it doesn't take *hours* to perform it's fine.

Nevertheless, the build tool still has a lot of work to do. In particular, it
has to:

- Be able to run arbitrary commands: every deployment scenario is different,
  everyone will need files copied to a different folder, zipped in a different
  format, or sanitized in a different way.

- Use the results of one command in another: building is almost always a
  multi-step process. You may need to:

  - Generate source files from some [IDL]
  - Compile your hand-written source files, together with the generated
    files, to form your executable
  - Pre-process your non-code resources: e.g. generating sprite-sheets or
    [mipmaps] from images.
  - Collect your executable, along with whatever other files it needs at
    runtime (images, configuration, music, 3D models, ...) in a folder
  - Compress that folder into whatever format is required (gz, zip, [MPQ], ...)

  You thus have to make sure you perform every step, in order, and feed the
  outputs from one command into the inputs of another command that needs them.

- Allowing configuration, re-configuration, re-re-configuration: you very often
  need to build your code for deployment with just-one-slight-change. Also, you
  may find yourself having multiple modules that are similar, but each needing
  their own custom tweaks to the configuration.

- Using external commands: the build tool virtually never has everything
  needed to build and deploy your source code built-in. It will need to shell
  out to other programs, perhaps to compile code, manipulate images, compress
  files, or other things.

- Being used *by* external commands: the build tool is never the "only" tool
  in town. While it can probably be run directly, inevitably it ends up being
  hooked by some other tool: you may have [Jenkins] automatically kick off
  build-&-deploys when it gets a tagged commit, a cron job that kicks off a new
  build-&-deploy every night, a supervisor process that keeps the build tool
  running, among an army of other tools, and restarts it when it falls downs.

- Download dependencies & compile stuff: last of all, the build tool needs to
  be able to download your code's dependencies and compile your code.

Naturally, there are always edge cases. For example, Google is famous for
performing remote caching of their build artifacts due to how long it takes,
while for most smaller codebases you can get away with a clean build every
time. Similarly, there are organizations that deploy every commit to
production, blurring the line between "Production" and "CI" builds.
Nevertheless, the above should describe a relatively typical workload for a
"production build".

### Continuous Integration

In [continuous integration], you are running a build-&-test flow across every
commit which lands in your repository, and perhaps even for commits that
haven't yet landed. You may be using TravisCI, CircleCI, Jenkins, or your own
company-internal system to do so.

The continuous integration (CI) workload is similar to the production
deployment workload, but with a few twists:

- Whatever frequency deployments happen, CI builds often happen 10x or 100x
  more frequently

- While deployments tend to happen one at a time, it's not unusual to have CI
  builds happening concurrently, with 10s to 100s happening at the same time

- While deployments just involve preparing the code so it can be run, CI
  involves running your code against a test suite of some sort.

Thus, the "CI" build often has a different set of requirements than the
"production" build: it needs to be faster, since it's happening so often. It
needs to be able to isolate its builds from other builds, which may be running
different versions of the code on the same cluster or even the same machine.

Therefore you end up looking for features like:

- Parallelizing unrelated commands: if you have 8 cores and have multiple
  unrelated commands (Compiling [SCSS]? Compiling Javascript?) that take less 
  than 8 cores each you can run them in parallel and save some time

- Caching command results: if your CI build is slow, you may want to avoid
  needlessly re-computing parts of it to speed it up. For example, third-party
  binaries are often `apt-get install`ed once and then used for multiple CI
  builds, and third-party dependencies from e.g. [Maven Central] are often
  downloaded once and then kept around.

Not every company's or individual's CI system looks the same; not every company
or individual even has CI! Nonetheless, I would argue that this is a relatively
representative sample of the kind of workload build-systems get in automated
builds.

### Developer Environments

Using a build tool in development environments is perhaps the most
demanding of the three use cases described. In a development environment, a
programmer is actively making changes to the codebase, and then executing code
to see if the changes have the effect they want. They could be navigating to
a local website in the browser that's running their copy of the code and
clicking around, or they could be running a unit test (or a whole suite), or
they could be opening a REPL, running code interactively, and inspecting the
output there.

Regardless of how the programmer is running code, the build system needs to
get the code ready to run the first time, as well as constantly "updating" the
system as the code changes underneath. Any compiled executables may need to be
re-compiled, any generated files may need to be re-generated. When the build
tool is done updating the system, the programmer should be able to interact
with the system as if the code was always in the current state. And the build
tool should ideally work in a fraction of a second: after all, the programmer
is sitting there waiting for you!

Given this scenario, these are the features you want your build tool to have,
on top of what you already want for CI and Deployment purposes:

- Caching command results: while in CI this happens sometimes, during local
  development this is basically mandatory. Nobody wants to wait 5 minutes for
  a clean build just because they added a print statement. Ideally it would
  take less than 1s to incrementally build things and bring the system up to
  date.

- Parallelizing unrelated commands: for performance. For the same reason as
  caching, this is even more important during local development.

- Running commands in response to file changes: this is unique to the local
  development workflow. While in Deployment and CI the code being built is
  static, here it's constantly changing. Most build systems have some kind
  of polling/filesystem-notification based watcher that saves you the hassle
  of running "build" every time you save.

## Common Features In Build Tools

From the three primary use cases above, here is the list of things that we
could conceivably ask a build system to do:

1. [Running ad-hoc commands][1]
2. [Knowing how to order the execution of commands based on dependencies][2]
3. [Caching command output if its inputs don't change][3]
4. [Parallelize different commands][4]
5. [Watch for file-changes and running commands based on them][5]
6. [Using external processes, including compilers][6]
7. [Being used by external processes][7]
8. [Allowing configuration, re-configuration, re-re-configuration][8]
9. [Download dependencies][9]

This isn't an exhaustive list, but it should be a pretty good sampling. This
section will discuss each of these features in greater detail.

### Running ad-hoc commands

The basic requirement here is that you need to be able to run ad-hoc code as
part of your build process. The list of things you may want to run are
infinite, but includes things like:

- Generating serialization code from IDL files
- Wiping the database of the development web-server
- Running migrations (in either production or development)
- Packaging and compressing the executable for deployment

There are a lot of things that you can possibly want to do with your code
during a "build" in development, CI, and production. Many of them don't apply
at all at runtime: they are purely a concern during developing, building and
packaging your code.

It's possible for this requirement to be satisfied by a bunch of
[Shell Scripts](#shell-scripts) used together, a simpler build tool which then
does not need to do this, but that has its downsides. For example, it means
that the build tool is then unaware of where these commands fit in to the
larger scheme of things. For example, if compilation of the "main" codebase
depends on the code generated from IDL files, you will then have to manually
ensure you run this code-generation script every time the IDL files change.

### Knowing how to order the execution of commands based on dependencies

The requirement here is that you should be able to take the output of one
command and feed it into another command. Furthermore, you should not need to
do the feeding yourself: if I have two commands

- `commandA` depends on `commandB`
- `commandB` depends on nothing

I should be able to ask the build tool to run `commandA`, and it should know
that it will need to run `commandB` first to make it work. Similarly, if I ask
it to run `commandB` alone, it should know not to run `commandA`.

In a trivial example it's easy enough to do it manually and remember what order
to do things in. But when your build grows, and you have things like:

- `test` depends on `package` and any files in the `test/` folder
- `run` depends on `package`
- `package` depends on `compile` and `resources`
- `compile` depends on `generateIDL` and any file in the `src/` folder
- `resources` depends on any file in the `resources/` folder

Remembering to do the right things in the right order becomes impossible. You
will pull down a patch, which will result in 100 files changing throughout the
project, and you want to run `test` while not doing any redundant work since
every task in this build may take 10+ seconds, and not *forgetting to do any
work* since stale results from `generateIDL` or `compile` or `resources` will
result in obscure hard-to-debug breakages. In this sort of scenario, you would
be very thankful if the build tool would do all the book-keeping for you and
not make any mistakes!

### Caching command output if its inputs don't change

This is the other common thing build tools do: they avoid doing redundant work.

It is easy to have a simple `test.sh` script that does everything, every time:

```
# test.sh
./bundleResources.sh
./compile.sh
./package.sh
./runTests.sh
```

However, this would end up being very slow if you redundantly kept re-bundling
resources and re-compiling code if we didn't need to. The build tool should
be able to tell what changed and what didn't, and only do the minimal amount
of work necessary.

This doesn't matter so much for production deployments, since you tend to run
"everything", once, and be done with it. But it does matter a lot for people's
development environments, where they're constantly making small tweaks and
wanting to get back to work as soon as possible without unnecessary waiting.

### Parallelize different commands

If we return to the earlier example-dependency-graph:

- `test` depends on `package` and any files in the `test/` folder
- `run` depends on `package`
- `package` depends on `compile` and `resources`
- `compile` depends on `generateIDL` and any file in the `src/` folder
- `resources` depends on any file in the `resources/` folder

If I haven't done anything, and I ask the build tool to `package`, it should be
able to perform `compile`/`generateIDL` and `resources` in parallel. After all,
those two tasks do not depend on each other! In this case it could cut the time
I spend waiting in half, and in a larger build with more items the savings
would be even greater.

Again, it doesn't matter as much if you're running the build once a day for
deployment. But if you're running it 4 times a minute any unnecessary slowness
results in frustration that adds up quickly!

### Watch for file-changes and running commands based on them

Given this dependency graph:

- `test` depends on `package` and any files in the `test/` folder
- `run` depends on `package`
- `package` depends on `compile` and `resources`
- `compile` depends on `generateIDL` and any file in the `src/` folder
- `resources` depends on any file in the `resources/` folder

If I edit a file in my hypothetical `src/` directory, and I'm currently
doing a `run`, I want it to automatically re-`compile`, re-`package` and
re-`run` without me having to flip over to the terminal and do stuff manually.

This can be done at a coarse grain, with sufficiently smart caching: if any
file changes, re-do "everything", and let the caching do the work of figuring
out which tasks don't *actually* need to be re-done. On the other hand, finer
grained file-watching (kicking off different commands based on different files)
would speed thing up over that coarse grained analysis:
you wouldn't need to repeatedly check dozens of caches for invalidation,
and can instead just do the small amount of work you know you need to do.

### Using external processes, including compilers

As builds grow, they tend to require all sorts of things that are not part
of the build tool:

- Compilers, which often live outside the build-tool. Often a build tool may
  bundle the compiler for that community's language, e.g. [Maven](#maven) 
  bundles Javac or SBT bundles Scalac. But as the build grows, it may end up 
  containing a half dozen different compilers, e.g. for Java, for SCSS, for 
  Javascript ES6, for `.proto` or `.thrift` files. At some point these will 
  end up being external

- Packagers: whether simple things like `gzip` or `zip`, or just `cp`-ing
  things into a folder, or running Java bytecode through [Proguard] and 
  Javascript through [UglifyJS].

There are many other things that a build-tool will end up needing, too many
for it to contain them all. A build tool thus should be able to work easily
with external programs, even those it has no knowledge of, and integrate them
into it's build.

### Being used by external processes

A build tool is rarely the be-all end-all of building the project. Inevitably
it ends up being used by other tools:

- Your CI system: TravisCI, CircleCI, Jenkins, etc. all want to ask your
  build tool to do things

- Your IDEs: IntelliJ, Eclipse, etc. all would like to extract project
  information from your build tool.

- A larger developer environment, with its own supervisor process and triggers,
  that ends up managing your build tool as one of many

In all these cases, the requirements are relatively straightforward: you need
your build-tool to be accessible programmatically. Whether it's being able
to programmatically run tasks or programmatically inspect the layout of the
project, it needs to be doable from an external process relatively efficiently
and easily.

### Allowing configuration, re-configuration, re-re-configuration

Builds are probably some of the most configurable pieces of software. It is not
uncommon to have "just one tweak" you want to control using a flag, and needing
that flag to get propagated throughout the build into multiple sub-processes
(e.g. compilers, packagers). Examples include:

- Using different compression formats: `.gz` on linux, `.zip` on windows?
- Compiling for different processors: ARM vs x86?
- Debug builds: debug symbols enabled? Profiling? Optimizations enabled?
- Enabling/disabling features: "Home" edition vs "Professional" vs "Enterprise"

There's a lot that you could want to configure in a build, and a build tool
needs to let you configure it in a reasonable way.

### Download dependencies

Lastly, a build tool should be able to do the language-specific work of
downloading dependencies. As mentioned earlier, the compiler is often just
another executable that the build tool shells out to, but the dependency
management is often language-specific. While there are attempts to make
generic, language agnostic dependency management systems like Nix, or
`apt-get` or `yum` on various linuxes, for many people working within a
single Java, Node.js or Python program, it's more likely you'll be getting the
bulk of your dependencies from your respective Maven Central, npm, or PyPI
repositories.

Traditionally, a you needed to install these dependencies manually beforehand,
e.g.

```
sudo pip install requests
sudo pip install Pillow
sudo pip install numpy
sudo pip install SQLAlchemy
sudo pip install six
sudo pip install simplejson
python test.py
```

and running your program without doing so would result in arbitrary
`ImportError`s. And as the dependencies change, you would need to make sure to
run the right command to install the new stuff, or you're going to be back to
`ImportError`s again!

With a build-tool managing this, you would do something like

```
sbt test
```

Where and it would automatically pull down everything necessary for the project
to be tested the first time, and not bother doing so the second and subsequent
times.


## Analyzing Build Tools

In this part of the post, I will go over a selection of build tools I've seen.
I am not deeply familiar to all of them, so there may be mistakes

This is not a comprehensive listing, but it will give a sense of how build
tools across a variety of languages and communities fare against the
requirements we listed above.

- [Shell Scripts](#shell-scripts)
- [Make](#make)
- [Ant](#ant)
- [Maven](#maven)
- [Rake](#rake)
- [Grunt](#grunt)
- [Gulp](#gulp)
- [SBT](#sbt)

Note that this list is in no way exhaustive; in particular, it focuses a lot
more on "language" build tools: for building application code, in one or a
small number of languages. In particular, it ignores a number of other similar
tools that can be classified as "build tools", e.g.

- Language-"agnostic" build tools like Twitter's [Pants], Facebook's [Buck] or
  Google's [Bazel]
- System-provisioning tools, like [Puppet] or [Salt] or [Ansible]

These other tools are just as valid considerations as those above, and I just
leave them out for brevity. Perhaps in a future blog post I can cover these
cases!

### Shell Scripts

```bash
sudo su

# install a whole lot of stuff
apt-get install --force-yes openssh-server
apt-get install --force-yes ack-grep

apt-get install --force-yes openjdk-7-jdk
apt-get install --force-yes vim
apt-get install --force-yes git
apt-get install --force-yes zip

# setup samba share
apt-get install --force-yes samba
cat <<EOF >> /etc/samba/smb.conf
[HostShare]
   path = /
   guest ok = yes
   public = yes
   writable = yes
   force user = lihaoyi
EOF
restart smbd

exit
```
These are probably the first "build tool" that you will bump up against: after
setting things up manually the first time, the obvious next step is to put the
setup into a shell script so you can run it over and over. At some point, it
might grow into a collection of shell scripts, or even a Python script, but the
overall setup doesn't change: you have a collection of commands that are run
every time you want to do something.

So how does that stack up against the requirements I described above?

1. [Running ad-hoc commands][1]: **Good**. You can
   kick off basically any command you could imagine, from anywhere in the
   script, with minimal fuss.

2. [Knowing how to order the execution of commands based on dependencies][2]:
   **Poor**. Loose scripts tend to "just do things", and dependencies between
   the things they're doing is implicit. If you put things in the wrong order,
   things just fail in ad-hoc ways.

3. [Caching command output if its inputs don't change][3]: **Poor**. It's
   relatively annoying to make loose scripts properly cache things and avoid
   redundant work. It's definitely possible, e.g. by having code that `mtime`s
   every source file before determining whether or not to recompile, but it's
   hard to get right and often not done at all.

4. [Parallelize different commands][4]: **Okay**: It's easy to parallelize
   things in loose scripts, even if it's not done automatically. If you *know*
   that two commands can happen independently, you can easily parallelize them
   using Bash's `&` syntax

5. [Watch for file-changes and running commands based on them][5]: **Poor**.
   Loose scripts just don't do this by default. You could hack something
   together using Watchman or Watchdog, but it's a pain to get right

6. [Using external processes, including compilers][6]: **Good**. It's really
   easy to kick off external processes in a shell script.

7. [Being used by external processes][7]: **Good**. It's equally easy to kick
   off shell scripts from external processes.

8. [Allowing configuration, re-configuration, re-re-configuration][8]:
   **Okay**. You can reconfigure things and through environment variables and
   have them automatically propagate everywhere. It's not *great*, but it's
   not any worse than anything else in shell-script-land

9. [Download dependencies][9]: **Poor**. You can hard-code it, but it's just as
   likely to either fall out of sync because you didn't download enough, or
   become really slow when you download too many.

In general, shell scripts do an OK job at being a build tool, but not great.
Their great advantage is their convenience: really easy to get started, really
easy to start using other tools or to be used from other tools, and can even
(surprisingly?) let you parallelize parts of your build acceptably, if
manually. On the other hand, they don't have a model of "what depends on what"
inside your build, and so do a lousy job at ensuring things are run in the
right order, or ensuring you aren't doing redundant work.

### Make

```make
all: hello

hello: main.o factorial.o hello.o
    g++ main.o factorial.o hello.o -o hello

main.o: main.cpp
    g++ -c main.cpp

factorial.o: factorial.cpp
    g++ -c factorial.cpp

hello.o: hello.cpp
    g++ -c hello.cpp

clean:
    rm *o hello

```

Make is a 40 year old build automation tool from the Unix/C tradition. It lets
you define multiple shell commands you can do, but more than that it lets you
define the inputs and output for each command as a sequence of files. For each
of the bullets above, the word before the `:` (e.g. `hello`) is the label of
the command, and the files after are the files or targets which that command
depends on.

That means that if you run `hello`, it will ensure that `main.o`,
`factorial.o` and `hello.o` are all pre-compiled before it links them all
into `hello`. Furthermore, if the timestamps on the already-generated files
are more recent than that of the source files, they will be re-used and
re-compilation will be avoided.

So how does that stack up against the requirements I described above?

1. [Running ad-hoc commands][1]: **Good**. Make rules are literally ad-hoc
   commands, and you can make a rule do anything as long as it generates
   files on the filesystem somewhere

2. [Knowing how to order the execution of commands based on dependencies][2]:
   **Okay**. The dependencies between targets is specified, but there's nothing
   ensuring that it matches up with the *real* dependencies of each target.
   It's up to the programmer to "know" what the dependencies and outputs of
   each target is: this both results in some duplication (need to specify
   input files as args to each command, as well as the same files as
   dependencies of the target) as well ask risk you'll mess up. Still, it's
   far better than Bash

3. [Caching command output if its inputs don't change][3]: **Good**. Assuming
   you got the dependencies right, Make does this transparently by default

4. [Parallelize different commands][4]: **Okay**: There's no automatic way to
   parallelize different targets, though you can still parallelize a single
   target using Bash.

5. [Watch for file-changes and running commands based on them][5]: **Poor**.
   Make doesn't do this

6. [Using external processes, including compilers][6]: **Good**. Make files are
   all about kicking off external processes

7. [Being used by external processes][7]: **Good**. `make target` is easy to
   run

8. [Allowing configuration, re-configuration, re-re-configuration][8]:
   **Okay**. You can reconfigure things using environment variables like Bash,
   as well as through macros, or passing arguments to `make`, but it's
   sufficiently annoying that many people use Autoconf to write out
   machine-generated make files instead.

9. [Download dependencies][9]: **Poor**. It usually ends up being punted to
   other tools like `./configure`

In practice, this fixes two large problems with Bash Scripts - the dependency
execution order and caching - while still being relatively close to what Bash
in most other ways. It integrates trivially with third party programs, and
often you can take whatever you would have run at the command-line and paste
it directly into a make target. Nevertheless, many of the other problems still
remain, and you get the additional annoyance of working with its own, strange
syntax.

### Ant

```xml
<?xml version="1.0"?>
<project name="Hello" default="compile">
    <target name="clean" description="remove intermediate files">
        <delete dir="classes"/>
    </target>
    <target name="clobber" depends="clean" description="remove all artifact files">
        <delete file="hello.jar"/>
    </target>
    <target name="compile" description="compile the Java source code to class files">
        <mkdir dir="classes"/>
        <javac srcdir="." destdir="classes"/>
    </target>
    <target name="jar" depends="compile" description="create a Jar file for the application">
        <jar destfile="hello.jar">
            <fileset dir="classes" includes="**/*.class"/>
            <manifest>
                <attribute name="Main-Class" value="HelloProgram"/>
            </manifest>
        </jar>
    </target>
</project>
```

[Ant](http://ant.apache.org/) scripts are basically isomorphic to shell 
scripts: they are effectively
shell scripts with an XML syntax implemented in Java, but function more or less
identically: you run `target`s with the XML (equivalent to bash functions) and
it executes the commands within from top to bottom.

Ant has the following main differences from shell scripts

- It's all XML, and is several times more verbose than Bash commands
- It's cross platform Java, which means it runs on Windows with Java installed
- You can specify that a target depends on another target, similar to
  [make](#make)

It's almost like a make-file, converted into XML, running on the Java Virtual
Machine rather than using Unix shell commands. The task-execution model is
similar to Make, and the innards of each command is similar to Bash, just
converted into a verbose XML syntax.

You also gain the ability to run "anywhere with Java", but lose the ability to
run "anywhere with Unix", so overall the cross-platform-ness of Ant scripts
isn't as much a gain as a sideways-change. It allows usage by some new
developers (e.g. those on Windows) at the expense of others (those on Unix
without a JVM).

### Maven

```xml
<project>
  <!-- model version is always 4.0.0 for Maven 2.x POMs -->
  <modelVersion>4.0.0</modelVersion>

  <!-- project coordinates, i.e. a group of values which
       uniquely identify this project -->

  <groupId>com.mycompany.app</groupId>
  <artifactId>my-app</artifactId>
  <version>1.0</version>

  <!-- library dependencies -->

  <dependencies>
    <dependency>

      <!-- coordinates of the required library -->

      <groupId>junit</groupId>
      <artifactId>junit</artifactId>
      <version>3.8.1</version>

      <!-- this dependency is only used for running and compiling tests -->

      <scope>test</scope>

    </dependency>
  </dependencies>
</project>
```

[Maven](https://maven.apache.org/) is another build tool from the Java 
community. While it's XML-based like Ant, that's where the similarity ends.

Rather than describing a build as a sequence of commands to run in response to
each named target, Maven describes your build as a set of *modules*. Each
module lists out metadata such as:

- It's coordinates: it's name, groupId (i.e. author's ID) and version
- It's dependencies, whether local or external

Along with a list of *phases*

1.   validate
2.   generate-sources
3.   process-sources
4.   generate-resources
5.   process-resources
6.   compile
7.   process-test-sources
8.   process-test-resources
9.   test-compile
10.  test
11.  package
12.  install
13.  deploy

You run commands via

```
mvn test
```

Which automatically will run all necessary phases in all modules up to the
`test` phase, which will run the unit tests for you.

You can also run tests for a single module

```
mvn -pl submodule test
```

Which will run all phases in the `submodule` submodule, as well as all modules
that it depends on, before running its tests.

In general, if you have a task that you want to do that does not fit into the
default set of phases, you need to write a Maven Plugin to do so.

There's a lot more to Maven than can be described in a few paragraphs, but
overall how does it fare against the list of features we decided we wanted in
a build tool?

1. [Running ad-hoc commands][1]: **Poor**. Maven actually makes it relatively
   clunky to have an extra phase to "do something" that's not part of the
   default. You have to go through the rigmarole of defining a plugin and then
   using the plugin in your build, which is no-where near as convenient as
   writing an ad-hoc shell script to do something.

2. [Knowing how to order the execution of commands based on dependencies][2]:
   **Good**. Maven knows what the order of commands is and can ensure it runs
   all the phases necessary before running the phase you want. You won't find
   yourself running `mvn test` and having it crash because you forgot to run
   some other command earlier.

3. [Caching command output if its inputs don't change][3]: **Good**. Maven does
   this.

4. [Parallelize different commands][4]: **Good**. Maven didn't let you do this
   originally, but in Maven 3 the ability to run builds in parallel appeared:

   ```
   mvn -T 4 install -- will use 4 threads
   mvn -T 1C install -- will use 1 thread per available CPU core
   ```

5. [Watch for file-changes and running commands based on them][5]: **Okay**.
   Maven doesn't do this by default, but there's a Maven Plugin that does it
   for you without too much difficulty in
   https://github.com/rzymek/watcher-maven-plugin. It doesn't let you run
   arbitrary goals when things change, but at least you can `watcher:run`

6. [Using external processes, including compilers][6]: **Okay**. Maven comes
   with the Java compiler built-in, but doing anything else requires custom
   plugin code. It's not hard to write, but definitely less convenient than
   invoking something from Bash.

7. [Being used by external processes][7]: **Good**. Maven's "dumb" XML config
   means that it's easy for third-party IDEs like IntelliJ to inspect the build
   and know how a project is laid out

8. [Allowing configuration, re-configuration, re-re-configuration][8]:
   **Good**. Maven supports [POM Inheritance], allowing you to configure things
   once and re-use the configuration, with tweaks, in multiple modules.

9. [Download dependencies][9]: **Good**. Maven does this automatically, and
   treats remote dependencies the same as it does local dependencies. You
   shouldn't ever need to worry about manually downloading or installing `.jar`
   files when dealing with Maven projects.


### Rake

```ruby
task :build_refact => [:clean] do
  target = SITE_DIR + 'refact/'
  mkdir_p target, QUIET
  require 'refactoringHome'
  OutputCapturer.new.run {run_refactoring}
end

file 'build/dev/rake.html' => 'dev/rake.xml' do |t|
  require 'paper'
  maker = PaperMaker.new t.prerequisites[0], t.name
  maker.run
end
```

[Rake](http://rake.rubyforge.org/) is a contraction of "Ruby Make", and 
its overall structure is very similar
to that of a [Makefile](#make): you define tasks, each one "doing something",
and define the dependencies between them. Once that's done, you can run
individual tasks using the `rake` executable similar to how you run Makefile
targets using `make`.

The primary differences from Make arise from the fact that your tasks are
defined in Ruby rather than in its own ad-hoc language.

- This introduces some syntactic cruft (e.g. needing `'` quotes around file
  names, `:`s before each task name, or `do` `end`s everywhere) and removes
  some (no more problems with tabs vs spaces, or file-names with spaces in
  them!).

- You get a more advanced language if you want to perform "real logic" in your
  rake files. In Make, any "real logic" involved in a single target would
  traditionally be punted into separate scripts the Makefile calls. On the
  other hand, "real logic" involving *the entire build* does not have such a
  convenient way to encapsulate it, and often people resort to having a
  separate script that generates a makefile to execute. With Rake, all this
  logic can comfortably live in the same Rakefile, in the same language as
  the rest of your build

In addition to these things, the Ruby world has standardized on the `gem` tool
for pulling down dependencies, so although it's not strictly part of Rake
(it used to be) for all intents and purposes the "downloading dependencies"
problem is solved for people using Rake in Ruby codebases

Overall, the main contribution Rake makes to the space of build tools is the
idea that your build tool can be in a real, concise, high-level programming
language. When programs were written in C or Java, code is so verbose that it
seems unreasonable to write your build rules in C or Java. As a result, you'd
rather invent your own syntax like Make, write everything in XML like Ant or
Maven, or just rely on loose collections of shell scripts to do what you want.

Rake shows that with Ruby, you can in fact have a full-blown programming
language inside your build tool, while still being almost (though not quite) as
concise as your own hand-crafted syntax, and still getting all the "nice"
dependency-tracking and other features that traditional custom build-languages
or build-XML-files provided.

### Grunt
```js
module.exports = function(grunt) {

  grunt.initConfig({
    jshint: {
      files: ['Gruntfile.js', 'src/**/*.js', 'test/**/*.js'],
      options: {
        globals: {
          jQuery: true
        }
      }
    },
    watch: {
      files: ['<%= jshint.files %>'],
      tasks: ['jshint']
    }
  });

  grunt.loadNpmTasks('grunt-contrib-jshint');
  grunt.loadNpmTasks('grunt-contrib-watch');

  grunt.registerTask('default', ['jshint']);

};
```

[Grunt](http://gruntjs.com/) is pretty similar to Maven in its overall 
structure: you pass in an
almost-dumb-struct to the `grunt.initConfig` function, and that configures
the entire build telling it where files are, what sources need to be compiled,
where the output should go, what files it needs to watch, and so on. It's not
*quite* a dumb struct, as you end up having some simple logic e.g. the
`'<%= jshint.files %>'` string above, which will get evaluated based on the
value of the `jshint.files` configuration option.

Like Maven, and unlike Ant or Make or Shell Scripts, a Grunt build doesn't
contain imperative code. All the code that actually does the work of dealing
with files or shelling out the `jshint` is pushed to plugins, e.g.
`grunt-contrib-jshint` and `grunt-contrib-watch` above, and the "main" build
configuration only deals with configuring these plugins.

So how does that stack up against the requirements I described above?

1. [Running ad-hoc commands][1]: **Okay**. While most plugins work via the
   configuration passed in through `grunt.initConfig`, you can also write
   custom tasks via `grunt.registerTask` to do ad-hoc work.

2. [Knowing how to order the execution of commands based on dependencies][2]:
   **Poor**. While grunt lets you define tasks, you [cannot define
   dependencies](https://github.com/gruntjs/grunt/issues/968) like you can in
   Make or Rake, nor does Grunt have a standardized-lifecycle like Maven
   that would allow you to make sure things run "in the right order" via their
   placement in the lifecycle.

3. [Caching command output if its inputs don't change][3]: **Poor**. Grunt
   doesn't provide any support for caching by default. It's left to the
   individual plugins to do their own caching and avoid redundant work.

4. [Parallelize different commands][4]: **Poor**: Grunt doesn't run things in
   parallel. There are plugins that attempt to do this, but none of them are
   widespread or ubiquitous

5. [Watch for file-changes and running commands based on them][5]: **Good**.
   Grunt doesn't do this either, but unlike the plugins for parallelizing
   tasks, the `grunt-contrib-watch` plugin is pretty ubiquitous. Everyone is
   using it, and it works.

6. [Using external processes, including compilers][6]: **Poor**. You need to
   write grunt plugins for all the various external processes you want to kick
   off.

7. [Being used by external processes][7]: **Good**. Calling `grunt foo` is
   easy and fast.

8. [Allowing configuration, re-configuration, re-re-configuration][8]:
   **Good**. While you can't easily write Javascript code to *do things* in
   your grunt build, it is pretty easy to use Javascript code to *configure
   things*. The `grunt.initConfig` function is just a Javascript call, and you
   can perform whatever logic you want to configure and re-configure the build
   before you pass it to Grunt.

9. [Download dependencies][9]: **Good**. Perhaps not by virtue of Grunt, but
   most people using Grunt are using Javascript which has NPM, which does a
   decent job at downloading dependencies.

Grunt Is both similar and different from the tools we saw earlier. It
provides the ability to define ad-hoc tasks like Ant/Make/Rake, while at the
same time providing a "Build Config" structure similar to what you have in
Maven where all your config lives in one XML tree.

Unlike Ant/Make/Rake, Grunt does not allow you to easily define
dependencies between tasks. Thus even if you-as-a-programmer *know* that
`taskA` depends on `taskB`, Grunt only allows you to fail-if-`taskB`-is-not-run
via `this.requires('taskB')`, and doesn't support running `taskB`
automatically. The tasks and plugins also do not fit into any sort of standard
"lifecycle" like they do in Maven, which would have allowed coarse-grained
ordering.

Like [Rake](#rake), you can define ad-hoc tasks and perform build configuration 
in a high-level language (Javascript) rather than XML or some custom syntax. 
Grunt is also probably one of the earlier build systems that supports the 
"live" watch-files-and-run-tasks workflow, avoiding needing to keep re-running 
`grunt` commands manually while you work.

### Gulp

```js
gulp.task('less', function () {
  return gulp.src('./client/styles/*.less')
    .pipe(less({paths: [path.join(__dirname, 'client', 'styles')]}))
    .pipe(gulp.dest('./dist/styles'))
    .pipe(refresh());
});

gulp.task('clean', function () {
  return gulp.src('./client/.index.js', { read: false })
    .pipe(clean());
});

gulp.task('emberate', ['clean'], function () {
  return emberate('./client', { pods: true })
    .pipe(source('.index.js'))
    .pipe(gulp.dest('./client'));
});

gulp.task('browserify', ['emberate'], function () {
  return browserify('./client/.index.js')
    .bundle()
    //Pass desired output filename to vinyl-source-stream
    .pipe(source('application.js'))
    // Start piping stream to tasks!
    .pipe(gulp.dest('./dist/scripts/'))
    .pipe(refresh());
});

gulp.task('watch', function () {
  gulp.watch('./client/styles/*.less', ['less']);
  gulp.watch('./client/**/*.{js,hbs}', ['browserify']);
});

gulp.task('default', ['less', 'browserify', 'watch']);
```

[Gulp](http://gulpjs.com/) is a popular alternative to Grunt in the Javascript 
build landscape.
Rather than having the triple of Config/Plugins/Tasks, Gulp basically *only*
has Tasks. Like Rake, tasks are defined via a `task()` function, which takes
a task name, a function that the task executes, and optionally a list of
dependencies that the task requires.

There are two main novelties to Gulp:

- **Tasks return things**! Almost all build systems up to this point have had
  some notion of a "task", but interaction between build steps was ad-hoc: you
  would write a file to a path, and some other task would read it. In Gulp, the
  tasks return (asynchronous) handles to the files they generate. That allows
  downstream tasks to know when a task is really-truly complete and its results
  are ready to be used.

- **`.pipe` allows you to anonymously combine build steps**. While many build
  systems allowed you to define chain multiple steps together, you always had
  to define one task per-step-you-want-to-chain. With Gulp, you can easily take
  the output from a task and put it through another step, without needing to
  manually define any intermediate names.


So how does that stack up against the requirements I described above?

1. [Running ad-hoc commands][1]: **Okay**. You can run arbitrary code, but you
   need to provide a specific interface if you want it to fit into Gulp's
   `.pipe`s.

2. [Knowing how to order the execution of commands based on dependencies][2]:
   **Good**. In Gulp, not only do top-level tasks allow you to specify
   dependencies, *within* a task you can easily use `.pipe` to make sure that
   things happen in the correct order and that the different steps all
   hook up properly and read/write files from the same places

3. [Caching command output if its inputs don't change][3]: **Poor**. Gulp
   doesn't provide any support for caching by default. It's left to the
   individual plugins to do their own caching and avoid redundant work.

4. [Parallelize different commands][4]: **Poor**: Grunt doesn't run things in
   parallel. There are plugins that attempt to do this, but none of them are
   widespread or ubiquitous.

5. [Watch for file-changes and running commands based on them][5]: **Good**.
   Gulp has built-in the ability to watch files and run tasks when they change.

6. [Using external processes, including compilers][6]: **Okay**. You need to
   write gulp plugins for all the various external processes you want to kick
   off.

7. [Being used by external processes][7]: **Good**.

8. [Allowing configuration, re-configuration, re-re-configuration][8]:
   **Good**. It's all Javascript.

9. [Download dependencies][9]: **Good**. NPM does a reasonable job.

In general, Gulp does a much better job at managing dependencies between tasks
than earlier build systems. At a macro-level it provides the same task
definition as Make or Rake or Ant: a Task is a name + dependencies + command,
but within a task it makes it easy to chain together multiple small steps. For
example, the following task:

```js
gulp.task('script', function(){
    return gulp.src('./js/src/*.js')
               .pipe(cached())
               .pipe(uglify())
               .pipe(remember())
               .pipe(concat('app.js'))
               .pipe(gulp.dest('./js/'));
});
```

Takes the `.js` code in a `js/src` folder and passes it through multiple
phases: caching (via `cached` and `remember`), minification (via uglify),
concatenation into a single file, and copying into a destination folder. You
do not need to define files to store these intermediate results in and make
sure they stay in sync (and don't collide with each other!): you just pipe
the stages together and Gulp figures out the rest.

### SBT

```scala
// Set the project name to the string "my-project" and the version to 1.0.0.
name := "my-project"

version := "1.0.0"

// Add a single dependency, for tests.
libraryDependencies += "junit" % "junit" % "4.8" % "test"

// Add multiple dependencies.
libraryDependencies ++= Seq(
  "net.databinder" %% "dispatch-google" % "0.7.8",
  "net.databinder" %% "dispatch-meetup" % "0.7.8"	
)

// Use the project version to determine the repository to publish to.
publishTo := Some(
  if (version.value endsWith "-SNAPSHOT") "http://example.com/maven/snapshots" 
  else "http://example.com/maven/releases"
)

// Apart from the "root" project, a SBT build can have sub-projects with their
// own configuration, or their own custom tasks associated.

val sampleStringTask = taskKey[String]("A sample string task.")
val sampleIntTask = taskKey[Int]("A sample int task.")

lazy val commonSettings = Seq(
  organization := "com.example",
  version := "0.1.0-SNAPSHOT"
)

lazy val library = (project in file("library")).
  settings(commonSettings: _*).
  settings(
    sampleStringTask := System.getProperty("user.home"),
    sampleIntTask := {
      val sum = 1 + 2
      println("sum: " + sum)
      sum
    }
  )
```

[SBT](http://www.scala-sbt.org/) used to be called the "Simple Build Tool", but 
on account of its reputation
of complexity and non-simple-ness the name has been since ret-conned to mean
"Scala Build Tool". It is the primary build tool used by the Scala community,
although some use Maven or Ant and Gradle, and SBT itself supports other JVM
languages (e.g. Java) as well.

SBT is comparatively different from any build tool we've seen so far, and
shares ideas with many of them:

- While earlier tools could be classified roughly into "task oriented" tools
  like Make/Rake/Ant/Gulp and "configuration oriented" tools like Maven/Gradle,
  SBT fuses both ideas: you have a global configuration you can override and
  work with, but configuration values are Tasks rather than simple values.

- Thus "tasks" like "compile code" live in the same configuration as tasks like
  "current version", and you can override both the "compile code" task and the
  "current version" task the same way, and the "current version" can depend on
  other tasks the same way that the "compile code" task can.

- The "configuration" for a SBT project follows a similar pattern to a Grunt
  build: you execute a bunch of code (JS for Grunt, Scala for SBT) to generate
  your "configuration" (a JSON object for Grunt, a list of task `:=`
  assignments for SBT) which is then passed to the engine to evaluate and
  decide what to do. Like how Grunt config values can depend on others via
  `<%= %>` syntax, SBT config values can depend on others via `.value` syntax.

- Like Rake, the body of each SBT task is arbitrary code (though Scala instead
  of Ruby).

- SBT supports watching for file changes and running arbitrary tasks by
  default.


SBT also has some novel ideas that haven't really been seen in earlier tools
discussed in this post.

- SBT includes "multi-module" builds by default, and you can trivially set it
  up to support the same operations (e.g. `test`, `compile`, `publishSigned`,
  whatever) on different modules in a project with only minor differences (e.g.
  different root folders).

- The code for the SBT build file is itself built using SBT; that means build
  "plugins" are just "normal" dependencies of the SBT build-project, and are
  just configured using the build-project's own build (in `project/build.sbt`
  instead of just `build.sbt`)

- SBT tasks return values! And these values are actually used: the primary
  way of depending on another task is to use its `.value`, which both provides
  the value of that task as well as creates the dependency between them. Thus
  there's much less "write something to disk, hope the next task picks it up
  from the right spot": the result of a task is directly handed to the tasks
  that depend on it.

- Partly as a consequence of the above, *everything* is a task! From
  heavyweight processes like compiling and packaging your code, to trivial
  properties like project version, all these are configured, overriden, or
  depended or or made to depend on other tasks in the same way!

- SBT has a more complex execution model than any tool we've seen so far.
  Shell Scripts, Ant or Maven work by "directly" executing things, and other
  tools first execute code to generate a build definition which is then
  executed, SBT adds an additional stage that the Scala code executes to
  generate a *Settings list*, and this Settings list is then executed to
  generate the value for each task, and only then are the tasks executed
  to perform your work.

So how does that stack up against the requirements I described above?

1. [Running ad-hoc commands][1]: **Okay**. You can run arbitrary code, but you
   need to define a `taskKey[T]` and some other boilerplate. It's not terrible,
   but it's slightly inconvenient.

2. [Knowing how to order the execution of commands based on dependencies][2]:
   **Good**. SBT executes things based on the dependency graph between tasks,
   which is known before-hand before any of the tasks run.

3. [Caching command output if its inputs don't change][3]: **Okay**. The built
   in Scala compilation caches and re-uses parts of the compiled code, and
   there is the built-in [FileFunction.cached] helper, but it's awkward to use.

4. [Parallelize different commands][4]: **Good**: SBT runs unrelated tasks in
   parallel based on the dependency graph.

5. [Watch for file-changes and running commands based on them][5]: **Good**.
   SBT does this by default. It's a bit finnicky sometimes, e.g. you can't
   easily watch different sets of files and run different commands based on
   which files change, but it does an acceptable job.

6. [Using external processes, including compilers][6]: **Okay**. Shelling out
   from SBT isn't as convenient as Rake, but it's not bad

7. [Being used by external processes][7]: **Poor**. SBT boots up terribly
   slowly, on the order of 5-10 seconds. This is fine when being used
   interactively, but if some other script wants to run `sbt compile` it will
   be paying this heavy startup each time. e.g. IDEs like IntelliJ take forever
   to extract a sensible project model from SBT due to its slow bootup. This is
   totally independent of the cost of Scala compiler warm up, and its own
   issue.

8. [Allowing configuration, re-configuration, re-re-configuration][8]:
   **Good**. SBT is probably one of the most re-configurable build tools out
   there. Every setting can be over-ridden, such that even baked-in defaults
   like "where to put your files" can be re-configured. You can even e.g.
   re-configure your dependencies to depend on your `scalaVersion` if you so
   wished, since they're all tasks. You can easily take a large blob of
   configuration and re-use it in a another sub-project with miner tweaks,
   letting you maintain consistency across lots of different sub-modules while
   avoiding copying & pasting code.

9. [Download dependencies][9]: **Good**. SBT by default uses Ivy which deals
   with Java and Scala dependencies just fine. It could be faster, but it's
   acceptable

In general, SBT takes a lot of steps forward from previous build tools analyzed
here. While Gulp allows finer-grained ad-hoc dependency graphs to be build up
using `.pipe`, SBT does it to an even greater degree, such that every constant
or string in your build configuration: `version`, `scalaVersion`,
`libraryDependencies`, `name`, all that participates in the dependency graph
and can depend on things or be depended on. Furthermore, SBT's multi-module
support is relatively unprecedented, letting you easily maintain consistency
across a lot of different sub-modules.

SBT has its problems: apart from the boot-slowness mentioned above, there are
also often complaints about the impenetrable build syntax, as well as
impenetrable build semantics that makes everything trivial, but only if you
know the magic incantation that is difficult to derive from first principles.
Those complaints, while valid, largely fall outside of the feature-comparison
of this post.

## Round Up

Here's the rough round-up of where the various tools fall in the various
features that we decided were desirable. I left out [Ant](#ant) and
[Rake](#rake), since they follow the featureset of [Make](#make) pretty closely
albeit in new languages.  

|                                       | [Shell Scripts](#shell-scripts) | [Make](#make) | [Ant](#ant) | [Maven](#maven) | [Rake](#rake) | [Grunt](#grunt) | [Gulp](#gulp) | [SBT](#sbt) |
|:--------------------------------------|:--------------------------------|:--------------|:------------|:----------------|:--------------|:----------------|:--------------|:------------|
| [Running ad-hoc commands][1]          | Good                            | Good          |             | Poor            |               | Okay            | Okay          | Okay        |
| [Dependency-based execution][2]       | Poor                            | Okay          |             | Good            |               | Poor            | Good          | Good        |
| [Caching command output][3]           | Poor                            | Good          |             | Good            |               | Poor            | Poor          | Okay        |
| [Parallelizing different commands][4] | Okay                            | Okay          |             | Good            |               | Poor            | Poor          | Good        |
| [Running commands on file change][5]  | Poor                            | Poor          |             | Okay            |               | Good            | Good          | Good        |
| [Using external processes][6]         | Good                            | Good          |             | Okay            |               | Poor            | Okay          | Okay        |
| [Being used by external processes][7] | Good                            | Good          |             | Good            |               | Good            | Good          | Poor        |
| [Configuration & re-configuration][8] | Okay                            | Okay          |             | Good            |               | Good            | Good          | Good        |
| [Downloading dependencies][9]         | Poor                            | Poor          |             | Good            |               | Good            | Good          | Good        |


This is no where near a comprehensive guide to build tool functionality. Apart
from being an arbitrary set of features, and an arbitrary set of judgements on
each feature, it also leaves out other tools like Pants, Buck, Bazel, Salt or
Puppet from the comparison. 

There are a lot of different ways a build tool can 
be designed: from command-based tools like Rake or Gulp, to configuration-based
tools like Maven or Grunt, to hybrids like SBT. Different build tools have 
different conceptions of "dependencies": from shell scripts having none at all,
to Maven with its single, linear lifecycle, to Make/Rake/Ant with its 
command-based dependencies and Gulp/SBT with their fine-grained dependencies.

Hopefully this post gave a good walk through of the space of 
various build tools, and demonstrates the progression of the field over-time as 
features appear and evolve. Hopefully this gave some understanding in what 
a build tool is, a basis in which to compare them, and perhaps guidance if some
day you decide existing tools are lacking and you want to build your own!


[1]: #running-ad-hoc-commands
[2]: #knowing-how-to-order-the-execution-of-commands-based-on-dependencies
[3]: #caching-command-output-if-its-inputs-dont-change
[4]: #parallelize-different-commands
[5]: #watch-for-file-changes-and-running-commands-based-on-them
[6]: #using-external-processes-including-compilers
[7]: #being-used-by-external-processes
[8]: #allowing-configuration-re-configuration-re-re-configuration
[9]: #download-dependencies
[IDL]: https://en.wikipedia.org/wiki/Interface_description_language
[FileFunction.cached]: http://www.scala-sbt.org/0.13.0/api/index.html#sbt.FileFunction$
[POM Inheritance]: https://maven.apache.org/pom.html#Inheritance
[MPQ]: https://en.wikipedia.org/wiki/MPQ
[mipmaps]: https://en.wikipedia.org/wiki/Mipmap
[Jenkins]: https://jenkins-ci.org/
[continuous integration]: https://en.wikipedia.org/wiki/Continuous_integration
[SCSS]: http://sass-lang.com/
[Maven Central]: http://central.sonatype.org/
[Proguard]: http://proguard.sourceforge.net/
[UglifyJS]: https://github.com/mishoo/UglifyJS
[Pants]: https://pantsbuild.github.io/
[Buck]: https://buckbuild.com/
[Bazel]: http://bazel.io/ 
[Puppet]: https://puppetlabs.com/puppet/what-is-puppet
[Salt]: http://saltstack.com/
[Ansible]: https://www.ansible.com/

A "Build tool" is a catch-all term that refers to anything that is needed to
get a piece of software set up, but isn't needed after that. Different
programming communities have a wealth of different tools: some use stalwarts
like `make`, some use loose collections of `.sh` scripts, some use XML-based
tools like Maven or Ant, JSON-based tools like Grunt, or code-based tools like
Gulp, Gradle, or SBT.

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
ever decide to go and write your own you'll be well prepared!

## Use Cases for Build Tools

For the purposes of this post, I'm going to draw an arbitrary line and say that
build tools are used in


- [Production Deployment](#ProductionDeployment): where you want to set up
  the codebase once, stop changing it, and ship the finished product. Whether
  you are shipping it to web servers or shipping it on CDs to install on your
  customers' laptops.

- [Continuous Integration](#ContinuousIntegration): where you are running
  a relatively large test suite on a codebase, perhaps once every several
  minute, with the code only changing between runs of the suite.

- [Development Environments](#DevelopmentEnvironments): where you are
  interactively working and changing a codebase, several times a second, and
  want it to execute many small actions (whether manually or via unit tests)
  in between modifying the code.

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

  - Generate source files from some IDL
  - Compile your hand-written source files, together with the generated
    files, to form your executable
  - Pre-process your non-code resources: e.g. generating sprite-sheets or
    mipmaps from images.
  - Collect your executable, along with whatever other files it needs at
    runtime (images, configuration, music, 3D models, ...) in a folder
  - Compress that folder into whatever format is required (gz, zip, MPQ, ...)

  You thus have to make sure you perform every step, in order, and feed the
  outputs from one command into the inputs of another command that needs them.

- Allowing configuration, re-configuration, re-re-configuration: you very often
  need to build your code for deployment with just-one-slight-change.

- Using external commands: the build tool virtually never has everything
  needed to build and deploy your source code built-in. It will need to shell
  out to other programs, perhaps to compile code, manipulate images, compress
  files, or other things.

- Being used *by* external commands: the build tool is never the "only" tool
  in town. While it can probably be run directly, inevitably it ends up being
  hooked by some other tool: you may have Jenkins automatically kick off
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

In continuous integration, you are running a build-&-test flow across every
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
  unrelated commands (Compiling SCSS? Compiling JS?) that take less than 8
  cores each you can run them in parallel and save some time

- Caching command results: if you're CI build is slow, you may want to avoid
  needlessly re-computing parts of it to speed it up. For example, third-party
  binaries are often `apt-get install`ed once and then used for multiple CI
  builds, and third-party dependencies from e.g. Maven Central are often
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

- Paralellizing unrelated commands: for performance. For the same reason as
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
2. [Knowing how to order the execution of commands based on dependencies[2]
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
part of your build process. The list if things you may want to run are
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
[Loose Scripts](#LooseScripts), used together a simpler build tool which then
does not need to do this, but that has its downsides. For example, it means
that the build tool is then un-aware of where these commands fit in to the
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
  bundle the compiler for that community's language, e.g. Maven bundles Javac
  or SBT bundles Scalac. But as the build grows, it may end up containing
  a half dozen different compilers, e.g. for Java, for SCSS, for Javascript
  ES6, for `.proto` or `.thrift` files. At some point these will end up being
  external

- Packagers: whether simple things like `gzip` or `zip`, or just `cp`-ing
  things into a folder, or running bytecode through Proguard and Javascript
  through UglifyJS.

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

This is not a comprehensive listing, but it will
give a sense of how build tools across a variety of languages and communities
fare against the requirements we listed above.

- [Shell Scripts](#ShellScripts)
- [Make](#Make)
- [Ant](#Ant)
- [Maven](#Maven)
- [Rake](#Rake)
- [Grunt](#Grunt)
- [Gulp](#Gulp)
- [Gradle](#Gradle)
- [Puppet](#Puppet)
- [SBT](#SBT)

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

2. [Knowing how to order the execution of commands based on dependencies[2]:
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
into `hello`. Furthermore,


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

Ant scripts are basically isomorphic to shell scripts: they are effectively
shell scripts with an XML syntax implemented in Java, but function more or less
identically: you run `target`s with the XML (equivalent to bash functions) and
it executes the commands within from top to bottom.

Ant has the following main differences from shell scripts

- It's all XML, and is several times more verbose than Bash commands
- It's cross platform Java, which means it runs on Windows with Java installed

Other than that, it's not materially different

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

Maven is another build tool from the Java community. While it's XML-based like
Ant, that's where the similarity ends.

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

2. [Knowing how to order the execution of commands based on dependencies[2]:
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

8. [Allowing configuration, re-configuration, re-re-configuration][8]: **???**

9. [Download dependencies][9]: **Good**. Maven does this automatically, and
   treats remote dependencies the same as it does local dependencies. You
   shouldn't ever need to worry about manually downloading or installing `.jar`
   files when dealing with Maven projects.


### Rake
### Grunt
### Gulp
### Gradle
### Puppet
### SBT


[1]: #Runningadhoccommands
[2]: #Knowinghowtoordertheexecutionofcommandsbasedondependencies
[3]: #Cachingcommandoutputifitsinputsdontchange
[4]: #Parallelizedifferentcommands
[5]: #Watchforfilechangesandrunningcommandsbasedonthem
[6]: #Usingexternalprocessesincludingcompilers
[7]: #Beingusedbyexternalprocesses
[8]: #Allowingconfigurationreconfigurationrereconfiguration
[9]: #Downloaddependencies

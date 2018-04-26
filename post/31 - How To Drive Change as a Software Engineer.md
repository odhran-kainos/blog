You have an idea. Your boss is indifferent, your team-mates apprehensive, and
that *other* team whose help you need are dubious. You are an
individual-contributor with no direct-reports. You still think it's a good idea,
but cannot make it happen alone. What next?

Driving change within an technical organization is hard, especially as someone
with no rank or authority, but is a skill that can be learned. If you've ever
found yourself with an idea but been unsure how to proceed, this post should
hopefully give you a good overview of what it takes to conceive, plan & execute
such an effort.

-------------------------------------------------------------------------------

While software engineers get a lot of autonomy, there are always things in an
organization that you, as an individual-contributor, cannot "just do":

- "Everyone should start writing integration tests"
- "All front-end teams should adopt React.js"
- "We need to re-architect our datastore to a normalized tables & relations"

These may be easier for some Director or VP to mandate by fiat, but as an
individual contributor, possibly a fresh-college-graduate, things are not so
easy. Nevertheless, it *is* possible for someone with no formal authority to
push through such initiative: I have seen it happen, and I have done it myself.
This post will explore the tools you have at your disposal, and go through a
case study of what one such effort looked like.

## Principles

Before we discuss strategy & tactics, I will lay out some basic principles that
underlie much of this post.

### Ideas are Cheap

The chances are, you are not the first person to have your idea.

In a good organization, where people are constantly evaluating options &
alternatives and trying to improve the state of the world, it's very likely that
the idea you are proposing has already been brought up, considered, and
discarded. Someone has already weighed the facts they know, the opinions of
their peers, their estimates for costs & benefits, and decided it wasn't worth
it.

If you just bring up the same idea in the same context, it will likely not
change that person's judgement.

What then?

The thing to realize here is that everything above is mutable: new facts can be
presented. Peers can be convinced. New estimates can provided & prototypes can
reveal new possibilities.

Ideas are cheap and plentiful, but you often need more than an idea: persuading
people to change long held belief is hard work. Having a good idea is just the
start of that process.

### It's a marathon, not a sprint

Changing people, and the way they work, is a slow process.

Organizations full of people have inertia. Even if you assume that everyone
instantly thinks your idea is brilliant, it takes time to change people's
existing practices to incorporate a new workflow, or to pull people off projects
to launch a new initiative. Work may need to be re-assigned, workflows
re-learned, projects postponed or delayed.

And that doesn't include trying to get buy-in on your idea in the first place!

Maybe a small team of 5 people can wrap things up and pivot quickly in a week,
but larger organizations of 50 or 100 people can easily takes months to start
moving. And that's totally normal!

Just because the "technical" part of your idea can be implemented in a week or a
month doesn't mean you should expect an organization to be able to react that
quickly. Things take time, and you need to be able to delay gratification and
keep pushing for your idea even when any expected "win" is far in the future.

### Losing the battle isn't losing the war

You will likely hear a lot of "No"s trying to pitch your idea: maybe you'll get
a straight "No", "Maybe"s from people who are really not interested, or "Yes but
maybe later" for people who are supportive but not nearly as supportive as you
think they should be. Regardless, they all mean the same thing: are not
sufficiently convinced.

This is expected. If everyone already thinks your idea is a good one, they would
have implemented it themselves already!

As mentioned earlier, there are a lot of ways you can try to push things forward
even in the face of a "No". Getting to "no" is just one of many steps in getting
to "yes", which is *itself* just one of many steps to changing how an
organization does it's work.

### People are people

The last thing to realize, before you start, is that organizations are made up
of people: while you may have technical goals or tools or metrics you want to
achieve with your changes, changing how things work in an organization is
fundamentally a people problem. Technical people may know how to write code, but
they aren't really that different from people you find in other walks of life.

Things you should know about people:

- Social proof often matters as much - if not more - than logic & facts.
  "Someone you respect says this is a good idea" is a very convincing argument!

- People are social animals; they often rely heavily on what their peers think
  in forming their own opinions, and even if they think differently may find it
  hard to object over the will of the majority. "All your colleagues think this
  is a good idea" is also very convincing!

- People can be swayed by well-chosen words, even if they convey nothing the
  individuals do not already know

Like it or not, this is how people operate. Dealing with people is a different
skill than dealing with code, but is one that can similarly be learned, trained
and improved upon. You cannot think of the people-stuff as "getting in the way"
of whatever technical work you want to do: when dealing with an organization,
the people-stuff *is* the work! It is just another challenge you can try to
understand, learn, and make progress with.

Mitigating, or leveraging, these facts of human nature will be a common theme of
the rest of this document.

## Your Assets

Above, we have discussed the basic principles to keep in mind when driving
change across an organization. Now we will consider the various assets you have
at your disposal, and how you can make use of them when trying to drive change.

### The Idea

While earlier on we said ideas are cheap, they are not totally valueless.

While it is likely someone has already considered your idea and discarded it, it
is unlikely that *everyone* who could be interested, has seriously considered
it!

This gives you an opportunity: there are a pool of people who may very well be
receptive to your idea right-off-the-bat, without needing any further
persuasion. These may not be the "right" people you need to persuade to
implement your idea, but they are useful nonetheless:

- Perhaps they aren't in the right positions *now*, but may end up in the right
  position in future. Organizations change, and someone random you sell the idea
  to now may move into a position later where they can be useful

- They can help you convince the right people that your idea is worth pursuing:
  people tend to conform to the opinions of those around them, and leaders often
  rely on their team to help them evaluate ideas & proposals

These are benefits you get "for free" just because you have an idea. You should
be sure to make use of them!

### Rhetoric

The fact of people is that words matter. You need to have the best words.

Whether in person, via email, design documents or project proposals, your words
are a chance to get people hyped up about your idea. If you can convince 20%
more people to pursue you idea just by shuffling words around, or you can kick
off a 6-month proof-of-concept rather than a 1-month proof-of-concept, it makes
no sense to waste the opportunity!

Having the best words is something that you can practice, or be trained.

These are table-stakes:

- You must be coherent: the follow of arguments or logic should be clear in your
  words. A reasonable person should be able to see how one point leads to
  another, even if they may not agree every step of the way.

- You be concise: nobody likes rambling speeches or repetitive documents.
  Excessive verbosity can easily distract from the core message you can to
  convey

- You need to be factually correct: no factual errors. If you are unsure of
  facts, make sure you find out before trying to use them. Nothing turns people
  off like basic factual errors in a proposal.

### Yourself

While this article is focused on ideas which you *cannot* implement yourself, do
not discount the asset that you yourself are in pushing ideas forward:

- You probably have free/down/flex-time you can use to work on it without
  needing *anyone's* permission: your main project is compiling, you are waiting
  for Jenkins CI, blocked on code-review.

- You can argue for yourself to spend some time, full or part-time, working on
  your idea: "I think it's a good idea and want to do it" is a much easier pitch
  than "I think it's a good idea and someone else needs to do it".

You cannot do *everything*, but you can often do *something*. Sometimes just
quietly pushing things along yourself gets you what you want faster than trying
to argue for headcount up-front, and can put your idea in a better state to
argue for headcount after (e.g. you've put together a proof-of-concept to
demonstrate the value).

Be extremely conscious of the limits of what you can do yourself, but be willing
to "just do it" strategically if you think it's the fastest way to get your idea
into a place you want it to be.

### Altruism

Hopefully, you are in organization with nice people. That means that you can
often ask things of people even if you have no authority over them nor are you
offering anything in return:

- Borrowing time to learn things from people

- Help pair-programming through a difficult challenge

- Asking people to try out a new tool, workflow or practice

Fundamentally, altruistic help is based on social relationships: you are calling
upon someone to help you not because they want to, or need to, but purely
because they know you are a nice person and are willing to help you out. Or
perhaps you've helped them in the past, and they're willing to pay back the
favor.

Building these relationships with people literally gives you manpower
you can then direct towards making your idea a reality. It's not a huge
amount of manpower - you probably won't be able to take more than a handful of
hours a week, since these people still have their primary jobs - but it's not
nothing.

### Self-interest

More powerful than altruism is rational self-interest. While people's altruism
is finite, their self-interest is unlimited.

If you can convince someone that helping you directly helps themselves, you're
likely to be able to ask far more of them than if you relied on altruism. The
more immediate the benefit, the better.

For example, consider these two pitches:

1. "You need to help us extract your code out of the monolith into a separate
   module to improve the health of our codebase"

2. "If you help us extract your code out of the monolith into a separate module,
   your compile times will drop from 60s to 10s and your Jenkins CI time from
   60min to 10min"

3. "If you extract your code out of the monolith into a separate module, it will
   speed up time-to-interactivity on the web pages your team owns by 400ms,
   making the experience much better for your users."

It might take some work up-front to make the second or third pitches a reality:
in the above examples, you may need to implement per-module compilation &
testing, or per-module Javascript delivery, even without any modular code to
take advantage of these features.

Nevertheless, allowing modular compiles/JS-delivery may be easier than
modularizing everyone's code. By tilting the incentives slightly, you can make
it so that the other person finds it in their self-interest to do the hard work
you want them to do, even without formal authority.

### New Facts

The last tool you have at your disposal, is the introduction of new Facts.

Earlier on, I had mentioned that one reason ideas are cheap is that they have
probably already been considered, and rejected, base on the facts at hand.

Re-introducing the idea to the same people won't change the rejection. But
introducing new facts could.

New facts could include:

- Grassroots Opinions: "In our last survey, we found 90% of the engineers in
  your department think we should do X". This is especially impactful on
  management who may be slightly out of touch with the people in the trenches.

- Social Proof: "Did you know that ABC company you think is really cool is
  actually doing X? Here I invited one of them down to give a tech talk about
  why they love X at ABC"

- Proof-of-concept results: "We thought that this would take 6 months & give us a 10%
  performance boost, but I spent 1 week on a prototype that suggests we could do
  it in 1.5 months and get a 50% performance boost"

Bringing new facts to the table is a lot of work: running surveys, bringing in
outside expert opinions, and especially doing proof-of-concept work or building
prototypes. None of these are easy, and can easily take weeks or months of
slogging away to find out something new. However, if you think the reason
someone is dubious of your idea is due to the facts available to them,
introducing new facts can quickly change their internal calculus of whether it's
a good idea or not.

## Tactics

### Planning for Momentum

Momentum is when a project keeps moving seemingly on it's own accord: there is
progress, people perceive benefit, more effort goes in, and more progress
results.

The converse is also true: if a project isn't moving, nobody sees benefit, no
effort goes in, and nothing happens.

How do you get from one to the other?

The basic principle behind building momentum is you need to make sure there is a
feedback loop between the effort going in, resulting in perceived benefit that
justifies more effort.

You often have a choice of how to organize a project: whether as a long effort
followed by big reward, or a long effort with incremental rewards. If at all
possible, as long as it gets you to the same end-state, you should plan for the
latter: this is what ensures that (often short-sighted) rational self interest
kicks in and people are willing to help for the immediate, short-term win.

Basically every asset described above feeds upon each other:

- People are more willing to provide altruistic help if you've benefited their
  rational self-interest

- You'll have more manpower to run proof-of-concepts to bring in fresh facts if
  people are giving you altruistic help

- Your idea will seem more persuasive, even if it's the same idea, if you have
  more people helping out and more facts backing it up

- You will be able to dedicate more of yourself to working on this idea, rather
  than your "day job", if the idea is persuasive

- You can dedicate your newfound personal freedom to spend time providing more
  and more incentives for people to help out of rational self-interest

The basic approach to getting momentum is planning the work to ensure that the
feedback loop exists, and then leveraging whatever assets you have to begin with
to give the project a push. You may have to argue for your own time, call in
favors from friends, prepare your best pitches & rhetoric to try and convince
people, all to try and catch the positive cycle where people start seeing
benefit, and the project can begin coasting on its own (partial) success.


### Get qualified, falsifiable "No"s

When someone says "No" to your idea, whether directly or indirectly, you should
try to get a commitment from them: exactly what would it take to get them to say
"Yes"?

The goal of this is two-fold:

- You want to truly understand why they are saying "No". You want to make them
  think hard about why they themselves are saying no, so both of you understand
  what their real concerns are

- You have something to hold them to later on: if someone says "I don't think
  it's a good idea but if you can show X then I'll support it", and you come
  back a month later with X, they are more likely to change their mind.

You want your "No"s to be qualified and falsifiable: it should always be "No
because Y, but if you can provide X, then i'll be ok".

People almost always have some good reason for saying "No". They may have
difficulty expressing exactly why they are a "No", but it is your responsibility
to try and fish it out of them so you can understand their concerns. Asking for
a concrete deliverable that can change their mind is a great way of forcing them
to think clearly about what their concerns are and how they can be resolved, so
you resolve them and win the person over.

### Count every individual

When pushing for an idea, as much as possible you should know what every
individual who has a say in the change, regardless of how small, thinks of it:

- Who supports it already?

- Who is on the fence?

- Who thinks it's a bad idea?

- Why?

Knowing these facts for each individual helps you piece together a picture of
what the lay of the land is:

- What are the key objections that many people have?

- What have proven to be the most persuasive selling points?

- Who supports it strongly enough you can get them to help you do stuff?

- Who are the key people who don't support it, but you need to convince?

These are questions which you simply cannot answer by thinking hard in
isolation: you have to go and talk to each individual to collect the facts. Even
so, it shouldn't take too long to talk to a few dozen people to piece together
an understanding of where everyone stands and what it would take to win each
person over.

### Count every win

Just as easily as counting people, is to count wins: both historical, and
potential.

- What benefit has the current work on the idea yielded so far?

- What benefit do we expect for the next quantum of effort?

The exact benefits could vary as widely as the idea itself: maybe some workflow
is faster? Engineers happier? Lower defect rate? Lower hosting costs? Greater
ease of implementing features? Less engineering spent on tedious/repetitive
tasks? Happy customers?

What matters here is that you can give *concrete* responses to the above
questions: answers that someone who isn't already convinced can understand and
appreciate. e.g.

- *"We'll have fewer bugs"* is not a good response, but *"Given the bugs we've
  had in the past month, 13/27 of them would have been avoided by our proposal"*

- *"The code is now simpler"* is not a good response, but *"80% of our engineers
  said this has fixed their largest pain point implementing new features"*

More often than not, someone asking you these questions isn't convinced that
your idea is a good idea: they don't understand why you want to do this, or why
you are *already* doing this, or what's in it for *them*. You need to be able to
translate your idea into a language that they can understand, so they can
support your idea not out of altruism or friendship, but instead out of rational
self-interest.

### Bootstrapping consensus

One paradox of human nature is that consensus is circular: an individual finds
it easiest to believe the current consensus, and the current consensus is made
up of what every individual believes.

That can be a problem if the current consensus is that your idea is a bad idea!

If you call a big meeting to try and convince people, everyone will be
implicitly glancing around at their peers as they form their own opinion. Even
if an individual is starting to come around to your idea, it is more difficult
for them to break ranks and say so in front of their un-convinced peers.

If you have an explicitly leadership position, you can use that to push your
idea more forcefully, but an individual contributor does not have that luxury.

The way around this problem is to change consensus one individual at a time:
find people who you think may be interested, and pitch your idea to them to see
if they bite. If they're not interested, move on. If they are, bring the
interested individuals together. At this point you have an interest group of
people who support your idea. This means:

- People who are interested can discuss the idea and how to push it forward,
  rather than brooding in isolation

- It's no longer "one guy with an idea", it's now an group, with many people
  interested, and more legitimacy in the eyes of critics and supporters alike

- You have people who you can leverage to do the necessary work to get momentum
  going.

Even if you don't have *everyone* on board with your idea, and don't have the
key decision makers, you're idea now has some momentum. You can invest that
momentum in the assets described earlier: gradually disseminating the idea
further, polishing the pitch, trying to get more people involved via
self-interest, and running proof-of-concept projects to bring in new facts that
demonstrate the idea's viability.

From there, you can slowly and iteratively grow your base of support.

Hopefully you'll eventually get most people, perhaps including the important
decision makers, interested. *That* is the point to call a big meeting - or send
out a wide email, to use the circular nature of consensus in your favor:

- To make sure any last hold-outs are aware of everyone else who supports your
  idea, which can help convince them

- You make sure everyone who supports your idea is aware of *everyone else* who
  *also* supports it, which reinforces their support

And at that point, you have successfully formed a new consensus that you can use
to push your idea forward!

## Case Study: Selenium

To tie everything down to concrete examples, I will discuss a project I helped
drive forward fresh out of school.

The basic idea here was I wanted everyone (i.e. the engineering organization) to
start using selenium integration tests to start guarding against regressions in
our website. The technical side of that project is outside the scope of this
post, but the way the project played out illustrates many of the points in this
post.

The basic timeline was a follows:

### October 2013

Data loss bug causes huge scramble within the company. Legal was involved, PR
was involved. Thousands of apology emails sent. Was not a great day.

We were spending huge amounts of time doing manual testing clicking-around our
website as we made changes to try and catch regressions. The data-loss-bug path
actually *was* manually tested, but due to time pressure (There was a *lot* of
stuff to click around!) it was not tested sufficiently thoroughly to notice
something was wrong. It was just one of many bugs that would slip through our
manual QA every week.

This could have been prevented by selenium integration tests.

Unsurprisingly, I was not the first person to have such an idea! We already
*had* a selenium integration test suite, but it wasn't very good: slow, flaky,
impossible to iterate on or debug, resulting in nobody writing new tests to
cover the ever-growing product footprint.

Everyone agreed that we should have a good integration test suite. I did not
manage to convince management that "now" is the time to invest effort in making
it good, due to competing priorities for rolling out new features.

### November 2013

In between feature work, I did a small proof-of-concept that demonstrated
selenium integration tests did not *need* to be flaky, or hard to work with. The
demo was showing how a single laptop could click around the website 4 times in
parallel. This could potentially save us a lot of time and effort doing manual
QA, and would probably catch more bugs too!

It would have taken more time productionize: so it would be useful to other
people, and run on other places that were not just my laptop, and test things
that were not what I was working on right-that-moment. Perhaps a month or two of
work by one or two people.

My team-mates were impressed - Selenium was no longer a hypothetical, but
something they could see would save time and improve quality - and were starting
to come over that this was a good idea that should happen sooner rather than
later. Regardless, I did not manage to convince leadership enough that we should
invest the time now.

### December 2013

December is always a slow month with some people on vacation for varying lengths
of time. It was even slower for me because I didn't take any vacation over
Christmas (I was saving it for Chinese New Year in February 2014). This gave me
perhaps 1-2 weeks of December un-committed: I used that time to further flesh
out the Selenium proof-of-concept.

My main work in December was to port all the *old* Selenium tests - the slow,
flaky, impossible-to-debug ones, over the the new setup that I had used for the
demo so far. There were maybe 60 tests in all. They were *still* slow, flaky,
and hard to debug, but orders of magnitude less so than the existing suite.

Exactly what I did, from a technical point of view, to make the Selenium tests
less slow/flaky/impossible-to-debug is an interesting discussion in it's own
right that is beyond the scope of this post.

When the office starting filling up again in January, I again pitched the idea
of investing the effort fully fleshing these out, but again was turned down: we
had a big product launch in March that took 110% priority. After that, perhaps,
we could consider it.

### January-February 2014

During this time, my Selenium effort was mostly on the back burner.

I called a few meetings with managers/leads on other teams that may be
interested to discuss the Selenium project. The response was overwhelmingly
negative:

- "We already have Selenium tests; they are awful"

- "I've seen Selenium tests play out in many other other companies before; it
  has always been a terrible experience not worth the time invested"

The main concerns were reliability and usability: my colleagues knew from
their prior experience that Selenium was terribly flaky, and writing/debugging
tests was terribly complex. While I could say that "this time it's different",
nobody was convinced.

Another thing that was happening during this time was the accounting of
failures: for each bug that slipped through to production (and there were many!)
what could have prevented them? We made sure to put "Selenium tests" on each bug
that they would apply to, to keep them front-of-mind and make sure anyone who
asked "why are we writing such buggy code" would come to their own conclusion
"we need selenium tests".

### March 2014

The big product launch date came, and was pushed back. The new launch date was
in April. No time to work on Selenium tests now!

At this point, we were probably working at 150% cadence on the upcoming product
launch: people were staying at work into the evenings, and Sunday work-parties
were a thing. This was clearly not sustainable in the long term, but as a
short-term push to make the product launch, it worked out OK over-all. No
Selenium work went on during this time.

### April 2014

The big product launch happened. I had spent 9am to 11pm the Saturday before in
the office grinding out the last bits of polish. Fancy product demos were
released, media coverage, everything went well. Many engineers took days to a
week of time off following the launch to de-compress. There were no clear
deadlines or roadmap for a while: the previous plans had only been made up to
the big product launch, which was now over, and contingency plans for
post-launch fire-fighting which thankfully didn't end up being used. It would
take time for people to get organized and decide what would happen next.

I took this chance to declare that I was going to work on Selenium tests. This
was a statement of fact, not a request for permission.

In the chaotic aftermath of the launch, I managed to pull in 2-3 other
interested individuals to start working on Selenium Tests full time.

### May-June 2014

These two months were mostly execution: the 3-4 of us needed to demonstrate to
people that their concerns about Selenium test flakiness and complexity have
been mitigated.

A lot of very interesting technical work went into this to make the Selenium
tests great again, but that's outside the scope of this post.

### July 2014

At this point we were pretty confident that what we had was a good thing: our
new selenium test suite was catching several bugs a week that no other test
suite caught, with 80-90% stability (i.e. 10-20% flaky failures). Because nobody
else trusted the selenium suite yet, we took it upon ourselves to take turns
watching it: investigating it if it turned red, and triaging a fix if a real
regression was found.

In July we also started getting other teams involved in the Selenium project:
apart from sending bugs their way if we caught something, we also would
pair-program with individuals to teach them how to write selenium tests, and
give tech talks to introduce Selenium to a wider audience. This would help
spread the knowledge that selenium tests didn't *need* to be hard to write or
debug.

Lastly, we spent the July tallying up the additional bugs that our Selenium
suite had caught that no other suite had caught; it added up to a list of maybe
30 bugs over the course of July, 5-6 of which would classify as "emergencies".

### August 2014

At this point, we finally sent out the email to create a consensus. This email
contained:

- How many bugs we had caught over the last 30 days (linking to each and every
  fix-commit)

- What the stability/flakiness numbers had been over the last 30 days

- Which teams had already started writing Selenium tests

By this time we already knew that many people already supported the idea of
treating our new selenium test suite seriously: they were already writing tests
for their own features, paying attention to the tests if they break, and getting
a lot of value from the increase in product quality & iteration speed that
Selenium was giving them.

However, we needed to create this consensus for the future:

- We needed our Selenium suite to be ingrained into the daily deploy process and
  tooling.

- We needed to justify ongoing investment/maintenance of the Selenium project.
  This would be a half-time to full-time job for a single engineer: not huge,
  but not trivial.

The important thing to realize at this point is that there wasn't any *debate*:
the debate had already happened one-on-one, person by person, in the weeks
preceding. The sole purpose of this broadcast was to lock in all those wins, and
to make sure that everyone knew that *everyone else* supported this project.

And from that, we now had broad consensus that the Selenium suite was an
institution that is, and will remain, a core part of our engineering
organization.

## Conclusion

Changing things in an organization takes time.

Even a simple thing, like getting people to start writing Selenium tests and
taking them seriously, is a long slog to get institutionalized: much longer than
it takes to "just write code". Nevertheless, the process was entirely
reasonable: we *did* need to get that product launch done, people *did* have
good reason to doubt that Selenium tests were a good idea. When the goal isn't
"I want to change how I work" but "I want to change how *everyone* works", it is
expected that it will take a bit longer.

While I dove deep into the Selenium test project, the same principles and
tactics often apply regardless of what organizational change you are trying to
accomplish as an individual contributor.

This blog post has gone through a lot of the principles and tactics you can use
when you try to drive change across an organization. While some things are
easier to do if you are a "big shot" manager or director, it is still possible
even for individual contributors: you just have to think extra-carefully about
what assets you have, and how you can bootstrap your idea to gain momentum and
acceptance across the org.

The flip side of changing an organization, rather than just changing code, is
that these things last: the number of Selenium tests grew from 60 to thousands.
People came and went, teams were broken up and re-formed, entire product
verticals were spun up or discarded. Through it all, the Selenium tests and the
culture around them remains.

```
          ||----------   
          || Selenium |
          || Tests    |
          ||----------
          ||
          || 
        _ ||_   _                       _   _   _
       | |_| |_| |                     | |_| |_| |
       |         |                     |         |
       |         |  _   _   _   _   _  |         |
       |         |_| |_| |_| |_| |_| |_|         |
       |         |                     |         |
       |         |       _______       |         |
       |         |      |   |   |      |         |
       |         |      |   |   |      |         |
       |         |      |   |   |      |         |  
   O   |         |      |   |   |      |         |       
   |                                                      
  \|/        guarding against web breakages     o           
                   since summer 2014            | 
                                      \/       \|/
           \/          ?                 
     \/               \|/                           \/
```
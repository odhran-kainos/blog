load.ivy("com.lihaoyi" %% "scalatags" % "0.5.3")

@

import scalatags.Text.all.{width, height, _}
import scalatags.Text._
import java.time.LocalDate
@
load.module(ammonite.ops.cwd/"pageStyles.scala")
@
def sanitize(s: String): String = {
  s.filter(_.isLetterOrDigit)
}
def sanitizeAnchor(s: String): String = {
  s.split(" |-", -1).map(_.filter(_.isLetterOrDigit)).mkString("-").toLowerCase
}
def pageChrome(titleText: Option[String], unNesting: String, contents: Frag): String = {
  val pageTitle = titleText.getOrElse("Haoyi's Programming Blog")
  val sheets = Seq(
    "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css",
    "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css",
    "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.1.0/styles/github-gist.min.css"
  )
  def icon(s: String) = div(i(cls:= s"fa fa-$s"))
  val headerLinks = Seq(
    Seq(
      div(icon("question-circle"), " About") -> s"$unNesting/post/HelloWorldBlog.html",
      div(icon("file-text-o"), " Resume") -> "https://lihaoyi.github.io/Resume/",
      div(icon("github"), " Github") -> "https://github.com/lihaoyi"
    ),
    Seq(
      div(icon("twitter"), " Twitter") -> s"https://twitter.com/li_haoyi",
      div(icon("youtube-play"), " Talks") -> s"$unNesting/post/TalksIveGiven.html",
      div(icon("rss"), "RSS") -> s"$unNesting/feed.xml"
    )
  )
  html(
    head(
      meta(charset := "utf-8"),
      for(sheet <- sheets)
        yield link(href := sheet, rel := "stylesheet", `type` := "text/css" ),
      tags2.title(pageTitle),
      tags2.style(s"@media (min-width: 60em) {${WideStyles.styleSheetText}}"),
      tags2.style(s"@media (max-width: 60em) {${NarrowStyles.styleSheetText}}"),
      tags2.style(Styles.styleSheetText),
      script(src:="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.1.0/highlight.min.js"),
      script(src:="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.1.0/languages/scala.min.js"),
      script(raw("hljs.initHighlightingOnLoad();")),
      // This makes media queries work on iphone (???)
      // http://stackoverflow.com/questions/13002731/responsive-design-media-query-not-working-on-iphone
      meta(name:="viewport", content:="initial-scale = 1.0,maximum-scale = 1.0"),
      googleAnalytics,
      forceHttps
    ),
    body(
      margin := 0,
      div(
        WideStyles.header,
        NarrowStyles.header,
        Styles.header,
        div(
          NarrowStyles.headerContent,
          WideStyles.headerContent,
          h1(
            a(
              i(cls:= "fa fa-cogs"),
              color := "white",
              " Haoyi's Programming Blog", href := s"$unNesting",
              Styles.subtleLink,
              NarrowStyles.flexFont,
              fontWeight.bold
            ),
            padding := "30px 30px",
            margin := 0
          ),
          div(
            Styles.headerLinkBox,
            NarrowStyles.linkFlex,
            // This is necessary otherwise it doesn't seem to render correctly
            // on iPhone 6S+ Chrome; presumably they have some bug with flexbox
            // which is making it take up insufficient space.
            minWidth := 175,
            for (headerLinksRow <- headerLinks) yield div(
              display.flex,
              flexDirection.row,
              for( (name, url) <- headerLinksRow) yield div(
                Styles.headerLink,
                a(name, href := url, Styles.subtleLink, color := "white")
              )
            )
          )
        )
      ),
      div(
        WideStyles.content,
        NarrowStyles.content,
        maxWidth := 900,
        titleText.map(h1(_)),
        contents
      ),
      div(
        WideStyles.footer,
        Styles.footer,
        "Last published ", currentTimeText
      )

    )
  ).render
}
val currentTimeText = LocalDate.now.toString

def commentBox(titleText: String): Frag = Seq(
  div(id:="disqus_thread"),
  script(raw(s"""
      /**
      * RECOMMENDED CONFIGURATION VARIABLES: EDIT AND UNCOMMENT THE SECTION BELOW TO INSERT DYNAMIC VALUES FROM YOUR PLATFORM OR CMS.
      * LEARN WHY DEFINING THESE VARIABLES IS IMPORTANT: https://disqus.com/admin/universalcode/#configuration-variables
      */

      var disqus_config = function () {
      this.page.url = "http://www.lihaoyi.com/post/${sanitize(titleText)}.html"; // Replace PAGE_URL with your page's canonical URL variable
      this.page.identifier = "$titleText"; // Replace PAGE_IDENTIFIER with your page's unique identifier variable
      };

      (function() { // DON'T EDIT BELOW THIS LINE
      var d = document, s = d.createElement('script');

      s.src = '//lihaoyi.disqus.com/embed.js';

      s.setAttribute('data-timestamp', +new Date());
      (d.head || d.body).appendChild(s);
      })();
  """))
)

def googleAnalytics: Frag = script(raw(
"""(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){
  |(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),
  |m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)
  |})(window,document,'script','//www.google-analytics.com/analytics.js','ga');
  |
  |ga('create', 'UA-27464920-5', 'auto');
  |ga('send', 'pageview');
""".stripMargin
))
def forceHttps: Frag = script(raw(
"""if (window.location.protocol == "https:")
  |    window.location.href = "http:" + window.location.href.substring(window.location.protocol.length);
""".stripMargin
))
def metadata(dates: Seq[(String, LocalDate)]) = div(
  color := "#999",
  marginBottom := 20,
  "Posted ",
  for ((sha, date) <- dates.lastOption) yield a(
    date.toString, href := s"https://github.com/lihaoyi/blog/commit/$sha"
  )
)
def mainContent(posts: Seq[(String, String, String, Seq[(String, LocalDate)])]) = pageChrome(
  None,
  ".",
  div(
    for((name, _, rawHtmlSnippet, dates) <- posts.reverse) yield {
      val url = s"post/${sanitize(name)}.html"
      div(
        h1(a(
          name,
          href := url,
          Styles.subtleLink,
          color := "rgb(34, 34, 34)"
        )),
        metadata(dates),
        raw(rawHtmlSnippet),
        a( // Snippet to make comment count appear
          href:=s"$url#disqus_thread",
          data.`disqus-identifier`:=name,
          "Comments"
        ),
        hr(margin := "50px 0px 50px 0px")
      )
    },
    // snippet to
    script(id:="dsq-count-scr", src:="//lihaoyi.disqus.com/count.js", "async".attr:="async")
  )
)
def postContent(name: String, rawHtmlContent: String, dates: Seq[(String, LocalDate)]) = pageChrome(
  Some(name),
  "..",
  Seq[Frag](
    metadata(dates),
    raw(rawHtmlContent),
    if (dates.length < 2) ""
    else {
      div(
        hr,
        div(
          color := "rgb(158, 167, 174)",
          "Updated ",
          for((sha, date) <- dates.drop(1)) yield a(
            date.toString, " ", href := s"https://github.com/lihaoyi/blog/commit/$sha"
          )

        )
      )
    },
    commentBox(name)
  )
)
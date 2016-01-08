load.ivy("com.lihaoyi" %% "scalatags" % "0.5.3")
load.module(ammonite.ops.cwd/"styles.scala")
@

import scalatags.Text.all.{width, height, _}
import scalatags.Text._
import java.time.LocalDate
@
def pageChrome(titleText: Option[String], unNesting: String, contents: Frag): String = {

  val sheets = Seq(
    "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css",
    "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css",
    "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.1.0/styles/github-gist.min.css"
  )
  val headerLinks = Seq(
    div(div(i(cls:= "fa fa-question-circle")), " About") -> s"$unNesting/post/Hello%20World%20Blog.html",
    div(div(i(cls:= "fa fa-file-text-o")), " Resume") -> "https://lihaoyi.github.io/Resume/",
    div(div(i(cls:= "fa fa-github")), " Github") -> "https://github.com/lihaoyi"
  )
  html(
    head(
      meta(charset := "utf-8"),
      for(sheet <- sheets)
        yield link(href := sheet, rel := "stylesheet", `type` := "text/css" ),
      tags2.title("lihaoyi.com" + titleText.map(": " + _).getOrElse("")),
      tags2.style(s"@media (min-width: 48em) {${WideStyles.styleSheetText}}"),
      tags2.style(s"@media (max-width: 48em) {${NarrowStyles.styleSheetText}}"),
      tags2.style(Styles.styleSheetText),
      script(src:="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.1.0/highlight.min.js"),
      script(raw("hljs.initHighlightingOnLoad();"))
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
              " Haoyi's Programming Blog", href := s"$unNesting/index.html",
              Styles.subtleLink,
              fontWeight.bold
            ),
            padding := "10px 10px",
            margin := 0
          ),
          div(
            Styles.headerLinkBox,
            NarrowStyles.linkFlex,
            for ((name, url) <- headerLinks) yield div(
              Styles.headerLink,
              a(name, href := url, Styles.subtleLink, color := "white")
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
      /*
      var disqus_config = function () {
      this.page.url = "https://www.lihaoyi.com/p/$titleText"; // Replace PAGE_URL with your page's canonical URL variable
      this.page.identifier = "$titleText"; // Replace PAGE_IDENTIFIER with your page's unique identifier variable
      };
      */
      (function() { // DON'T EDIT BELOW THIS LINE
      var d = document, s = d.createElement('script');

      s.src = '//lihaoyi.disqus.com/embed.js';

      s.setAttribute('data-timestamp', +new Date());
      (d.head || d.body).appendChild(s);
      })();
  """))
)

def metadata(dates: Seq[(String, LocalDate)]) = div(
  color := "#999",
  marginBottom := 20,
  "Posted ",
  for ((sha, date) <- dates.headOption) yield a(
    date.toString, href := s"https://github.com/lihaoyi/site/commit/$sha"
  )
)
def mainContent(posts: Seq[(String, String, String, Seq[(String, LocalDate)])]) = pageChrome(
  None,
  ".",
  div(
    for((name, _, rawHtmlSnippet, dates) <- posts.reverse) yield div(
      h1(a(
        name,
        href := s"post/${name.replace(" ", "")}.html",
        Styles.subtleLink,
        color := "rgb(34, 34, 34)"
      )),
      metadata(dates),
      raw(rawHtmlSnippet),
      hr(margin := "50px 0px 50px 0px")
    )
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
            date.toString, " ", href := s"https://github.com/lihaoyi/site/commit/$sha"
          )

        )
      )
    },
    commentBox(name)
  )
)
import $ivy.`com.lihaoyi::scalatags:0.6.5`
import scalatags.Text.all._, scalatags.Text.tags2
import java.time.LocalDate
import $file.pageStyles, pageStyles._

case class PostInfo(name: String,
                    headers: Seq[(String, Int)],
                    rawHtmlContent: String,
                    rawHtmlSnippet: String,
                    updates: Seq[(String, java.time.LocalDate)])

def sanitize(s: String): String = {
  s.filter(_.isLetterOrDigit)
}
def sanitizeAnchor(s: String): String = {
  s.split(" |-", -1).map(_.filter(_.isLetterOrDigit)).mkString("-").toLowerCase
}
def pageChrome(titleText: Option[String],
               unNesting: String,
               contents: Frag,
               contentHeaders: Seq[(String, Int)]): String = {
  val pageTitle = titleText.getOrElse("Haoyi's Programming Blog")
  val sheets = Seq(
    "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.6/css/bootstrap.min.css",
    "https://maxcdn.bootstrapcdn.com/font-awesome/4.5.0/css/font-awesome.min.css",
    "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/9.1.0/styles/github-gist.min.css"
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
    body(margin := 0, backgroundColor := "#f8f8f8")(
      navBar(unNesting, contentHeaders),
      div(
        WideStyles.content,
        NarrowStyles.content,
        maxWidth := 900,
        titleText.map(h1(_)),
        contents
      ),
      if (contentHeaders.nonEmpty) frag()
      else div(
        WideStyles.footer,
        Styles.footerStyle,
        "Last published ", currentTimeText
      )

    )
  ).render
}

def navBar(unNesting: String, contentHeaders: Seq[(String, Int)]) = {
  def icon(s: String) = div(i(cls:= s"fa fa-$s"))
  val headerLinks = Seq(
    Seq(
      div(icon("question-circle"), " About") -> s"$unNesting/post/HelloWorldBlog.html",
      div(icon("file-text"), " Resume") -> "https://lihaoyi.github.io/Resume/",
      div(icon("github"), " Github") -> "https://github.com/lihaoyi"
    ),
    Seq(
      div(icon("twitter"), " Twitter") -> s"https://twitter.com/li_haoyi",
      div(icon("envelope"), " Subscribe") -> s"http://eepurl.com/c3A5Tz",
      div(icon("youtube-play"), " Talks") -> s"$unNesting/post/TalksIveGiven.html"
      //      div() -> ""
    )
  )

  val headerBox = div(
    NarrowStyles.headerContent,
    WideStyles.headerContent,
    h1(
      a(
        i(cls:= "fa fa-cogs"),
        color := "#f8f8f8",
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
          a(name, href := url, Styles.subtleLink, color := "#f8f8f8")
        )
      )
    )
  )

  val tableOfContents = if (contentHeaders.isEmpty) frag()
  else div(WideStyles.tableOfContents, NarrowStyles.tableOfContents, color := "#f8f8f8")(

    div(textAlign.center)(
      b("Table of Contents")
    ),
    div(overflowY.scroll, flexShrink := 1, minHeight := 0)(
      ul(
        overflow.hidden,
        textAlign.start,
        marginTop := 10,
        whiteSpace.nowrap,
        textOverflow.ellipsis,
        marginRight := 10
      )(
        for {
          (header, indent) <- contentHeaders
          offset <- indent match{
            case 2 => Some(0)
            case 3 => Some(20)
            case _ => None
          }
        } yield li(marginLeft := offset)(

          a(
            color := "#f8f8f8",
            WideStyles.tableOfContentsItem,
            href := s"#${sanitizeAnchor(header)}"
          )(
            header
          )
        )
      )
    )
  )

  div(
    WideStyles.header,
    NarrowStyles.header,
    Styles.headerStyle,
    headerBox,
    tableOfContents
  )
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
      this.page.identifier = "${titleText.replace("\"", "\\\"")}"; // Replace PAGE_IDENTIFIER with your page's unique identifier variable
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
def metadata(lastDate: Option[(String, LocalDate)]) = div(opacity := 0.6, marginBottom := 10)(
  i(cls:="fa fa-calendar" , aria.hidden:=true),
  i(
    " Posted ",
    for ((sha, date) <- lastDate) yield a(
      date.toString, href := s"https://github.com/lihaoyi/blog/commit/$sha"
    )
  )
)
def mainContent(posts: Seq[(String, String, Option[(String, LocalDate)])]) = pageChrome(
  None,
  ".",
  div(
    for((name, rawHtmlSnippet, lastDate) <- posts.reverse) yield {
      val url = s"post/${sanitize(name)}.html"
      div(
        h1(a(
          name,
          href := url,
          Styles.subtleLink,
          color := "rgb(34, 34, 34)"
        )),
        metadata(lastDate),
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
    script(id:="dsq-count-scr", src:="//lihaoyi.disqus.com/count.js", attr("async"):="async")
  ),
  contentHeaders = Nil
)

def renderAdjacentLink(next: Boolean, name: String) = {
  a(href := s"${sanitize(name)}.html")(
    if(next) frag(name, " ", i(cls:="fa fa-arrow-right" , aria.hidden:=true))
    else frag(i(cls:="fa fa-arrow-left" , aria.hidden:=true), " ", name)
  )
}
def postContent(post: PostInfo, adjacentLinks: Frag) = pageChrome(
  Some(post.name),
  "..",
  Seq[Frag](
    metadata(post.updates.lastOption),
    div(adjacentLinks, marginBottom := 10),
    raw(post.rawHtmlContent),
    adjacentLinks,
    if (post.updates.length < 2) ""
    else {
      div(
        hr,
        div(opacity := 0.6)(
          i(
            "Updated ",
            for((sha, date) <- post.updates.drop(1)) yield a(
              date.toString, " ", href := s"https://github.com/lihaoyi/blog/commit/$sha"
            )
          )
        )
      )
    },
    commentBox(post.name)
  ),
  post.headers
)

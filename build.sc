// Load dependencies
import $ivy.{`org.pegdown:pegdown:1.6.0`, `com.lihaoyi::scalatags:0.6.5`}
import $file.pageStyles, pageStyles._
import $file.pages, pages._
import scalatags.Text.all._

import ammonite.ops._
import collection.JavaConverters._
import org.pegdown.{PegDownProcessor, ToHtmlSerializer, LinkRenderer, Extensions}
import org.pegdown.ast.{VerbatimNode, ExpImageNode, HeaderNode, TextNode, SimpleNode, TableNode}

val postsFolder = cwd/'post

interp.watch(postsFolder)

val targetFolder = cwd/'target

object DatesFor{
  implicit val wd = ImplicitWd.implicitCwd
  val commitChunks = %%('git, 'log, "--date=short").out.string.split("\n(?=commit)")
  val commits = for(chunk <- commitChunks) yield {
    val lines = chunk.lines.toSeq
    val sha = lines(0).stripPrefix("commit ")
    val author = lines.find(_.startsWith("Author: ")).get.stripPrefix("Author: ")
    val date = lines.find(_.startsWith("Date: ")).get.stripPrefix("Date:   ")
    val files = %%('git, 'show, "--pretty=format:", "--name-only", sha).out.lines
    (sha, author, date, files)
  }
  val fileChanges = for{
    (sha, author, date, files) <- commits
    file <- files
  } yield (file, sha, author, date)

  def apply(filePrefix: String) = for {
    (file, sha, author, date) <- fileChanges
    // Git sometimes quotes the filenames if they contain quotes, so make sure
    // we strip off any quotes preceding it to get at the actual `$number -` prefix
    if file.stripPrefix("\"").startsWith(filePrefix)
  } yield {
    println("XXX " + date)
    (sha, java.time.LocalDate.parse(date))
  }

}

val (markdownFiles, otherFiles) = ls! postsFolder partition (_.ext == "md")
markdownFiles.foreach(println)
// Walk the posts/ folder and parse out the name, full- and first-paragraph-
// HTML of each post to be used on their respective pages and on the index

val posts = {
  val split = for(path <- markdownFiles) yield {
    val Array(number, name) = path.last.split(" - ", 2)
    (number, name.stripSuffix(".md"), path)
  }
  for ((index, name, path) <- split.sortBy(_._1.toInt)) yield {
    val processor = new PegDownProcessor(
      Extensions.FENCED_CODE_BLOCKS | Extensions.TABLES | Extensions.AUTOLINKS
    )
    val ast = processor.parseMarkdown(read! path toArray)
    val headers = collection.mutable.Buffer.empty[(String, Int)]
    class Serializer extends ToHtmlSerializer(new LinkRenderer){
      override def printImageTag(rendering: LinkRenderer.Rendering) {
        printer.print("<div style=\"text-align: center\"><img")
        printAttribute("src", rendering.href)
        // shouldn't include the alt attribute if its empty
        if(!rendering.text.equals("")){
          printAttribute("alt", rendering.text)
        }
        import collection.JavaConversions._
        for (attr <- rendering.attributes) {
          printAttribute(attr.name, attr.value)
        }
        printer.print(" style=\"max-width: 100%; max-height: 500px\"")
        printer.print(" /></div>")
      }
      override def visit(node: HeaderNode) = {
        val tag = "h" + node.getLevel()


        val id =
          node
            .getChildren
            .asScala
            .collect{case t: TextNode => t.getText}
            .mkString



        headers.append(id -> node.getLevel())


        val setId = s"id=${'"'+sanitizeAnchor(id)+'"'}"
        printer.print(s"""<$tag $setId class="${Styles.hoverBox.name}">""")
        visitChildren(node)
        printer.print(
          a(href := ("#" + sanitizeAnchor(id)), Styles.hoverLink)(
            i(cls := "fa fa-link", aria.hidden := true)
          ).render
        )
        printer.print(s"</$tag>")
      }

      override def visit(node: VerbatimNode) = {
        printer.println().print(
          """<pre style="background-color: #f8f8f8">""" +
          s"""<code style="white-space:pre; background-color: #f8f8f8" class="${node.getType()}">"""
        )

        var text = node.getText()
        // print HTML breaks for all initial newlines
        while(text.charAt(0) == '\n') {
          printer.print("\n")
          text = text.substring(1)
        }
        printer.printEncoded(text)
        printer.print("</code></pre>")
      }
      override def visit(node: TableNode) = {
        currentTableNode = node
        printer.print("<table class=\"table table-bordered\">")
        visitChildren(node)
        printer.print("</table>")
        currentTableNode = null
      }
    }

    val splitIndex =
      ast.getChildren
        .asScala
        .indexWhere{
          case n: SimpleNode if n.getType == SimpleNode.Type.HRule => true
          case _ => false
        }
    val (headerNodes, bodyNodes) = ast.getChildren.asScala.splitAt(splitIndex)

    ast.getChildren.clear()
    headerNodes.foreach(ast.getChildren.add)

    val rawHtmlSnippet = new Serializer().toHtml(ast)
    ast.getChildren.clear()
    bodyNodes.foreach(ast.getChildren.add)
    val postlude = Seq[Frag](
      hr,

      p(
        b("About the Author:"),
        i(
          " Haoyi is a software engineer, an early contributor to ", 
          a(href:="http://www.scala-js.org/")("Scala.js"), 
          ", and the author of many open-source Scala tools such as the ", 
          a(href:="lihaoyi.com/Ammonite", "Ammonite REPL"), " and ",
          a(href:="https://github.com/lihaoyi/fastparse", "FastParse"), ". "
        )
      ),  
      p(
        i(
          "If you've enjoyed this blog, or enjoyed using Haoyi's other open ",
          "source libraries, please chip in (or get your Company to chip in!) via ", 
          a(href:="https://www.patreon.com/lihaoyi", "Patreon"), " so he can ", "continue his open-source work"
        )
      ),
      hr
    ).render

    val rawHtmlContent = rawHtmlSnippet + new Serializer().toHtml(ast) + postlude
    // Handle both post/ and posts/ for legacy reasons
    val updates = DatesFor(s"post/$index - ").toSeq ++ DatesFor(s"posts/$index - ").toSeq
    PostInfo(name, headers, rawHtmlContent, rawHtmlSnippet, updates)
  }
}

def formatRssDate(date: java.time.LocalDate) = {
  date
    .atTime(0, 0)
    .atZone(java.time.ZoneId.of("UTC"))
    .format(java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME)
}
val rssXml = {
  val snippet = tag("rss")(attr("version") := "2.0")(
    tag("channel")(
      tag("title")("Haoyi's Programming Blog"),
      tag("link")("http://www.lihaoyi.com/"),
      tag("description"),

      for(post <- posts) yield tag("item")(
        tag("title")(post.name),
        tag("link")(s"http://www.lihaoyi.com/post/${sanitize(post.name)}.html"),
        tag("description")(post.rawHtmlSnippet),
        for ((sha, date) <- post.updates.lastOption)
        yield tag("pubDate")(formatRssDate(date)),
        for ((sha, date) <- post.updates.headOption)
        yield tag("lastBuildDate")(formatRssDate(date))
      )

    )
  )
  """<?xml version="1.0"?>""" + snippet.render
}

@main
def main(publish: Boolean = false) = {

  rm! targetFolder

  write(
    targetFolder/s"index.html",
    mainContent(posts.map(p => (p.name, p.rawHtmlSnippet, p.updates.lastOption)))
  )

  mkdir! targetFolder/'post
  for(otherFile <- otherFiles){
    cp(otherFile, targetFolder/'post/(otherFile relativeTo postsFolder))
  }

  cp(cwd/"favicon.png", targetFolder/"favicon.ico")

  for(i <- posts.indices){
    val post = posts(i)


    val adjacentLinks = div(display.flex, flexDirection.row, justifyContent.spaceBetween)(
      for((j, isNext) <- Seq(i-1 -> false, i+1 -> true))
      yield posts.lift(j) match{
        case None => div()
        case Some(dest) => renderAdjacentLink(isNext, dest.name)
      }
    )

    write(
      targetFolder/'post/s"${sanitize(post.name)}.html",
      postContent(post, adjacentLinks)
    )
  }

  write(targetFolder/"feed.xml", rssXml)
  if (publish){
    implicit val wd = cwd/'target
    write(wd/'CNAME, "www.lihaoyi.com")
    %git 'init
    %git('add, "-A", ".")
    %git('commit, "-am", "first commit")
    %git('remote, 'add, 'origin, "git@github.com:lihaoyi/lihaoyi.github.io.git")
    %git('push, "-uf", 'origin, 'master)
  }
}

// Load dependencies
load.ivy("org.pegdown" % "pegdown" % "1.6.0")
load.ivy("com.lihaoyi" %% "scalatags" % "0.5.3")
load.module(ammonite.ops.cwd/"styles.scala")
load.module(ammonite.ops.cwd/"pages.scala")
@
import scalatags.Text.all.{width, height, _}

import scalatags.Text._
import ammonite.ops._
import collection.JavaConverters._
import org.pegdown.{PegDownProcessor, ToHtmlSerializer, LinkRenderer, Extensions}
import org.pegdown.ast.{VerbatimNode, ExpImageNode, HeaderNode, TextNode, SimpleNode}


val postsFolder = cwd/'posts
val targetFolder = cwd/'target

object DatesFor{
  import ammonite.ops.ImplicitWd._
  val commitChunks = %%('git, 'log, "--date=short").out.string.split("\n(?=commit)")
  val commits = for(chunk <- commitChunks) yield {
    val lines = chunk.lines.toSeq
    val sha = lines(0).stripPrefix("commit ")
    val author = lines(1).stripPrefix("Author: ")
    val date = lines(2).stripPrefix("Date:   ")
    val files = %%('git, 'show, "--pretty=format:", "--name-only", sha).out.lines
    (sha, author, date, files)
  }

  val fileChanges = for{
    (sha, author, date, files) <- commits
    file <- files
  } yield (file, sha, author, date)

  def apply(filePrefix: String) = for {
    (file, sha, author, date) <- fileChanges
    if file.startsWith(filePrefix)
  } yield (sha, java.time.LocalDate.parse(date))

}

val (markdownFiles, otherFiles) = ls.rec! postsFolder partition (_.ext == "md")
// Walk the posts/ folder and parse out the name, full- and first-paragraph-
// HTML of each post to be used on their respective pages and on the index
val posts = {
  val split = for(path <- markdownFiles) yield {
    val Array(number, name) = path.last.split(" - ", 2)
    (number, name.stripSuffix(".md"), path)
  }
  for ((index, name, path) <- split.sortBy(_._1.toInt)) yield {
    val processor = new PegDownProcessor(Extensions.FENCED_CODE_BLOCKS)
    val ast = processor.parseMarkdown(read! path toArray)
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

        val setId = s"id=${'"'+sanitize(id)+'"'}"
        printer.print(s"<$tag $setId>")
        visitChildren(node)
        printer.print(s"</$tag>")
      }
      override def visit(node: VerbatimNode) = {
        printer.println().print("<pre><code class=\"" + node.getType() + "\">");

        var text = node.getText();
        // print HTML breaks for all initial newlines
        while(text.charAt(0) == '\n') {
          printer.print("<br/>");
          text = text.substring(1);
        }
        printer.printEncoded(text);
        printer.print("</code></pre>");
      }
    }
    val rawHtmlContent = new Serializer().toHtml(ast)
    val snippetNodes =
      ast.getChildren
         .asScala
         .takeWhile{
           case n: SimpleNode if n.getType == SimpleNode.Type.HRule => false
           case _ => true
         }

    ast.getChildren.clear()
    snippetNodes.foreach(ast.getChildren.add)

    val rawHtmlSnippet = new Serializer().toHtml(ast)
    val updates = DatesFor(s"posts/$index - ").toSeq
    (name, rawHtmlContent, rawHtmlSnippet, updates)
  }
}


def main(publish: Boolean = false) = {

  rm! targetFolder

  write(
    targetFolder/s"index.html",
    mainContent(posts)
  )

  mkdir! targetFolder/'post
  for(otherFile <- otherFiles){
    cp(otherFile, targetFolder/'post/(otherFile relativeTo postsFolder))
  }

  cp(cwd/"favicon.png", targetFolder/"favicon.ico")

  for((name, rawHtmlContent, _, dates) <- posts){
    write(
      targetFolder/'post/s"${sanitize(name)}.html",
      postContent(name, rawHtmlContent, dates)
    )
  }

  if (publish){
    implicit val publishWd = cwd/'target
    %git 'init
    %git('add, "-A", ".")
    %git('commit, "-am", "first commit")
    %git('remote, 'add, 'origin, "git@github.com:lihaoyi/lihaoyi.github.io.git")
    %git('push, "-uf", 'origin, 'master)
  }
}
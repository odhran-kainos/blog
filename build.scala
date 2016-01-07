// Load dependencies
load.ivy("org.pegdown" % "pegdown" % "1.6.0")
load.ivy("com.lihaoyi" %% "scalatags" % "0.5.3")
load.module(ammonite.ops.cwd/"styles.scala")
load.module(ammonite.ops.cwd/"pages.scala")
@
import scalatags.Text.all.{width, height, _}

import scalatags.Text._
import ammonite.ops._
import org.pegdown.{PegDownProcessor, ToHtmlSerializer, LinkRenderer}


val postsFolder = cwd/'posts
val targetFolder = cwd/'target

object DatesFor{
  import ammonite.ops.ImplicitWd._
  val commitChunks = %%('git, 'log, "--date=short").out.string.split("\n(?=commit)")
  val commits = for(chunk <- commitChunks.dropRight(1)) yield {
    val lines = chunk.lines.toSeq
    val sha = lines(0).stripPrefix("commit ")
    val author = lines(1).stripPrefix("Author: ")
    val date = lines(2).stripPrefix("Date:   ")
    val files = %%('git, 'show, "--pretty=\"format\"", "--name-only", sha).out.lines
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


// Walk the posts/ folder and parse out the name, full- and first-paragraph-
// HTML of each post to be used on their respective pages and on the index
val posts = {
  val split = for(path <- ls.rec! postsFolder if path.ext == "md") yield {
    val Array(number, name) = path.last.split(" - ")
    (number, name.stripSuffix(".md"), path)
  }
  for ((index, name, path) <- split.sortBy(_._1.toInt)) yield {
    val processor = new PegDownProcessor()
    val ast = processor.parseMarkdown(read! path toArray)
    val rawHtmlContent = new ToHtmlSerializer(new LinkRenderer).toHtml(ast)
    if (ast.getChildren.size > 0) {
      val firstNode = ast.getChildren.get(0)
      ast.getChildren.clear()
      ast.getChildren.add(firstNode)
    }
    val rawHtmlSnippet = new ToHtmlSerializer(new LinkRenderer).toHtml(ast)
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

  for((name, rawHtmlContent, _, dates) <- posts){
    write(
      targetFolder/'post/s"$name.html",
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
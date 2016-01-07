load.ivy("com.lihaoyi" %% "scalatags" % "0.5.3")
@

import scalatags.stylesheet._
import scalatags.Text.all.{width, height, _}
import scalatags.Text._

@

val marginWidth = "25%"
trait WideStyles extends StyleSheet{
  def header = cls(
    position.fixed,
    top := 0,
    bottom := 0,
    styles.width := marginWidth,
    justifyContent.center
  )
  def headerContent = cls(
    textAlign.center
  )
  def content = cls(
    padding := "2em 3em 0",
    padding := 48,
    marginLeft := marginWidth,
    boxSizing.`border-box`
  )
  def footer = cls(
    position.fixed,
    bottom := 0,
    styles.height := 50,
    styles.width := marginWidth
  )
}
val WideStyles = Sheet[WideStyles]
trait NarrowStyles extends StyleSheet{
  def header = cls(
    marginBottom := 10
  )
  def content = cls(
    padding := 16
  )
  def headerContent = cls(
    flexDirection.row,
    styles.width := "100%",
    display.flex
  )
  def linkFlex = cls(
    alignSelf.flexEnd
  )
}
val NarrowStyles = Sheet[NarrowStyles]
trait Styles extends StyleSheet{
  def header = cls(
    backgroundColor := "rgb(61, 79, 93)",
    padding := 20,
    display.flex,
    alignItems.center,
    boxSizing.`border-box`
  )
  def headerLinkBox = cls(
    flex := 1,
    display.flex,
    flexDirection.row,

    textAlign.center
  )
  def headerLink = cls(
    flex := 1,
    display.flex,
    justifyContent.center,
    alignItems.center,
    padding := "10px 10px"
  )
  def footer = cls(
    display.flex,
    justifyContent.center,
    color := "rgb(158, 167, 174)"
  )
  def subtleLink = cls(
    textDecoration.none
  )
}
val Styles = Sheet[Styles]
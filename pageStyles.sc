import $ivy.`com.lihaoyi::scalatags:0.6.0`

import scalatags.stylesheet._
import scalatags.Text.all._


val marginWidth = "25%"
object WideStyles extends StyleSheet{
  override def customSheetName = Some("WideStyles")
  def header = cls(
    position.fixed,
    top := 0,
    bottom := 0,
    width := marginWidth,
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
    height := 50,
    width := marginWidth
  )
}
object NarrowStyles extends StyleSheet{
  override def customSheetName = Some("NarrowStyles")
  def header = cls(
    marginBottom := 10
  )
  def content = cls(
    padding := 16
  )
  def headerContent = cls(
    flexDirection.row,
    width := "100%",
    display.flex
  )
  def linkFlex = cls(
    alignSelf.flexEnd
  )

  def flexFont = cls(
    fontSize := "4vw"
  )
}
object Styles extends StyleSheet{
  override def customSheetName = Some("Styles")
  def header = cls(
    backgroundColor := "rgb(61, 79, 93)",
    display.flex,
    alignItems.center,
    boxSizing.`border-box`
  )
  def headerLinkBox = cls(
    flex := 1,
    display.flex,
    flexDirection.column,

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

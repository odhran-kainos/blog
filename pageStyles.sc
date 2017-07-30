import $ivy.`com.lihaoyi::scalatags:0.6.5`

import scalatags.stylesheet._
import scalatags.Text.all._


val marginWidth = "25%"
object WideStyles extends StyleSheet{
  initStyleSheet()
  override def customSheetName = Some("WideStyles")
  def header = cls(
    position.fixed,
    top := 0,
    bottom := 0,
    width := marginWidth,
    justifyContent.center,
    display.flex,
    flexDirection.column
  )
  def tableOfContentsItem = cls(
    // We have to use inline-block and verticalAlign.middle and width: 100%
    // here, instead of simply using display.block, because display.block items
    // with overflow.hidden seem to misbehave and render badly in different ways
    // between firefox (renders correctly), chrome (body of list item is offset
    // one row from the bullet) and safari (bullet is entirely missing)
    display.`inline-block`,
    width := "100%",
    verticalAlign.middle,
    overflow.hidden,
    textOverflow.ellipsis

  )
  def tableOfContents = cls(
    display.flex,
    flexDirection.column,
    flexGrow := 1,
    flexShrink := 1,
    minHeight := 0,
    marginTop := 20,
    width := "100%"

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
  initStyleSheet()
  override def customSheetName = Some("NarrowStyles")
  def header = cls(
    marginBottom := 10
  )
  def tableOfContents = cls(display.none)
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
object Styles extends CascadingStyleSheet{
  initStyleSheet()
  override def customSheetName = Some("Styles")
  def hoverBox = cls(
    display.flex,
    flexDirection.row,
    alignItems.center,
    justifyContent.spaceBetween,
    &hover(
      hoverLink(
        opacity := 0.5
      )
    )
  )
  def hoverLink = cls(
    opacity := 0.1,
    &hover(
      opacity := 1.0
    )
  )
  def headerStyle = cls(
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
  def footerStyle = cls(
    display.flex,
    justifyContent.center,
    color := "rgb(158, 167, 174)"
  )
  def subtleLink = cls(
    textDecoration.none
  )
}

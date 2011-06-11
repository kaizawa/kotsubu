/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kotsubu

/**
 * SclollPane for each Timeline.
 * This ScrollPane exists for every Timeline Tab.
 */
import java.awt.Color
import java.awt.Dimension
import scala.swing.ScrollPane

/**
 * ScrollPane for TimeLine
 */
class TlScrollPane extends ScrollPane{
  var isFirstLoad:Boolean = true
  background = Color.white
  this.horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
  preferredSize = new Dimension(Main.mainFrameInitialWidth, Main.mainFrameInitialHeight)
}
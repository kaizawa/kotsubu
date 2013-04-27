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
import scala.swing.ScrollPane

/**
 * ScrollPane for TimeLine
 */
class TimeLineScrollPane extends ScrollPane{
  background = Color.white
  this.horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
}
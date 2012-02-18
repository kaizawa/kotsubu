/*
 * Copyright 2011 Kazuyoshi Aizawa
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package kotsubu

import java.awt.Dimension
import javax.swing.SwingUtilities
import scala.swing.Button
import scala.swing.GridPanel
import java.awt.Color
import scala.swing.Swing
import scala.swing.event.ButtonClicked
import twitter4j.Status

class OperationButtonPanel(status:Status) extends GridPanel(2,2) {
  val replyIcon = getImageIcon("kotsubu/arrow_turn_left.png")
  val rtIcon = getImageIcon("kotsubu/arrow_refresh.png")
  val rtwcIcon = getImageIcon("kotsubu/comment_add.png")
  val directIcon = getImageIcon("kotsubu/email_go.png")
  val testXArea = status.getText
  val user = status.getUser
  val username = user.getScreenName

  // ReTweet button
  val retweetButton = new Button {
    tooltip = "Official Retweet"
    icon = rtIcon
  }
  // Reply button
  val replyButton = new Button {
    tooltip = " Reply "
    icon = replyIcon
  }
  // Direct Messge Button
  val directMessageButton = new Button {
    tooltip = "Direct Message"
    icon = directIcon
  }
  // Official Retweet Button
  val retweetWithCommentButton = new Button {
    tooltip = "Retweet with comments"
    icon = rtwcIcon
  }

  background = Color.white
  contents += replyButton
  contents += retweetButton
  contents += retweetWithCommentButton
  contents += directMessageButton
  background_=(java.awt.Color.WHITE)
  border = Swing.EmptyBorder(0,0,5,0)
  preferredSize = new Dimension(Main.operationPanelWidth, Main.operationPanelWidth)
 
  /**
   * Function implictly convert function into Runnable.
   * This function is used by SingUtilities.invokeLater
   */
  implicit def functionToRunable[T](x: => T) : Runnable = new Runnable() { def run = x }  

  // Register hander for GUI event
  reactions += {
    case ButtonClicked(`replyButton`) =>  SwingUtilities invokeLater {
        Main.messageTextArea.text_=("@" + username + " ")
      }
    case ButtonClicked(`retweetWithCommentButton`) => SwingUtilities invokeLater {
        Main.messageTextArea.text_=("RT @" + username + " " +  status.getText)
      }
    case ButtonClicked(`retweetButton`) => {
        import scala.swing.Dialog._
        scala.swing.Dialog.showConfirmation(title="Retweet", message="Are you sure you want to retweet?") match {
          case Result.Yes => {
              try {
                TWFactory.getInstance.retweetStatus(status.getId)
              } catch { case _ =>}
            }
          case _ => 
        }
      }            
    case ButtonClicked(`directMessageButton`) =>  SwingUtilities invokeLater {
        Main.messageTextArea.text_=("d " + username + " ")
      }            
  }
  listenTo(replyButton, retweetButton, directMessageButton, retweetWithCommentButton)  
  
  def getImageIcon (path:String): javax.swing.ImageIcon = {
    new javax.swing.ImageIcon(javax.imageio.ImageIO.read(getClass().getClassLoader().getResource(path)))
  }
}
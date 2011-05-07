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

/*
 * Panel for infomation for each Status
 */
import java.awt.Color
import java.awt.Desktop
import java.net.URI
import java.text.SimpleDateFormat
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import scala.swing.BorderPanel
import scala.swing.EditorPane
import scala.swing.TextArea
import twitter4j.Status
import twitter4j.User

class StatusInfoPanel(status:Status) extends BorderPanel {
  val user = status.getUser
  val username = user.getScreenName
  // Create link to user's page
  val sbname:StringBuffer = new StringBuffer
  sbname.append("<B><a href=\"" + Main.friendsPage + username + "\">" + username + "</a> " + user.getName + "</B>")
  val usernameTextPane = new EditorPane(){
    background = Color.white
    contentType = ("text/html")        
    text = sbname.toString()
    editable = false
  }
  usernameTextPane.peer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  usernameTextPane.peer.addHyperlinkListener(new HyperlinkListener() {
      def hyperlinkUpdate(e:HyperlinkEvent) :Unit = {
        if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
          Desktop.getDesktop().browse(new URI(e.getDescription()));
        } }
    });
  
  val createdDate = status.getCreatedAt  
  
  import BorderPanel.Position._
  background = Color.white
  add(usernameTextPane, West)
  add(new TextArea(Main.simpleFormat.format(createdDate)), East)  
}
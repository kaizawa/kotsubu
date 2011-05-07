/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kotsubu

import java.awt.Color
import java.awt.Desktop
import java.awt.Dimension
import java.net.URI
import java.util.StringTokenizer
import javax.swing.JEditorPane
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import scala.swing.EditorPane
import twitter4j.Status

class MessageTextPane(status:Status, tlScrollPane:TlScrollPane) extends EditorPane {
  background = Color.white
  contentType = "text/html"
  editable = false
  
  // Regexp to check an user name start with @ char.
  val namefilter = """@([^:.]*)""".r
  // Regexp to check if it is URL
  val urlfilter = """([a-z]+)://(.*)""".r
  // Regexp ended by full-width space
  val urlfilterFullWidthSpace = """([a-z]+)://([^　]*)([　]*)(.*)""".r
  // Regexp ended by usernam
  val namefilterFullWidthSpace = """@([^　]*)([　]*)(.*)""".r
  // Create a link
  val tokenizer:StringTokenizer = new StringTokenizer(status.getText)
  val sb:StringBuffer = new StringBuffer()
  while (tokenizer.hasMoreTokens()) {
    tokenizer.nextToken() match {
      case urlfilter(protocol, url)
        =>  {sb.append("<a href=\"" + protocol + "://" + url + "\">"
                       + protocol + "://" + url + "</a>")}
      case namefilter(name)
        => {sb.append("<a href=\"" + Main.friendsPage + name + "\">@" + name + "</a>")}
      case word => {sb.append(word)}
        //TODO: make it handle Full-Width space char
    }
    sb.append(" ")
  }  
  text = sb.toString()
  // TODO: Consider the way of calculating height of timeline
  // -20 is not good way...
  // preferredSize = new Dimension(tlScrollPane.size.width - iconLabel.size.width - operationPanel.size.width, timeLineInitialHeight)
  preferredSize = new Dimension(tlScrollPane.size.width - Main.userIconSize - Main.operationPanelWidth - 40, Main.timeLineInitialHeight)
  peer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  peer.addHyperlinkListener(new HyperlinkListener() {
      def hyperlinkUpdate(e:HyperlinkEvent) :Unit = {
        if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
          Desktop.getDesktop().browse(new URI(e.getDescription()));
        } }
    });
}

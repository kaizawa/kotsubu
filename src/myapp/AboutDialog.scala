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

package myapp
import scala.swing._
import java.awt.Desktop
import java.net.URI
import javax.swing.JEditorPane
import javax.swing.UIManager
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import scala.swing.event._
import scala.xml._
import scala.actors._
import scala.actors.Actor._
import scala.collection.mutable._

/**
 * About ダイアログ
 * @param preferences 設定情報
 */
class AboutDialog(version:String)  extends scala.swing.Dialog() {
  val okButton = new Button ("OK")
  title = "About kotsubu"

  modal = true

  val sourceCodeUrl = "https://github.com/kaizawa/kotsubu"
  val linkTextPane = new EditorPane()
  linkTextPane.contentType_=("text/html")
  val sb:StringBuffer = new StringBuffer()
  sb.append("kotsubu ver " + version + "<br><br>")
  sb.append("Source code is available <a href=\"" + sourceCodeUrl + "\">here</a>.")
  linkTextPane.text_=(sb.toString())
  linkTextPane.editable_=(false)
  linkTextPane.peer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
  linkTextPane.background_=(UIManager.getColor("control")); // 背景色をウィンドウの背景色に合わせる
  linkTextPane.peer.addHyperlinkListener(new HyperlinkListener() {
      def hyperlinkUpdate(e:HyperlinkEvent) :Unit = {
        if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
          Desktop.getDesktop().browse(new URI(e.getDescription()));
        } }
    });

  contents = new BoxPanel (Orientation.Vertical){
    contents += linkTextPane
    contents += okButton
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }
  listenTo(okButton)

  reactions += {
    case ButtonClicked(`okButton`) => okAndClose()
  }

  /**
   * ダイヤログを閉じる
   */
  def okAndClose () :Unit = {
    closeOperation
    dispose
    close
  }

  size = new Dimension(270,140)
  this.resizable_=(false)
}


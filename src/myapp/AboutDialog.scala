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
 * About Dialog
 * @param preferences configuration
 */
class AboutDialog(version:String)  extends scala.swing.Dialog() {
  val okButton = new Button ("OK")
  title = "About kotsubu"

  modal = true

  val sourceCodeUrl = "https://github.com/kaizawa/kotsubu/wiki"
  val twitter4jUrl = "http://twitter4j.org/"
  val scalaUrl = "http://www.scala-lang.org/"
  val linkTextPane = new EditorPane()
  linkTextPane.contentType_=("text/html")
  val sb:StringBuffer = new StringBuffer()
  sb.append("<div align=\"CENTER\"><B><h2>kotsubu</h2></B>")  
  sb.append("kotsubu ver " + version + "<br>")
  sb.append("Copyright (c) 2011 Kazuyoshi Aizawa All rights reserved.<br>")  
  sb.append("<a href=\"" + sourceCodeUrl + "\">" + sourceCodeUrl +"</a><br><br>")
  sb.append("This program uses following libraries.<br><br>")
  sb.append("Twitter4J<br>")
  sb.append("Copyright (c) 2007, Yusuke Yamamoto All rights reserved.<br>")
  sb.append("<a href=\"" + twitter4jUrl + "\"> " + twitter4jUrl + "</a><br><br>")
  sb.append("Scala<br>")  
  sb.append("Copyright (c) 2002-2011 EPFL, Lausanne, unless otherwise specified.<br>")
  sb.append("All rights reserved.<br>")
  sb.append("<a href=\"" + scalaUrl + "\"> " + scalaUrl + "</a></DIV>")  

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
   * Close dialog
   */
  def okAndClose () :Unit = {
    closeOperation
    dispose
    close
  }

  size = new Dimension(510,350)
  this.resizable_=(false)
}


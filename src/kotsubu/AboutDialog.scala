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
  val famfamfamUrl = "http://www.famfamfam.com/lab/icons/silk/"


  val sb:StringBuffer = new StringBuffer()
  val sbLibs:StringBuffer = new StringBuffer()  
  
  sb.append("<div align=\"CENTER\"><B><h2>kotsubu</h2></B>")  
  sb.append("kotsubu ver " + version + "<br>")
  sb.append("Copyright (c) 2011 Kazuyoshi Aizawa All rights reserved.<br>")  
  sb.append("<a href=\"" + sourceCodeUrl + "\">" + sourceCodeUrl +"</a><br><br>")
  sb.append("This program uses Twitter4J, Scala and FAMFAMFAM SILK ICONS.<br><br></div>")
  sbLibs.append("<div align=\"CENTER\"><br>Twitter4J<br>")
  sbLibs.append("Copyright (c) 2007, Yusuke Yamamoto All rights reserved.<br>")
  sbLibs.append("<a href=\"" + twitter4jUrl + "\"> " + twitter4jUrl + "</a><br><br>")
  sbLibs.append("Scala<br>")  
  sbLibs.append("Copyright (c) 2002-2011 EPFL, Lausanne, unless otherwise specified.<br>")
  sbLibs.append("All rights reserved.<br>")
  sbLibs.append("<a href=\"" + scalaUrl + "\"> " + scalaUrl + "</a><br><br>")  
  sbLibs.append("FAMFAMFAM Silk Icons<br>")  
  sbLibs.append("Icons used for navigate buttons were writtern by Mark James.<br>")
  sbLibs.append("<a href=\"" + famfamfamUrl + "\"> " + famfamfamUrl + "</a><br><br></div>")    

  val linkTextPane = new EditorPane(){ 
    contentType = "text/html"
    text_=(sb.toString())
    editable_=(false)
    border = Swing.LineBorder(java.awt.Color.BLACK)
    background_=(java.awt.Color.WHITE)    
  }
  linkTextPane.peer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)  
  linkTextPane.peer.addHyperlinkListener(new HyperlinkListener() {
      def hyperlinkUpdate(e:HyperlinkEvent) :Unit = {
        if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
          Desktop.getDesktop().browse(new URI(e.getDescription()));
        } }
    });

  val linkTextPane4Libs = new EditorPane(){ 
    contentType = "text/html"    
    text_=(sbLibs.toString())
    editable_=(false)
    background_=(UIManager.getColor("control")); 
  }
  linkTextPane4Libs.peer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)  
  linkTextPane4Libs.peer.addHyperlinkListener(new HyperlinkListener() {    
      def hyperlinkUpdate(e:HyperlinkEvent) :Unit = {
        if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
          Desktop.getDesktop().browse(new URI(e.getDescription()));
        } }
    });  
 

  contents = new BoxPanel (Orientation.Vertical){
    contents += linkTextPane
    contents +=linkTextPane4Libs
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
  this.resizable_=(false)
}


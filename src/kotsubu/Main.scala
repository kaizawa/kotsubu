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
 * 
 * Changes: 

 1) Remove pref variable, instead uses Prefs object.

 */

package kotsubu

import scala.swing._
import scala.swing.event._
import scala.xml._
import javax.swing.ImageIcon
import javax.swing.JViewport
import javax.swing.SwingUtilities
import scala.actors._
import scala.actors.Actor._
import scala.collection.immutable._
import java.util.prefs.Preferences
import javax.imageio._
import scala.collection._
import java.text.SimpleDateFormat
import java.util.Date
import java.awt.Color
import twitter4j.TwitterException
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.auth.AccessToken
import scala.swing.Dialog._
import scala.collection.JavaConversions._ 

case class UpdateType(name:String)

/**
 * kotsubu - trival GUI twitter client utilizing twitter4j.
 *
 * Main Window
 */
object Main extends SimpleSwingApplication {
  val version = "0.1.21"  // version
  var currentUpdateType = UpdateType("home") // default time line  
  val mainFrameInitialWidth = 600
  val mainFrameInitialHeight = 600
  val operationPanelWidth = 60 // button size
  val userIconSize = 50 // Size of user icon
  val timeLineInitialHeight = 60 // Height of time line
  val friendsPage = "http://twitter.com/#!/"  
  val simpleFormat = new SimpleDateFormat("MM/dd HH:mm:ss")  

  /////////////  Panel for update button  //////////////
  val updateButton = Button("Update"){
    actor{
      updateTimeLine(currentUpdateType)
    }    
  }
  val updateButtonPanel = new BoxPanel(Orientation.Horizontal){
    contents += updateButton
  }

  ////////  Panel for timeline ////////////////////////
  val homeTlScrollPane = new TlScrollPane()
  val userTlScrollPane = new TlScrollPane()
  val publicTlScrollPane = new TlScrollPane()
  val mentionTlScrollPane = new TlScrollPane()  
  val tabbedPane = new TabbedPane {
    pages += new TabbedPane.Page("Home", homeTlScrollPane)
    pages += new TabbedPane.Page("My tweets", userTlScrollPane)
    pages += new TabbedPane.Page("Mention", mentionTlScrollPane)        
    pages += new TabbedPane.Page("Public", publicTlScrollPane)
  }

  //////// Panel for post message  /////////
  val postButton = Button("Post ") {
    postMessageAndClear(messageTextArea)   
  }
  val clearButton = Button("Clear") {
    SwingUtilities invokeLater {messageTextArea.text_=("")}
  }    
  val postButtonPanel = new BoxPanel (Orientation.Vertical){
    contents += postButton
    contents += clearButton
  }
  val messageTextArea = new TextArea{
    lineWrap = true
    text = "What's on your mind?"
    border = Swing.EmptyBorder(2, 2, 2, 2)
  }
  val postMessageScrollPane = new ScrollPane{
    scala.swing.ScrollPane
    this.horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
    viewportView = messageTextArea
    border = Swing.BeveledBorder(Swing.Lowered)
  }
  val kotsubuIconLabel = new Label
  val originalImage = javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("kotsubu/kotsubu.png"))
  val smallImage = originalImage.getScaledInstance(userIconSize,userIconSize, java.awt.Image.SCALE_SMOOTH)        
  kotsubuIconLabel.icon = new javax.swing.ImageIcon(smallImage)  
  val postMessagePanel = new BoxPanel(Orientation.Horizontal){
    contents += kotsubuIconLabel
    contents += postMessageScrollPane
    contents += postButtonPanel
  }

  ///////// Status Panel  ///////////
  val progressbar = new ProgressBar {
    labelPainted=true
    label = "No timeline fetched yet"
  }
  val progressBarPanel = new BoxPanel (Orientation.Horizontal){
    contents += progressbar
    preferredSize = new Dimension(30,30)
  }
  val statusPanel = new BoxPanel (Orientation.Horizontal ){
    contents += progressBarPanel
  }

  ///////////  Main Panel /////////////////////
  val mainPanel = new BoxPanel(Orientation.Vertical){
    contents += postMessagePanel
    contents += tabbedPane
    contents += updateButtonPanel
    contents += statusPanel
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }

  /**
   * Main window of kotsubu
   */
  def top = new MainFrame {
    title = "kotsubu"
    
    /*
     * Changed default thread number to 6, since default of 4 threads are
     * consumed by actor which sleeps on waiting periodical auto update.
     */    
    System.setProperty("actors.corePoolSize", Prefs.getInt("corePoolSize").toString)

    contents = mainPanel

    listenTo(mainPanel,tabbedPane.selection)    

    // Register handler for GUI event
    reactions += {       
      case SelectionChanged(`tabbedPane`) => tabbedPane.selection.page.title match {
          case "Home" => currentUpdateType_= (UpdateType("home"))
          case "My tweets" => currentUpdateType_=(UpdateType("user"))
          case "Public" => currentUpdateType_=(UpdateType("public"))
          case "Mention" => currentUpdateType_=(UpdateType("mention"))            
        }
    }

    // Menu bar
    menuBar = new MenuBar{
      contents += new Menu("File"){
        contents += new MenuItem(Action("Preferences"){
            new PreferencesDialog().visible_=(true)
          })
        contents += new MenuItem( Action("Quit"){System.exit(0)})        
      }
      contents += new Menu("Help") {
        contents += new MenuItem(Action ("About kotsubu"){
            new AboutDialog(version).visible_=(true)
          })}
    }
    size = new Dimension(mainFrameInitialWidth, mainFrameInitialHeight)
    minimumSize = size
    
    //Following line is used for OAuth test
    //prefs.put("accessToken", "")    
    
    // Star background auto-update thread.
    UpdateDaemon.startDaemon()
  }

  /**
   * Function implictly convert function into Runnable.
   * This function is used by SingUtilities.invokeLater
   */
  implicit def functionToRunable[T](x: => T) : Runnable = new Runnable() { def run = x }

  /**
   * Updte timeline 
   * @param updateType Timeline type to be updated.
   */
  def updateTimeLine(updateType:UpdateType) :Unit = {      
    try {
      // Start progress bar, if needed.
      if(Prefs.getBoolean("progressBarEnabled")){
        progressbar.indeterminate_=(true)
      }
      progressbar.label_=("Updating " + updateType.name + " timeline ...")

      val twitter:Twitter = TWFactory.getInstance
      val (tlScrollPane:TlScrollPane, statuses:ResponseList[Status]) = updateType match {
        case UpdateType("home") => (Main.homeTlScrollPane, twitter.getHomeTimeline())
        case UpdateType("user") => (Main.userTlScrollPane, twitter.getUserTimeline())
        case UpdateType("public") => (Main.publicTlScrollPane, twitter.getPublicTimeline())
        case UpdateType("mention") => (Main.mentionTlScrollPane, twitter.getMentions())
      }

      val oldTimeLinePanel = (tlScrollPane.viewportView) match {
        case Some(oldPanel:BoxPanel) => oldPanel 
        case None =>  new BoxPanel(Orientation.Vertical){background = Color.white}
      }      
      
      // get latest status ID of current timeline
      val lastid:Long = oldTimeLinePanel.contents.headOption match {
        case Some(statusPanel:StatusPanel) => statusPanel.status.getId
        case None => 0L
      }
      val oldStatusPanelList = oldTimeLinePanel.contents.toList
      
      // Convert java.util.List to scala.collection.List
      val statusList = asScalaBuffer(statuses).toList

      // Process statuses one by one.
      // Filter Statuses by status id, and create List of StatusPanel
      val newStatusPanelList:List[StatusPanel] 
      = (statusList filter (st => st.getId > lastid) map {
          new StatusPanel (tlScrollPane, _)
        }) 
      val numNewStatus = newStatusPanelList.length
      var cnt:Int = 0
      // Concatinate old and new statuses      
      // and shorten to meet maxStatuses      
      val statusPanelList = newStatusPanelList ::: oldStatusPanelList takeWhile( _ => {
          cnt = cnt + 1                  
          cnt <= Prefs.getInt("maxStatuses")
        }
      )

      val newTimeLinePanel = new BoxPanel(Orientation.Vertical){background = Color.white }
      statusPanelList foreach (newTimeLinePanel.contents.append(_))
      
      // Check position of current viewport
      val y = tlScrollPane.peer.getViewport.getViewPosition.y
      // replace previous viewport with new one.       
      // I can not use implicit declaration here, because it caused method to be
      // executed under other thread than EDT...I'm not sure how come it was..
      SwingUtilities invokeLater ( new Runnable(){
          def run = {
            newTimeLinePanel.visible_=(false) // don't show before changing position.
            tlScrollPane.viewportView_=(newTimeLinePanel)                
            val vp = tlScrollPane.peer.getViewport
            // Height of each statusPanels seems to be 89.
            val newY = tlScrollPane.isFirstLoad match {
              case true => {tlScrollPane.isFirstLoad_=(false); 0}
              case false => y + numNewStatus * 89
            }
            vp.setViewPosition(new Point(0, newY))             
            newTimeLinePanel.visible_=(true)        
          }
        }
      )
      // Stop progress bar
      progressbar.indeterminate_=(false)
      progressbar.label_= (updateType.name + " timeline updated on "
                           + simpleFormat.format(new Date()))
    } catch {
      case ex:TwitterException => {
          if(401 == ex.getStatusCode()){
            println("Unable to get the access token.");
          }else{
            println("Updating " + updateType.name + " timeline failed.")
            ex.printStackTrace
          }}}      
  }

  /**
   * Create new actor and post message
   * 
   * @param tf TextField which has message to post.
   */
  def postMessageAndClear(tf:TextArea) :Unit ={
    println("postMessageAcnClear called")
    val msg = tf.text
    SwingUtilities invokeLater tf.text_=("")
    actor {
      // Start progress bar if enabled
      if(Prefs.getBoolean("progressBarEnabled")){
        progressbar.indeterminate_=(true)
      }
      progressbar.label_=("Posting message ...")
      
      val accessToken:AccessToken = new AccessToken(Prefs.get("accessToken"),Prefs.get("accessTokenSecret"))
      val twitter:Twitter = TWFactory.getInstance
      val status = twitter.updateStatus(msg)

      // Stop progress bar
      if(Prefs.getBoolean("progressBarEnabled")){
        progressbar.indeterminate_=(false)
      }
      progressbar.label_=("Message posted")

      actor(updateTimeLine(currentUpdateType))
    }
  }
}
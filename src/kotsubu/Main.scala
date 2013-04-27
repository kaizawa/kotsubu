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
 */

package kotsubu

import scala.swing._
import scala.swing.event._
import scala.xml._
import javax.swing.ImageIcon
import javax.swing.SwingUtilities
import scala.actors._
import scala.actors.Actor._
import scala.collection.immutable._
import javax.imageio._
import scala.collection._
import java.text.SimpleDateFormat
import java.util.Date
import java.awt.Color
import twitter4j.TwitterException
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.Twitter
import twitter4j._
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
  val VERSION = "1.2.0"  // version
  var currentUpdateType = UpdateType("home") // default time line  
  val MAIN_FRAME_INITIAL_WIDTH = 600
  val MAIN_FRAME_INITIAL_HEIGHT = 600
  val OPERATION_PANEL_WIDTH = 60 // button size
  val OPERATION_BUTTON_WIDTH = 20  
  val USER_ICON_SIZE = 50 // Size of user icon
  val TIMELINE_INITIAL_HEIGHT = 110 // Height of time line
  val FRIEND_PAGE = "http://twitter.com/#!/"  
  val SIMPLE_FORMAT = new SimpleDateFormat("MM/dd HH:mm:ss")  

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
  val homeTimeLineScrollPane = new TimeLineScrollPane()
  val userTimeLineScrollPane = new TimeLineScrollPane()
  val mentionTimeLineScrollPane = new TimeLineScrollPane()  
  val tabbedPane = new TabbedPane {
    pages += new TabbedPane.Page("Home", homeTimeLineScrollPane)
    pages += new TabbedPane.Page("My tweets", userTimeLineScrollPane)
    pages += new TabbedPane.Page("Mention", mentionTimeLineScrollPane)        
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
  val smallImage = originalImage.getScaledInstance(USER_ICON_SIZE,USER_ICON_SIZE, java.awt.Image.SCALE_SMOOTH)        
  kotsubuIconLabel.icon = new javax.swing.ImageIcon(smallImage)  
  val postMessagePanel = new BoxPanel(Orientation.Horizontal){
    contents += kotsubuIconLabel
    contents += postMessageScrollPane
    contents += postButtonPanel
    maximumSize_=(new Dimension(Integer.MAX_VALUE, USER_ICON_SIZE))    
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
  val statusBarPanel = new BoxPanel (Orientation.Horizontal ){
    contents += progressBarPanel
  }

  ///////////  Main Panel /////////////////////
  val mainPanel = new BoxPanel(Orientation.Vertical){
    contents += postMessagePanel
    contents += tabbedPane
    contents += updateButtonPanel
    contents += statusBarPanel
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }

  /**
   * Main window of kotsubu
   */
  def top = new MainFrame {
    title = "kotsubu"
    
    /*
     * This is needed to avoid requestToken.getAuthorizationURL from 
     * returning null.
     */
    System.setProperty("twitter4j.http.useSSL", "true"); 
    
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
            new AboutDialog(VERSION).visible_=(true)
          })}
    }
    minimumSize_=(new Dimension(MAIN_FRAME_INITIAL_WIDTH, MAIN_FRAME_INITIAL_HEIGHT))
    
    //Following line is used for OAuth test
    //Prefs.put("accessToken", "")    
    
    // Start Stream API 
    startStream()

    // Star background auto-update thread.
    UpdateDaemon.startDaemon()  
  }
  
  def startStream(): Unit = {
    /*
     * Stream API (only home)
     * Thread created in TwitterStream.sample()
     */
    val twitterStream:TwitterStream = KotsubuTwitterFactory.getStreamInstance()
    twitterStream.addListener(listener);
    twitterStream.user();
  }  
  
  val listener:UserStreamListener  = new UserStreamListener() {

    def onStatus(status:Status) :Unit = {
      updateTimeLinePanel(UpdateType("home"), Main.homeTimeLineScrollPane, 
                          List(status));
    }

    // Members declared in twitter4j.StatusListener
    def onDeletionNotice(x$1: twitter4j.StatusDeletionNotice): Unit = {}
    def onScrubGeo(x$1: Long,x$2: Long): Unit = {}
    def onStallWarning(x$1: twitter4j.StallWarning): Unit = {}
    def onTrackLimitationNotice(x$1: Int): Unit = {}
  
    // Members declared in twitter4j.StreamListener
    def onException(x$1: Exception): Unit = {}
  
    // Members declared in twitter4j.UserStreamListener
    def onBlock(x$1: twitter4j.User,x$2: twitter4j.User): Unit = {}
    def onDeletionNotice(x$1: Long,x$2: Long): Unit = {}
    def onDirectMessage(x$1: twitter4j.DirectMessage): Unit = {}
    def onFavorite(x$1: twitter4j.User,x$2: twitter4j.User,
                   x$3: twitter4j.Status): Unit = {}
    def onFollow(x$1: twitter4j.User,x$2: twitter4j.User): Unit = {}
    def onFriendList(x$1: Array[Long]): Unit = {}
    def onUnblock(x$1: twitter4j.User,x$2: twitter4j.User): Unit = {}
    def onUnfavorite(x$1: twitter4j.User,x$2: twitter4j.User,
                     x$3: twitter4j.Status): Unit = {}
    def onUserListCreation(x$1: twitter4j.User,
                           x$2: twitter4j.UserList): Unit = {}
    def onUserListDeletion(x$1: twitter4j.User,
                           x$2: twitter4j.UserList): Unit = {}
    def onUserListMemberAddition(x$1: twitter4j.User,
                                 x$2: twitter4j.User,
                                 x$3: twitter4j.UserList): Unit = {}
    def onUserListMemberDeletion(x$1: twitter4j.User,x$2: twitter4j.User,
                                 x$3: twitter4j.UserList): Unit = {}
    def onUserListSubscription(x$1: twitter4j.User,x$2: twitter4j.User,
                               x$3: twitter4j.UserList): Unit = {}
    def onUserListUnsubscription(x$1: twitter4j.User,x$2: twitter4j.User,
                                 x$3: twitter4j.UserList): Unit = {}
    def onUserListUpdate(x$1: twitter4j.User,x$2: twitter4j.UserList): Unit = {}
    def onUserProfileUpdate(x$1: twitter4j.User): Unit = {}
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

    // Start progress bar, if needed.
    if(Prefs.getBoolean("progressBarEnabled")){
      progressbar.indeterminate_=(true)
    }
    progressbar.label_=("Updating " + updateType.name + " timeline ...")

    val twitter:Twitter = KotsubuTwitterFactory.getInstance
    val (tlScrollPane:TimeLineScrollPane, statuses:ResponseList[Status]) = updateType match {
      case UpdateType("user") => (Main.userTimeLineScrollPane, twitter.getUserTimeline())
      case UpdateType("mention") => (Main.mentionTimeLineScrollPane, twitter.getMentionsTimeline())
    }
    // Convert java.util.List to scala.collection.List
    val statusList = asScalaBuffer(statuses).toList
      
    updateTimeLinePanel(updateType, tlScrollPane, statusList);
  }
  
  def updateTimeLinePanel(updateType:UpdateType, timeLineScrollPane:TimeLineScrollPane, 
                          statusList:List[Status])
  {
    try {
      val oldTimeLinePanel = (timeLineScrollPane.viewportView) match {
        case Some(oldPanel:BoxPanel) => oldPanel 
        case _ => new BoxPanel(Orientation.Vertical){background = Color.white}
      }      
      
      // get latest status ID of current timeline
      val lastid:Long = oldTimeLinePanel.contents.headOption match {
        case Some(statusPanel:StatusPanel) => statusPanel.status.getId
        case _ => 0L
      }
      val oldStatusPanelList = oldTimeLinePanel.contents.toList
      
      // Process statuses one by one.
      // Filter Statuses by status id, and create List of StatusPanel
      val newStatusPanelList:List[StatusPanel] 
      = (statusList filter (st => st.getId > lastid) map {
          new StatusPanel (timeLineScrollPane, _)
        }) 
      val numNewStatus = newStatusPanelList.length
      // Concatinate old and new statuses      
      // and shorten to meet maxStatuses      
      val statusPanelList = newStatusPanelList ::: oldStatusPanelList take Prefs.getInt("maxStatuses")

      val newTimeLinePanel = new BoxPanel(Orientation.Vertical){background = Color.white }
      statusPanelList foreach (newTimeLinePanel.contents.append(_))
      
      // Check position of current viewport
      val y = timeLineScrollPane.peer.getViewport.getViewPosition.y
      // replace previous viewport with new one.       
      // I can not use implicit declaration here, because it caused method to be
      // executed under other thread than EDT...I'm not sure how come it was..
      SwingUtilities invokeLater ( new Runnable(){
          def run = {
            // don't show before changing position.
            newTimeLinePanel.visible_=(false) 
            timeLineScrollPane.viewportView_=(newTimeLinePanel)                
            val vp = timeLineScrollPane.peer.getViewport
            // Height of each statusPanels seems to be TIMELINE_INITIAL_HEIGHT(previously 89).
            val newY = timeLineScrollPane.contents.size match {
              case 0 => 0
              case _ => y + numNewStatus * TIMELINE_INITIAL_HEIGHT
            }
            vp.setViewPosition(new Point(0, newY))             
            newTimeLinePanel.visible_=(true)        
          }
        }
      )
      // Stop progress bar
      progressbar.indeterminate_=(false)
      progressbar.label_= (updateType.name + " timeline updated on "
                           + SIMPLE_FORMAT.format(new Date()))
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
    //println("postMessageAcnClear called")
    val msg = tf.text
    
    SwingUtilities invokeLater tf.text_=("")
    actor {
      // Start progress bar if enabled
      if(Prefs.getBoolean("progressBarEnabled")){
        progressbar.indeterminate_=(true)
      }
      progressbar.label_=("Posting message ...")
      
      val accessToken:AccessToken = 
        new AccessToken(Prefs.get("accessToken"),Prefs.get("accessTokenSecret"))
      val twitter:Twitter = KotsubuTwitterFactory.getInstance
      val status = try { 
        twitter.updateStatus(msg)
      }  catch {
        case ex:TwitterException => null
      } 

      // Stop progress bar
      if(Prefs.getBoolean("progressBarEnabled")){
        progressbar.indeterminate_=(false)
      }
      
      status match {
        case null => progressbar.label_=("Post failed.")        
        case _ => {
            progressbar.label_=("Message posted")
            actor(updateTimeLine(currentUpdateType))
          }
      }
    }
  }  
}
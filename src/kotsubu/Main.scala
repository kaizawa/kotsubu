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
 * Changes: Post,Cancel,Update button to Actions model.
 */

package kotsubu

import scala.swing._
import javax.swing.event.HyperlinkEvent
import javax.swing.event.HyperlinkListener
import scala.swing.event._
import scala.xml._
import javax.swing.ImageIcon
import javax.swing.JEditorPane
import javax.swing.SwingUtilities
import scala.actors._
import scala.actors.Actor._
import java.awt.Desktop
import java.awt.image.BufferedImage
import java.net.URI
import scala.collection.mutable._
import java.util.StringTokenizer
import java.util.prefs.Preferences
import javax.imageio._
import scala.collection._
import java.text.SimpleDateFormat
import java.util.Date
import java.awt.Color
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.User
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken
import scala.swing.Dialog._

case class UpdateType(name:String)

/**
 * kotsubu - trival GUI twitter client utilizing twitter4j.
 *
 * Main Window
 */
object Main extends SimpleSwingApplication {
  val version = "0.1.14"  // version
  val prefs:Preferences = Preferences.userNodeForPackage(this.getClass())
  var currentUpdateType = UpdateType("home") // default time line  
  val imageIconMap = mutable.Map.empty[String, javax.swing.ImageIcon]
  val mainFrameInitialWidth = 600
  val mainFrameInitialHeight = 600
  val operationPanelWidth = 60 // button size
  val userIconSize = 50 // Size of user icon
  val timeLineInitialHeight = 60 // Height of time line
  val defAutoUpdateEnabled = true // Default value for auto-update
  val defHomeUpdateInterval = 30 // Default interval of auto-update
  val defUserUpdateInterval = 120 // Default interval of auto-update
  val defPublicUpdateInterval = 360 // Default interval of auto-update
  val defMentionUpdateInterval = 120 // Default interval of auto-update  
  val defProgressBarEnabled = false // Enable/Disable progress bar.
  val defNumTimeLines = 100 // Number of tweets to be shown.
  val defOAuthConsumerKey = "PaWXdbUBNZGJVuDqxFY8wg"
  val defConsumerSecret = "fD9SO5gUNYP9AuhCSYuob9inQU0jKl5bPMuGj1QRkFo"
  val replyIcon = new javax.swing.ImageIcon(javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("kotsubu/arrow_turn_left.png")))
  val rtIcon = new javax.swing.ImageIcon(javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("kotsubu/arrow_refresh.png")))  
  val rtwcIcon = new javax.swing.ImageIcon(javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("kotsubu/comment_add.png")))  
  val directIcon = new javax.swing.ImageIcon(javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("kotsubu/email_go.png")))  

  
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
     *  常に各タイムラインの自動アップデートの待機用の Actor が Timeline 数分だけ(今は4つ)
     *  動いているので、デフォルトの4つ(Core数x2)では、他の Actor が(例えば Update 用)
     *  自動アップデートが終了する合間でしか動作できなくなってしまう。
     *  なので、余裕をもって 10 この core Pool を設定しておく。
     */    
    System.setProperty("actors.corePoolSize", "10")

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
//    println(updateType.name + ":calling updateTimeLine") //TODO: remove
    try {
      // Start progress bar, if needed.
      if(prefs.getBoolean("progressBarEnabled", defProgressBarEnabled)){
        progressbar.indeterminate_=(true)
      }
      progressbar.label_=("Updating " + updateType.name + " timeline ...")

      if(prefs.get("accessToken", "").isEmpty){
        val twitter:Twitter = new TwitterFactory().getInstance()
        twitter.setOAuthConsumer(prefs.get("OAuthConsumerKey", defOAuthConsumerKey), prefs.get("consumerSecret", defConsumerSecret));
        val requestToken:RequestToken  = twitter.getOAuthRequestToken()

        Desktop.getDesktop().browse(new URI(requestToken.getAuthorizationURL()))
        scala.swing.Dialog.showMessage(title="confirm", message="After you allowed , click OK")
        val accessToken:AccessToken = twitter.getOAuthAccessToken();
        prefs.put("accessToken", accessToken.getToken)
        prefs.put("accessTokenSecret", accessToken.getTokenSecret)        
      }

      val accessToken:AccessToken = new AccessToken(prefs.get("accessToken", ""),prefs.get("accessTokenSecret", ""))
      val twitter:Twitter = new TwitterFactory().getInstance()
      twitter.setOAuthConsumer(prefs.get("OAuthConsumerKey", defOAuthConsumerKey), prefs.get("consumerSecret", defConsumerSecret));
      twitter.setOAuthAccessToken(accessToken)

      val (tlScrollPane:TlScrollPane, statuses:ResponseList[Status]) = updateType match {
        case UpdateType("home") => (Main.homeTlScrollPane, twitter.getHomeTimeline())
        case UpdateType("user") => (Main.userTlScrollPane, twitter.getUserTimeline())
        case UpdateType("public") => (Main.publicTlScrollPane, twitter.getPublicTimeline())
        case UpdateType("mention") => (Main.mentionTlScrollPane, twitter.getMentions())
      }
//      println(updateType.name + ": getTimeline completed") //TODO: remove
      
      val timeLinePanel = (tlScrollPane.viewportView) match {
        case Some(oldPanel:BoxPanel) => oldPanel
        case None =>  new BoxPanel(Orientation.Vertical){background = Color.white}
      }      
      tlScrollPane.viewportView match {
        case None =>  SwingUtilities invokeLater {
            tlScrollPane.viewportView_=(timeLinePanel)}              
        case _ =>
      }      
        
      println(updateType.name + ": size=" + timeLinePanel.contents.length 
              + ", x=" +tlScrollPane.peer.getViewport.getViewPosition.x
              + ", y=" +tlScrollPane.peer.getViewport.getViewPosition.y) //TODO: remove     

      val simpleFormat = new SimpleDateFormat("MM/dd HH:mm")
      val friendsPage = "http://twitter.com/#!/"
      
      val lastid:Long = timeLinePanel.contents.headOption match {
        case Some(statusPanel:StatusPanel) => statusPanel.status.getId
        case None => 0L
      }

//      println("Calling for Last ID = " + lastid)//TODO: remove
      // Process statuses one by one.
      var count:Int = 0
      for (status:Status <- statuses.toArray(new Array[Status](0)).reverse) {
        val user = status.getUser        
//        println("  " + status.getId + ":" + updateType.name + ":" + user.getScreenName) //TODO: remove

        // すでに読み込んだツイートは無視。        
        if(status.getId > lastid){
          // Icon. Load from server, if it is not cached yet.
          val iconLabel = new Label
          iconLabel.icon = imageIconMap get (user.getScreenName) match {
            case Some(icon) => icon
            case None => loadIconAndStore(user)
          }

          val createdDate = status.getCreatedAt
          val username = user.getScreenName

          // Create link to user's page
          val sbname:StringBuffer = new StringBuffer()
          sbname.append("<B><a href=\"" + friendsPage + username + "\">" + username + "</a> " + user.getName + "</B>")
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
          // Information Panel 
          val tweetInfoPanel = new BorderPanel(){
            import BorderPanel.Position._
            background = Color.white
            add(usernameTextPane, West)
            add(new TextArea(simpleFormat.format(createdDate)), East)
          }

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
        
          // Operational Panel
          val operationPanel = new GridPanel(2,2){
            background = Color.white
            import BorderPanel.Position._
            contents += replyButton
            contents += retweetButton
            contents += retweetWithCommentButton
            contents += directMessageButton
            background_=(java.awt.Color.WHITE)
            border = Swing.EmptyBorder(0,0,5,0)
          }

          // Register hander for GUI event
          operationPanel.reactions += {
            case ButtonClicked(`replyButton`) =>  SwingUtilities invokeLater {
                messageTextArea.text_=("@" + username + " ")
              }
            case ButtonClicked(`retweetWithCommentButton`) => SwingUtilities invokeLater {
                messageTextArea.text_=("RT @" + username + " " +  status.getText)
              }
            case ButtonClicked(`retweetButton`) => {
                scala.swing.Dialog.showConfirmation(title="Retweet", message="Are you sure you want to retweet?") match {
                  case Result.Yes => {
                      try {
                        twitter.retweetStatus(status.getId)
                      } catch { case _ =>}
                    }
                  case _ => 
                }
              }            
            case ButtonClicked(`directMessageButton`) =>  SwingUtilities invokeLater {
                messageTextArea.text_=("d " + username + " ")
              }            
          }

          operationPanel.listenTo(replyButton, retweetButton, directMessageButton, retweetWithCommentButton)

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
                => {sb.append("<a href=\"" + friendsPage + name + "\">@" + name + "</a>")}
              case word => {sb.append(word)}
                //TODO: make it handle Full-Width space char
            }
            sb.append(" ")
          }
          val messageTextPane = new EditorPane(){
            background = Color.white
            contentType = "text/html"
            editable = false
            text = sb.toString()
            // TODO: Consider the way of calculating height of timeline
            // -20 is not good way...
            // preferredSize = new Dimension(tlScrollPane.size.width - iconLabel.size.width - operationPanel.size.width, timeLineInitialHeight)
            preferredSize = new Dimension(tlScrollPane.size.width - userIconSize - operationPanelWidth - 40, timeLineInitialHeight)
          }
          messageTextPane.peer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
          messageTextPane.peer.addHyperlinkListener(new HyperlinkListener() {
              def hyperlinkUpdate(e:HyperlinkEvent) :Unit = {
                if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
                  Desktop.getDesktop().browse(new URI(e.getDescription()));
                } }
            });

          // Consolidate user info, icon and status
          val statusPanel = new StatusPanel (){
            background = Color.white
            import BorderPanel.Position._
            add(new BorderPanel (){
                background = Color.white
                border = Swing.EmptyBorder(2, 0, 2, 0)
                add(iconLabel, North)
              }, West)
            add(new BorderPanel (){
                border = Swing.EmptyBorder(0, 5, 0, 5)
                background = Color.white
                add(tweetInfoPanel, North)
                add(messageTextPane, Center)
                add(operationPanel, East)
              }, Center)
            add(new Separator{background = Color.white}, South)
          }
          statusPanel.status_=(status)
          if(timeLinePanel.contents.length >= Main.prefs.getInt("numTimeLines", Main.defNumTimeLines)){
            SwingUtilities invokeLater {
              timeLinePanel.contents.remove(timeLinePanel.contents.length -1)
            }            
          }
          SwingUtilities invokeLater {
            timeLinePanel.contents.insert(0, statusPanel)
          }
          count += 1
        }
      }
      val vp = tlScrollPane.peer.getViewport
      vp.setViewPosition(new Point(vp.getViewPosition.x, vp.getViewPosition.y + count * 89))
      SwingUtilities invokeLater {
        tlScrollPane.viewportView_=(timeLinePanel)
      }
      println("scrollPane height="+tlScrollPane.size.height)                  
      println("timeLinePa height="+timeLinePanel.size.height)                        
      // Stop progress bar
      if(prefs.getBoolean("progressBarEnabled",defProgressBarEnabled)){
        progressbar.indeterminate_=(false)
      }
      progressbar.label_= (updateType.name + " timeline updated on "
                           + simpleFormat.format(new Date()))
    } catch {
      case ex:TwitterException => {
          if(401 == ex.getStatusCode()){
            println("Unable to get the access token.");
          }else{
            println("Updating " + updateType.name + " timeline failed.")
          }
        }
    }      
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
      if(prefs.getBoolean("progressBarEnabled",defProgressBarEnabled)){
        progressbar.indeterminate_=(true)
      }
      progressbar.label_=("Posting message ...")
      
      val accessToken:AccessToken = new AccessToken(prefs.get("accessToken", ""),prefs.get("accessTokenSecret", ""))
      val twitter:Twitter = new TwitterFactory().getInstance()
      twitter.setOAuthConsumer(prefs.get("OAuthConsumerKey", defOAuthConsumerKey), prefs.get("consumerSecret", defConsumerSecret));
      twitter.setOAuthAccessToken(accessToken)    
      val status = twitter.updateStatus(msg)

      // Stop progress bar
      if(prefs.getBoolean("progressBarEnabled",defProgressBarEnabled)){
        progressbar.indeterminate_=(false)
      }
      progressbar.label_=("Message posted")

      actor(updateTimeLine(currentUpdateType))
    }
  }

  /**
   * Add icons to Map 
   * @param user user entry.
   * @return added icon
   *
   * TODO: icon cache mechanism is too bad..
   */
  def loadIconAndStore(user:User) :javax.swing.ImageIcon = {
    val username = user.getScreenName
    var originalImage:BufferedImage = null
    try {
      // Read original icon stored in Twitter.      
      originalImage = javax.imageio.ImageIO.read(user.getProfileImageURL)
    } catch {
      case ex: Exception => 
        println("Can't read icon from " + user.getProfileImageURL.toString + ". Use default icon.")
    }

    val image:ImageIcon = originalImage match {
      // Use default icon, if original icon is not found.
      case null => new javax.swing.ImageIcon(javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("kotsubu/default.png")))
        // Set size to 50x50        
      case _ => new javax.swing.ImageIcon(originalImage.getScaledInstance(userIconSize,userIconSize, java.awt.Image.SCALE_SMOOTH))
    }
    // Clear all cached icon, if it exceed 500....
    if(imageIconMap.size > 500){
      println("Clear imageIconMap.")
      imageIconMap.clear
    }
    // Set icon as a label's icon.
    imageIconMap += (username -> image)
    return imageIconMap(username)
  }
}

/**
 * SclollPane for each Timeline.
 * This ScrollPane exists for every Timeline Tab.
 */
class TlScrollPane extends ScrollPane{
  this.horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
  preferredSize = new Dimension(Main.mainFrameInitialWidth, Main.mainFrameInitialHeight)
}

class StatusPanel extends BorderPanel {
  var status:Status = null
}


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
import org.apache.http.auth._
import twitter4j.TwitterException
import twitter4j.TwitterFactory
import twitter4j.User
import twitter4j.ResponseList
import twitter4j.Status
import twitter4j.Twitter
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken

case class UpdateType(name:String)

/**
 * kotsubu - trival GUI twitter client utilizing twitter4j.
 *
 * Main Window
 */
object Main extends SimpleSwingApplication {
  val version = "0.1.3"  // version
  var currentUpdateType = UpdateType("home")  // default time line
  val prefs:Preferences = Preferences.userNodeForPackage(this.getClass())
  val imageIconMap = mutable.Map.empty[String, javax.swing.ImageIcon]
  val mainFrameInitialWidth = 600
  val mainFrameInitialHeight = 600
  val operationPanelWidth = 60 // button size
  val userIconSize = 50 // Size of user icon
  val timeLineInitialHeight = 60 // Height of time line
  val defAutoUpdateEnabled = true // Default value for auto-update
  val defHomeUpdateInterval = 30 // Default interval of auto-update
  val defMyUpdateInterval = 30 // Default interval of auto-update
  val defEveryoneUpdateInterval = 30 // Default interval of auto-update
  val defProgressBarEnabled = false // Enable/Disable progress bar.
  val defNumTimeLines = 20 // Number of tweets to be shown.
  val defOAuthConsumerKey = "PaWXdbUBNZGJVuDqxFY8wg"
  val defConsumerSecret = "fD9SO5gUNYP9AuhCSYuob9inQU0jKl5bPMuGj1QRkFo"
  val  replyIcon = new javax.swing.ImageIcon(javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("myapp/arrow_turn_left.png")))
  val  rtIcon = new javax.swing.ImageIcon(javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("myapp/arrow_refresh.png")))  


  /////////////  Panel for update button  //////////////
  val updateButton = new Button("update")
  val updateButtonPanel = new BoxPanel(Orientation.Horizontal){
    contents += updateButton
  }

  ////////  Panel for timeline ////////////////////////
  val homeTlScrollPane = new TlScrollPane()
  val myTlScrollPane = new TlScrollPane()
  val everyoneTlScrollPane = new TlScrollPane()
  val mentionTlScrollPane = new TlScrollPane()  
  val tabbedPane = new TabbedPane {
    pages += new TabbedPane.Page("Home", homeTlScrollPane)
    pages += new TabbedPane.Page("My tweets", myTlScrollPane)
    pages += new TabbedPane.Page("Mention", mentionTlScrollPane)        
    pages += new TabbedPane.Page("Everyone", everyoneTlScrollPane)
  }

  //////// Panel for post message  /////////
  val postButton = new Button {
    text = "Post "
  }
  val clearButton = new Button {
    text = "Clear"
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
  val originalImage = javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("myapp/kotsubu.png"))
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

    contents = mainPanel

    listenTo(updateButton,postButton,clearButton,mainPanel,tabbedPane.selection)

    // Register handler for GUI event
    reactions += {
      case ButtonClicked(`updateButton`) => actor{
          updateTimeLine(currentUpdateType)
        }
      case ButtonClicked(`postButton`) => postMessageAndClear(messageTextArea)
      case ButtonClicked(`clearButton`) => SwingUtilities invokeLater {
          messageTextArea.text_=("")
        }
      case SelectionChanged(`tabbedPane`) => tabbedPane.selection.page.title match {
          case "Home" => currentUpdateType_= (UpdateType("home"))
          case "My tweets" => currentUpdateType_=(UpdateType("users"))
          case "Everyone" => currentUpdateType_=(UpdateType("public"))
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
    var tlScrollPane:TlScrollPane = null
    var timezone = ""

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
      var accessToken:AccessToken = null;
      scala.swing.Dialog.showMessage(title="confirm", message="Click OK")
      try{
        accessToken = twitter.getOAuthAccessToken();
      } catch {
        case ex:TwitterException => {
            if(401 == ex.getStatusCode()){
              System.out.println("Unable to get the access token.");
            }else{
              ex.printStackTrace();
            }
          }
          return
      }
      prefs.put("accessToken", accessToken.getToken)
      prefs.put("accessTokenSecret", accessToken.getTokenSecret)
    }
    val accessToken:AccessToken = new AccessToken(prefs.get("accessToken", ""),prefs.get("accessTokenSecret", ""))
    val twitter:Twitter = new TwitterFactory().getInstance()
    twitter.setOAuthConsumer(prefs.get("OAuthConsumerKey", defOAuthConsumerKey), prefs.get("consumerSecret", defConsumerSecret));
    twitter.setOAuthAccessToken(accessToken)

    var statuses:ResponseList[Status] = null
    updateType match {
      case t if t == UpdateType("home") => {
          tlScrollPane = Main.homeTlScrollPane 
          statuses = twitter.getHomeTimeline()          
        }
      case t if t == UpdateType("users")   => {
          tlScrollPane = Main.myTlScrollPane 
          statuses = twitter.getUserTimeline()                    
        }
      case t if t == UpdateType("public")  => {
          tlScrollPane = Main.everyoneTlScrollPane 
          statuses = twitter.getPublicTimeline()                    
        }
      case t if t == UpdateType("mention")  => {
          tlScrollPane = Main.mentionTlScrollPane 
          statuses = twitter.getMentions()                    
        }        
    }

    val timeLineList = new BoxPanel(Orientation.Vertical){
      background = Color.white
    }
    val simpleFormat = new SimpleDateFormat("MM/dd HH:mm")
    val friendsPage = "http://twitter.com/#!/"

    // Process statuses one by one.
    for (status <- statuses.toArray(new Array[Status](0))) {
      val user = status.getUser

      // Icon. Load from server, if it is not cached yet.
      val iconLabel = new Label
      iconLabel.icon = imageIconMap get (user.getScreenName) match {
        case Some(status) => status
        case None => loadIconAndStore(user)
      }

      val createdDate = status.getCreatedAt
      val username = user.getScreenName

      // Create link to user's page
      val sbname:StringBuffer = new StringBuffer()
      sbname.append("<B><a href=\"" + friendsPage + username + "\">" + username + "</a></B>")
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
        tooltip = "Retweet"
        icon = rtIcon
      }

      // Reply button
      val replyButton = new Button {
        tooltip = " Reply "
        icon = replyIcon
      }

      // Operational Panel
      val operationPanel = new BorderPanel(){
        background = Color.white
        import BorderPanel.Position._
        add(replyButton, North)
        add(retweetButton, South)
        background_=(java.awt.Color.WHITE)
        border = Swing.EmptyBorder(0,0,5,0)
      }

      // Register hander for GUI event
      operationPanel.reactions += {
        case ButtonClicked(`replyButton`) =>  SwingUtilities invokeLater {
            messageTextArea.text_=("@" + username + " ")
          }
        case ButtonClicked(`retweetButton`) => SwingUtilities invokeLater {
            messageTextArea.text_=("RT @" + username + " " +  status.getText)
          }
      }

      operationPanel.listenTo(replyButton, retweetButton)

      // Regexp to check an user name start with @ char.
      val namefilter = """@([^:.]*)""".r
      // Regexp to check if it is URL
      val urlfilter = """([a-z]+)://(.*)""".r
      // Regexp ended by full-width space
      val urlfilterFullWidthSpace = """([a-z]+)://([^　]*)([　]*)(.*)""".r
      // Regexp ended by usernam
      val namefilterFullWidthSpace = """@([^　]*)([　]*)(.*)""".r
      /*
       * メッセージをスペース区切りで分割し、 @ から始まるユーザ名と URL を探して HTML
       * の A タグを使ってリンクテキストを生成する。
       */
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
            //TODL: 全角スペース付きのURL,名前の場合を追加
        }
        sb.append(" ")
      }
      val messageTextPane = new EditorPane(){
        background = Color.white
        contentType = "text/html"
        editable = false
        text = sb.toString()
        // TODO: TLの高さを自動計算. 幅の計算方法の変更
        // -20 という数値は Windows 上で実行咲いた際のごさを埋めるためのもの。ただしい計算方法を考える必要がある。
        // preferredSize = new Dimension(tlScrollPane.size.width - iconLabel.size.width - operationPanel.size.width, timeLineInitialHeight)
        preferredSize = new Dimension(tlScrollPane.size.width - userIconSize - operationPanelWidth - 20, timeLineInitialHeight)
      }
      messageTextPane.peer.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, true)
      messageTextPane.peer.addHyperlinkListener(new HyperlinkListener() {
          def hyperlinkUpdate(e:HyperlinkEvent) :Unit = {
            if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
              Desktop.getDesktop().browse(new URI(e.getDescription()));
            } }
        });

      // アイコンとメッセージを一つにまとめる。
      val timeLine = new BorderPanel (){
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
      }
      timeLineList.contents += timeLine
      timeLineList.contents += new Separator{
        background = Color.white
      }
    }

    // 以下は functionToRunable で暗黙的に Runnable に変換される。
    SwingUtilities invokeLater {
      //取得したタイムラインで既存のものを置き換える
      tlScrollPane.viewportView_=(timeLineList)
    }
    //プログレスバー停止
    if(prefs.getBoolean("progressBarEnabled",defProgressBarEnabled)){
      progressbar.indeterminate_=(false)
    }
    progressbar.label_= (updateType.name + " timeline updated on "
                         + simpleFormat.format(new Date()))
  }

  /**
   * 新いい actor を生成し、actor TextField の内容を postMessage() に渡し、
   * サーバにポストしてもらう。その後、TextField は空にする。
   * @param tf postするメッセージを含むTextField
   */
  def postMessageAndClear(tf:TextArea) :Unit ={
    // アクターでバックグラウンドでポストする
    //println("postMessageAcnClear called")
    val msg = tf.text
    SwingUtilities invokeLater tf.text_=("")
    actor {
      //プログレスバー開始
      if(prefs.getBoolean("progressBarEnabled",defProgressBarEnabled)){
        progressbar.indeterminate_=(true)
      }
      progressbar.label_=("Posting message ...")
      
      val accessToken:AccessToken = new AccessToken(prefs.get("accessToken", ""),prefs.get("accessTokenSecret", ""))
      val twitter:Twitter = new TwitterFactory().getInstance()
      twitter.setOAuthConsumer(prefs.get("OAuthConsumerKey", defOAuthConsumerKey), prefs.get("consumerSecret", defConsumerSecret));
      twitter.setOAuthAccessToken(accessToken)    
      val status = twitter.updateStatus(msg)

      //プログレスバー停止
      if(prefs.getBoolean("progressBarEnabled",defProgressBarEnabled)){
        progressbar.indeterminate_=(false)
      }
      progressbar.label_=("Message posted")

      actor(updateTimeLine(currentUpdateType))
    }
  }

  /**
   * ユーザアイコンをサーバから読み込みマップに追加する。
   * @param サーバから返却されてきた1メッセージ分のノード
   * @return 追加されたアイコン
   *
   * TODO: icon マップのエントリ数の制限が設定されていない。危険は低いと思うが上限値を持つべき。
   */
  def loadIconAndStore(user:User) :javax.swing.ImageIcon = {
    val username = user.getScreenName
    var image:ImageIcon = null
    var originalImage:BufferedImage = null

    
    // Twitter に格納されているオリジナルのアイコンを読み込む
    try {
      originalImage = javax.imageio.ImageIO.read(user.getProfileImageURL)
    } catch {
      case ex: IIOException => 
        println("Can't read icon from " + user.getProfileImageURL.toString + ". Use default icon.")
    }

    if(originalImage == null){
      // もしユーザアイコンが見つけられなかったらデフォルトアイコンを表示
      image = new javax.swing.ImageIcon(javax.imageio.ImageIO.read(getClass().getClassLoader().getResource("myapp/default.png")))
    } else {
      // オリジナルのアイコンから指定サイズ(デフォルトは50x50)のイメージを作り出す。
      val smallImage = originalImage.getScaledInstance(userIconSize,userIconSize, java.awt.Image.SCALE_SMOOTH)
      image = new javax.swing.ImageIcon(smallImage)
    }
    // 作った新しいアイコンをラベルのアイコンとして設定

    imageIconMap += (username -> image)
    return imageIconMap(username)
  }
}

class TlScrollPane extends ScrollPane{
  this.horizontalScrollBarPolicy = ScrollPane.BarPolicy.Never
  preferredSize = new Dimension(Main.mainFrameInitialWidth, Main.mainFrameInitialHeight)
}

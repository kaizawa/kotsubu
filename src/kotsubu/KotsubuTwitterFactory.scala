/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kotsubu

import java.awt.Desktop
import java.net.URI
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.TwitterStream
import twitter4j.TwitterStreamFactory
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken

/**
 * factory class for Twitter
 */
object KotsubuTwitterFactory {
  var twitter:Twitter = null
  var twitterStream:TwitterStream = null
 
  /**
   * Return instance of Twitter class which has AccessToken been set.
   * @return twitter instance of Twitter class
   */
  def getInstance(): Twitter = {
    if(null == twitter){
      val accessToken:AccessToken = getAccessToken()
      if(null == twitter) {
        twitter = TwitterFactory.getSingleton
        twitter.setOAuthConsumer(Prefs.get("OAuthConsumerKey"), Prefs.get("consumerSecret"))
      }
      twitter.setOAuthAccessToken(accessToken) 
    }
    return twitter
  }
  
  def getStreamInstance(): TwitterStream = {
    val accessToken:AccessToken = getAccessToken()   
    if(null == twitterStream){
      twitterStream = TwitterStreamFactory.getSingleton
      twitterStream.setOAuthConsumer(Prefs.get("OAuthConsumerKey"), Prefs.get("consumerSecret"))
      twitterStream.setOAuthAccessToken(accessToken);
    }
    return twitterStream
  }
  
  /**
   * Get accesstoken 
   */
  private def getAccessToken(): AccessToken = {
    var accessToken:AccessToken = null    
    if(Prefs.get("accessToken").isEmpty){   
      if(null == twitter) {
        twitter = TwitterFactory.getSingleton
      }
      twitter.setOAuthConsumer(Prefs.get("OAuthConsumerKey"), Prefs.get("consumerSecret"));
      val requestToken:RequestToken  = twitter.getOAuthRequestToken()

      while(null == accessToken){
        Desktop.getDesktop().browse(new URI(requestToken.getAuthorizationURL()))
        accessToken = scala.swing.Dialog.showInput(
          title="Input PIN", 
          message="Copy and past PIN Number",
          initial="",
          icon = Main.kotsubuIconLabel.icon
        ) match {
          case Some(pin:String) => pin.length match {
              case 0 => null
              case _ => twitter.getOAuthAccessToken(requestToken, pin)
          }
          case None => System.exit(1) ; null
          case _ =>  null
        }        
      }
      Prefs.put("accessToken", accessToken.getToken)
      Prefs.put("accessTokenSecret", accessToken.getTokenSecret) 
    }
    return new AccessToken(Prefs.get("accessToken"),Prefs.get("accessTokenSecret"))
  }
}

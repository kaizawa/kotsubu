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

  /* Get Access Token */  
  val accessToken:AccessToken = getAccessToken()        

  /* Setup Twitter instance */
  val twitter:Twitter = TwitterFactory.getSingleton
  twitter.setOAuthConsumer(Prefs.get("OAuthConsumerKey"), Prefs.get("consumerSecret"))  
  twitter.setOAuthAccessToken(accessToken)   

  /* Setup TwitterStream instance */
  val twitterStream:TwitterStream = TwitterStreamFactory.getSingleton  
  twitterStream.setOAuthConsumer(Prefs.get("OAuthConsumerKey"), Prefs.get("consumerSecret"))  
  twitterStream.setOAuthAccessToken(accessToken);  

  /**
   * Return instance of Twitter class which has AccessToken been set.
   * @return twitter instance of Twitter class
   */
  def getInstance(): Twitter = {
    return twitter
  }
  
  def getStreamInstance(): TwitterStream = {
    return twitterStream
  }
  
  /**
   * Get accesstoken 
   */
  private def getAccessToken(): AccessToken = {
    if(Prefs.get("accessToken").isEmpty) {   
      twitter.setOAuthConsumer(Prefs.get("OAuthConsumerKey"), Prefs.get("consumerSecret"));
      val requestToken:RequestToken  = twitter.getOAuthRequestToken()
      
      Desktop.getDesktop().browse(new URI(requestToken.getAuthorizationURL()))
      return scala.swing.Dialog.showInput(
        title="Input PIN", 
        message="Copy and past PIN Number",
        initial="",
        icon = Main.kotsubuIconLabel.icon
      ) match {
        case Some(pin:String) => pin.length match {
            case 0 => getAccessToken()
            case _ => twitter.getOAuthAccessToken(requestToken, pin)
          }          
        case None => {
            System.exit(1)
            null
        }
        case _ =>  getAccessToken()
      }
      Prefs.put("accessToken", accessToken.getToken)
      Prefs.put("accessTokenSecret", accessToken.getTokenSecret) 
    }
    return new AccessToken(Prefs.get("accessToken"),Prefs.get("accessTokenSecret"))
  }
}

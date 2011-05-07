/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kotsubu

import java.awt.Desktop
import java.net.URI
import java.util.prefs.Preferences
import twitter4j.Twitter
import twitter4j.TwitterFactory
import twitter4j.auth.AccessToken
import twitter4j.auth.RequestToken

/**
 * factory class for Twitter
 */
object TWFactory {
 
  val factory = new TwitterFactory

  /**
   * Return instance of Twitter class which has AccessToken been set.
   * @return twitter instance of Twitter class
   */
  def getInstance(): Twitter = {
    if(Prefs.get("accessToken").isEmpty){
      val twitter:Twitter = new TwitterFactory().getInstance()
      twitter.setOAuthConsumer(Prefs.get("OAuthConsumerKey"), Prefs.get("consumerSecret"));
      val requestToken:RequestToken  = twitter.getOAuthRequestToken()

      Desktop.getDesktop().browse(new URI(requestToken.getAuthorizationURL()))
      scala.swing.Dialog.showMessage(title="confirm", message="After you allowed , click OK")
      val accessToken:AccessToken = twitter.getOAuthAccessToken();
      Prefs.put("accessToken", accessToken.getToken)
      Prefs.put("accessTokenSecret", accessToken.getTokenSecret)      
    }        
     val accessToken:AccessToken = new AccessToken(Prefs.get("accessToken"),Prefs.get("accessTokenSecret"))
     val twitter:Twitter = factory.getInstance
     twitter.setOAuthConsumer(Prefs.get("OAuthConsumerKey"), Prefs.get("consumerSecret"));
     twitter.setOAuthAccessToken(accessToken) 
     return twitter
  }
}

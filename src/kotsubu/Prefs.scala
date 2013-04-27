/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package kotsubu

import java.util.prefs.Preferences

/**
 * Preferences for kotsubu
 */
object Prefs {
  val prefs:Preferences = Preferences.userNodeForPackage(Main.getClass())
  
  val booleanPrefDefaultMap  = Map ( 
    "autoUpdateEnabled" -> true, // Default value for auto-update
    "progressBarEnabled" -> false // Enable/Disable progress bar.
    )
    
  val stringPreDefaultfMap  = Map ( 
    "OAuthConsumerKey" -> "PaWXdbUBNZGJVuDqxFY8wg", 
    "consumerSecret" -> "fD9SO5gUNYP9AuhCSYuob9inQU0jKl5bPMuGj1QRkFo"    
    )  
    
  val intPrefDefaultMap  = Map ( 
    "homeUpdateInterval" -> 30, // Default interval of auto-update
    "userUpdateInterval" -> 300, // Default interval of auto-update
    "publicUpdateInterval" -> 300, // Default interval of auto-update
    "mentionUpdateInterval" -> 120, // Default interval of auto-update  
    "maxStatuses" -> 100, // Number of tweets to be shown.
    "corePoolSize" -> 6, // NUmber of Threads used for actor
    "maxCacheIcons" -> 100 // Max number of user icons to be cached.
    )    

  /**
   * return String preference
   * @param name preference name
   * @return value value of preference
   */
  def get(name:String) : String = {
    val value:String = stringPreDefaultfMap.get(name) match {
      case Some(v:String) => v
      case _ => ""
    }
    prefs.get(name, value)
  } 
  /**
   * return Int preference
   * @param name preference name
   * @return value value of preference
   */  
  def getInt(name:String) : Int = {
    val value:Int = intPrefDefaultMap.get(name) match {
      case Some(v:Int) => v
      case _ => 0
    }
    prefs.getInt(name, value)
  }
  /**
   * return Boolean preference
   * @param name preference name
   * @return value value of preference
   */  
  def getBoolean(name:String) : Boolean = {
    val value:Boolean = booleanPrefDefaultMap.get(name) match {
      case Some(v:Boolean) => v
      case _ => false
    }
    prefs.getBoolean(name, value)
  }  
  /**
   * Set String preference
   * @param name preference name
   * @param value value of preference
   */  
  def put(key:String,value:String) : Unit = {
    prefs.put(key, value)
  }
  /**
   * Set Int preference
   * @param name preference name
   * @param value value of preference
   */    
  def putInt(key:String,value:Int) : Unit = {
    prefs.putInt(key, value)
  }
  /**
   * Set Boolean preference
   * @param name preference name
   * @param value value of preference
   */    
  def putBoolean(key:String, value:Boolean) : Unit = {
    prefs.putBoolean(key, value)
  }  
}

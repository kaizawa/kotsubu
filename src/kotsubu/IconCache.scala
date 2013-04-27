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

import java.awt.image.BufferedImage
import twitter4j.User
import javax.swing.ImageIcon
import scala.collection.mutable._
import java.net.URL

/*
 * Object which store user icon cache
 */
object IconCache {
  // Each cache entry consists of User name, Iconv image and Reference count.
  val imageIconMap = Map.empty[String, (javax.swing.ImageIcon, Int)]
  
  /**
   * Get icons image
   * @param user user entry.
   * @return stored icon
   */  
  def getIcon(user:User): javax.swing.ImageIcon = {
    val username = user.getScreenName    
    imageIconMap get (username) match {
      case None => loadIconAndStore(user)
      case _ =>
    }  
    val taple = imageIconMap(username)
    // Increment reference count
    imageIconMap += (username -> (taple._1, taple._2 + 1))
    return taple._1
  }
  /**
   * Add icons to Map 
   * @param user user entry.
   * @return added icon
   */
  def loadIconAndStore(user:User) :Unit = {
    val username = user.getScreenName
    
    val image:ImageIcon = 
    try {
      // Read original icon stored in Twitter.      
      javax.imageio.ImageIO.read(new URL(user.getOriginalProfileImageURL())) match {
        // Use default icon, if original icon is not found.
        case null => new javax.swing.ImageIcon(javax.imageio.ImageIO.read(
              getClass().getClassLoader().getResource("kotsubu/default.png")))
        // Set size to 50x50        
        case originalImage:BufferedImage => new javax.swing.ImageIcon(
            originalImage.getScaledInstance(
              Main.USER_ICON_SIZE,Main.USER_ICON_SIZE, java.awt.Image.SCALE_SMOOTH))
      }
    } catch {
      case ex: Exception => {
        println("Can't read icon from " + user.getProfileImageURL.toString + ". Use default icon.")
        null
      }
    } 
    
    // Remove least frequently used User's icon, if max cache icons exceeds.
    if(imageIconMap.size > Prefs.getInt("maxCacheIcons")){
      val head = imageIconMap.head
      val leastFrequentlyUsedUserName = getLFUUser(imageIconMap, head._1, head._2._2)
      imageIconMap.remove(leastFrequentlyUsedUserName)
    }
    // Set icon as a label's icon.
    imageIconMap += (username -> (image, 0))
  }
  
  def getLFUUser(
    map:Map[String, (javax.swing.ImageIcon, Int)], username:String, count:Int): String  = 
  {
    if (map.isEmpty){
      //println(username + " is used only " + count +" times. Removing from cache...")
      return username
    }
    if (map.head._2._2 < count)
      return getLFUUser(map.tail, map.head._1, map.head._2._2)
    else 
      return getLFUUser(map.tail, username, count)
  }
}

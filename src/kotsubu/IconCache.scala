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

/*
 * Object which store user icon cache
 */
object IconCache {
  val imageIconMap = Map.empty[String, javax.swing.ImageIcon]
  
  /**
   * Get icons image
   * @param user user entry.
   * @return stored icon
   *
   * TODO: icon cache mechanism is too bad..
   */  
  def getIcon(user:User): javax.swing.ImageIcon = {
    imageIconMap get (user.getScreenName) match {
      case Some(icon) => icon
      case None => loadIconAndStore(user)
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
      case _ => new javax.swing.ImageIcon(originalImage.getScaledInstance(Main.userIconSize,Main.userIconSize, java.awt.Image.SCALE_SMOOTH))
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

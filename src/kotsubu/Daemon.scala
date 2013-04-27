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

package kotsubu

import scala.actors.Actor._
import kotsubu.Main._
import kotsubu._
import twitter4j.Status
import twitter4j.TwitterStream
import twitter4j.UserStreamListener
import scala.collection.immutable._

/**
 * kotsubu - Twitter Client
 *
 * Actor auto update thread
 */
object UpdateDaemon extends {
  
  /*
   * Start back ground threads for auto udpate
   */
  def startDaemon() :Unit = {
    actor {
      def timeLineChecker(updatetype:UpdateType) {
        val updateActor = self
        actor {
          // stop thread if auto-update is disabled
          if(Prefs.getBoolean("autoUpdateEnabled") == false){ return }

          val waittime = updatetype match {
            case UpdateType("user") => Prefs.getInt("userUpdateInterval")
            case UpdateType("mention") => Prefs.getInt("mentionUpdateInterval")
          }
          // Start scheduled update          
          Thread.sleep(waittime * 1000)
          updateActor ! updatetype
        }
      } 

      // Start scheduled update      
      List("user", "mention") map {tl => {
          updateTimeLine(UpdateType(tl))
          timeLineChecker(UpdateType(tl))
        }
      }
      
      loop {
        react {
          case updatetype:UpdateType => {
              updateTimeLine(updatetype)     
              timeLineChecker(updatetype)
            }
        }
      }
    }
  }
}

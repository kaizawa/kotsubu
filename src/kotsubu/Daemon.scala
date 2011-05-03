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

/**
 * kotsubu - Twitter Client
 *
 * Actor auto update thread
 */
object UpdateDaemon extends {
  
  def startDaemon() :Unit = {
    actor {
      def tlChecker(updatetype:UpdateType) {
        val updateActor = self
        actor {
          // stop thread if auto-update is disabled
          if(Main.prefs.getBoolean("autoUpdateEnabled", Main.defAutoUpdateEnabled) == false){ return }

          val waittime = updatetype match {
            case UpdateType("home") => prefs.getInt("homeUpdateInterval", Main.defHomeUpdateInterval)
            case UpdateType("user") => prefs.getInt("userUpdateInterval", Main.defUserUpdateInterval)
            case UpdateType("public") => prefs.getInt("publicUpdateInterval", Main.defPublicUpdateInterval)
            case UpdateType("mention") => prefs.getInt("mentionUpdateInterval", Main.defMentionUpdateInterval)  
          }
          // Start scheduled update          
          //println(updatetype.name + " waiting for " + waittime + " sec.")
          Thread.sleep(waittime * 1000)
          updateActor ! updatetype
        }
      } 

      // Start scheduled update      
      List("home", "user", "public", "mention") map {tl => {
          updateTimeLine(UpdateType(tl))
          tlChecker(UpdateType(tl))
        }
      }
      
      loop {
        react {
          case updatetype:UpdateType => {
              updateTimeLine(updatetype)     
              tlChecker(updatetype)
            }
        }
      }
    }
  }
}

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

import scala.actors.Actor._
import myapp.Main._
import myapp._

/**
 * kotsubu - Twitter Client
 *
 * Actor auto update thread
 */
object UpdateDaemon extends {

  val DEFAULT_WAITTIME:Int = 60000
  def startDaemon() :Unit = {
    actor {
      def tlChecker(updatetype:UpdateType) {
        val updateActor = self
        actor {
          // stop thread if auto-update is disabled
          if(Main.prefs.getBoolean("autoUpdateEnabled", Main.defAutoUpdateEnabled)
             == false){ return }
          updateActor ! updatetype
          val waittime = Main.prefs.getInt("updateInterval", Main.defEveryoneUpdateInterval) * 1000
          Thread.sleep(waittime)
        }
      }

      tlChecker(UpdateType("users"))
      tlChecker(UpdateType("public"))
      tlChecker(UpdateType("home"))
      tlChecker(UpdateType("mention"))      

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

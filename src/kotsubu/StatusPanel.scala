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

import java.awt.Color
import java.awt.Dimension
import scala.swing.BorderPanel
import scala.swing.Label
import scala.swing.Separator
import scala.swing.Swing
import twitter4j.Status

/**
 * Panel corresponding to each tweet.
 * @param tlScrollPane ScrollPane of TimeLine
 * @param status status of tweet
 */
class StatusPanel(tlScrollPane:TimeLineScrollPane, val status:Status) extends BorderPanel {
  // Icon. Load from server, if it is not cached yet.
  val user = status.getUser
  val iconLabel = new Label{ icon = IconCache.getIcon(user) }  
  val statusInfoPanel = new StatusInfoPanel(status)
  val messageTextPane = new MessageTextPane(status, tlScrollPane:TimeLineScrollPane)
  val operationPanel = new OperationButtonPanel(status)
  
  
  preferredSize_=( new Dimension(Main.MAIN_FRAME_INITIAL_WIDTH - 60, 
                                Main.TIMELINE_INITIAL_HEIGHT))
  maximumSize_=( new Dimension(Integer.MAX_VALUE, Main.TIMELINE_INITIAL_HEIGHT))

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
      add(statusInfoPanel, North)
      add(messageTextPane, Center)
      add(operationPanel, East)
    }, Center)
  add(
    new Separator{background = Color.white}, South)  
}
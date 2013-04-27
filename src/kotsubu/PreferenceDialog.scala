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
import scala.swing._
import scala.swing.event._
import scala.xml._
import scala.actors._
import scala.actors.Actor._
import scala.collection.mutable._

/**
 * Preferences configuration dialog
 */
class PreferencesDialog()  extends scala.swing.Dialog() {
  title = "Preferences"  
  modal = true
  
  val autoUpdateCheckBox = new CheckBox(){selected = Prefs.getBoolean("autoUpdateEnabled")}
  val progressBarEnabledCheckBoxDialog = new CheckBox(){selected = Prefs.getBoolean("progressBarEnabled")}
  val homeUpdateIntervalTextField = new TextField (Prefs.getInt("homeUpdateInterval").toString)
  val userUpdateIntervalTextField = new TextField (Prefs.getInt("userUpdateInterval").toString)
  val mentionUpdateIntervalTextField = new TextField (Prefs.getInt("mentionUpdateInterval").toString)  
  val maxStatusesTextField = new TextField (Prefs.getInt("maxStatuses").toString)
  val maxCacheIconsTextField = new TextField (Prefs.getInt("maxCacheIcons").toString)
  val okButton = new Button ("OK")
  val cancelButton = new Button("Cancel")
  
  val prefsPanel = new GridPanel (8, 2){
    contents += new Label("Enable Progress Bar")
    contents += progressBarEnabledCheckBoxDialog
    contents += new Label("Enable AutoUpdate")
    contents += autoUpdateCheckBox
    contents += new Label("Home TL interval(sec): ")
    contents += homeUpdateIntervalTextField
    contents += new Label("My TL interval(sec): ")
    contents += userUpdateIntervalTextField
    contents += new Label("Mention TL interval(sec): ")
    contents += mentionUpdateIntervalTextField    
    contents += new Label("Num tweets: ")
    contents += maxStatusesTextField
    contents += new Label("Num Cache Icons: ")
    contents += maxCacheIconsTextField
  }

  contents= new BoxPanel (Orientation.Vertical){
    contents += prefsPanel
    contents += new BoxPanel(Orientation.Horizontal){
      contents += okButton
      contents += cancelButton
    }
    border = Swing.EmptyBorder(10, 10, 10, 10)
  }
  listenTo(okButton, cancelButton)

  reactions += {
    case ButtonClicked(`okButton`) => saveAndClose()
    case ButtonClicked(`cancelButton`) => cancelAndClose()
  }

  /**
   * Save new settings and close dialog
   */
  def saveAndClose () :Unit = {
    if(Prefs.getBoolean("autoUpdateEnabled")
       == false && autoUpdateCheckBox.selected == true){
      // if auto-update is enabled, then start background update thread.
      // TODO: should check the existance of background thread
      UpdateDaemon.startDaemon()
    }

    Prefs.putBoolean("autoUpdateEnabled", autoUpdateCheckBox.selected)
    Prefs.putInt("homeUpdateInterval", Integer.parseInt(homeUpdateIntervalTextField.text))
    Prefs.putInt("userUpdateInterval", Integer.parseInt(userUpdateIntervalTextField.text))
    Prefs.putInt("mentionUpdateInterval", Integer.parseInt(mentionUpdateIntervalTextField.text))    
    Prefs.putBoolean("progressBarEnabled",progressBarEnabledCheckBoxDialog.selected)
    Prefs.putInt("maxStatuses", Integer.parseInt(maxStatusesTextField.text))
    Prefs.putInt("maxCacheIcons", Integer.parseInt(maxCacheIconsTextField.text))    

    closeOperation
    dispose
    close
  }

  /**
   * Cancel and Close dialog
   */
  def cancelAndClose () :Unit = {
    closeOperation
    dispose
    close
  }

  this.resizable_=(false)
}

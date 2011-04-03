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
  val autoUpdateCheckBox = new CheckBox("Enable AutoUpdate") {
    selected = Main.prefs.getBoolean("autoUpdateEnabled",Main.defAutoUpdateEnabled)}
  val progressBarEnabledCheckBoxDialog = new CheckBox("Enable Progress Bar") {
    selected = Main.prefs.getBoolean("progressBarEnabled",Main.defProgressBarEnabled) }
  val homeUpdateIntervalTextField = new TextField (Main.prefs.getInt("homeUpdateInterval", Main.defHomeUpdateInterval).toString)
  val myUpdateIntervalTextField = new TextField (Main.prefs.getInt("myUpdateInterval", Main.defMyUpdateInterval).toString)
  val mentionUpdateIntervalTextField = new TextField (Main.prefs.getInt("mentionUpdateInterval", Main.defMentionUpdateInterval).toString)  
  val everyoneUpdateIntervalTextField = new TextField (Main.prefs.getInt("everyoneUpdateInterval", Main.defEveryoneUpdateInterval).toString)
  val numTimeLinesTextField = new TextField (Main.prefs.getInt("numTimeLines", Main.defNumTimeLines).toString)
  val okButton = new Button ("OK")
  val cancelButton = new Button("Cancel")
  val intervalPanel = new GridPanel (5, 2){
    contents += new Label("Home TL interval(sec): ")
    contents += homeUpdateIntervalTextField
    contents += new Label("My TL interval(sec): ")
    contents += myUpdateIntervalTextField
    contents += new Label("Everyone TL interval(sec): ")
    contents += everyoneUpdateIntervalTextField
    contents += new Label("Mention TL interval(sec): ")
    contents += mentionUpdateIntervalTextField    
    contents += new Label("Num timelines: ")
    contents += numTimeLinesTextField
  }

  contents= new BoxPanel (Orientation.Vertical){
    contents += progressBarEnabledCheckBoxDialog
    contents += autoUpdateCheckBox
    contents += intervalPanel
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
    if(Main.prefs.getBoolean("autoUpdateEnabled", Main.defAutoUpdateEnabled)
       == false && autoUpdateCheckBox.selected == true){
      // if auto-update is enabled, then start background update thread.
      // TODO: should check the existance of background thread
      UpdateDaemon.startDaemon()
    }

    Main.prefs.putBoolean("autoUpdateEnabled", autoUpdateCheckBox.selected)
    Main.prefs.putInt("homeUpdateInterval", Integer.parseInt(homeUpdateIntervalTextField.text))
    Main.prefs.putInt("myUpdateInterval", Integer.parseInt(myUpdateIntervalTextField.text))
    Main.prefs.putInt("everyoneUpdateInterval", Integer.parseInt(everyoneUpdateIntervalTextField.text))
    Main.prefs.putInt("mentionUpdateInterval", Integer.parseInt(mentionUpdateIntervalTextField.text))    
    Main.prefs.putBoolean("progressBarEnabled",progressBarEnabledCheckBoxDialog.selected)
    Main.prefs.putInt("numTimeLines", Integer.parseInt(numTimeLinesTextField.text))

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

//  size = new Dimension(400,250)
  this.resizable_=(false)
}

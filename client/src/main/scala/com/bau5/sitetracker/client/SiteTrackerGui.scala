package com.bau5.sitetracker.client

import java.awt.Color
import javax.swing.FocusManager
import javax.swing.border.Border
import javax.swing.plaf.BorderUIResource

import com.bau5.sitetracker.common.AnomalyDetails.User

import scala.swing.TabbedPane.Page
import scala.swing.event.{WindowClosing, ActionEvent, ValueChanged, ButtonClicked}
import scala.swing._
import scala.swing.BorderPanel.Position._


/**
  * Created by Rick on 1/23/16.
  */
class SiteTrackerGui(client: Client) extends SimpleSwingApplication {
  override def top: Frame = new MainFrame {
//    val ret = JOptionPane.showInputDialog("Please enter username:", "")
//    client.user = User(ret)

    val removeButton = new Button("Remove")
    val removeTextFields = TextAndButtonPanel (
      removeButton,
      new TextFieldWithPrompt("System", 10),
      new TextFieldWithPrompt("ID", 5)
    )

    val addButton = new Button("Add") {
      peer.setPreferredSize(removeButton.peer.getPreferredSize)
    }
    val addTextFields = TextAndButtonPanel (
      addButton,
      new TextFieldWithPrompt("System", 10),
      new TextFieldWithPrompt("ID", 5),
      new TextFieldWithPrompt("Name", 10),
      new TextFieldWithPrompt("Type", 10)
    )

    val editButton = new Button("Edit") {
      peer.setPreferredSize(removeButton.peer.getPreferredSize)
    }
    val editTextFields = TextAndButtonPanel (
      editButton,
      new TextFieldWithPrompt("System", 10),
      new TextFieldWithPrompt("ID", 5)
    )
    val editNewAttributes = new TextFieldPanel (
      attributeFields,
      FlowPanel.Alignment.Center
    )

    val findButton = new Button("Find") {
      peer.setPreferredSize(removeButton.peer.getPreferredSize)
    }
    val findTextFields = TextAndButtonPanel(
      findButton,
      attributeFields:_*
    )

    val mutatorPane = new BoxPanel(Orientation.Vertical) {
      contents += addTextFields
      contents += removeTextFields
      contents += findTextFields
      contents += new GridPanel(2,1) {
        contents += editTextFields
        contents += editNewAttributes
      }
    }

    val tabbedPane = new TabbedPane {
      title = "Test pane"
      pages += new Page("Edit", mutatorPane)
      pages += new Page("View", new TextField("asdf"))
    }
    contents = new BorderPanel {
      layout(tabbedPane) = Center
    }

    listenTo(addTextFields, editTextFields, findTextFields, removeTextFields)
    reactions += {
      case TextAndButtonPanelEvent(`addTextFields`) =>
        val toActor = s"${addTextFields.buttonName} " + addTextFields.text.map { case (name, txt) =>
          if (name == "Name") {
            s"'$txt'"
          } else {
            txt
          }
        }.mkString(" ")
        println(toActor)
        client.handleInput(toActor)
      case TextAndButtonPanelEvent(`editTextFields`) =>
        println("Edit panel " + editTextFields.text)
        println("Edit panel new " + editNewAttributes.text)
      case TextAndButtonPanelEvent(panel) =>
        println("Panel " + panel.text)
        panel.clear()
      case ValueChanged(a) => a match {
        case b: TextField => println(b.text)
      }

      case WindowClosing(a) =>
        client.handleInput("quit")
    }
    resizable = false
  }

  def attributeFields = List(
    new TextFieldWithPrompt("System", 10),
    new TextFieldWithPrompt("ID", 5),
    new TextFieldWithPrompt("Name", 10),
    new TextFieldWithPrompt("Type", 10)
  )
}

class TextFieldWithPrompt(val placeholderText: String, cols: Int) extends TextField {
  this.columns = cols

  override def border: Border = BorderUIResource.getBlackLineBorderUIResource

  override protected def paintComponent(g: Graphics2D): Unit = {
    super.paintComponent(g)
    if (peer.getText.isEmpty && FocusManager.getCurrentManager.getFocusOwner != this.peer) {
      g.setBackground(Color.GRAY)
      g.drawString(placeholderText, 5, 18)
      g.dispose()
    }
  }
}

object TextAndButtonPanel {
  def apply(button: Button, fields: TextFieldWithPrompt*): TextAndButtonPanel = {
    new TextAndButtonPanel(fields, button)
  }
  def apply(button: Button, alignment: FlowPanel.Alignment.Value, fields: TextFieldWithPrompt*) = {
    new TextAndButtonPanel(fields, button, alignment)
  }
}
class TextAndButtonPanel(fields: Seq[TextFieldWithPrompt], button: Button, alignment: FlowPanel.Alignment.Value = FlowPanel.Alignment.Left) extends TextFieldPanel(fields, alignment) {
  // Used when translating the button clicked event to a command for the client Actor
  val buttonName = button.text.toLowerCase
  listenTo(button)
  contents.clear()
  contents += {
    button
  }
  contents ++= fields
  reactions += {
    case ButtonClicked(b) => publish(new TextAndButtonPanelEvent(this))
  }
}

class TextFieldPanel(fields: Seq[TextFieldWithPrompt], alignment: FlowPanel.Alignment.Value = FlowPanel.Alignment.Left) extends FlowPanel(alignment)() {
  def text: Seq[(String, String)] = fields.map(e => e.placeholderText -> e.text)

  def clear(): Unit = fields foreach (_.text = "")

  contents ++= fields

  reactions += {
    case ButtonClicked(b) => publish(new TextFieldPanelEvent(this))
  }
}

case class TextFieldPanelEvent(override val source: TextFieldPanel) extends ActionEvent(source)
case class TextAndButtonPanelEvent(override val source: TextAndButtonPanel) extends ActionEvent(source)

package com.bau5.sitetracker.client

import java.awt.Color
import javax.swing.FocusManager

import scala.swing.TabbedPane.Page
import scala.swing.event.{ValueChanged, ButtonClicked}
import scala.swing._
import scala.swing.BorderPanel.Position._


/**
  * Created by Rick on 1/23/16.
  */
object SiteTrackerGui extends SimpleSwingApplication {
  override def top: Frame = new MainFrame {

    val removeButton = new Button("Remove")
    val removeTextFields = TextFieldFlowPanel (
      new TextFieldWithPrompt("System", 10),
      new TextFieldWithPrompt("ID", 5)
    )

    val addButton = new Button("Add") {
      peer.setPreferredSize(removeButton.peer.getPreferredSize)
    }
    val addTextFields = TextFieldFlowPanel (
      new TextFieldWithPrompt("System", 10),
      new TextFieldWithPrompt("ID", 5),
      new TextFieldWithPrompt("Name", 10),
      new TextFieldWithPrompt("Type", 10)
    )

    val editButton = new Button("Edit") {
      peer.setPreferredSize(removeButton.peer.getPreferredSize)
    }
    val editTextFields = TextFieldFlowPanel (
      new TextFieldWithPrompt("System", 10),
      new TextFieldWithPrompt("ID", 5)
    )
    val editNewAttributes = TextFieldFlowPanel (
      FlowPanel.Alignment.Center,
      new TextFieldWithPrompt("New System", 10),
      new TextFieldWithPrompt("New ID", 5),
      new TextFieldWithPrompt("New Name", 10),
      new TextFieldWithPrompt("New Type", 10)
    )

    val commandPane = new BoxPanel(Orientation.Vertical) {
      contents += new FlowPanel(FlowPanel.Alignment.Left)(addButton, addTextFields)
      contents += new FlowPanel(FlowPanel.Alignment.Left)(removeButton, removeTextFields)
      contents += new GridPanel(2, 1) {
        contents += new FlowPanel(FlowPanel.Alignment.Left)(editButton, editTextFields)
        contents += editNewAttributes
      }
    }

    val tabbedPane = new TabbedPane {
      title = "Test pane"
      pages += new Page("First", commandPane)
      pages += new Page("Second", new TextField("asdf"))
    }
    contents = new BorderPanel {
      layout(tabbedPane) = Center
    }

    listenTo(addButton)
    reactions += {
      case ButtonClicked(`addButton`) =>
        println(s"Got text from the field ${addTextFields.text}")
      case ValueChanged(a) => a match {
        case b: TextField => println(b.text)
      }
    }
  }
}

class TextFieldWithPrompt(placeholderText: String, cols: Int) extends TextField {
  this.columns = cols
  override protected def paintComponent(g: Graphics2D): Unit = {
    super.paintComponent(g)
    if (peer.getText.isEmpty && FocusManager.getCurrentManager.getFocusOwner != this.peer) {
      g.setBackground(Color.GRAY)
      g.drawString(placeholderText, 5, 18)
      g.dispose()
    }
  }
}

object TextFieldFlowPanel {
  def apply(fields: TextFieldWithPrompt*): TextFieldFlowPanel = {
    new TextFieldFlowPanel(fields)
  }
  def apply(alignment: FlowPanel.Alignment.Value, fields: TextFieldWithPrompt*) = {
    new TextFieldFlowPanel(fields, alignment)
  }
}
class TextFieldFlowPanel(fields: Seq[TextFieldWithPrompt], alignment: FlowPanel.Alignment.Value = FlowPanel.Alignment.Left) extends FlowPanel(alignment)(fields:_*) {
  def text: String = fields.map(_.text).mkString(",")
}
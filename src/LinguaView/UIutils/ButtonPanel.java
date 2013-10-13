package LinguaView.UIutils;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

@SuppressWarnings("serial")
public class ButtonPanel extends JPanel
{
	private ButtonGroup group;
	
	public ButtonPanel(String title, String...options) {
		setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title));
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		group = new ButtonGroup();
		for(String option: options) {
			JRadioButton b = new JRadioButton(option);
			b.setActionCommand(option);
			add(b);
			group.add(b);
			b.setSelected(option == options[0]);
		}
	}
	
	public String getSelection()
	{
		return group.getSelection().getActionCommand();
	}
}

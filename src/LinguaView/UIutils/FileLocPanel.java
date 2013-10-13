package LinguaView.UIutils;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JPanel;
import javax.swing.JTextField;

@SuppressWarnings("serial")
public class FileLocPanel extends JPanel
{
	private JTextField LocTextComponent = new JTextField(20);
	private JButton browseButton = new JButton("Browse...");
	
	public FileLocPanel() {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		browseButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				BrowseFile();
			}
		});
		add(LocTextComponent);
		add(browseButton);
	}
	
	public void BrowseFile() {
		JFileChooser chooser = new JFileChooser();
		chooser.setCurrentDirectory(new File("."));
		int result = chooser.showSaveDialog(this);
		if(result == 0) {
			String FileLoc = chooser.getSelectedFile().getAbsolutePath();
			LocTextComponent.setText(FileLoc);
		}
	}
	
	public String getLoc() {
		return LocTextComponent.getText();
	}
}
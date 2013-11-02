package LinguaView;

import java.awt.*;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

import java.awt.event.*;
import java.io.*;
import java.util.ArrayList;

import javax.swing.border.EmptyBorder;
import javax.swing.event.*;
import javax.swing.undo.UndoManager;
import javax.swing.undo.UndoableEdit;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import net.sf.epsgraphics.ColorMode;
import net.sf.epsgraphics.EpsGraphics;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import LinguaView.syntax.*;
import LinguaView.UIutils.*;
/**
 * LinguaView is the top-most class in the whole class hierarchy
 * It constructs the top UI object and starts the whole program.
 * 
 * @author shuoyang
 */
public class LinguaView {
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				JFrame frame = new TabbedPaneFrame();
				try {
					for (LookAndFeelInfo info : UIManager
							.getInstalledLookAndFeels()) {
						if ("Nimbus".equals(info.getName())) {
							UIManager.setLookAndFeel(info.getClassName());
							break;
						}
					}
				} catch (Exception e) {
					// If Nimbus is not available, you can set the GUI to
					// another look and feel.
					e.printStackTrace();
				}

				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setVisible(true);
			}
		});
	}
}

/**
 * TabbedPaneFrame is the top-most UI class in the class hierarchy.
 * It contains a toolbar, a status bar, a menubar, and a panel with multiple tabs to show different content.
 * 
 * @author shuoyang
 */
@SuppressWarnings("serial")
class TabbedPaneFrame extends JFrame {
	/**
	 * provides a panel that contains multiple tab to show different contents
	 */
	private JTabbedPane tabbedPane = new JTabbedPane();
	
	/**
	 * contains the name of the input file that is currently opened by the viewer
	 */
	private String filename;

	/**
	 * viewers for each syntax to show corresponding graphical representations 
	 */
	ConstTreePanel constcomponent = new ConstTreePanel();
	DepTreePanel depcomponent = new DepTreePanel();
	DepTreePanel deepdepcomponent = new DepTreePanel();
	CCGTreePanel CCGcomponent = new CCGTreePanel();
	LFGStructPanel LFGcomponent = new LFGStructPanel();
	
	/**
	 * constitute a text editor
	 */
	JTextPane Textcomponent = new JTextPane();

	/**
	 * controls all the undo/redo actions in text editor
	 */
	UndoManager undoManager = new UndoManager();
	
	/**
	 * a wrapper for Textcomponent to show the text in a no-wrap style
	 */
	JPanel TextNoWrapPanel = new JPanel();
	
	/**
	 * wrappers for each component to make them scrollable
	 */
	final JScrollPane constScrollPane = new JScrollPane(constcomponent);
	final JScrollPane depScrollPane = new JScrollPane(depcomponent);
	final JScrollPane deepdepScrollPane = new JScrollPane(deepdepcomponent);
	final JScrollPane CCGScrollPane = new JScrollPane(CCGcomponent);
	final JScrollPane LFGScrollPane = new JScrollPane(LFGcomponent);
	final JScrollPane TextScrollPane = new JScrollPane(TextNoWrapPanel);

	/**
	 * default treebanks that are shown as default or when error occurs
	 */
	TSNodeLabel defaultConstTreebank;
	DepTree defaultDepTreebank;
	DepTree defaultDeepdepTreebank;
	CCGNode defaultCCGTreebank;
	Element defaultLFGStructbank;

	/**
	 * the menu bar lays on the top of the frame, while the popupMenu only appears when user fires
	 * a right-click in the text editor
	 */
	JMenuBar menuBar = new JMenuBar();
	JPopupMenu popupMenu = new JPopupMenu();
	
	/**
	 * the tool bar lays on the left side of the frame, the user is able to drag it to attach it on
	 * any side of the panel or leave it floating somewhere
	 */
	JToolBar bar = new JToolBar();
	JButton newButton = new JButton();
	JButton importButton = new JButton();
	JButton exportButton = new JButton();
	JButton zoomInButton = new JButton();
	JButton zoomOutButton = new JButton();
	JButton prevButton = new JButton();
	JButton nextButton = new JButton();
	JButton jumpButton = new JButton();
	JButton saveTextButton = new JButton();

	/**
	 * the status bar tells the user which sentence they are processing
	 * it also shows warnings when something abnormal happens
	 */
	StatusBar statusBar = new StatusBar();

	public TabbedPaneFrame() {
		// deal with the panel: size, font, layout, etc.
		setTitle("LinguaView");
		Toolkit kit = Toolkit.getDefaultToolkit();
		Dimension screenSize = kit.getScreenSize();
		int screenHeight = screenSize.height;
		int screenWidth = screenSize.width;
		setSize(screenWidth / 2, screenHeight / 2);
		setMinimumSize(new Dimension(screenWidth / 2, screenHeight / 2));
		setLayout(new BorderLayout());

		try {
			GraphicsEnvironment ge = GraphicsEnvironment
					.getLocalGraphicsEnvironment();
			Font Lucida = Font.createFont(Font.TRUETYPE_FONT,
					TabbedPaneFrame.class.getResourceAsStream("UIsrc" + File.separator + "LUCON.ttf"));
			Font Ubuntu = Font.createFont(Font.TRUETYPE_FONT,
					TabbedPaneFrame.class.getResourceAsStream("UIsrc" + File.separator + "Ubuntu-R.ttf"));
			ge.registerFont(Lucida);
			ge.registerFont(Ubuntu);

			Font font = new Font(Ubuntu.getFontName(), Font.PLAIN, 14);
			Font codeFont = new Font(Lucida.getFontName(), Font.PLAIN, 14);
			UIManager.put("TabbedPane.font", font);
			UIManager.put("Menu.font", font);
			UIManager.put("MenuItem.font", font);
			UIManager.put("CheckBoxMenuItem.font", font);
			UIManager.put("Label.font", font);
			SwingUtilities.updateComponentTreeUI(tabbedPane);
			SwingUtilities.updateComponentTreeUI(menuBar);
			SwingUtilities.updateComponentTreeUI(statusBar);
			Textcomponent.setFont(codeFont);
		} catch (FontFormatException e1) {
			e1.printStackTrace();
		} catch (IOException e1) {
			e1.printStackTrace();
		}

		TextNoWrapPanel.setLayout(new BorderLayout());
		TextNoWrapPanel.setBackground(Color.white);
		TextNoWrapPanel.add(Textcomponent);

		// deal with the menu bar
		setJMenuBar(menuBar);

		// the file menu enables users to open, save the xml files they're processing
		JMenu FileMenu = new JMenu("File");
		JMenuItem newItem = new JMenuItem("New XML File...");
		FileMenu.add(newItem);
		FileMenu.addSeparator();
		JMenuItem importItem = new JMenuItem("Import From File...");
		FileMenu.add(importItem);
		JMenuItem exportItem = new JMenuItem("Export To File...");
		FileMenu.add(exportItem);
		FileMenu.addSeparator();
		JMenuItem saveItem = new JMenuItem("Save XML File");
		FileMenu.add(saveItem);
		JMenuItem saveAsItem = new JMenuItem("Save XML File As...");
		FileMenu.add(saveAsItem);
		FileMenu.addSeparator();
		JMenuItem exitItem = new JMenuItem("Exit");
		FileMenu.add(exitItem);
		menuBar.add(FileMenu);

		// the edit menu contains operations for editors, like copy and undo
		JMenu EditMenu = new JMenu("Edit");
		JMenuItem cutItem = new JMenuItem("Cut");
		EditMenu.add(cutItem);
		JMenuItem copyItem = new JMenuItem("Copy");
		EditMenu.add(copyItem);
		JMenuItem pasteItem = new JMenuItem("Paste");
		EditMenu.add(pasteItem);
		EditMenu.addSeparator();
		JMenuItem selectAllItem = new JMenuItem("Select All");
		EditMenu.add(selectAllItem);
		EditMenu.addSeparator();
		JMenuItem undoItem = new JMenuItem("Undo");
		EditMenu.add(undoItem);
		JMenuItem redoItem = new JMenuItem("Redo");
		EditMenu.add(redoItem);
		menuBar.add(EditMenu);

		// the tool menu controls the appearance of the tool bar
		// users may add or dispose buttons or the tool bar at their will
		JMenu ToolMenu = new JMenu("Tools");
		JMenuItem NewFileToolItem = new JCheckBoxMenuItem("New File Button",
				true);
		ToolMenu.add(NewFileToolItem);
		JMenuItem FileToolkitItem = new JCheckBoxMenuItem(
				"Import/Export Button", true);
		ToolMenu.add(FileToolkitItem);
		JMenuItem ZoomToolkitItem = new JCheckBoxMenuItem(
				"Zoom in/Zoom Out Button", true);
		ToolMenu.add(ZoomToolkitItem);
		JMenuItem SentToolkitItem = new JCheckBoxMenuItem("Prev/Next Button",
				true);
		ToolMenu.add(SentToolkitItem);
		JMenuItem JumpToolItem = new JCheckBoxMenuItem("Jump Button", true);
		ToolMenu.add(JumpToolItem);
		JMenuItem SaveTextToolItem = new JCheckBoxMenuItem("Save XML Button",
				true);
		ToolMenu.add(SaveTextToolItem);
		ToolMenu.addSeparator();
		JMenuItem ToolbarItem = new JMenuItem("Show/Hide Toolbar");
		ToolMenu.add(ToolbarItem);
		menuBar.add(ToolMenu);

		// the layout menu controls the appearance of the viewers
		JMenu LayoutMenu = new JMenu("Layout");
		JMenuItem zoomInItem = new JMenuItem("Zoom In");
		LayoutMenu.add(zoomInItem);
		JMenuItem zoomOutItem = new JMenuItem("Zoom Out");
		LayoutMenu.add(zoomOutItem);
		LayoutMenu.addSeparator();
		JMenuItem prevSentItem = new JMenuItem("Previous Sentence");
		LayoutMenu.add(prevSentItem);
		JMenuItem nextSentItem = new JMenuItem("Next Sentence");
		LayoutMenu.add(nextSentItem);
		JMenuItem jumpSentItem = new JMenuItem("Jump To Sentence...");
		LayoutMenu.add(jumpSentItem);
		LayoutMenu.addSeparator();
		JMenuItem prevTabItem = new JMenuItem("Previous Tab");
		LayoutMenu.add(prevTabItem);
		JMenuItem nextTabItem = new JMenuItem("Next Tab");
		LayoutMenu.add(nextTabItem);
		LayoutMenu.addSeparator();
		JMenuItem skewItem = new JMenuItem("Straight/Skew Constituent Lines");
		LayoutMenu.add(skewItem);
		JMenuItem colorItem = new JMenuItem("Color/BW LFG Correspondence Lines");
		LayoutMenu.add(colorItem);
		JMenuItem showItem = new JMenuItem("Show/Hide LFG Correspondence Lines");
		LayoutMenu.add(showItem);
		menuBar.add(LayoutMenu);

		// the help menu provides link to the online introduction to input format and access to the author
		JMenu HelpMenu = new JMenu("Help");
		JMenuItem formatItem = new JMenuItem("Input Format");
		HelpMenu.add(formatItem);
		JMenuItem aboutItem = new JMenuItem("About");
		HelpMenu.add(aboutItem);
		menuBar.add(HelpMenu);

		// the pop up menu contains the same items as the edit menu
		// it is just designed for the convenience of users
		JMenuItem PopupCutItem = new JMenuItem("Cut");
		popupMenu.add(PopupCutItem);
		JMenuItem PopupCopyItem = new JMenuItem("Copy");
		popupMenu.add(PopupCopyItem);
		JMenuItem PopupPasteItem = new JMenuItem("Paste");
		popupMenu.add(PopupPasteItem);
		JMenuItem PopupSelAllItem = new JMenuItem("Select All");
		popupMenu.add(PopupSelAllItem);
		Textcomponent.setComponentPopupMenu(popupMenu);

		// add listeners and accelerators to each item by menu
		// when the item is clicked, it is the listeners that carry out supposed operations
		// the accelerators enables users to fire an operation without mouse click
		newItem.addActionListener(new ImportItemListener());
		importItem.addActionListener(new ImportItemListener());
		exportItem.addActionListener(new ExportItemListener());
		saveItem.addActionListener(new SaveTextListener());
		saveAsItem.addActionListener(new SaveTextAsListener());
		newItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_N,
						java.awt.event.KeyEvent.CTRL_MASK));
		importItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_O,
						java.awt.event.KeyEvent.CTRL_MASK));
		exportItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_P,
						java.awt.event.KeyEvent.CTRL_MASK));
		saveItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_S,
						java.awt.event.KeyEvent.CTRL_MASK));
		saveAsItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_S, java.awt.event.KeyEvent.CTRL_MASK
						| java.awt.event.KeyEvent.SHIFT_MASK));

		exitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				System.exit(0);
			}
		});

		cutItem.addActionListener(new CutItemListener());
		copyItem.addActionListener(new CopyItemListener());
		pasteItem.addActionListener(new PasteItemListener());
		selectAllItem.addActionListener(new SelAllItemListener());

		undoItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (undoManager.canUndo()) {
					undoManager.undo();
				}
			}
		});

		redoItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				if (undoManager.canRedo()) {
					undoManager.redo();
				}
			}
		});

		cutItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_X,
						java.awt.event.KeyEvent.CTRL_MASK));
		copyItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_C,
						java.awt.event.KeyEvent.CTRL_MASK));
		pasteItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_V,
						java.awt.event.KeyEvent.CTRL_MASK));
		selectAllItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_A,
						java.awt.event.KeyEvent.CTRL_MASK));
		undoItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_Z,
						java.awt.event.KeyEvent.CTRL_MASK));
		redoItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_Z, java.awt.event.KeyEvent.CTRL_MASK
						| java.awt.event.KeyEvent.SHIFT_MASK));

		NewFileToolItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				newButton.setVisible(!newButton.isVisible());
			}
		});

		FileToolkitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				importButton.setVisible(!importButton.isVisible());
				exportButton.setVisible(!exportButton.isVisible());
			}
		});

		ZoomToolkitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				zoomInButton.setVisible(!zoomInButton.isVisible());
				zoomOutButton.setVisible(!zoomOutButton.isVisible());
			}
		});

		SentToolkitItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				prevButton.setVisible(!prevButton.isVisible());
				nextButton.setVisible(!nextButton.isVisible());
			}
		});

		JumpToolItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				jumpButton.setVisible(!jumpButton.isVisible());
			}
		});

		SaveTextToolItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				saveTextButton.setVisible(!saveTextButton.isVisible());
			}
		});

		ToolbarItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				bar.setVisible(!bar.isVisible());
			}
		});

		zoomInItem.addActionListener(new ZoomInListener());
		zoomOutItem.addActionListener(new ZoomOutListener());
		prevSentItem.addActionListener(new PrevSentListener());
		nextSentItem.addActionListener(new NextSentListener());
		jumpSentItem.addActionListener(new JumpSentListener());
		prevTabItem.addActionListener(new PrevTabListener());
		nextTabItem.addActionListener(new NextTabListener());
		skewItem.addActionListener(new SkewLineListener());
		colorItem.addActionListener(new ColorLineListener());
		showItem.addActionListener(new ShowLineListener());
		zoomInItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_EQUALS,
				java.awt.event.KeyEvent.CTRL_MASK));
		zoomOutItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_MINUS,
				java.awt.event.KeyEvent.CTRL_MASK));
		prevSentItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_LEFT,
				java.awt.event.KeyEvent.CTRL_MASK));
		nextSentItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_RIGHT,
				java.awt.event.KeyEvent.CTRL_MASK));
		jumpSentItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_J,
						java.awt.event.KeyEvent.CTRL_MASK));
		prevTabItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_LEFT,
				java.awt.event.KeyEvent.SHIFT_MASK
						| java.awt.event.KeyEvent.CTRL_MASK));
		nextTabItem.setAccelerator(KeyStroke.getKeyStroke(
				java.awt.event.KeyEvent.VK_RIGHT,
				java.awt.event.KeyEvent.SHIFT_MASK
						| java.awt.event.KeyEvent.CTRL_MASK));

		formatItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				try {
					String url = "https://github.com/shuoyangd/LinguaView/blob/master/Input_Format_1_x.md";
					java.awt.Desktop.getDesktop().browse(java.net.URI.create(url));
				}
				catch (java.io.IOException e) {
					System.out.println(e.getMessage());
				}
			}
		});
		aboutItem.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				JOptionPane.showConfirmDialog(TabbedPaneFrame.this,
						"LinguaView 1.0.1 by Shuoyang Ding\n"
						+ "Language Computing & Web Mining Group, Peking University\n" 
						+ "2013.11\n"
						+ "dsy100@gmail.com\n\n"
						+ "LinguaView is an light-weight graphical tool aiming to\naid manual construction of linguistically-deep corpuses.\n"
						+ "To help make this tool better, if you find any problems or bugs,\ndo not hesitate to email me.\n\n"
						+ "Special acknowledgement to Federico Sangati,\nthe original author of the constituent viewer part.\n"
						+ "And also to Weiwei Sun and Chen Wang,\nauthors of the data structures that constitute an implementation of CCG.", "About",
						JOptionPane.DEFAULT_OPTION, JOptionPane.PLAIN_MESSAGE);
			}
		});

		PopupCutItem.addActionListener(new CutItemListener());
		PopupCopyItem.addActionListener(new CopyItemListener());
		PopupPasteItem.addActionListener(new PasteItemListener());
		PopupSelAllItem.addActionListener(new SelAllItemListener());
		PopupCutItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_X,
						java.awt.event.KeyEvent.CTRL_MASK));
		PopupCopyItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_C,
						java.awt.event.KeyEvent.CTRL_MASK));
		PopupPasteItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_V,
						java.awt.event.KeyEvent.CTRL_MASK));
		PopupSelAllItem.setAccelerator(KeyStroke
				.getKeyStroke(java.awt.event.KeyEvent.VK_A,
						java.awt.event.KeyEvent.CTRL_MASK));

		// deal with the tool bar
		Icon newIcon = new ImageIcon(TabbedPaneFrame.class.getResource("UIsrc" + File.separator + "new.png"));
		Icon importIcon = new ImageIcon(TabbedPaneFrame.class.getResource("UIsrc" + File.separator + "import.png"));
		Icon exportIcon = new ImageIcon(TabbedPaneFrame.class.getResource("UIsrc" + File.separator + "export.png"));
		Icon zoomInIcon = new ImageIcon(TabbedPaneFrame.class.getResource("UIsrc" + File.separator + "zoomin.png"));
		Icon zoomOutIcon = new ImageIcon(TabbedPaneFrame.class.getResource("UIsrc" + File.separator + "zoomout.png"));
		Icon prevIcon = new ImageIcon(TabbedPaneFrame.class.getResource("UIsrc" + File.separator + "prev.png"));
		Icon nextIcon = new ImageIcon(TabbedPaneFrame.class.getResource("UIsrc" + File.separator + "next.png"));
		Icon jumpIcon = new ImageIcon(TabbedPaneFrame.class.getResource("UIsrc" + File.separator + "jump.png"));
		Icon saveTextIcon = new ImageIcon(TabbedPaneFrame.class.getResource("UIsrc" + File.separator + "ok.png"));

		newButton.setIcon(newIcon);
		importButton.setIcon(importIcon);
		exportButton.setIcon(exportIcon);
		zoomInButton.setIcon(zoomInIcon);
		zoomOutButton.setIcon(zoomOutIcon);
		prevButton.setIcon(prevIcon);
		nextButton.setIcon(nextIcon);
		jumpButton.setIcon(jumpIcon);
		saveTextButton.setIcon(saveTextIcon);

		newButton.addActionListener(new ImportItemListener());
		importButton.addActionListener(new ImportItemListener());
		exportButton.addActionListener(new ExportItemListener());
		zoomInButton.addActionListener(new ZoomInListener());
		zoomOutButton.addActionListener(new ZoomOutListener());
		prevButton.addActionListener(new PrevSentListener());
		nextButton.addActionListener(new NextSentListener());
		jumpButton.addActionListener(new JumpSentListener());
		saveTextButton.addActionListener(new SaveTextListener());

		bar.add(newButton);
		bar.add(importButton);
		bar.add(exportButton);
		bar.add(zoomInButton);
		bar.add(zoomOutButton);
		bar.add(prevButton);
		bar.add(nextButton);
		bar.add(jumpButton);
		bar.add(saveTextButton);
		bar.setOrientation(JToolBar.VERTICAL);

		add(bar, "West");

		// deal with the tabs
		tabbedPane.addTab("Constituent Tree", null, null);
		tabbedPane.addTab("Dependency Tree", null, null);
		tabbedPane.addTab("Deep Dependency Graph", null, null);
		tabbedPane.addTab("Combinatorial Categorial Tree", null, null);
		tabbedPane.addTab("Lexical Functional Structure", null, null);
		tabbedPane.addTab("Text Editor", null, null);

		// default constTree initialization
		ArrayList<TSNodeLabel> constTreebank = new ArrayList<TSNodeLabel>();
		String defaultConstStr = "( (S (NP-SBJ (NP (NNP Pierre) (NNP Vinken) ) (, ,)"
				+ " (ADJP (NP (CD 61) (NNS years) ) (JJ old) ) (, ,) )"
				+ " (VP (MD will) (VP (VB join) (NP (DT the) (NN board) )"
				+ " (PP-CLR (IN as) (NP (DT a) (JJ nonexecutive) (NN director) ))"
				+ " (NP-TMP (NNP Nov.) (CD 29) ))) (. .) ))";
		defaultConstTreebank = new TSNodeLabel(defaultConstStr);
		constTreebank.add(defaultConstTreebank);
		constcomponent.loadTreebank(constTreebank);
		constcomponent.init();
		constcomponent.setBackground(Color.WHITE);

		// default dependencyTree initialization
		ArrayList<DepTree> DepTreebank = new ArrayList<DepTree>();
		defaultDepTreebank = new DepTree();
		defaultDepTreebank
				.loadTokens("Pierre Vinken , 61 years old , will join the board as "
						+ "a nonexecutive director Nov. 29 .");
		defaultDepTreebank
				.loadEdges("(0, 1, _) (1, 7, _) (2, 1, _) (3, 4, _) (4, 5, _) (5, 1, _)"
						+ "(6, 1, _) (7, -1, _) (8, 7, _) (9, 10, _) (10, 8, _) (11, 8, _)"
						+ " (12, 14, _) (13, 14, _) (14, 11, _) (15, 8, _) (16, 15, _) (17, 7, _)");
		DepTreebank.add(defaultDepTreebank);
		depcomponent.loadTreebank(DepTreebank);
		depcomponent.init();
		depcomponent.setBackground(Color.WHITE);

		// default deep dependency tree initialization
		ArrayList<DepTree> DeepdepTreebank = new ArrayList<DepTree>();
		defaultDeepdepTreebank = new DepTree();
		defaultDeepdepTreebank
				.loadTokens("Pierre Vinken , 61 years old , will join the board as "
						+ "a nonexecutive director Nov. 29 .");
		defaultDeepdepTreebank
				.loadEdges("(0, 1, _) (1, 7, _) (2, 1, _) (3, 4, _) (4, 5, _) (5, 1, _)"
						+ "(6, 1, _) (7, -1, _) (8, 7, _) (9, 10, _) (10, 8, _) (11, 8, _)"
						+ " (12, 14, _) (13, 14, _) (14, 11, _) (15, 8, _) (16, 15, _) (17, 7, _)");
		DeepdepTreebank.add(defaultDeepdepTreebank);
		deepdepcomponent.loadTreebank(DeepdepTreebank);
		deepdepcomponent.init();
		deepdepcomponent.setBackground(Color.WHITE);

		// default CCG parse tree initialization
		ArrayList<CCGNode> CCGTreebank = new ArrayList<CCGNode>();
		String defaultCCGStr = "(<T S[dcl] 0 2> (<T S[dcl] 1 2> (<T NP 0 2> (<T NP 0 2> "
				+ "(<T NP 0 2> (<T NP 0 1> (<T N 1 2> (<L N/N NNP NNP Pierre N_73/N_73>) "
				+ "(<L N NNP NNP Vinken N>) ) ) (<L , , , , ,>) ) (<T NP\\NP 0 1> "
				+ "(<T S[adj]\\NP 1 2> (<T NP 0 1> (<T N 1 2> (<L N/N CD CD 61 N_93/N_93>) "
				+ "(<L N NNS NNS years N>) ) ) (<L (S[adj]\\NP)\\NP JJ JJ old (S[adj]\\NP_83)\\NP_84>) ) ) ) "
				+ "(<L , , , , ,>) ) (<T S[dcl]\\NP 0 2> "
				+ "(<L (S[dcl]\\NP)/(S[b]\\NP) MD MD will (S[dcl]\\NP_10)/(S[b]_11\\NP_10:B)_11>) "
				+ "(<T S[b]\\NP 0 2> (<T S[b]\\NP 0 2> (<T (S[b]\\NP)/PP 0 2> "
				+ "(<L ((S[b]\\NP)/PP)/NP VB VB join ((S[b]\\NP_20)/PP_21)/NP_22>) "
				+ "(<T NP 1 2> (<L NP[nb]/N DT DT the NP[nb]_29/N_29>) (<L N NN NN board N>) ) ) "
				+ "(<T PP 0 2> (<L PP/NP IN IN as PP/NP_34>) (<T NP 1 2> "
				+ "(<L NP[nb]/N DT DT a NP[nb]_48/N_48>) (<T N 1 2> "
				+ "(<L N/N JJ JJ nonexecutive N_43/N_43>) (<L N NN NN director N>) ) ) ) ) "
				+ "(<T (S\\NP)\\(S\\NP) 0 2> (<L ((S\\NP)\\(S\\NP))/N[num] NNP NNP Nov. "
				+ "((S_61\\NP_56)_61\\(S_61\\NP_56)_61)/N[num]_62>) "
				+ "(<L N[num] CD CD 29 N[num]>) ) ) ) ) (<L . . . . .>) ) ";
		defaultCCGTreebank = CCGNode.getCCGNodeFromString(defaultCCGStr);
		CCGTreebank.add(defaultCCGTreebank);
		CCGcomponent.loadTreebank(CCGTreebank);
		CCGcomponent.init();
		CCGcomponent.setBackground(Color.WHITE);

		// default LFG structure initialization
		ArrayList<Element> LFGStructbank = new ArrayList<Element>();
		String defaultLFGStr = "<?xml version=\"1.0\" encoding=\"utf-8\" ?>"
				+ "<lfg><cstruct>( (S#1 (NP#2 (NP#2 (NNP Pierre) (NNP Vinken) ) (, ,) "
				+ "(ADJP (NP (CD 61) (NNS years) ) (JJ old) ) (, ,) ) (VP (MD will) (VP#1 (VB join) "
				+ "(NP (DT the) (NN board) ) (PP (IN as) (NP (DT a) (JJ nonexecutive) (NN director) )) "
				+ "(NP-TMP (NNP Nov.) (CD 29) ))) (. .) ))</cstruct>"
				+ "<fstruct id=\"1\"><attr name=\"PRED\" valtype=\"sem\"> 'join(SUBJ,OBJ,XCOMP)' </attr>"
				+ "<attr name=\"SUBJ\" valtype=\"fstruct\">"
				+ "<fstruct id=\"2\"><attr name=\"PRED\" valtype=\"sem\"> 'vinken' </attr>"
				+ "<attr name=\"NUM\" valtype=\"atomic\"> SG </attr>"
				+ "<attr name=\"ADJ\" valtype=\"set\">"
				+ "<fstruct id=\"3\"><attr name=\"PRED\" valtype=\"sem\"> 'pierre' </attr></fstruct>"
				+ "<fstruct id=\"4\"><attr name=\"PRED\" valtype=\"sem\"> 'old(SPEC)' </attr>"
				+ "<attr name=\"SPEC\" valtype=\"fstruct\">"
				+ "<fstruct id=\"5\"><attr name=\"PRED\" valtype=\"sem\"> 'year(SPEC)' </attr>"
				+ "<attr name=\"SPEC\" valtype=\"fstruct\">"
				+ "<fstruct id=\"6\"><attr name=\"PRED\" valtype=\"sem\"> '61' </attr>"
				+ "</fstruct></attr></fstruct></attr></fstruct></attr></fstruct></attr>"
				+ "<attr name=\"OBJ\" valtype=\"fstruct\">"
				+ "<fstruct id=\"7\"><attr name=\"PRED\" valtype=\"sem\"> 'broad' </attr>"
				+ "<attr name=\"DEF\" valtype=\"atomic\"> + </attr>"
				+ "<attr name=\"NUM\" valtype=\"atomic\"> SG </attr></fstruct></attr>"
				+ "<attr name=\"XCOMP\" valtype=\"fstruct\">"
				+ "<fstruct id=\"8\"><attr name=\"PRED\" valtype=\"sem\"> 'as(OBJ)' </attr>"
				+ "<attr name=\"OBJ\" valtype=\"fstruct\">"
				+ "<fstruct id=\"9\"><attr name=\"PRED\" valtype=\"sem\"> 'director' </attr>"
				+ "<attr name=\"NUM\" valtype=\"atomic\"> SG </attr>"
				+ "<attr name=\"ADJ\" valtype=\"set\">"
				+ "<fstruct id=\"10\"><attr name=\"PRED\" valtype=\"sem\"> 'nonexecutive' </attr>"
				+ "</fstruct></attr><attr name=\"DEF\" valtype=\"atomic\"> + </attr></fstruct></attr></fstruct></attr>"
				+ "<attr name=\"TENSE\" valtype=\"atomic\"> FUTURE </attr>"
				+ "<attr name=\"ADJ\" valtype=\"set\">"
				+ "<fstruct id=\"11\"><attr name=\"PRED\" valtype=\"set\">"
				+ "<fstruct id=\"12\"><attr name=\"PRED\" valtype=\"sem\"> 'Nov.' </attr>"
				+ "<attr name=\"TIME\" valtype=\"atomic\"> + </attr></fstruct>"
				+ "<fstruct id=\"13\"><attr name=\"PRED\" valtype=\"sem\"> '29' </attr>"
				+ "<attr name=\"TIME\" valtype=\"atomic\"> + </attr></fstruct></attr>"
				+ "</fstruct></attr></fstruct></lfg>";
		try {
			InputStream stream = new ByteArrayInputStream(
					defaultLFGStr.getBytes("UTF-8"));
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			Document doc = builder.parse(stream);
			defaultLFGStructbank = doc.getDocumentElement();
			LFGStructbank.add(defaultLFGStructbank);
			LFGcomponent.loadTreebank(LFGStructbank);
			LFGcomponent.init();
			LFGcomponent.setBackground(Color.WHITE);
		} catch (Exception e) {
			e.printStackTrace();
		}

		// default text editor content initialization
		Textcomponent.setText("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n"
				+ "<hint lang=\"English\">\n\tLoad a corpus\n</hint>\n"
				+ "<hint lang=\"Francais\">\n\tCharger un corpus\n</hint>\n"
				+ "<hint lang=\"Deutsch\">\n\tLaden ein Korpus\n</hint>"
				+ "<hint lang=\"中文\">\n\t请载入语料\n</hint>");
		Textcomponent.setEditable(false);

		add(tabbedPane, "Center");

		// filling in the tabs with components
		tabbedPane.addChangeListener(new ChangeListener() {
			public void stateChanged(ChangeEvent event) {
				if (tabbedPane.getSelectedComponent() == null) {

					int n = tabbedPane.getSelectedIndex();
					if (n == tabbedPane.indexOfTab("Constituent Tree")) {
						tabbedPane.setComponentAt(n, constScrollPane);
					}
					else if (n == tabbedPane.indexOfTab("Dependency Tree")) {
						tabbedPane.setComponentAt(n, depScrollPane);
					}
					else if (n == tabbedPane
							.indexOfTab("Deep Dependency Graph")) {
						tabbedPane.setComponentAt(n, deepdepScrollPane);
					}
					else if (n == tabbedPane
							.indexOfTab("Combinatorial Categorial Tree")) {
						tabbedPane.setComponentAt(n, CCGScrollPane);
					}
					else if (n == tabbedPane
							.indexOfTab("Lexical Functional Structure")) {
						tabbedPane.setComponentAt(n, LFGScrollPane);
					}
					else if (n == tabbedPane.indexOfTab("Text Editor")) {
						tabbedPane.setComponentAt(n, TextScrollPane);
					}
				}
			}
		});

		tabbedPane.setComponentAt(0, constScrollPane);
		tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);

		add(statusBar, "South");
	}

	/**
	 * increase all the text (except text editor) by 1 in font size
	 * other lines and spaces also augments by scale
	 */
	public void zoomIn() {
		constcomponent.fontSize++;
		depcomponent.fontSize++;
		deepdepcomponent.fontSize++;
		CCGcomponent.fontSize++;
		LFGcomponent.fontSize++;

		constcomponent.init();
		depcomponent.init();
		deepdepcomponent.init();
		CCGcomponent.init();
		LFGcomponent.init();
	}

	/**
	 * decrease all the text (except text editor) by 1 in font size
	 * other lines and spaces also shrinks by scale
	 */
	public void zoomOut() {
		if (constcomponent.fontSize > 0) {
			constcomponent.fontSize--;
		}

		if (depcomponent.fontSize > 0) {
			depcomponent.fontSize--;
		}

		if (deepdepcomponent.fontSize > 0) {
			deepdepcomponent.fontSize--;
		}

		if (CCGcomponent.fontSize > 0) {
			CCGcomponent.fontSize--;
		}

		if (LFGcomponent.fontSize > 0) {
			LFGcomponent.fontSize--;
		}

		constcomponent.init();
		depcomponent.init();
		deepdepcomponent.init();
		CCGcomponent.init();
		LFGcomponent.init();
	}

	/**
	 * decrease sentenceNumber by 1 for each viewer
	 * that switch the viewers to the graphical representations of the previous sentence
	 */
	public void prevSent() {
		boolean Success = true;
		
		if (constcomponent.sentenceNumber > 0) {
			constcomponent.sentenceNumber--;
			try {
				constcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in constituent tree part.");
				constcomponent.replaceCurrentSentence(defaultConstTreebank);
				constcomponent.init();
			}
		}

		if (depcomponent.sentenceNumber > 0) {
			depcomponent.sentenceNumber--;
			try {
				depcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar
					.setMessage("Please check for problems in dependency tree part.");
				for (StackTraceElement tr : e.getStackTrace()) {
					if (tr.getMethodName().contains("loadTokens")) {
						statusBar
							.setMessage("Please check for problems in wordlist part.");
					}
				}
				depcomponent.replaceCurrentSentence(defaultDepTreebank);
				depcomponent.init();
			}
		}

		if (deepdepcomponent.sentenceNumber > 0) {
			deepdepcomponent.sentenceNumber--;
			try {
				deepdepcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar
					.setMessage("Please check for problems in deep dependency tree part.");
				for (StackTraceElement tr : e.getStackTrace()) {
					if (tr.getMethodName().contains("loadTokens")) {
						statusBar
							.setMessage("Please check for problems in wordlist part.");
					}
				}
				deepdepcomponent.replaceCurrentSentence(defaultDeepdepTreebank);
				deepdepcomponent.init();
			}
		}

		if (CCGcomponent.sentenceNumber > 0) {
			CCGcomponent.sentenceNumber--;
			try {
				CCGcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in CCG part.");
				CCGcomponent.replaceCurrentSentence(defaultCCGTreebank);
				CCGcomponent.init();
			}
		}

		if (LFGcomponent.sentenceNumber > 0) {
			LFGcomponent.sentenceNumber--;
			try {
				LFGcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in LFG part.");
				LFGcomponent.replaceCurrentSentence(defaultLFGStructbank);
				LFGcomponent.init();
			}
		}
		
		// note that the sentenceNumber starts from 0 
		// but the display on the status bar starts from 1
		if(Success) {
			statusBar.setMessage("You are at sentence "
					+ Integer.toString(constcomponent.sentenceNumber + 1) + "/"
					+ Integer.toString(constcomponent.lastIndex + 1));
		}
	}

	/**
	 * increase sentenceNumber by 1 for each viewer
	 * that switch the viewers to the graphical representations of the next sentence
	 */
	public void nextSent() {
		boolean Success = true;
		
		if (constcomponent.sentenceNumber < constcomponent.lastIndex) {
			constcomponent.sentenceNumber++;
			try {
				constcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in constituent tree part.");
				constcomponent.replaceCurrentSentence(defaultConstTreebank);
				constcomponent.init();
			}
		}

		if (depcomponent.sentenceNumber < depcomponent.lastIndex) {
			depcomponent.sentenceNumber++;
			try {
				depcomponent.init();
			}
			catch(Exception e) {
				e.printStackTrace();
				Success = false;
				statusBar
					.setMessage("Please check for problems in dependency tree part.");
				for (StackTraceElement tr : e.getStackTrace()) {
					if (tr.getMethodName().contains("loadTokens")) {
						statusBar
							.setMessage("Please check for problems in wordlist part.");
					}
				}
				depcomponent.replaceCurrentSentence(defaultDepTreebank);
				depcomponent.init();
			}
		}

		if (deepdepcomponent.sentenceNumber < deepdepcomponent.lastIndex) {
			deepdepcomponent.sentenceNumber++;
			try {
				deepdepcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar
					.setMessage("Please check for problems in deep dependency tree part.");
				for (StackTraceElement tr : e.getStackTrace()) {
					if (tr.getMethodName().contains("loadTokens")) {
						statusBar
							.setMessage("Please check for problems in wordlist part.");
					}
				}
				deepdepcomponent.replaceCurrentSentence(defaultDeepdepTreebank);
				deepdepcomponent.init();
			}
		}

		if (CCGcomponent.sentenceNumber < CCGcomponent.lastIndex) {
			CCGcomponent.sentenceNumber++;
			try {
				CCGcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in CCG part.");
				CCGcomponent.replaceCurrentSentence(defaultCCGTreebank);
				CCGcomponent.init();
			}
		}

		if (LFGcomponent.sentenceNumber < LFGcomponent.lastIndex) {
			LFGcomponent.sentenceNumber++;
			try {
				LFGcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in LFG part.");
				LFGcomponent.replaceCurrentSentence(defaultLFGStructbank);
				LFGcomponent.init();
			}
		}
		
		// note that the sentenceNumber starts from 0 
		// but the display on the status bar starts from 1
		if(Success) {
			statusBar.setMessage("You are at sentence "
					+ Integer.toString(constcomponent.sentenceNumber + 1) + "/"
					+ Integer.toString(constcomponent.lastIndex + 1));
		}
	}

	/**
	 * set sentenceNumber to a certain value for each viewer
	 * that switch the viewers to the graphical representations of a specified sentence
	 */
	public void jumpSent(int newSentenceNumber) {
		boolean Success = true;
		
		newSentenceNumber--;
		if (newSentenceNumber <= constcomponent.lastIndex
				&& newSentenceNumber > 0) {
			constcomponent.sentenceNumber = newSentenceNumber;
			try {
				constcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in constituent tree part.");
			}
		}

		if (newSentenceNumber <= depcomponent.lastIndex
				&& newSentenceNumber > 0) {
			depcomponent.sentenceNumber = newSentenceNumber;
			try {
				depcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar
					.setMessage("Please check for problems in dependency tree part.");
				for (StackTraceElement tr : e.getStackTrace()) {
					if (tr.getMethodName().contains("loadTokens")) {
						statusBar
							.setMessage("Please check for problems in wordlist part.");
					}
				}
			}
		}

		if (newSentenceNumber <= deepdepcomponent.lastIndex
				&& newSentenceNumber > 0) {
			deepdepcomponent.sentenceNumber = newSentenceNumber;
			try {
				deepdepcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar
					.setMessage("Please check for problems in deep dependency tree part.");
				for (StackTraceElement tr : e.getStackTrace()) {
					if (tr.getMethodName().contains("loadTokens")) {
						statusBar
							.setMessage("Please check for problems in wordlist part.");
					}
				}
			}
		}

		if (newSentenceNumber <= CCGcomponent.lastIndex
				&& newSentenceNumber > 0) {
			CCGcomponent.sentenceNumber = newSentenceNumber;
			try {
				CCGcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in CCG part.");
			}
		}

		if (newSentenceNumber <= LFGcomponent.lastIndex
				&& newSentenceNumber > 0) {
			LFGcomponent.sentenceNumber = newSentenceNumber;
			try {
				LFGcomponent.init();
			}
			catch(Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in LFG part.");
			}
		}
		
		// note that the sentenceNumber starts from 0 
		// but the display on the status bar starts from 1
		if (Success) {
			statusBar.setMessage("You are at sentence "
				+ Integer.toString(constcomponent.sentenceNumber + 1) + "/"
				+ Integer.toString(constcomponent.lastIndex + 1));
		}
	}

	/**
	 * import data from parsed XML document "doc"
	 * @param doc
	 */
	public void importView(Document doc) {
		Element docRoot = doc.getDocumentElement();
		NodeList children = docRoot.getChildNodes();
		ArrayList<Element> Sentences = new ArrayList<Element>();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Element) {
				Sentences.add((Element) child);
			}
		}
		ArrayList<TSNodeLabel> constTreebank = new ArrayList<TSNodeLabel>();
		ArrayList<DepTree> depTreebank = new ArrayList<DepTree>();
		ArrayList<DepTree> deepdepTreebank = new ArrayList<DepTree>();
		ArrayList<CCGNode> CCGTreebank = new ArrayList<CCGNode>();
		ArrayList<Element> LFGStructbank = new ArrayList<Element>();
		// "Success" indicates whether the whole import operation has been successfully implemented
		boolean Success = true;
		int sentNum = 0;

		//iterate through all the sentences
		for (sentNum = 0; sentNum < Sentences.size(); sentNum++) {
			Element sent = Sentences.get(sentNum);
			
			// indicate if each grammar tree is loaded for this sentence
			// if not, use a default tree to fill in
			boolean constTreeAdded = false;
			boolean depTreeAdded = false;
			boolean deepdepTreeAdded = false;
			boolean CCGTreeAdded = false;
			boolean LFGStructAdded = false;
			
			// get the i-th sentence
			if (sent instanceof Element) {
				NodeList sentChildren = sent.getChildNodes();
				Element tokenls = null;
				// iterate through all the elements in a sentence
				for (int j = 0; j < sentChildren.getLength(); j++) {
					Node sentElement = sentChildren.item(j);

					// extract element for wordlist
					// but it is only loaded when <deptree> tag is encountered
					if ((sentElement instanceof Element)
							&& (sentElement.getNodeName() == "wordlist")) {
						tokenls = (Element) sentElement;
					}

					// extract and load constituent tree
					else if ((sentElement instanceof Element)
							&& (sentElement.getNodeName() == "constree")) {
						try {
							children = sentElement.getChildNodes();
							String constStr = new String();
							for (int k = 0; k < children.getLength(); k++) {
								Node constTextNode = children.item(k);
								if (constTextNode instanceof Text) {
									constStr = ((Text) constTextNode)
											.getTextContent();
								}
							}
							if (!constStr.trim().isEmpty()) {
								TSNodeLabel constTree = new TSNodeLabel(
										constStr);
								constTreebank.add(constTree);
							}
							else {
								constTreebank.add(defaultConstTreebank);
							}
							constTreeAdded = true;
						} catch (Exception e) {
							Success = false;
							statusBar
									.setMessage("Please check for problems in constituent tree part.");
						}
					}

					//extract and load dependency tree & wordlist
					else if ((sentElement instanceof Element)
							&& (sentElement.getNodeName() == "deptree")) {
						try {
							children = sentElement.getChildNodes();
							String depStr = new String();
							for (int k = 0; k < children.getLength(); k++) {
								Node depTextNode = children.item(k);
								if (depTextNode instanceof Text) {
									depStr = ((Text) depTextNode)
											.getTextContent();
								}
							}
							if (!depStr.trim().isEmpty()) {
								DepTree depTree = new DepTree();
								depTree.loadTokens(tokenls);
								depTree.loadEdges(depStr.trim());
								depTreebank.add(depTree);
							}
							else {
								depTreebank.add(defaultDepTreebank);
							}
							depTreeAdded = true;
						} catch (Exception e) {
							Success = false;
							statusBar
									.setMessage("Please check for problems in dependency tree part.");
							for (StackTraceElement tr : e.getStackTrace()) {
								if (tr.getMethodName().contains("loadTokens")) {
									statusBar
											.setMessage("Please check for problems in wordlist part.");
								}
							}
						}
					}

					//extract and load deep dependency graph & wordlist
					else if ((sentElement instanceof Element)
							&& (sentElement.getNodeName() == "deepdep")) {
						try {
							children = sentElement.getChildNodes();
							String depStr = new String();
							for (int k = 0; k < children.getLength(); k++) {
								Node depTextNode = children.item(k);
								if (depTextNode instanceof Text) {
									depStr = ((Text) depTextNode)
											.getTextContent();
								}
							}
							if (!depStr.trim().isEmpty()) {
								DepTree depTree = new DepTree();
								depTree.loadTokens(tokenls);
								depTree.loadEdges(depStr.trim());
								deepdepTreebank.add(depTree);
							}
							else {
								deepdepTreebank.add(defaultDepTreebank);
							}
							deepdepTreeAdded = true;
						} catch (Exception e) {
							Success = false;
							statusBar
									.setMessage("Please check for problems in deep dependency tree part.");
							for (StackTraceElement tr : e.getStackTrace()) {
								if (tr.getMethodName().contains("loadTokens")) {
									statusBar
											.setMessage("Please check for problems in wordlist part.");
								}
							}
						}
					}

					// extract LFG structure
					// it is loaded in LFGcomponent.init()
					else if ((sentElement instanceof Element)
							&& (sentElement.getNodeName() == "lfg")) {
						try {
							LFGStructbank.add((Element) sentElement);
							LFGStructAdded = true;
						} catch (Exception e) {
							Success = false;
							statusBar
									.setMessage("Please check for problems in LFG part.");
						}
					}

					// extract and load CCG
					else if ((sentElement instanceof Element)
							&& (sentElement.getNodeName() == "ccg")) {
						try {
							children = sentElement.getChildNodes();
							String CCGStr = new String();
							for (int k = 0; k < children.getLength(); k++) {
								Node CCGTextNode = children.item(k);
								if (CCGTextNode instanceof Text) {
									CCGStr = ((Text) CCGTextNode)
											.getTextContent();
								}
							}
							if (CCGStr.trim().matches("^\\( *\\)$")) {
								CCGNode CCGTree = new CCGTerminalNode("<L _ _ _ _ _>", 0);
								CCGTreebank.add(CCGTree);
							}
							else if (!CCGStr.trim().isEmpty()) {
								CCGStr = CCGStr.replace('{', '<');
								CCGStr = CCGStr.replace('}', '>');
								CCGNode CCGTree = CCGNode
										.getCCGNodeFromString(CCGStr.trim());
								CCGTreebank.add(CCGTree);
							}
							else {
								CCGTreebank.add(defaultCCGTreebank);
							}
							CCGTreeAdded = true;
						} catch (Exception e) {
							Success = false;
							statusBar
									.setMessage("Please check for problems in CCG part.");
							e.printStackTrace();
						}
					}
				}
			}
			// if any grammar tree is not loaded for this sentence
			// use a default one to fill the position
			if (!constTreeAdded) {
				constTreebank.add(defaultConstTreebank);
			}
			if (!depTreeAdded) {
				depTreebank.add(defaultDepTreebank);
			}
			if (!deepdepTreeAdded) {
				deepdepTreebank.add(defaultDeepdepTreebank);
			}
			if (!CCGTreeAdded) {
				CCGTreebank.add(defaultCCGTreebank);
			}
			if (!LFGStructAdded) {
				LFGStructbank.add(defaultLFGStructbank);
			}
		}

		constcomponent.sentenceNumber = 0;
		depcomponent.sentenceNumber = 0;
		deepdepcomponent.sentenceNumber = 0;
		CCGcomponent.sentenceNumber = 0;
		LFGcomponent.sentenceNumber = 0;

		//load treebank and initiate the graphical representations
		if (!constTreebank.isEmpty()) {
			try {
				constcomponent.loadTreebank(constTreebank);
				constcomponent.init();
			} catch (Exception e) {
				Success = false;
				statusBar
						.setMessage("Please check for problems in constituent tree part.");
				constcomponent.replaceCurrentSentence(defaultConstTreebank);
				constcomponent.init();
			}
		}

		if (!depTreebank.isEmpty()) {
			try {
				depcomponent.loadTreebank(depTreebank);
				depcomponent.init();
			} catch (Exception e) {
				Success = false;
				statusBar
						.setMessage("Please check for problems in dependency tree part.");
				depcomponent.replaceCurrentSentence(defaultDepTreebank);
				depcomponent.init();
			}
		}

		if (!deepdepTreebank.isEmpty()) {
			try {
				deepdepcomponent.loadTreebank(deepdepTreebank);
				deepdepcomponent.init();
			} catch (Exception e) {
				Success = false;
				statusBar
						.setMessage("Please check for problems in deep dependency tree part.");
				deepdepcomponent.replaceCurrentSentence(defaultDeepdepTreebank);
				deepdepcomponent.init();
			}
		}

		if (!CCGTreebank.isEmpty()) {
			try {
				CCGcomponent.loadTreebank(CCGTreebank);
				CCGcomponent.init();
			} catch (Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in CCG part.");
				CCGcomponent.replaceCurrentSentence(defaultCCGTreebank);
				CCGcomponent.init();
			}
		}

		if (!LFGStructbank.isEmpty()) {
			try {
				LFGcomponent.loadTreebank(LFGStructbank);
				LFGcomponent.init();
				if(!LFGcomponent.isAllRefValid()) {
					Success = false;
					statusBar.setMessage("Invalid c-structure & f-structure correspondence detected.");
				}
			} catch (Exception e) {
				Success = false;
				statusBar.setMessage("Please check for problems in LFG part.");
				LFGcomponent.replaceCurrentSentence(defaultLFGStructbank);
				LFGcomponent.init();
			}
		}
		
		// note that the sentenceNumber starts from 0 
		// but the display on the status bar starts from 1
		if(Success) {
			statusBar.setMessage("You are at sentence "
					+ Integer.toString(constcomponent.sentenceNumber + 1) + "/"
					+ Integer.toString(constcomponent.lastIndex + 1));
		}
	}

	/**
	 * this function imports XML text into the text editor, configure the undoManager,
	 * and then calls the importView() function to load data
	 * 
	 * @param filename
	 */
	public void importFromFile(String filename) {
		try {
			int res = 0;
			if ((new File(filename)).length() > 1024000) {
				res = JOptionPane.showConfirmDialog(tabbedPane,
						"This file may results in "
								+ "a long time wait due to its large size.\n"
								+ "Would you like to continue anyway?", "Warning",
						JOptionPane.YES_NO_OPTION);
			}

			if (res == 0) {
				new TextComponentLayout(Textcomponent, filename);
				Textcomponent.getDocument().addUndoableEditListener(
						new XMLUndoableEditListener());
				undoManager.discardAllEdits();

//				BufferedReader in = new BufferedReader(new FileReader(filename));
//				String file = "";
//				String line = in.readLine();
//				while (line != null) {
//					file += line;
//					file += "\n";
//					line = in.readLine();
//				}
//				in.close();
//				Textcomponent.setText(file);
//				Textcomponent.setEditable(true);
//				undoManager.discardAllEdits();

				DocumentBuilderFactory factory = DocumentBuilderFactory
						.newInstance();
				DocumentBuilder builder = factory.newDocumentBuilder();
				Document doc = builder.parse(new InputSource(new FileReader(
						filename)));
				importView(doc);
			}
		} catch (Exception e) {
			statusBar.setMessage("Please check for problems in XML format.");
		}
	}
	
	/**
	 * This function does not import text into editor
	 * It is only called to set up the default view when the program starts
	 * 
	 * @param ViewStr
	 */
	public void importFromString(String ViewStr) {
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory
					.newInstance();
			DocumentBuilder builder = factory.newDocumentBuilder();
			InputStream stream = new ByteArrayInputStream(
					ViewStr.getBytes("UTF-8"));
			Document doc = builder.parse(stream);
			importView(doc);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * This listener is fired when importItem is clicked
	 * The target directory will be fetched from a pop up dialog
	 * If the file indicated by the directory exists, just open it up
	 * Elsewise, the file would be created first and default content will be filled in
	 * 
	 * @author shuoyang
	 */
	class ImportItemListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			JFileChooser chooser = new JFileChooser();
			chooser.setCurrentDirectory(new File("."));
			int result = chooser.showOpenDialog(TabbedPaneFrame.this);
			if (chooser.getSelectedFile() != null && result == 0) {
				try {
					if (!chooser.getSelectedFile().exists()) {
						chooser.getSelectedFile().createNewFile();
						BufferedWriter in = new BufferedWriter(new FileWriter(
								chooser.getSelectedFile()));
						in.write("<?xml version=\"1.0\" encoding=\"utf-8\" ?>\n");
						in.write("<viewer>\n");
						in.write("\t<sentence id=\"1\">\n");
						in.write("\t</sentence>\n");
						in.write("</viewer>\n");
						in.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
				filename = chooser.getSelectedFile().getAbsolutePath();
				importFromFile(filename);
			}
		}
	}

	/**
	 * This listener is fired when exportItem is clicked
	 * It shows an ExportOptionPanel and leaves the rest work to it
	 * 
	 * @author shuoyang
	 */
	class ExportItemListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			ExportOptionPanel exp = new ExportOptionPanel();
			exp.showDialog(TabbedPaneFrame.this, "Export");
		}
	}
	
	class CutItemListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Textcomponent.cut();
		}
	}

	class CopyItemListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Textcomponent.copy();
		}
	}

	class PasteItemListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Textcomponent.paste();
		}
	}

	class SelAllItemListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			Textcomponent.selectAll();
		}
	}

	class ZoomInListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			zoomIn();
		}
	}

	class ZoomOutListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			zoomOut();
		}
	}

	class PrevSentListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			prevSent();
		}
	}

	/**
	 * This listener is fired when jumpSentItem is clicked
	 * It shows a dialog to fetch the id of destination sentence and jumps to it
	 * 
	 * @author shuoyang
	 */
	class JumpSentListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			String res = JOptionPane.showInputDialog(tabbedPane,
					"To which sentence do you what to jump?",
					"Jump To Sentence...", JOptionPane.PLAIN_MESSAGE);
			if (res != null && !res.trim().isEmpty()) {
				jumpSent(Integer.parseInt(res));
			}
		}
	}

	class NextSentListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			nextSent();
		}
	}

	/**
	 * This listener is fired when prevTabItem is clicked
	 * 
	 * @author shuoyang
	 */
	class PrevTabListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			int n = tabbedPane.getSelectedIndex();
			if (n == tabbedPane.indexOfTab("Dependency Tree")) {
				n = tabbedPane.indexOfTab("Constituent Tree");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, constScrollPane);
			}
			else if (n == tabbedPane.indexOfTab("Deep Dependency Graph")) {
				n = tabbedPane.indexOfTab("Dependency Tree");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, depScrollPane);
			}
			else if (n == tabbedPane
					.indexOfTab("Combinatorial Categorial Tree")) {
				n = tabbedPane.indexOfTab("Deep Dependency Graph");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, deepdepScrollPane);
			}
			else if (n == tabbedPane.indexOfTab("Lexical Functional Structure")) {
				n = tabbedPane.indexOfTab("Combinatorial Categorial Tree");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, CCGScrollPane);
			}
			else if (n == tabbedPane.indexOfTab("Text Editor")) {
				n = tabbedPane.indexOfTab("Lexical Functional Structure");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, LFGScrollPane);
			}
		}
	}

	/**
	 * This listener is fired when nextTabItem is clicked
	 * 
	 * @author shuoyang
	 */
	class NextTabListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			int n = tabbedPane.getSelectedIndex();
			if (n == tabbedPane.indexOfTab("Constituent Tree")) {
				n = tabbedPane.indexOfTab("Dependency Tree");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, depScrollPane);
			}
			else if (n == tabbedPane.indexOfTab("Dependency Tree")) {
				n = tabbedPane.indexOfTab("Deep Dependency Graph");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, deepdepScrollPane);
			}
			else if (n == tabbedPane.indexOfTab("Deep Dependency Graph")) {
				n = tabbedPane.indexOfTab("Combinatorial Categorial Tree");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, CCGScrollPane);
			}
			else if (n == tabbedPane
					.indexOfTab("Combinatorial Categorial Tree")) {
				n = tabbedPane.indexOfTab("Lexical Functional Structure");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, LFGScrollPane);
			}
			else if (n == tabbedPane.indexOfTab("Lexical Functional Structure")) {
				n = tabbedPane.indexOfTab("Text Editor");
				tabbedPane.setSelectedIndex(n);
				tabbedPane.setComponentAt(n, TextScrollPane);
			}
		}
	}

	class SkewLineListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			constcomponent.skewedLines = !constcomponent.skewedLines;
			constcomponent.init();
		}
	}

	class ColorLineListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			LFGcomponent.isColor = !LFGcomponent.isColor;
			LFGcomponent.init();
		}
	}

	class ShowLineListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			LFGcomponent.isShown = !LFGcomponent.isShown;
			LFGcomponent.init();
		}
	}

	/**
	 * This listener is fired when saveTextItem is clicked
	 * It writes content in the editor to the linked destination and automatically reload from it
	 * 
	 * @author shuoyang
	 */
	class SaveTextListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			try {
				String File = Textcomponent.getText();
				if (filename != null && !filename.trim().isEmpty()) {
					BufferedWriter in = new BufferedWriter(new FileWriter(
							filename));
					in.write(File);
					in.close();
					importFromFile(filename);
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * This listener almost the same as the previous one
	 * The only difference is that a file dialog pops up to let you choose the destination
	 * 
	 * @author shuoyang
	 */
	class SaveTextAsListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			if (filename != null) {
				JFileChooser chooser = new JFileChooser();
				chooser.setCurrentDirectory(new File("."));
				int result = chooser.showSaveDialog(TabbedPaneFrame.this);
				filename = chooser.getSelectedFile().getAbsolutePath();
				if(result == 0) {
					try {
						String File = Textcomponent.getText();
						if (filename != null && !filename.trim().isEmpty()) {
							BufferedWriter in = new BufferedWriter(new FileWriter(
									filename));
							in.write(File);
							in.close();
							importFromFile(filename);
						}
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}
	}

	/**
	 * the status bar tells the user which sentence they are processing
	 * it also shows warnings when something abnormal happens
	 * 
	 * @author shuoyang
	 */
	class StatusBar extends JLabel {
		public StatusBar() {
			super();
			super.setPreferredSize(new Dimension(500, 32));
			setMessage("Default treebank displaying, load a corpus to begin.");
		}

		public void setMessage(String message) {
			setText(message + " ");
			setHorizontalAlignment(TRAILING);
			setVerticalTextPosition(CENTER);
		}
	}

	/**
	 * The ExportOptionPanel enables users to export a certain view of the current sentence to a EPS graph
	 * It pops up a dialog first to let you choose which view and what location to export
	 * After that it exports the graph using an EPSGraphics object
	 * 
	 * @author shuoyang
	 */
	class ExportOptionPanel extends JPanel {
		/**
		 * the pop-up dialog
		 */
		JDialog dialog;
		
		/**
		 * the file selected
		 */
		String FileLoc = new String();
		
		/**
		 * the choice of view
		 */
		ButtonPanel tabChoicePanel = new ButtonPanel("Tab Choice",
				"Constituent", "Dependency", "Deep Dependency", "CCG", "LFG");
		
		/**
		 * the panel that enables you to select export location
		 */
		FileLocPanel locPanel = new FileLocPanel();
		
		/**
		 * the "DO-IT" button
		 */
		JButton ExportButton = new JButton("Export");

		/**
		 * Sets the layout of the pop-up dialog
		 */
		public ExportOptionPanel() {
			ExportButton.addActionListener(new ExportActionListener());

			setLayout(new BorderLayout());
			Toolkit kit = Toolkit.getDefaultToolkit();
			Dimension screenSize = kit.getScreenSize();
			int screenHeight = screenSize.height;
			int screenWidth = screenSize.width;
			setSize(screenWidth / 3, screenHeight / 4);

			setBorder(new EmptyBorder(10, 10, 10, 10));
			Dimension origSize = tabChoicePanel.getPreferredSize();
			tabChoicePanel.setPreferredSize(new Dimension(screenWidth / 3,
					origSize.height));
			locPanel.setBorder(new EmptyBorder(5, 0, 5, 0));

			add(tabChoicePanel, "North");
			add(locPanel, "Center");

			JPanel ExportButtonWrapperPanel = new JPanel();
			ExportButtonWrapperPanel.add(ExportButton);
			ExportButtonWrapperPanel.setPreferredSize(new Dimension(100, 40));
			add(ExportButtonWrapperPanel, "South");
		}

		/**
		 * To show the export option dialog, call this function
		 * 
		 * @param parent
		 * @param title
		 */
		public void showDialog(Component parent, String title) {
			Frame owner = null;
			if (parent instanceof Frame) {
				owner = (Frame) parent;
			}
			else {
				owner = (Frame) SwingUtilities.getAncestorOfClass(Frame.class,
						parent);
			}

			if (dialog == null || dialog.getOwner() != owner) {
				dialog = new JDialog(owner, true);
				dialog.add(this);
				dialog.getRootPane().setDefaultButton(ExportButton);
				dialog.pack();
			}

			dialog.setTitle(title);
			dialog.setVisible(true);
		}

		/**
		 * This listener is attached to "ExportButton" and carries out the export operation
		 * 
		 * @author shuoyang
		 */
		class ExportActionListener implements ActionListener {
			public void actionPerformed(ActionEvent event) {
				FileLoc = locPanel.getLoc();
				if (FileLoc != null && !FileLoc.isEmpty()) {
					String sel = tabChoicePanel.getSelection();
					try {
						if (sel == "Constituent") {
							Dimension dim = constcomponent.getDimension();
							EpsGraphics g = new EpsGraphics("Title",
									new FileOutputStream(FileLoc), 0, 0,
									(int) dim.getWidth() + 2,
									(int) dim.getHeight(), ColorMode.COLOR_RGB);
							constcomponent.paint(g);
							g.flush();
							g.close();
						}
						else if (sel == "Dependency") {
							Dimension dim = depcomponent.getDimension();
							EpsGraphics g = new EpsGraphics("Title",
									new FileOutputStream(FileLoc), 0, 0,
									(int) dim.getWidth() + 2,
									(int) dim.getHeight(), ColorMode.COLOR_RGB);
							depcomponent.paint(g);
							g.flush();
							g.close();
						}
						else if (sel == "Deep Dependency") {
							Dimension dim = deepdepcomponent.getDimension();
							EpsGraphics g = new EpsGraphics("Title",
									new FileOutputStream(FileLoc), 0, 0,
									(int) dim.getWidth() + 2,
									(int) dim.getHeight(), ColorMode.COLOR_RGB);
							deepdepcomponent.paint(g);
							g.flush();
							g.close();
						}
						else if (sel == "LFG") {
							Dimension dim = LFGcomponent.getDimension();
							EpsGraphics g = new EpsGraphics("Title",
									new FileOutputStream(FileLoc), 0, 0,
									(int) dim.getWidth() + 2,
									(int) dim.getHeight(), ColorMode.COLOR_RGB);
							LFGcomponent.paint(g);
							g.flush();
							g.close();
						}
						else if (sel == "CCG") {
							Dimension dim = CCGcomponent.getDimension();
							EpsGraphics g = new EpsGraphics("Title",
									new FileOutputStream(FileLoc), 0, 0,
									(int) dim.getWidth() + 2,
									(int) dim.getHeight(), ColorMode.COLOR_RGB);
							CCGcomponent.paint(g);
							g.flush();
							g.close();
						}
						JOptionPane.showMessageDialog((Component)event.getSource(),
								"Export successful!");
					} catch (FileNotFoundException e) {
						JOptionPane.showMessageDialog((Component)event.getSource(),
								"Invalid file directory, or you are not authorized to access.");
					} catch (IOException e) {
						JOptionPane.showMessageDialog((Component)event.getSource(),
								"An I/O exception occurred.");
					}
				}
			}
		}
	}

	/**
	 * This listener register edits to undoManager to enable undo/redo operation
	 * Note that only insert/delete/text change operations are registered
	 * style changes (coloring) are filtered
	 * 
	 * @author shuoyang
	 */
	class XMLUndoableEditListener implements UndoableEditListener {
		public void undoableEditHappened(UndoableEditEvent event) {
			if (!event
					.getEdit()
					.getPresentationName()
					.equals(UIManager
							.getString("AbstractDocument.styleChangeText"))) {
				UndoableEdit edit = event.getEdit();
				undoManager.addEdit(edit);
			}
		}
	}
}
package LinguaView.syntax;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.GeneralPath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;

import LinguaView.TreePanel;
/**
 * LFGStructPanel loads in one specified LFG structure at a time.
 * Then it passes it to FeatureLayoutPanel and ConstTreePanel, respectively, to arrange their layout.
 * After individual layout of c-structure and f-structure is done,
 * LFGStructPanel adjust their y-loc to make it looks better, and adds correspondences between c-structures and f-structures.
 * 
 * @author shuoyang
 *
 */
@SuppressWarnings("serial")
public class LFGStructPanel extends TreePanel<Element> {
	/**
	 * fstruct arranges layout of f-structure
	 */
	private FeatureStructure fstruct = new FeatureStructure();
	/**
	 * cstruct arranges layout of c-structure
	 */
	private ConstStructure cstruct = new ConstStructure();
	/**
	 * cfGap is the margin (on x-axis) between c-structure and f-structure
	 */
	int cfGap = 50;
	/**
	 * the left and top margin of c-structure of f-structure before adjustment of LFGStructPanel
	 */
	int cLeftMarginOrig = cstruct.leftMargin;
	int fLeftMarginOrig = fstruct.leftMargin;
	int cTopMarginOrig = cstruct.topMargin;
	int fTopMarginOrig = fstruct.topMargin;
	/**
	 * a mapping from TSNodeLabel to the index of AVM, represents the correspondences
	 */
	Map<TSNodeLabel, Integer> CorrespondenceTable = new HashMap<TSNodeLabel, Integer>();
	/**
	 * indicates whether the correspondence lines should be rendered in black or magenta
	 */
	public boolean isColor = true;
	/**
	 * indicates whether the correspondence lines should be shown
	 */
	public boolean isShown = true;

	/**
	 * FeatureStructure is a wrapper class for using FeatureLayoutPanel in LFGStructPanel.
	 * Except for overridden init() method and a new SetXStartPos() method for adjustment,
	 * this class is the same as FeatureLayoutPanel
	 * 
	 * @author shuoyang
	 *
	 */
	class FeatureStructure extends FeatureLayoutPanel {

		public void init() {
			textTopMargin = 0;
			levelSizeFactor = 0.5;
			XLeftMargin = 15;
			XBoarderLineMargin = 20;
			CurlyBracketMargin = 10;
			XRightMargin = 15;
			RefLineHeight = 10;
			loadFont();
			loadSentence();
			setPreferredSize(area);
		}

		public void setXStartPos(int XStartPos) {
			fLeftMarginOrig = leftMargin;
			leftMargin += XStartPos;
			init();
		}
	}

	/**
	 * ConstStructure is a wrapper class for using ConstTreePanel in LFGStructPanel.
	 * Except for overridden init() method and a new SetXStartPos() method for adjustment,
	 * this class is the same as ConstTreePanel
	 * 
	 * @author shuoyang
	 *
	 */
	class ConstStructure extends ConstTreePanel {

		public void init() {
			loadFont();
			loadSentence();
			setPreferredSize(area);
		}

		public void setXStartPos(int XStartPos) {
			cLeftMarginOrig = leftMargin;
			leftMargin += XStartPos;
			init();
		}
	}
	
	public void loadFont() {
		font = new Font("SansSerif", Font.PLAIN, fontSize);
		metrics = getFontMetrics(font);
		fontDescendent = metrics.getDescent();
		fontHight = metrics.getHeight();
		levelSize = fontHight * levelSizeFactor;

		cstruct.fontSize = this.fontSize;
		cstruct.font = this.font;
		cstruct.metrics = this.metrics;
		cstruct.fontDescendent = this.fontDescendent;
		cstruct.fontHight = this.fontHight;
		cstruct.levelSize = this.levelSize;

		fstruct.fontSize = this.fontSize;
		fstruct.font = this.font;
		fstruct.metrics = this.metrics;
		fstruct.fontDescendent = this.fontDescendent;
		fstruct.fontHight = this.fontHight;
		fstruct.levelSize = this.levelSize;
	}

	public void init() {
		cstruct.leftMargin = cLeftMarginOrig;
		fstruct.leftMargin = fLeftMarginOrig;
		cstruct.topMargin = cTopMarginOrig;
		fstruct.topMargin = fTopMarginOrig;
		loadFont();
		loadSentence();
		setPreferredSize(area);
		setSize(area);
		revalidate();
		repaint();
	}

	/**
	 * render the LFG structure according to the layout arranged
	 */
	@SuppressWarnings("unused")
	public void render(Graphics2D g2) {
		g2.setFont(font);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke());
		
		// adjust the y-pos of c-structure or f-structure to get their middle line
		// on the same y level
		int cSpan = cstruct.getDimension().height - cstruct.topMargin;
		int fSpan = fstruct.getDimension().height - fstruct.topMargin;
		int cYMiddle = cstruct.topMargin + cSpan / 2;
		int fYMiddle = fstruct.topMargin + fSpan / 2;
		if (cYMiddle > fYMiddle) {
			int diff = cYMiddle - fYMiddle;
			fstruct.topMargin += diff;
		}
		else {
			int diff = fYMiddle - cYMiddle;
			cstruct.topMargin += diff;
		}
		cstruct.init();
		fstruct.init();
		
		// render the c-structure and f-structure
		cstruct.render(g2);
		fstruct.render(g2);

		// if the correspondences should be shown and are all valid,
		// draw the correspondence lines
		if (isShown && isAllRefValid()) {
			int RefLineFringe = cstruct.getDimension().height;
			int RefLineCount = 1;
			
			if(isColor) {
				g2.setColor(Color.MAGENTA);
			}
			
			for (int i = 0; i < cstruct.nodesCount; i++) {
				if (CorrespondenceTable.get(cstruct.nodesArray[i]) != null) {
					int fID = CorrespondenceTable.get(cstruct.nodesArray[i]);
					AttributeValueMatrix headNode = fstruct.treebank.get(0);
					AttributeValueMatrix targNode = headNode
							.getSpecifiedNode(fID);
					AttributeValueMatrix realTargNode = AttributeValueMatrix
							.getRealContent(targNode);
					int j = fstruct.indexTable.get(realTargNode);
					int Xs = cstruct.XMiddleArray[i]
							+ (cstruct.XMiddleArray[i] - cstruct.XLeftArray[i])
							+ 5;
					int Ys = cstruct.YArray[i] - fontHight / 2;
					int Xe = fstruct.XLeftArray[j];
					int Ye = (fstruct.YUpArray[j] + fstruct.YDownArray[j]) / 2;
					drawRefLine(Xs, Ys, Xe, Ye, g2);
					RefLineCount++;
				}
			}
			g2.setColor(Color.BLACK);
		}
	}

	public boolean isAllRefValid() {
		AttributeValueMatrix headNode = fstruct.treebank.get(0);
		for (int i = 0; i < cstruct.nodesCount; i++) {
			if (CorrespondenceTable.get(cstruct.nodesArray[i]) != null) {
				int fID = CorrespondenceTable.get(cstruct.nodesArray[i]);
				AttributeValueMatrix targNode = headNode
						.getSpecifiedNode(fID);
				if(targNode == null) {
					return false;
				}
			}
		}
		return true;
	}
	
	/**
	 * draw correspondence line with arrow from (x1, y1) to (x2, y2)
	 * 
	 * @param x1
	 * @param y1
	 * @param x2
	 * @param y2
	 * @param g2
	 */
	private void drawRefLine(int x1, int y1, int x2, int y2, Graphics2D g2) {
		GeneralPath shape = new GeneralPath();
		Point p1 = new Point(x1, y1);
		Point p2 = new Point((x1 + x2) / 2, (y1 + y2) / 2);
		Point p3 = new Point(x2, y2);
		shape.moveTo(p1.x, p1.y);
		shape.curveTo(p1.x, p1.y, (p1.x + p2.x) / 2, p1.y, p2.x, p2.y);
		shape.curveTo(p2.x, p2.y, (p2.x + p3.x) / 2, p3.y, p3.x, p3.y);
		shape.moveTo(p3.x, p3.y);
		shape.closePath();
		g2.draw(shape);

		int arrowSize = g2.getFont().getSize() / 5;
		int[] arrowX = new int[4];
		int[] arrowY = new int[4];
		arrowX[0] = x2 - arrowSize;
		arrowX[1] = arrowX[0] - arrowSize;
		arrowX[2] = x2;
		arrowX[3] = arrowX[0] - arrowSize;
		arrowY[0] = y2;
		arrowY[1] = y2 - arrowSize;
		arrowY[2] = y2;
		arrowY[3] = y2 + arrowSize;
		g2.fillPolygon(arrowX, arrowY, 4);
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		render(g2);
	}

	/**
	 * call the loadSentence() function of c-structure and f-structure to
	 * arrange the layout of LFG structure
	 * 
	 * note that when f-structure is arranged, its x-pos needs to be shifted
	 */
	public void loadSentence() {
		
		Element headNode = treebank.get(sentenceNumber);
		NodeList children = headNode.getChildNodes();
		String constStr = new String();
		Element cstructNode = null, fstructNode = null;
		
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if ((child instanceof Element)
					&& (child.getNodeName() == "cstruct")) {
				cstructNode = (Element) child;
			}
			else if ((child instanceof Element)
					&& (child.getNodeName() == "fstruct")) {
				fstructNode = (Element) child;
			}
		}
		
		// load the c-structure and arrange its layout
		if (cstructNode != null) {
			children = cstructNode.getChildNodes();
		}
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child instanceof Text) {
				constStr = ((Text) child).getTextContent();
			}
		}
		if (!constStr.isEmpty()) {
			ArrayList<TSNodeLabel> cStructbank = new ArrayList<TSNodeLabel>();
			TSNodeLabel cStructHead = new TSNodeLabel(constStr);
			if(cStructHead.label != null) {
				buildCorrepondenceTable(cStructHead);
			}
			cStructbank.add(cStructHead);
			cstruct.loadTreebank(cStructbank);
			cstruct.setXStartPos(0);
		}

		// load the f-structure and arrange its layout
		Dimension cArea = cstruct.getDimension();
		int fstructStartPos = 0;
		if(cArea.width != 0) {
			int cstructEndPos = cArea.width;
			fstructStartPos = cstructEndPos + cfGap;
		}
		if (fstructNode != null) {
			ArrayList<AttributeValueMatrix> fStructbank = new ArrayList<AttributeValueMatrix>();
			AttributeValueMatrix fStructHead = AttributeValueMatrix
					.parseXMLSentence(fstructNode);
			fStructbank.add(fStructHead);
			fstruct.loadTreebank(fStructbank);
			fstruct.setXStartPos(fstructStartPos);
		}

		// set the layout size
		Dimension fArea = fstruct.getDimension();
		area.height = cArea.height > fArea.height ? cArea.height : fArea.height;
		area.width = cArea.width > fArea.width ? cArea.width : fArea.width;
	}

	/**
	 * parse the correspondence representation like "NP#1" and
	 * build correspondence table
	 * 
	 * @param headNode
	 */
	private void buildCorrepondenceTable(TSNodeLabel headNode) {
		ArrayList<TSNodeLabel> L = headNode.collectAllNodes();
		for (TSNodeLabel n : L) {
			if (n.label().trim().matches("^[A-Za-z]*#[0-9]*$")) {
				String[] splittedParts = n.label.trim().split("#");
				String labelOrig = splittedParts[0];
				String idStr = splittedParts[1];
				n.label = labelOrig;
				CorrespondenceTable.put(n, Integer.parseInt(idStr));
			}
		}
	}
}
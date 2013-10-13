package LinguaView.syntax;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

import LinguaView.TreePanel;
/**
 * CCGTreePanel loads one specified CCG tree at a time and arranges its layout in 
 * an recursive manner.
 * Note that CCGTreePanel doesn't load tree from strings. This work is left to CCGNode.
 * 
 * @author shuoyang
 *
 */
@SuppressWarnings("serial")
public class CCGTreePanel extends TreePanel<CCGNode> {
	/**
	 * sentenceLength stores the number of tokens in the raw sentence
	 * nodesCount stores the number of all the nodes, both terminal and internal
	 * maxDepth is the maximum depth of the parse tree
	 */
	int sentenceLength, nodesCount, maxDepth;
	/**
	 * For the convenience of process, we assign an integer id to each CCGNode
	 * indexTable maps each CCGNode to this integer id
	 */
	IdentityHashMap<CCGNode, Integer> indexTable;
	/**
	 * nodesArray stores all the nodes in the CCG tree
	 */
	CCGNode[] nodesArray;
	/**
	 * lexicalsArray stores all the terminal nodes in the CCG tree
	 */
	CCGTerminalNode[] lexicalsArray;
	/**
	 * labelArray stores the CCG category in string form for each corresponding CCGNode
	 */
	String[] labelArray;
	/**
	 * This is a group of layout information.
	 * XLeftArray is the x-loc of the left edge of CCG nodes
	 * YArray is the y-loc of the lower edge of CCG nodes
	 * XMiddleArray is the x-loc of the mid of CCG nodes
	 * XLabelLeftArray is the x-loc of the left edge of CCG node labels
	 * nodeLengthsArray is the length on x-axis of each CCG node
	 * 
	 * Since the YArray doesn't always depends on the depth of a node
	 * A "graphical height", the depth of a node on the graphical representation is stored.
	 * That's what graphicalHeightArray does.
	 * 
	 * Please note that, to get the layout information of a CCG node n,
	 * one should first call indexTable.get(n) to get its index in these arrays.
	 */
	int[] XLeftArray, YArray, XMiddleArray, XLabelLeftArray,
			graphicalHeightArray, nodeLengthsArray;
	
	public void init() {
		textTopMargin = 0;
		levelSizeFactor = 1.5;
		loadFont();
		loadSentence();
		setPreferredSize(area);
		setSize(area);
		revalidate();
		repaint();
	}

	/**
	 * arranges the layout in an recursive manner.
	 */
	public void loadSentence() {
		CCGNode headNode = treebank.get(sentenceNumber);
		// deals with the empty node
		if(headNode instanceof CCGTerminalNode &&
			((CCGTerminalNode)headNode).categoryToString().equals("_")) {
			sentenceLength = 0;
			nodesCount = 0;
			maxDepth = 0;
			indexTable = null;
			nodesArray = null;
			lexicalsArray = null;
			labelArray = null;
			XLeftArray = null;
			YArray = null;
			XMiddleArray = null;
			XLabelLeftArray = null;
			graphicalHeightArray = null;
			nodeLengthsArray = null;
			area.height = 0;
			area.width = 0;
			return;
		}
		
		// initialization
		indexTable = new IdentityHashMap<CCGNode, Integer>();
		nodesCount = headNode.countAllNodes();
		sentenceLength = headNode.countTerminalNodes();
		List<CCGNode> nodesList = headNode.collectAllNodes();
		nodesArray = new CCGNode[nodesList.size()];
		nodesList.toArray(nodesArray);
		List<CCGTerminalNode> lexicalsList = headNode.collectTerminalNodes();
		lexicalsArray = new CCGTerminalNode[lexicalsList.size()];
		lexicalsList.toArray(lexicalsArray);
		maxDepth = headNode.maxDepth();
		for (int i = 0; i < nodesCount; i++) {
			CCGNode n = nodesArray[i];
			indexTable.put(n, i);
		}

		labelArray = new String[nodesCount];
		XLeftArray = new int[nodesCount];
		XMiddleArray = new int[nodesCount];
		XLabelLeftArray = new int[nodesCount];
		graphicalHeightArray = new int[nodesCount];
		nodeLengthsArray = new int[nodesCount];
		Arrays.fill(XLeftArray, -1);
		YArray = new int[nodesCount];
		int treeWidth = 0, treeHeight;
		
		
		int previousWordLength = 0;
		int previousXLeft = leftMargin;
		// first set a initial x-position for all terminal nodes
		for (int j = 0; j < sentenceLength; j++) {
			CCGTerminalNode n = lexicalsArray[j];
			int i = indexTable.get(n);
			labelArray[i] = n.categoryWithNoFeature();
			int labelLength = metrics.stringWidth(labelArray[i]);
			int wordLength = metrics.stringWidth(n.word());
			nodeLengthsArray[i] = labelLength > wordLength ? labelLength
					: wordLength;
			XMiddleArray[i] = previousXLeft + previousWordLength + wordSpace
					+ nodeLengthsArray[i] / 2;
			XLeftArray[i] = XMiddleArray[i] - nodeLengthsArray[i] / 2;
			XLabelLeftArray[i] = XMiddleArray[i] - labelLength / 2;
			graphicalHeightArray[i] = maxDepth;
			previousWordLength = nodeLengthsArray[i];
			previousXLeft = XLeftArray[i];
		}
		// recursively update the position of internal nodes (both x and y)
		// Upon some circumstances, a too long CCG node might be encountered, 
		// at that time, the positions of terminal nodes are also adjusted
		if (headNode instanceof CCGInternalNode) {
			updateValues((CCGInternalNode) headNode);
		}
		// update the y-position of terminal nodes
		for (int j = 0; j < sentenceLength; j++) {
			CCGTerminalNode n = lexicalsArray[j];
			int i = indexTable.get(n);
			YArray[i] = (int) (topMargin + fontHight + levelSize);
		}
		// set layout size
		int RightMostNodeIndex = getMaximalIndex(XLeftArray);
		treeWidth = XLeftArray[RightMostNodeIndex]
				+ nodeLengthsArray[RightMostNodeIndex] + wordSpace;
		treeWidth += rightMargin;
		treeHeight = (int) (topMargin + fontHight + (maxDepth + 1) * levelSize + bottomMargin);
		area.width = treeWidth;
		area.height = treeHeight;
	}

	public void render(Graphics2D g2) {
		g2.setFont(font);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke());

		CCGNode headNode = treebank.get(sentenceNumber);
		drawNodes(headNode, g2);
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		render(g2);
	}

	/**
	 * draw a single CCG node, with layout information set
	 * @param n
	 * @param g2
	 */
	private void drawNodes(CCGNode n, Graphics2D g2) {
		int i;
		if(indexTable != null) {
			i = indexTable.get(n);
		}
		else {
			return;
		}
		// for internal nodes
		if (n instanceof CCGInternalNode) {
			CCGInternalNode ni = (CCGInternalNode) n;
			String catDisplay = ni.categoryWithNoFeature();
			catDisplay = catDisplay.replace('{', '(');
			catDisplay = catDisplay.replace('}', ')');
			g2.drawString(catDisplay, XLabelLeftArray[i], YArray[i]);
			int lineY = (int) (YArray[i] - (levelSize + fontSize) / 2);
			// if node n has only one child, see of it's type raising
			if (ni.prole() == 1) {
				int i_ = indexTable.get((ni.daughters())[0]);
				if (CategoryObject.typeRaising(ni.category(), n.category(),
						null)) {
					if (n.category().isForward()) {
						drawTypeRaisingArrow(XLeftArray[i_], XLeftArray[i_]
								+ nodeLengthsArray[i_], lineY, true, g2);
					}
					else {
						drawTypeRaisingArrow(XLeftArray[i_], XLeftArray[i_]
								+ nodeLengthsArray[i_], lineY, false, g2);
					}
				}
				else {
					g2.drawLine(XLeftArray[i_], lineY, XLeftArray[i_]
							+ nodeLengthsArray[i_], lineY);
				}
				drawNodes(nodesArray[i_], g2);
			}
			// if node n has two children, examine each possibility
			else if (ni.prole() == 2) {
				CCGNode[] children = ((CCGInternalNode) n).daughters();
				int[] XChildrenLeft = new int[children.length];
				int j = 0;
				for (CCGNode c : children) {
					XChildrenLeft[j] = XLeftArray[indexTable.get(c)];
					j++;
				}
				int x1, x2;
				int LeftMostChild = indexTable
						.get(children[getMinimalIndex(XChildrenLeft)]);
				int RightMostChild = indexTable
						.get(children[getMaximalIndex(XChildrenLeft)]);
				x1 = XLeftArray[LeftMostChild];
				x2 = XLeftArray[RightMostChild]
						+ nodeLengthsArray[RightMostChild];
				if (CategoryObject.forwardApply(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawArrow(x1, x2, lineY, true, g2);
				}
				else if (CategoryObject.backwardApply(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawArrow(x1, x2, lineY, false, g2);
				}
				else if (CategoryObject.simpleForwardCompose(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawSimpleCompositionArrow(x1, x2, lineY, true, g2);
				}
				else if (CategoryObject.simpleBackwardCompose(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawSimpleCompositionArrow(x1, x2, lineY, false, g2);
				}
				else if (CategoryObject.crossForwardCompose(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawSimpleCompositionArrow(x1, x2, lineY, true, g2);
				}
				else if (CategoryObject.crossBackwardCompose(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawSimpleCompositionArrow(x1, x2, lineY, false, g2);
				}
				else if (CategoryObject.generalizedForwardCompose(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawGeneralizedCompositionArrow(x1, x2, lineY, true, false,
							g2);
				}
				else if (CategoryObject.generalizedBackwardCompose(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawGeneralizedCompositionArrow(x1, x2, lineY, false,
							false, g2);
				}
				else if (CategoryObject.generalizedForwardCrossCompose(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawGeneralizedCompositionArrow(x1, x2, lineY, true, true,
							g2);
				}
				else if (CategoryObject.generalizedBackwardCrossCompose(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawGeneralizedCompositionArrow(x1, x2, lineY, true, true,
							g2);
				}
				else if (CategoryObject.forwardSubstitute(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawSubstitutionArrow(x1, x2, lineY, true, g2);
				}
				else if (CategoryObject.backwardSubstitute(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawSubstitutionArrow(x1, x2, lineY, false, g2);
				}
				else if (CategoryObject.coordination(
						nodesArray[LeftMostChild].category(),
						nodesArray[RightMostChild].category(), n.category(),
						null)) {
					drawClusterCoordinationArrow(x1, x2, lineY, g2);
				}
				for (CCGNode c : children) {
					drawNodes(c, g2);
				}
			}
		}
		// for terminal nodes
		else {
			CCGTerminalNode ni = (CCGTerminalNode) n;
			String catDisplay = ni.categoryWithNoFeature();
			catDisplay = catDisplay.replace('{', '(');
			catDisplay = catDisplay.replace('}', ')');
			g2.drawString(catDisplay, XLabelLeftArray[i], YArray[i]);
			int lineY = (int) (YArray[i] - (levelSize + fontHight) / 2);
			g2.drawLine(XLeftArray[i], lineY, XLeftArray[i]
					+ nodeLengthsArray[i], lineY);
			int wordY = (int) (YArray[i] - levelSize);
			g2.drawString(ni.word(),
					XMiddleArray[i] - metrics.stringWidth(ni.word()) / 2, wordY);
		}
	}

	/**
	 * draw an simple derivation arrow between x1 and x2
	 * set dir = true for forward arrow ------>
	 * set dir = false for backward arrow ------<
	 * 
	 * @param x1
	 * @param x2
	 * @param y
	 * @param dir
	 * @param g2
	 */
	private void drawArrow(int x1, int x2, int y, boolean dir, Graphics2D g2) {
		Font fontOrig = g2.getFont();
		int sizeOrig = fontOrig.getSize();
		Font fontTag = new Font(fontOrig.getName(), Font.BOLD,
				(int) (sizeOrig * 0.8));
		metrics = getFontMetrics(fontTag);
		int x2p = x2 - metrics.stringWidth(">");
		g2.setFont(fontTag);
		g2.drawLine(x1, y, x2p, y);
		if (dir) {
			g2.drawString(">", x2p, (int) (y + metrics.getHeight() * 0.3));
		}
		else {
			g2.drawString("<", x2p, (int) (y + metrics.getHeight() * 0.3));
		}
		metrics = getFontMetrics(fontOrig);
		g2.setFont(fontOrig);
	}

	/**
	 * draw a type raising arrow between x1 and x2
	 * set dir = true for forward arrow ------>T
	 * set dir = false for backward arrow ------<T 
	 * 
	 * @param x1
	 * @param x2
	 * @param y
	 * @param dir
	 * @param g2
	 */
	private void drawTypeRaisingArrow(int x1, int x2, int y, boolean dir,
			Graphics2D g2) {
		Font fontOrig = g2.getFont();
		int sizeOrig = fontOrig.getSize();
		Font fontTag = new Font(fontOrig.getName(), Font.BOLD,
				(int) (sizeOrig * 0.8));
		metrics = getFontMetrics(fontTag);
		int x2p = x2 - metrics.stringWidth(">T");
		g2.setFont(fontTag);
		g2.drawLine(x1, y, x2p, y);
		if (dir) {
			g2.drawString(">T", x2p, (int) (y + metrics.getHeight() * 0.3));
		}
		else {
			g2.drawString("<T", x2p, (int) (y + metrics.getHeight() * 0.3));
		}
		metrics = getFontMetrics(fontOrig);
		g2.setFont(fontOrig);
	}

	/**
	 * draw a simple composition arrow between x1 and x2
	 * set dir = true for forward arrow ------>B
	 * set dir = false for backward arrow ------<B 
	 * 
	 * @param x1
	 * @param x2
	 * @param y
	 * @param dir
	 * @param g2
	 */
	private void drawSimpleCompositionArrow(int x1, int x2, int y, boolean dir,
			Graphics2D g2) {
		Font fontOrig = g2.getFont();
		int sizeOrig = fontOrig.getSize();
		Font fontTag = new Font(fontOrig.getName(), Font.BOLD,
				(int) (sizeOrig * 0.8));
		metrics = getFontMetrics(fontTag);
		int x2p = x2 - metrics.stringWidth(">B");
		g2.setFont(fontTag);
		g2.drawLine(x1, y, x2p, y);
		if (dir) {
			g2.drawString(">B", x2p, (int) (y + metrics.getHeight() * 0.3));
		}
		else {
			g2.drawString("<B", x2p, (int) (y + metrics.getHeight() * 0.3));
		}
		metrics = getFontMetrics(fontOrig);
		g2.setFont(fontOrig);
	}

	/**
	 * draw a generalized composition arrow between x1 and x2
	 * set dir = true for forward arrow ------>B
	 * set dir = false for backward arrow ------<B
	 * set crossing = true for a crossing arrow ------>Bx
	 * 
	 * @param x1
	 * @param x2
	 * @param y
	 * @param dir
	 * @param crossing
	 * @param g2
	 */
	private void drawGeneralizedCompositionArrow(int x1, int x2, int y,
			boolean dir, boolean crossing, Graphics2D g2) {
		Font fontOrig = g2.getFont();
		int sizeOrig = fontOrig.getSize();
		Font fontTag = new Font(fontOrig.getName(), Font.BOLD,
				(int) (sizeOrig * 0.8));
		Font fontSmallTag = new Font(fontOrig.getName(), Font.PLAIN,
				(int) (sizeOrig * 0.4));
		metrics = getFontMetrics(fontSmallTag);
		int SmallFontHeight = metrics.getHeight();
		int x2p1 = 0;
		int x2p2 = 0;
		int x2p3 = 0;
		if (crossing) {
			x2p1 = x2 - metrics.stringWidth(" x");
			x2p2 = x2p1 - metrics.stringWidth("+");
		}
		else {
			x2p2 = x2 - metrics.stringWidth("+");
		}
		metrics = getFontMetrics(fontTag);
		int FontHeight = metrics.getHeight();
		x2p3 = x2p2 - metrics.stringWidth(">B");
		g2.setFont(fontTag);
		g2.drawLine(x1, y, x2p3, y);
		if (dir) {
			g2.drawString(">B", x2p3, y + FontHeight / 2);
		}
		else {
			g2.drawString("<B", x2p3, y + FontHeight / 2);
		}
		g2.drawString("+", x2p2, y - FontHeight / 2 + SmallFontHeight);
		if (crossing) {
			g2.drawString(" x", x2p1, y);
		}
		metrics = getFontMetrics(fontOrig);
		g2.setFont(fontOrig);
	}

	/**
	 * draw a substitution arrow between x1 and x2
	 * set dir = true for forward arrow ------>S
	 * set dir = false for backward arrow ------<S 
	 * 
	 * @param x1
	 * @param x2
	 * @param y
	 * @param dir
	 * @param g2
	 */
	private void drawSubstitutionArrow(int x1, int x2, int y, boolean dir,
			Graphics2D g2) {
		Font fontOrig = g2.getFont();
		int sizeOrig = fontOrig.getSize();
		Font fontTag = new Font(fontOrig.getName(), Font.BOLD,
				(int) (sizeOrig * 0.8));
		metrics = getFontMetrics(fontTag);
		int x2p = x2 - metrics.stringWidth(">S");
		g2.setFont(fontTag);
		g2.drawLine(x1, y, x2p, y);
		if (dir) {
			g2.drawString(">S", x2p, (int) (y + metrics.getHeight() * 0.3));
		}
		else {
			g2.drawString("<S", x2p, (int) (y + metrics.getHeight() * 0.3));
		}
		metrics = getFontMetrics(fontOrig);
		g2.setFont(fontOrig);
	}

	/**
	 * draw a cluster coordination arrow arrow between x1 and x2
	 * this arrow has no directions ------phi
	 * 
	 * @param x1
	 * @param x2
	 * @param y
	 * @param dir
	 * @param g2
	 */
	private void drawClusterCoordinationArrow(int x1, int x2, int y,
			Graphics2D g2) {
		Font fontOrig = g2.getFont();
		int sizeOrig = fontOrig.getSize();
		Font fontTag = new Font(fontOrig.getName(), Font.BOLD,
				(int) (sizeOrig * 0.8));
		metrics = getFontMetrics(fontTag);
		int x2p = x2 - metrics.stringWidth(">\u03A6");
		g2.setFont(fontTag);
		g2.drawLine(x1, y, x2p, y);
		g2.drawString(">\u03A6", x2p, (int) (y + metrics.getHeight() * 0.3));
		metrics = getFontMetrics(fontOrig);
		g2.setFont(fontOrig);
	}

	/**
	 * recursively update the location of internal nodes
	 * @param n
	 */
	public void updateValues(CCGInternalNode n) {
		CCGNode[] children = n.daughters();
		// the x-loc for the left edge of each child node
		int[] XChildrenLeft = new int[children.length];
		// the x-loc for the right edge of each child node
		int[] XChildrenRight = new int[children.length];
		// the height of each children
		int[] ChildrenHeight = new int[children.length];
		int i = 0;
		// The trick is, for each node n, first set the location of the children, then itself.
		// set the location of children
		for (CCGNode c : children) {
			if (c instanceof CCGTerminalNode) {
				XChildrenLeft[i] = XLeftArray[indexTable.get(c)];
				XChildrenRight[i] = XChildrenLeft[i]
						+ nodeLengthsArray[indexTable.get(c)];
				ChildrenHeight[i] = graphicalHeightArray[indexTable.get(c)];
				i++;
			}
			else {
				updateValues((CCGInternalNode) c);
				XChildrenLeft[i] = XLeftArray[indexTable.get(c)];
				XChildrenRight[i] = XChildrenLeft[i]
						+ nodeLengthsArray[indexTable.get(c)];
				ChildrenHeight[i] = graphicalHeightArray[indexTable.get(c)];
				i++;
			}
		}
		// set the location of itself
		i = indexTable.get(n);
		labelArray[i] = n.categoryWithNoFeature();
		int labelLength = metrics.stringWidth(labelArray[i]);
		// if this node n is too long and adjustment is needed
		if (labelLength > getMaximal(XChildrenRight)
				- getMinimal(XChildrenLeft)) {
			// we are going to move the rightmost node, its descendants,
			// and all the terminal nodes to the right of this node
			// the location of internal nodes to the right of this node will be set in the following calls
			int RightMostChildIndex = getMaximalIndex(XChildrenRight);
			CCGNode RightMostChild = children[RightMostChildIndex];
			int increment = labelLength - (getMaximal(XChildrenRight)
					- getMinimal(XChildrenLeft));
			// if the node has only one children, then the whole node needs to be moved
			if(children.length > 1) {
				int newXLeft = XLeftArray[indexTable.get(RightMostChild)]
					+ increment;
				int newXRight = XLeftArray[indexTable.get(RightMostChild)] + 
						nodeLengthsArray[indexTable.get(RightMostChild)] + increment;
				updateValues(RightMostChild, newXLeft, newXRight);
			}
			// Otherwise, it only needs to be stretched
			else {
				int newXLeft = XLeftArray[indexTable.get(RightMostChild)];
				int newXRight = XLeftArray[indexTable.get(RightMostChild)] + 
						nodeLengthsArray[indexTable.get(RightMostChild)] + increment;
				updateValues(RightMostChild, newXLeft, newXRight);
			}
			nodeLengthsArray[i] = labelLength;
			XLeftArray[i] = getMinimal(XChildrenLeft);
		}
		// if adjustment is not needed
		else {
			nodeLengthsArray[i] = getMaximal(XChildrenRight)
					- getMinimal(XChildrenLeft);
			XLeftArray[i] = getMinimal(XChildrenLeft);
		}
		
		graphicalHeightArray[i] = getMinimal(ChildrenHeight) - 1;
		XMiddleArray[i] = XLeftArray[i] + nodeLengthsArray[i] / 2;
		XLabelLeftArray[i] = XMiddleArray[i] - labelLength / 2;
		YArray[i] = (int) (topMargin + fontHight + (maxDepth
				- graphicalHeightArray[i] + 1)
				* levelSize);
	}

	/**
	 * adjust the x-position of certain nodes and its descendants in a recursive manner
	 * 
	 * @param n
	 * @param newXLeft
	 * @param newXRight
	 */
	public void updateValues(CCGNode n, int newXLeft, int newXRight) {
		int i = indexTable.get(n);
		int increment = newXRight - (XLeftArray[i] + nodeLengthsArray[i]);
		XLeftArray[i] = newXLeft;
		XMiddleArray[i] = (newXLeft + newXRight) / 2;
		nodeLengthsArray[i] = newXRight - newXLeft;
		XLabelLeftArray[i] = XMiddleArray[i] - metrics.stringWidth(labelArray[i]) / 2;
		int RightMostChildIndex = indexTable.get(n);
		CCGNode RightMostChild = n;
		if (n instanceof CCGInternalNode) {
			CCGNode[] children = ((CCGInternalNode) n).daughters();
			int[] XChildrenLeft = new int[children.length];
			i = 0;
			for (CCGNode c : children) {
				XChildrenLeft[i] = XLeftArray[indexTable.get(c)];
				i++;
			}
			RightMostChildIndex = getMaximalIndex(XChildrenLeft);
			RightMostChild = children[RightMostChildIndex];
			// if the node has only one children, then the whole node needs to be moved
			if(children.length > 1) {
				newXLeft = newXRight - nodeLengthsArray[indexTable.get(RightMostChild)];
				updateValues(RightMostChild, newXLeft, newXRight);
			}
			// Otherwise, it only needs to be stretched
			else {
				updateValues(RightMostChild, newXLeft, newXRight);
			}
		}
		int XRightMostChild = XLeftArray[RightMostChildIndex] 
				+ nodeLengthsArray[RightMostChildIndex];
		// adjust all the terminal nodes to the right of this node
		for(CCGNode c : nodesArray) {
			int j = indexTable.get(c);
			int XRight = XLeftArray[j] + nodeLengthsArray[j];
			if((n instanceof CCGTerminalNode) 
					&& (XRight > XRightMostChild)) {
				XLeftArray[j] += increment;
				XMiddleArray[j] += increment;
				XLabelLeftArray[j] += increment;
			}
		}
	}

	public int getMinimal(int[] Array) {
		int min = Array[0];
		for (int e : Array) {
			if (e < min) {
				min = e;
			}
		}
		return min;
	}

	public int getMinimalIndex(int[] Array) {
		int min = Array[0];
		int minIndex = 0;
		int i = 0;
		for (int e : Array) {
			if (e < min) {
				min = e;
				minIndex = i;
			}
			i++;
		}
		return minIndex;
	}

	public int getMaximal(int[] Array) {
		int min = Array[0];
		for (int e : Array) {
			if (e > min) {
				min = e;
			}
		}
		return min;
	}

	public int getMaximalIndex(int[] Array) {
		int max = Array[0];
		int maxIndex = 0;
		int i = 0;
		for (int e : Array) {
			if (e > max) {
				max = e;
				maxIndex = i;
			}
			i++;
		}
		return maxIndex;
	}

	public int sum(int[] Array) {
		int sum = 0;
		for (int e : Array) {
			sum += e;
		}
		return sum;
	}
}
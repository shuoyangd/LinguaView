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
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Set;
import LinguaView.TreePanel;
/**
 * FeatureLayoutPanel loads one specified f-structure at a time and arrange its layout.
 * Note that FeatureLayoutPanel does not load f-structure from XML inputs.
 * This work is left to AttributeValueMatrix.
 * 
 * @author shuoyang
 *
 */
@SuppressWarnings("serial")
public class FeatureLayoutPanel extends TreePanel<AttributeValueMatrix> {
	/**
	 * nodesCount stores the number of all the AVMs
	 */
	int nodesCount;
	/**
	 * For the convenience of process, we assign an integer id to each AVM
	 * indexTable maps each AVM to this integer id
	 */
	IdentityHashMap<AttributeValueMatrix, Integer> indexTable;
	/**
	 * nodesArray stores all the AVMs
	 */
	AttributeValueMatrix[] nodesArray;
	/**
	 * This is a group of layout information.
	 * XLeftArray is the x-loc for the left edge of AVMs.
	 * XRightArray is the x-loc for the right edge of AVMs.
	 * 
	 * The "BorderLine" is an invisible line between the attribute names and values.
	 * XBoarderLineArray is the x-loc for the borderline of AVM.
	 * 
	 * YUpArray is the y-loc for the top edge of AVMs.
	 * YDownArray is the y-loc for the lower edge of AVMs.
	 */
	int[] XLeftArray, XBoarderLineArray, XRightArray, YUpArray, YDownArray;
	/**
	 * This is another group of layout information.
	 * XLeftMargin is the margin between the left edge of attribute name and the left bracket
	 * XBoarderLineMargin is the margin between the right edge of the attribute name and the boarderline
	 * CurlyBracketMargin is the margin between the square bracket and the curly bracket used for sets
	 * XRightMargin is the margin between the right edge of attribute value and the right bracket
	 * RefLineMargin is the margin between the boarderline and the reference line tip
	 */
	int XLeftMargin, XBoarderLineMargin, CurlyBracketMargin, XRightMargin,
			RefLineMargin;
	/**
	 * ReflineHeight is the distance reference line detours before it head up/down to the
	 * referred AVM
	 */
	int RefLineHeight;
	/**
	 * YFeatureTable has all the y-pos of the corresponding attribute values
	 */
	HashMap<Value, Integer> YFeatureTable = new HashMap<Value, Integer>();
	/**
	 * RefList stores all the referenced AVMs
	 */
	ArrayList<AttributeValueMatrix> RefList = new ArrayList<AttributeValueMatrix>();

	public void init() {
		loadFont();
		loadSentence();
        setPreferredSize(area);
        revalidate();
        repaint();
	}
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		render(g2);
	}
	
	/**
	 * layout the f-structure according to the layout arranged
	 */
	public void render(Graphics2D g2) {
		g2.setFont(font);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke());
		// iterate through all the AVMs
		for (int i = 0; i < nodesCount; i++) {
			// first deal with the reference lines
			if (containedInRefList(nodesArray[i])) {
				AttributeValueMatrix realVal = AttributeValueMatrix
						.getRealContent((AttributeValueMatrix) nodesArray[i]);
				int j = indexTable.get((AttributeValueMatrix) nodesArray[i]);
				int k = indexTable.get(realVal);
				int x2 = XRightArray[j];
				int y2 = (YUpArray[j] + YDownArray[j]) / 2;
				int x1 = XRightArray[k];
				int y1 = (YUpArray[k] + YDownArray[k]) / 2;
				drawRefLine(x1, y1, x2, y2, RefLineHeight, g2);
			}
			else if (YUpArray[i] == 0) {
				continue;
			}
			else {
				AttributeValueMatrix avm = nodesArray[i];
				Set<String> Keys = avm.getAllAttributeNames();
				// render square brackets
				if (!containedInRefList(avm)) {
					drawLeftSquareBracket(XLeftArray[i], YUpArray[i],
							YDownArray[i], g2);
					drawRightSquareBracket(XRightArray[i], YUpArray[i],
							YDownArray[i], g2);
				}
				// render attributes
				for (String Key : Keys) {
					Value Val = avm.getAttributeValue(Key);
					int YPos = YFeatureTable.get(Val);
					// atomic attribute values
					if (Val instanceof Atomic) {
						YPos += fontHight;
						g2.drawString(Key, XLeftArray[i] + XLeftMargin, YPos);
						g2.drawString(((Atomic) Val).getValue(),
								XBoarderLineArray[i], YPos);
					}
					// semantic forms
					else if (Val instanceof SemanticForm) {
						YPos += fontHight;
						String sfStr = "\'"; 
						sfStr += ((SemanticForm) Val).getPred();
						sfStr += " <";
						for (String arg : ((SemanticForm) Val).getStringArgs()) {
							sfStr += arg;
							sfStr += ", ";
						}
						if (sfStr.lastIndexOf(", ") != -1) {
							sfStr = sfStr.substring(0, sfStr.lastIndexOf(", "));
						}
						sfStr += ">\'";
						g2.drawString(Key, XLeftArray[i] + XLeftMargin, YPos);
						g2.drawString(sfStr, XBoarderLineArray[i], YPos);
					}
					// AVMs
					else if (Val instanceof AttributeValueMatrix) {
						if (!containedInRefList(Val)) {
							Val = AttributeValueMatrix
									.getRealContent((AttributeValueMatrix) Val);
						}
						int j = indexTable.get((AttributeValueMatrix) Val);
						YPos = (YUpArray[j] + YDownArray[j]) / 2;
						g2.drawString(Key, XLeftArray[i] + XLeftMargin, YPos);
					}
					// set of AVMs
					else if (Val instanceof SetOfAttributeValueMatrix) {
						Set<AttributeValueMatrix> avmset = ((SetOfAttributeValueMatrix) Val)
								.getSet();
						int XRightPos = 0;
						int YDownPos = 0;
						for (AttributeValueMatrix e : avmset) {
							e = AttributeValueMatrix.getRealContent(e);
							int j = indexTable.get(e);
							if (YDownArray[j] > YDownPos) {
								YDownPos = YDownArray[j];
							}
							if (XRightArray[j] > XRightPos) {
								XRightPos = XRightArray[j];
							}
						}
						YDownPos += levelSize;
						g2.drawString(Key, XLeftArray[i] + XLeftMargin,
								(YPos + YDownPos) / 2);
						drawLeftCurlyBracket(XBoarderLineArray[i], YPos,
								(int)(YDownPos - levelSize), g2);
						drawRightCurlyBracket(XRightPos + CurlyBracketMargin,
								YPos, (int)(YDownPos - levelSize), g2);
					}
				}
			}
		}
	}

	private boolean containedInRefList(Object avm) {
		for (AttributeValueMatrix e : RefList) {
			if (avm == e) {
				return true;
			}
		}
		return false;
	}

	/**
	 * drawing left square bracket using default tail length
	 * 
	 * @param x
	 * @param y1
	 * @param y2
	 * @param g2
	 */
	private void drawLeftSquareBracket(int x, int y1, int y2, Graphics2D g2) {
		Font g2font = g2.getFont();
		int tailLength = g2font.getSize() / 2;
		drawSquareBracket(x, x + tailLength, y1, y2, g2);
	}

	/**
	 * drawing right square bracket using default tail length
	 * 
	 * @param x
	 * @param y1
	 * @param y2
	 * @param g2
	 */
	private void drawRightSquareBracket(int x, int y1, int y2, Graphics2D g2) {
		Font g2font = g2.getFont();
		int tailLength = g2font.getSize() / 2;
		drawSquareBracket(x, x - tailLength, y1, y2, g2);
	}

	/**
	 * drawing square bracket
	 * 
	 * for left bracket: 
	 * (x1, y1)  ___ (x2, y1)  
	 *    		|
	 *    		|
	 *    		|
	 *    		|
	 *    		|
	 * (x1, y2) |___ (x2, y2)
	 * 
	 * for right bracket:
	 * (x2, y1)  ___  (x1, y1)
	 * 				|
	 * 				|
	 * 				|
	 * 				|
	 * 				|
	 * (x2, y2)  ___| (x1, y2)
	 * 
	 * tail length = |x2 - x1|
	 * 
	 * @param x
	 * @param y1
	 * @param y2
	 * @param g2
	 */
	private void drawSquareBracket(int x1, int x2, int y1, int y2, Graphics2D g2) {
		g2.drawLine(x1, y1, x1, y2);
		g2.drawLine(x1, y1, x2, y1);
		g2.drawLine(x1, y2, x2, y2);
	}

	/**
	 * drawing left curly bracket
	 * 
	 * @param x
	 * @param y1
	 * @param y2
	 * @param g2
	 */
	private void drawLeftCurlyBracket(int x, int y1, int y2, Graphics2D g2) {
		Font g2font = g2.getFont();
		int d = (int) (g2font.getSize() * 0.618);
		g2.drawArc(x, y1 + d / 2, d, d, 90, 90);
		g2.drawArc(x - d, (y1 + y2) / 2 - d / 2, d, d, -90, 90);
		g2.drawArc(x - d, (y1 + y2) / 2 + d / 2, d, d, 0, 90);
		g2.drawArc(x, y2 - d / 2, d, d, 180, 90);
		g2.drawLine(x, y1 + d, x, (y1 + y2) / 2 + 1);
		g2.drawLine(x, y2, x, (y1 + y2) / 2 + d - 1);
	}

	/**
	 * drawing right curly bracket
	 * 
	 * @param x
	 * @param y1
	 * @param y2
	 * @param g2
	 */
	private void drawRightCurlyBracket(int x, int y1, int y2, Graphics2D g2) {
		Font g2font = g2.getFont();
		int d = (int) (g2font.getSize() * 0.618);
		g2.drawArc(x - d, y1 + d / 2, d, d, 0, 90);
		g2.drawArc(x, (y1 + y2) / 2 - d / 2, d, d, 180, 90);
		g2.drawArc(x, (y1 + y2) / 2 + d / 2, d, d, 90, 90);
		g2.drawArc(x - d, y2 - d / 2, d, d, 270, 90);
		g2.drawLine(x, y1 + d, x, (y1 + y2) / 2 + 1);
		g2.drawLine(x, y2, x, (y1 + y2) / 2 + d - 1);
	}

	/**
	 * drawing reference line from (x1, y1) to (x2, y2) with a detour distance of "height"
	 * 
	 * @param x
	 * @param y1
	 * @param y2
	 * @param g2
	 */
	private void drawRefLine(int x1, int y1, int x2, int y2, int height,
			Graphics2D g2) {
		int x = x1 > x2 ? x1 + height : x2 + height;
		GeneralPath shape = new GeneralPath();
		Point p1 = new Point(x1, y1);
		Point p2 = new Point(x, y1);
		Point p3 = new Point(x, y2);
		Point p4 = new Point(x2, y2);
		shape.moveTo(p1.x, p1.y);
		shape.curveTo(p2.x, p2.y, p2.x, p2.y, p2.x, p2.y + (p3.y - p2.y) / 2);
		shape.curveTo(p3.x, p3.y, p3.x, p3.y, p4.x, p4.y);
		shape.moveTo(p3.x, p3.y);
		shape.closePath();
		g2.draw(shape);
	}

	/**
	 * initialize and call recursizeUpdateX and recursiveUpdateY to arrange layout
	 */
	public void loadSentence() {
		AttributeValueMatrix headNode = treebank.get(sentenceNumber);
		RefList = headNode.getRefList();
		indexTable = new IdentityHashMap<AttributeValueMatrix, Integer>();
		nodesCount = headNode.countAllNodes();
		Collection<AttributeValueMatrix> nodesList = headNode.collectAllNodes();
		nodesArray = new AttributeValueMatrix[nodesList.size()];
		nodesList.toArray(nodesArray);
		for (int i = 0; i < nodesCount; i++) {
			AttributeValueMatrix n = nodesArray[i];
			indexTable.put(n, i);
		}

		XLeftArray = new int[nodesCount];
		XBoarderLineArray = new int[nodesCount];
		XRightArray = new int[nodesCount];
		YUpArray = new int[nodesCount];
		YDownArray = new int[nodesCount];

		Arrays.fill(XLeftArray, -1);
		int Width = recursiveUpdateX(headNode, leftMargin) + rightMargin;
		int Height = recursiveUpdateY(headNode, topMargin) + bottomMargin;
		area = new Dimension(Width, Height);
	}
	
	/**
	 * recursively update the x-pos of each AVMs
	 * the return value is the x-pos of the right edge
	 * 
	 * to understand this segment of code, please first read AttributeValueMatrix.java
	 * 
	 * @param avm
	 * @param lastX
	 * @return
	 */
	public int recursiveUpdateX(AttributeValueMatrix avm, int lastX) {
		int currentX = (int) lastX;
		AttributeValueMatrix OrigAVM = new AttributeValueMatrix();
		if (!avm.isContentOrPointer) {
			OrigAVM = avm;
			avm = AttributeValueMatrix.getRealContent(avm);
		}
		// if the AVM is contained in the reference list,
		// there is no need to assign space for it
		if (containedInRefList(OrigAVM)) {
			int i = indexTable.get(OrigAVM);
			XLeftArray[i] = currentX;
			XBoarderLineArray[i] = currentX;
			return XRightArray[i] = XBoarderLineArray[i] + RefLineMargin;
		}
		// assign space on x-axis for AVM
		else {
			int i = indexTable.get(avm);
			Set<String> Keys = avm.getAllAttributeNames();

			// assign space on x-axis for all the attribute names
			XLeftArray[i] = currentX;
			int MaxAttributeNameLength = 0;
			for (String Key : Keys) {
				if (metrics.stringWidth(Key) > MaxAttributeNameLength) {
					MaxAttributeNameLength = metrics.stringWidth(Key);
				}
			}
			XBoarderLineArray[i] = XLeftArray[i] + MaxAttributeNameLength
					+ XBoarderLineMargin;

			// assign space on x-axis for all the attribute value
			currentX = XBoarderLineArray[i];
			int MaxAttributeValuePos = currentX;
			int ValueEndPos = 0;
			for (String Key : Keys) {
				Value Val = avm.getAttributeValue(Key);
				if (Val instanceof Atomic) {
					ValueEndPos = currentX
							+ metrics.stringWidth(((Atomic) Val).getValue());
					if (ValueEndPos > MaxAttributeValuePos) {
						MaxAttributeValuePos = ValueEndPos;
					}
				}
				else if (Val instanceof SemanticForm) {
					String sfStr = "\'";
					sfStr += ((SemanticForm) Val).getPred();
					sfStr += " <";
					for (String arg : ((SemanticForm) Val).getStringArgs()) {
						sfStr += arg;
						sfStr += ", ";
					}
					if (sfStr.lastIndexOf(", ") != -1) {
						sfStr = sfStr.substring(0, sfStr.lastIndexOf(", "));
					}
					sfStr += ">\'";
					ValueEndPos = currentX + metrics.stringWidth(sfStr);
					if (ValueEndPos > MaxAttributeValuePos) {
						MaxAttributeValuePos = ValueEndPos;
					}
				}
				else if (Val instanceof AttributeValueMatrix) {
					ValueEndPos = recursiveUpdateX((AttributeValueMatrix) Val,
							currentX);
					if (ValueEndPos > MaxAttributeValuePos) {
						MaxAttributeValuePos = ValueEndPos;
					}
				}
				else if (Val instanceof SetOfAttributeValueMatrix) {
					for (AttributeValueMatrix e : ((SetOfAttributeValueMatrix) Val)
							.getSet()) {
						ValueEndPos = recursiveUpdateX(e, currentX
								+ CurlyBracketMargin)
								+ CurlyBracketMargin;
						if (ValueEndPos > MaxAttributeValuePos) {
							MaxAttributeValuePos = ValueEndPos;
						}
					}
				}
			}
			
			// return the rightmost edge to enable recursive update
			XRightArray[i] = MaxAttributeValuePos + XRightMargin;
			currentX = XRightArray[i];
			return currentX;
		}
	}

	/**
	 * recursively update the y-pos of each AVMs
	 * the return value is the y-pos of the lower edge
	 * 
	 * to understand this segment of code, please first read AttributeValueMatrix.java
	 * 
	 * @param avm
	 * @param lastY
	 * @return
	 */
	public int recursiveUpdateY(AttributeValueMatrix avm, int lastY) {
		int currentY = (int) (lastY + levelSize);
		if (!avm.isContentOrPointer) {
			avm = AttributeValueMatrix.getRealContent(avm);
		}
		int i = indexTable.get(avm);
		Set<String> Keys = avm.getAllAttributeNames();
		YUpArray[i] = currentY;
		for (String Key : Keys) {
			// build YFeatureTable
			Value Val = avm.getAttributeValue(Key);
			YFeatureTable.put(Val, currentY);
			
			// if the AVM is contained in the reference list,
			// there is no need to assign space for it
			if (containedInRefList(Val)) {
				int j = indexTable.get(Val);
				YUpArray[j] = (int) (currentY + levelSize);
				YDownArray[j] = YUpArray[j] + fontHight;
				currentY += (fontHight + levelSize);
			}
			else if ((Val instanceof Atomic) || (Val instanceof SemanticForm)) {
				currentY += (fontHight + 0.5 * levelSize);
			}
			else if (Val instanceof AttributeValueMatrix) {
				currentY = recursiveUpdateY((AttributeValueMatrix) Val,
						currentY);
			}
			else if (Val instanceof SetOfAttributeValueMatrix) {
				currentY += levelSize;
				for (AttributeValueMatrix e : ((SetOfAttributeValueMatrix) Val)
						.getSet()) {
					currentY = recursiveUpdateY(e, currentY);
				}
				currentY += levelSize;
			}
		}
		
		// return the lowest edge to enable recursive update
		YDownArray[i] = currentY;
		return currentY;
	}
}

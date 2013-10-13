package LinguaView.syntax;

import java.awt.*;
import java.awt.geom.GeneralPath;
import java.util.*;
import LinguaView.TreePanel;
/**
 * DepTreePanel loads one specified dependency tree/graph at a time and arrange its layout.
 * Note that DepTreePanel doesn't load tree from strings. This work is left to DepTree.
 * 
 * @author shuoyang
 *
 */
@SuppressWarnings("serial")
public class DepTreePanel extends TreePanel<DepTree> {
	/**
	 * tokenXLeftArray is the x-loc for the left edge of token
	 * tokenXMiddleArray is the x-loc for the mid of token
	 * tokenLengthsArray is the length on x-axis of each token
	 */
	int[] tokenXLeftArray, tokenXMiddleArray, tokenLengthsArray;
	/**
	 * edgeStartArray is the x-loc for the start point of each edge
	 * edgeEndArray is the x-loc for the end point of each edge
	 * edgeYArray is the y-loc for the upper span of each edge
	 * edgeLevel is the "level" of each edge
	 * edge with a higher level will correspondingly be drawn higher
	 */
	int[] edgeStartArray, edgeEndArray, edgeYArray, edgeLevel;
	/**
	 * There're probably several tips on one single token,
	 * a tip level is needed to decide which should tip should be put left and which should be put right
	 * 
	 * STipsLevel deals with all the tips corresponding to the starting points
	 * ETipsLevel deals with all the tips corresponding to the ending points
	 * tokTipsNumber deals with the tip number of each token
	 */
	int[] STipsLevel, ETipsLevel, tokTipsNumber;
	/**
	 * TE, stands for token-edge.
	 * TELeftPairs is a map from the index of a token to indexes of several edges which takes
	 * the token as the left tip.
	 * TERightPairs is a map from the index of a token to indexes of several edges which takes
	 * the token as the right tip.
	 */
	Map<Integer, ArrayList<Integer>> TELeftPairs = new HashMap<Integer, ArrayList<Integer>>();
	Map<Integer, ArrayList<Integer>> TERightPairs = new HashMap<Integer, ArrayList<Integer>>();
	/**
	 * The two charts maps from the index of a single edge to indexes of several edges.
	 * NestedChart stores all the edges that are nested with the given edge.
	 * CrossedChart stores all the edges that are crossed with the given edge.
	 */
	Map<Integer, ArrayList<Integer>> NestedChart = new HashMap<Integer, ArrayList<Integer>>();
	Map<Integer, ArrayList<Integer>> CrossedChart = new HashMap<Integer, ArrayList<Integer>>();
	/**
	 * SpanSortedEdges are array of all the edges sorted by the span length
	 * SpanSortedIndexes are the indexes of the sorted edges in the original edge map in DepTree
	 */
	Edge[] SpanSortedEdges;
	int[] SpanSortedIndexes;
	/**
	 * y-axis baseline of all tokens and edge tips
	 */
	int tokenBaseline, edgeBaseline;
	/**
	 * margin between tips
	 */
	int edgeTipMargin = fontSize / 3;
	/**
	 * margin between different levels of upper span lines
	 */
	int edgeSpanMargin = fontSize * 2;
	/**
	 * margin between the top of tokens and the tip of edges
	 */
	int TEMargin = fontSize / 3;
	/**
	 * margin between different properties
	 */
	int propertyMargin = fontSize / 3;
	/**
	 * margin between the upper span line and the label
	 */
	int labelMargin = fontSize / 3;

	public void init() {
		wordSpace = fontSize;
		loadFont();
		loadSentence();
		setPreferredSize(area);
		setSize(area);
		revalidate();
		repaint();
	}
	
	/**
	 * render the dependency tree according to the layout arranged
	 */
	public void render(Graphics2D g2) {
		g2.setFont(font);
		g2.setColor(Color.BLACK);
		g2.setStroke(new BasicStroke());

		DepTree dep = getSentence(sentenceNumber);
		// render the tokens
		for (int i = 0; i < dep.tokens.size(); i++) {
			Token curTok;
			// if this dependency tree has a root node
			// get the root node when i == dep.tokens.size() - 1 or invalid index would be visited
			if(dep.hasRoot && i == dep.tokens.size() - 1)
			{
				curTok = dep.tokens.get(-1);
			}
			else {
				curTok = dep.tokens.get(i);
			}
			// render the token
			if (curTok != null) {
				// render the id
				int plen = metrics.stringWidth(String.valueOf(curTok.getID()));
				int pXLeft = tokenXMiddleArray[i] - plen / 2;
				g2.drawString(String.valueOf(curTok.getID()), pXLeft,
						tokenBaseline);

				// render the head
				plen = metrics.stringWidth(curTok.gethead());
				pXLeft = tokenXMiddleArray[i] - plen / 2;
				g2.drawString(curTok.gethead(), pXLeft, tokenBaseline
						+ propertyMargin + fontHight);

				// render the properties
				int plevel = 2;
				g2.setColor(Color.GRAY);
				Set<String> pl = curTok.getPropertyList();
				for (String pn : pl) {
					String p = curTok.getProperty(pn);
					plen = metrics.stringWidth(p);
					pXLeft = tokenXMiddleArray[i] - plen / 2;
					g2.drawString(p, pXLeft, tokenBaseline + plevel
							* (propertyMargin + fontHight));
					plevel++;
				}
				g2.setColor(Color.BLACK);
			}
		}

		// render the edges
		for (int i = 0; i < dep.edges.size(); i++) {
			Point p1, p2, p3, p4;
			p1 = new Point(edgeStartArray[i], edgeBaseline);
			p2 = new Point(edgeStartArray[i], edgeYArray[i]);
			p3 = new Point(edgeEndArray[i], edgeYArray[i]);
			p4 = new Point(edgeEndArray[i], edgeBaseline);
			createCurveArrow(p1, p2, p3, p4, g2);

			String label = dep.edges.get(i).getLabel();
			if (!label.trim().equals("_")) {
				int labelX = (edgeStartArray[i] + edgeEndArray[i]) / 2
						- metrics.stringWidth(label) / 2;
				int labelY = edgeYArray[i] - labelMargin;
				g2.drawString(label, labelX, labelY);
			}
		}
	}

	/**
	 * Create an curved path that starts at p1 and ends at p4. Points p2 and p3
	 * are used as bezier control points.
	 * 
	 * @param p1
	 *            the first point
	 * @param p2
	 *            the second point
	 * @param p3
	 *            the third point
	 * @param p4
	 *            the last point
	 * @return an a path over the given points.
	 */
	private void createCurveArrow(Point p1, Point p2, Point p3, Point p4,
			Graphics2D g2) {
		GeneralPath shape = new GeneralPath();
		shape.moveTo(p1.x, p1.y);
		shape.curveTo(p2.x, p2.y, p2.x, p2.y, p2.x + (p3.x - p2.x) / 2, p2.y);
		shape.curveTo(p3.x, p3.y, p3.x, p3.y, p4.x, p4.y);
		shape.moveTo(p3.x, p3.y);
		shape.closePath();
		g2.draw(shape);

		int arrowSize = g2.getFont().getSize() / 5;
		int[] arrowX = new int[4];
		int[] arrowY = new int[4];
		arrowX[0] = p4.x;
		arrowX[1] = p4.x + arrowSize;
		arrowX[2] = p4.x;
		arrowX[3] = p4.x - arrowSize;
		arrowY[0] = p4.y;
		arrowY[1] = arrowY[0] - 2 * arrowSize;
		arrowY[2] = arrowY[0] - arrowSize;
		arrowY[3] = arrowY[0] - 2 * arrowSize;
		g2.fillPolygon(arrowX, arrowY, 4);
	}

	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		render(g2);
	}

	public void setSentenceNumber(int sn) {
		this.sentenceNumber = sn;
	}
	
	/**
	 * arranges the layout of the dependency tree
	 */
	public void loadSentence() {
		// initialization
		DepTree dep = getSentence(sentenceNumber);
		tokenXLeftArray = new int[dep.tokens.size()];
		tokenXMiddleArray = new int[dep.tokens.size()];
		tokenLengthsArray = new int[dep.tokens.size()];

		for (int i = 0; i < dep.tokens.size(); i++) {
			tokenXLeftArray[i] = -1;
		}

		// deal with the tokens
		int currentXLeft = leftMargin;
		for (int i = 0; i < dep.tokens.size(); i++) {
			// deal with the root token
			if (dep.hasRoot && i == dep.tokens.size() - 1) {
				break;
			}
			if (dep.hasRoot && i == 0) {
				i = dep.tokens.size() - 1;
				Token rootToken = dep.tokens.get(-1);
				tokenXLeftArray[i] = currentXLeft;
				tokenLengthsArray[i] = metrics
						.stringWidth(getLongestProperty(rootToken));
				tokenXMiddleArray[i] = currentXLeft + tokenLengthsArray[i] / 2;
				currentXLeft = tokenXLeftArray[i] + tokenLengthsArray[i]
						+ wordSpace;
				i = 0;
			}
			// deal with normal tokens
			Token token = dep.tokens.get(i);
			if (token != null) {
				tokenXLeftArray[i] = currentXLeft;
				tokenLengthsArray[i] = metrics
						.stringWidth(getLongestProperty(token));
				tokenXMiddleArray[i] = currentXLeft + tokenLengthsArray[i] / 2;
				currentXLeft = tokenXLeftArray[i] + tokenLengthsArray[i]
						+ wordSpace;
			}
		}
		
		// deal with edges
		// Sort edges according to their span length
		SpanSortedEdges = new Edge[0];
		SpanSortedEdges = dep.edges.values().toArray(SpanSortedEdges);
		SpanSortedIndexes = new int[SpanSortedEdges.length];
		Arrays.sort(SpanSortedEdges, new EdgeSpanComparator());
		for (int i = 0; i < SpanSortedEdges.length; i++) {
			for (int j = 0; j < dep.edges.size(); j++) {
				if (SpanSortedEdges[i].equals(dep.edges.get(j))) {
					SpanSortedIndexes[i] = j;
				}
			}
		}

		TELeftPairs.clear();
		TERightPairs.clear();
		NestedChart.clear();
		CrossedChart.clear();

		// relate edges with tokens
		for (int i = 0; i < dep.edges.size(); i++) {
			Edge e = dep.edges.get(i);
			if (e != null) {
				int lp = e.getSmallIndex();
				int rp = e.getLargeIndex();

				ArrayList<Integer> LArray = TELeftPairs.get(lp);
				if (LArray == null) {
					LArray = new ArrayList<Integer>();
				}
				LArray.add(i);
				TELeftPairs.put(lp, LArray);

				ArrayList<Integer> RArray = TERightPairs.get(rp);
				if (RArray == null) {
					RArray = new ArrayList<Integer>();
				}
				RArray.add(i);
				TERightPairs.put(rp, RArray);
			}
		}

		// find out nested and crossed edges and put them into the map
		for (int i = 0; i < dep.edges.size(); i++) {
			Edge e1 = dep.edges.get(i);
			ArrayList<Integer> tNested = new ArrayList<Integer>();
			ArrayList<Integer> tCrossed = new ArrayList<Integer>();
			for (int j = 0; j < dep.edges.size(); j++) {
				if (i == j) {
					continue;
				}
				Edge e2 = dep.edges.get(j);
				if (e2 != null) {
					if (Edge.isNested(e1, e2)) {
						tNested.add(j);
					}
					if (Edge.isCross(e1, e2)) {
						tCrossed.add(j);
					}
				}
			}
			if (!tNested.isEmpty()) {
				NestedChart.put(i, tNested);
			}
			if (!tCrossed.isEmpty()) {
				CrossedChart.put(i, tCrossed);
			}
		}

		// assign spanning levels to edges
		// 
		// level = max{l1, l2}
		// l1 = max{levels of nested edges} + 1
		// l2 = max{levels of crossing edges on the left} + 1
		edgeLevel = new int[dep.edges.size()];
		for (int i = 0; i < dep.edges.size(); i++) {
			edgeLevel[i] = 1;
		}

		for (int i = 0; i < SpanSortedIndexes.length; i++) {
			int index = SpanSortedIndexes[i];

			ArrayList<Integer> NestedEdges = NestedChart.get(index);
			int maxNestedLevel = 0;
			if (NestedEdges != null) {
				for (int ne : NestedEdges) {
					if (edgeLevel[ne] > maxNestedLevel) {
						maxNestedLevel = edgeLevel[ne];
					}
				}
			}

			ArrayList<Integer> CrossedEdges = CrossedChart.get(index);
			int maxCrossedLevel = 0;
			if (CrossedEdges != null) {
				for (int ce : CrossedEdges) {
					if (edgeLevel[ce] > maxCrossedLevel) {
						Edge e1 = dep.edges.get(index);
						Edge e2 = dep.edges.get(ce);
						if (e1.getSpanLength() < e2.getSpanLength()) {
							maxCrossedLevel = edgeLevel[ce];
						}
						else if (e1.getSpanLength() == e2.getSpanLength()) {
							if (e1.getSmallIndex() > e2.getSmallIndex()) {
								maxCrossedLevel = edgeLevel[ce];
							}
						}
					}
				}
			}

			edgeLevel[index] = maxNestedLevel > maxCrossedLevel ? maxNestedLevel + 1
					: maxCrossedLevel + 1;
		}

		// assign tip levels to edges
		//
		// For a single token,
		// the tip with lowest level is the leftmost tip,
		// while the one with highest level lays rightmost.
		// 
		// For left tips,
		// tips on edges with a shorter span length (lower spanning level) gets a higher tip level.
		//
		// For right tips,
		// tips on edges with a longer span length (higher spanning level) gets a higher tip level.
		STipsLevel = new int[dep.edges.size()];
		ETipsLevel = new int[dep.edges.size()];
		tokTipsNumber = new int[dep.tokens.size()];
		for (int i = 0; i < dep.tokens.size(); i++) {
			Token curTok;
			if(dep.hasRoot && i == dep.tokens.size() - 1) {
				curTok = dep.tokens.get(-1);
			}
			else {
				curTok = dep.tokens.get(i);
			}
			if (curTok != null) {
				Integer[] LTipEdges = new Integer[0];
				Integer[] RTipEdges = new Integer[0];

				if(dep.hasRoot && i == dep.tokens.size() - 1) {
					i = -1;
				}
				if (TELeftPairs.get(i) != null) {
					LTipEdges = TELeftPairs.get(i).toArray(LTipEdges);
					Arrays.sort(LTipEdges, new EdgeLevelComparator());
				}

				if (TERightPairs.get(i) != null) {
					RTipEdges = TERightPairs.get(i).toArray(RTipEdges);
					Arrays.sort(RTipEdges, new EdgeLevelComparator());
				}
				
				if(i == -1) {
					tokTipsNumber[dep.tokens.size() - 1] = LTipEdges.length + RTipEdges.length;
				}
				else {
					tokTipsNumber[i] = LTipEdges.length + RTipEdges.length;
				}

				if (TELeftPairs.get(i) != null || TERightPairs.get(i) != null) {
					int levelAssigned = 0;
					for (int j = 0; j < RTipEdges.length; j++) {
						boolean dir = dep.edges.get(RTipEdges[j])
								.getDirection();
						if (dir) {
							ETipsLevel[RTipEdges[j]] = levelAssigned;
						}
						else {
							STipsLevel[RTipEdges[j]] = levelAssigned;
						}
						levelAssigned++;
					}
					for (int j = LTipEdges.length - 1; j >= 0; j--) {
						boolean dir = dep.edges.get(LTipEdges[j])
								.getDirection();
						if (dir) {
							STipsLevel[LTipEdges[j]] = levelAssigned;
						}
						else {
							ETipsLevel[LTipEdges[j]] = levelAssigned;
						}
						levelAssigned++;
					}
				}
				if(i == -1) {
					break;
				}
			}
		}

		// if there are too many tips on a single token,
		// the token length needs to be stretched
		for (int i = 0; i < dep.tokens.size(); i++) {
			if (tokTipsNumber[i] * edgeTipMargin > tokenLengthsArray[i]) {
				int incre = tokTipsNumber[i] * edgeTipMargin
						- tokenLengthsArray[i];
				tokenLengthsArray[i] += incre;
				tokenXLeftArray[i] += incre / 2;
				tokenXMiddleArray[i] += incre / 2;
				if (i + 1 < dep.tokens.size()) {
					for (int j = i + 1; j < dep.tokens.size(); j++) {
						if(dep.hasRoot && j == dep.tokens.size() - 1) {
							break;
						}
						tokenXLeftArray[j] += incre;
						tokenXMiddleArray[j] += incre;
					}
				}
				else if(dep.hasRoot) {
					for(int j = 0; j < dep.tokens.size() - 1; j++) {
						tokenXLeftArray[j] += incre;
						tokenXMiddleArray[j] += incre;
					}
				}
			}
		}

		// if the label of an edge is too long,
		// the span length of the edge needs to be stretched,
		// or to say, the right-token and the tokens to the right of the right-token
		// should be adjusted
		for (int i = 0; i < dep.edges.size(); i++) {
			Edge e = dep.edges.get(i);
			int labelLength = metrics.stringWidth(e.getLabel());
			int lp = e.getSmallIndex();
			int rp = e.getLargeIndex();
			if (lp == -1) {
				lp = dep.tokens.size() - 1;
			}
			else if (rp == -1) {
				rp = dep.tokens.size() - 1;
			}
			int spanLength = tokenXLeftArray[rp] - tokenXLeftArray[lp]
					- tokenLengthsArray[lp];
			if (spanLength < labelLength) {
				int incre = labelLength - spanLength;
				tokenXLeftArray[rp] += incre;
				tokenXMiddleArray[rp] += incre;
				if (rp + 1 < dep.tokens.size()) {
					for (int j = rp + 1; j < dep.tokens.size(); j++) {
						if(dep.hasRoot && j == dep.tokens.size() - 1) {
							break;
						}
						tokenXLeftArray[j] += incre;
						tokenXMiddleArray[j] += incre;
					}
				}
				else if(dep.hasRoot) {
					for(int j = 0; j < dep.tokens.size() - 1; j++) {
						tokenXLeftArray[j] += incre;
						tokenXMiddleArray[j] += incre;
					}
				}
			}
		}

		// assign x-pos and y-pos to the edges according to the tokens, edge levels and tip levels
		edgeStartArray = new int[dep.edges.size()];
		edgeEndArray = new int[dep.edges.size()];
		edgeYArray = new int[dep.edges.size()];
		for (int i = 0; i < dep.tokens.size(); i++) {
			ArrayList<Integer> leftToks;
			if(dep.hasRoot && i == dep.tokens.size() - 1) {
				leftToks = TELeftPairs.get(-1);
			}
			else {
				leftToks = TELeftPairs.get(i);
			}
			if (leftToks != null) {
				int midPoint = tokTipsNumber[i] / 2;
				for (int ei : leftToks) {
					boolean dir = dep.edges.get(ei).getDirection();
					if (dir) {
						edgeStartArray[ei] = tokenXMiddleArray[i]
								+ (STipsLevel[ei] - midPoint) * edgeTipMargin;
					}
					else {
						edgeEndArray[ei] = tokenXMiddleArray[i]
								+ (ETipsLevel[ei] - midPoint) * edgeTipMargin;
					}
				}
			}

			ArrayList<Integer> rightToks;
			if(dep.hasRoot && i == dep.tokens.size() - 1) {
				rightToks = TERightPairs.get(-1);
			}
			else {
				rightToks = TERightPairs.get(i);
			}
			if (rightToks != null) {
				int midPoint = tokTipsNumber[i] / 2;
				for (int ei : rightToks) {
					boolean dir = dep.edges.get(ei).getDirection();
					if (!dir) {
						edgeStartArray[ei] = tokenXMiddleArray[i]
								+ (STipsLevel[ei] - midPoint) * edgeTipMargin;
					}
					else {
						edgeEndArray[ei] = tokenXMiddleArray[i]
								+ (ETipsLevel[ei] - midPoint) * edgeTipMargin;
					}
				}
			}
		}

		int maxLevel = 0;
		for (int i = 0; i < dep.edges.size(); i++) {
			if (edgeLevel[i] > maxLevel) {
				maxLevel = edgeLevel[i];
			}
		}

		edgeBaseline = (int) (topMargin + maxLevel * levelSize);
		for (int i = 0; i < dep.edges.size(); i++) {
			edgeYArray[i] = (int) (edgeBaseline - edgeLevel[i] * levelSize);
		}

		tokenBaseline = edgeBaseline + TEMargin + fontHight;

		// see what is the maximum length of the property list
		// in order to get the layout size of the dependency tree/graph
		int maxPropertyListLength = 0;
		for (int i = 0; i < dep.tokens.size(); i++) {
			if (dep.hasRoot && i == dep.tokens.size() - 1) {
				i = -1;
				Token curTok = dep.tokens.get(i);
				int curPropertyListLength = curTok.getPropertyList().size();
				if (curPropertyListLength > maxPropertyListLength) {
					maxPropertyListLength = curPropertyListLength;
				}
				break;
			}
			Token curTok = dep.tokens.get(i);
			int curPropertyListLength = curTok.getPropertyList().size();
			if (curPropertyListLength > maxPropertyListLength) {
				maxPropertyListLength = curPropertyListLength;
			}
		}

		// set size of the layout
		area.height = tokenBaseline + (maxPropertyListLength + 1)
				* (propertyMargin + fontHight) + bottomMargin;
		if (dep.hasRoot) {
			area.width = tokenXLeftArray[tokenXLeftArray.length - 2]
					+ tokenLengthsArray[tokenLengthsArray.length - 2]
					+ rightMargin;
		}
		else {
			area.width = tokenXLeftArray[tokenXLeftArray.length - 1]
					+ tokenLengthsArray[tokenLengthsArray.length - 1]
					+ rightMargin;
		}
	}

	private String getLongestProperty(Token tok) {
		Set<String> pl = tok.getPropertyList();
		String res = String.valueOf(tok.getID());
		if (metrics.stringWidth(res) < metrics.stringWidth(tok.gethead())) {
			res = tok.gethead();
		}
		for (String pn : pl) {
			String p = tok.getProperty(pn);
			if (metrics.stringWidth(p) > metrics.stringWidth(res)) {
				res = p;
			}
		}
		return res;
	}

	public DepTree getSentence(int n) {
		return treebank.get(n);
	}

	/**
	 * For EdgeSpanComparator
	 * return value > 0 when length(edge0) > length(edge1)
	 * return value = 0 when length(edge0) = length(edge1)
	 * return value < 0 when length(edge0) < length(edge1)
	 * 
	 * @author shuoyang
	 *
	 */
	class EdgeSpanComparator implements Comparator<Edge> {

		@Override
		public int compare(Edge arg0, Edge arg1) {
			return arg0.getSpanLength() - arg1.getSpanLength();
		}
	}

	/**
	 * For EdgeLevelComparator
	 * return value > 0 when level(edge0) > level(edge1)
	 * return value = 0 when level(edge0) = level(edge1)
	 * return value < 0 when level(edge0) < level(edge1)
	 * 
	 * @author shuoyang
	 *
	 */
	class EdgeLevelComparator implements Comparator<Integer> {

		@Override
		public int compare(Integer arg0, Integer arg1) {
			return edgeLevel[arg0] - edgeLevel[arg1];
		}
	}
}
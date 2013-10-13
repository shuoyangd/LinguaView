package LinguaView.syntax;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import LinguaView.TreePanel;
/**
 * ConstTreePanel is originally a part of Federico Sangati's constituent viewer. I revised
 * the code to fit it into LinguaView.
 * ConstTreePanel loads one specified constituent tree at a time and arranges its layout in 
 * an recursive manner.
 * Note that ConstTreePanel doesn't load tree from strings. This work is left to TSNodeLabel.
 * 
 * @author Federico Sangati (original code)
 * @author shuoyang
 *
 */
@SuppressWarnings("serial")
public class ConstTreePanel extends TreePanel<TSNodeLabel> {	
	/**
	 * sentenceLength stores the number of tokens in the raw sentence
	 * nodesCount stores the number of all the nodes, both terminal and internal
	 */
	int sentenceLength, nodesCount;
	/**
	 * For the convenience of process, we assign an integer id to each TSNodeLabel
	 * indexTable maps each TSNodeLabel to this integer id
	 */
	IdentityHashMap<TSNodeLabel, Integer> indexTable;
	/**
	 * nodesArray stores all the nodes in the constituent tree
	 * lexicalsArray stores all the terminal nodes in the constituent tree
	 */
	TSNodeLabel[] nodesArray, lexicalsArray;
	/**
	 * labelArray stores the CFG label in string form for each corresponding constituent TSNodeLabel
	 */
	String[] labelArray;
	/**
	 * This is a group of layout information.
	 * XLeftArray is the x-loc of the left edge of constituent nodes
	 * YArray is the y-loc of the lower edge of constituent nodes
	 * XMiddleArray is the x-loc of the mid of constituent nodes
	 * wordLengthsArray is the length on x-axis of each constituent node
	 * 
	 * Please note that, to get the layout information of a TSNodeLabel n,
	 * one should first call indexTable.get(n) to get its index in these arrays.
	 */
	int[] XLeftArray, YArray, XMiddleArray, wordLengthsArray;
	/**
	 * skewedLines indicate whether the lines between levels should be skew or straight
	 */
	public boolean skewedLines = true;
	
	public ConstTreePanel() {
		
	}
	
	public ConstTreePanel(ArrayList<TSNodeLabel> treebank) {
		loadTreebank(treebank);
	}
	
	public void setSentenceNumber(int sn) {
		this.sentenceNumber = sn;
	}
	
	public void init() {
		loadFont();
		loadSentence();
        setPreferredSize(area);
        revalidate();
        repaint();
	}
	
	public void render(Graphics2D g2) {
		g2.setFont(font); 
        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke());
        
        if(indexTable != null) {
        	for(TSNodeLabel n : nodesArray) {
        		int i = indexTable.get(n);
        		g2.drawString(labelArray[i], XLeftArray[i], YArray[i]);
        		TSNodeLabel p = n.parent;  
        		if (p==null) continue;
        		int pIndex = indexTable.get(p);
        		drawLine(XMiddleArray[i],YArray[i]-fontSize - textTopMargin,
					XMiddleArray[pIndex],YArray[pIndex] + textTopMargin, g2);
        	}
		}
	}
	
	public void renderText(Graphics2D g2, String text) {
		g2.setFont(font); 
		g2.setColor(Color.black);
		g2.drawString(text, 0, 0);
	}
	
	private void drawLine(int x1, int y1, int x2, int y2, Graphics2D g2) {
		if (!skewedLines) {
			int yM = y1 + (y2-y1)/2;
			g2.drawLine(x1,y1,x1,yM);
			g2.drawLine(x1,yM,x2,yM);
			g2.drawLine(x2,yM,x2,y2);
		}
		else {
			g2.drawLine(x1,y1,x2,y2);
		}
	}
    
	protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        render(g2);
    }

	/**
	 * arranges the layout in an recursive manner
	 */
    public void loadSentence() {
    	TSNodeLabel t = treebank.get(sentenceNumber);
    	// deals with the empty node
    	if(t.label == null) {
    		sentenceLength = 0;
    		nodesCount = 0;
    		indexTable = null;
    		nodesArray = null;
    		lexicalsArray = null;
    		labelArray = null;
    		XLeftArray = null;
    		YArray = null;
    		XMiddleArray = null;
    		wordLengthsArray = null;
    		area.width = 0;
            area.height = 0;
    		return;
    	}
    	// initialization
    	indexTable = new IdentityHashMap<TSNodeLabel, Integer>();
    	nodesCount = t.countAllNodes();
    	sentenceLength = t.countLexicalNodes();
    	lexicalsArray = t.collectLexicalItems().toArray(new TSNodeLabel[sentenceLength]);
    	nodesArray = t.collectAllNodes().toArray(new TSNodeLabel[nodesCount]);        	
    	int maxDepth = t.maxDepth();
    	for(int i=0; i<nodesCount; i++) {
    		TSNodeLabel n = nodesArray[i];
    		indexTable.put(n, i);
    	}
    	
		labelArray = new String[nodesCount];
		XLeftArray = new int[nodesCount];
		XMiddleArray = new int[nodesCount];
		wordLengthsArray = new int[nodesCount];
		Arrays.fill(XLeftArray, -1);
		YArray = new int[nodesCount];
		int treeWidth=0, treeHeight;
		// first set a initial x-position for all terminal nodes
		int previousWordLength=0;
		int previousXLeft = leftMargin;
		for(int j=0; j<sentenceLength; j++) {
			TSNodeLabel n = lexicalsArray[j];
			int i = indexTable.get(n);
			labelArray[i] = n.label();
			int wordLength = metrics.stringWidth(labelArray[i]);
			int wordLengthColumn = getWordLengthColumn(n, wordLength);    			
			wordLengthsArray[i] = wordLengthColumn;    			
			XMiddleArray[i] = wordLengthColumn/2 + previousXLeft + previousWordLength + wordSpace;
			XLeftArray[i] = XMiddleArray[i] - (wordLength/2);
			YArray[i] = (int) (topMargin + fontHight + n.height() * levelSize);			
			previousWordLength = wordLengthColumn;
			previousXLeft = XLeftArray[i];			
			if (j==sentenceLength-1) treeWidth = previousXLeft + previousWordLength + wordSpace;
		}
		// recursively update the position of internal nodes (both x and y)
		for(TSNodeLabel n : nodesArray) {
			if (n.isLexical) continue;
			int i = indexTable.get(n);
			updateValues(n,i);    			    			    			    	
		}
		// set layout size
		treeWidth += rightMargin;
		treeHeight = (int) (topMargin + fontHight + maxDepth*levelSize + bottomMargin);
		area.width = treeWidth;
        area.height = treeHeight;
    }
    
    private int getWordLengthColumn(TSNodeLabel n, int wordLength) {
    	while (n.parent!=null && n.isUniqueDaughter()) {
    		n = n.parent;
    		int length = metrics.stringWidth(n.label());
    		if (length > wordLength) wordLength = length;
    	}
    	return wordLength;
    }
    
    /**
     * recursively update the location of internal nodes
     * @param n
     * @param i
     */
    private void updateValues(TSNodeLabel n, int i) {        	
    	if (XLeftArray[i]!=-1) return;
		labelArray[i] = n.label();    			
		YArray[i] = (int) (topMargin + fontHight + n.height() * levelSize);			
    	int wordLength = metrics.stringWidth(labelArray[i]);
    	wordLengthsArray[i] = wordLength;
    	TSNodeLabel[] daughters = n.daughters;
    	TSNodeLabel firstDaughter = n.daughters[0];
    	int iDF = indexTable.get(firstDaughter);
    	if (!firstDaughter.isLexical) updateValues(firstDaughter, iDF);
    	if (daughters.length==1) {        		        		
    		XMiddleArray[i] = XMiddleArray[iDF];  
    		XLeftArray[i] = XMiddleArray[i] - (wordLength/2);
    	}
    	else {        		
    		TSNodeLabel lastDaughter = n.daughters[n.prole()-1];        		
    		int iDL = indexTable.get(lastDaughter);        		
    		if (!lastDaughter.isLexical) updateValues(lastDaughter, iDL);
    		XMiddleArray[i] = XLeftArray[iDF] + (XLeftArray[iDL] + wordLengthsArray[iDL] - XLeftArray[iDF]) / 2;
    		XLeftArray[i] = XMiddleArray[i] - (wordLength/2);
    	}        	
    }

	public TSNodeLabel getSentence(int n) {
		return treebank.get(n);
	}
}

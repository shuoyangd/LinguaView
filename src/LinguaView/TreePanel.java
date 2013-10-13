package LinguaView;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.util.ArrayList;

import javax.swing.JPanel;
/**
 * TreePanel is a part of Federico Sangati's constituent tree viewer.  It is originally designed
 * only for class ConstTreePanel, but in LinguaView it is extended by every tree panel.
 * 
 * TreePanel provides basic schema for dimension control, font loading, sentence switching,
 * zooming, and leaves for each specified trees the definition of method to arrange layouts
 * according to the parse tree and render them in the desirable way.
 * 
 * @author Federico Sangati (original code)
 * @author shuoyang
 *
 * @param <T>
 */
@SuppressWarnings("serial")
public abstract class TreePanel<T> extends JPanel {

	public Dimension area = new Dimension(0,0);
	public static int minAreaWidth = 175;
	public static int minAreaHeight = 80;
	
	public ArrayList<T> treebank;
	
	String flatSentence;
	public int sentenceNumber;
	public int lastIndex;
	
	public int fontSize = 15;
	public int smallFontSize = 11;
	public int fontDescendent;
    public Font font, smallFont;    
    public FontMetrics metrics;
    
    public int topMargin = 60;
    public int bottomMargin = 60;	
	public int leftMargin = 20; 
	public int rightMargin = 20;
	public int wordSpace = 10;	
	public int textTopMargin = 4;
	public int smallfontHight, fontHight, doubleFontHight, tripleFontHight;
	public double levelSize;
	public double levelSizeFactor = 2.0;
	
	public void loadTreebank(ArrayList<T> treebank) {
		this.treebank = treebank;
		lastIndex = treebank.size() - 1;
	}
	
	public void replaceCurrentSentence(T sent) {
		this.treebank.set(sentenceNumber, sent);
	}
	
	public abstract void init();
	
	protected void paintComponent(Graphics g) {
		super.paintComponent(g);
	}
	
	public abstract void render(Graphics2D g2);
	
	public int sentenceNumber() {
		return sentenceNumber();
	}
	
	public int increaseFontSize() {
		return ++fontSize;
	}
	
	public int decreaseFontSize() {
		if (fontSize==1) return 1;
		return --fontSize;
	}
	
	public int nextSentence() {
		if (sentenceNumber==lastIndex) return sentenceNumber=0;
		return ++sentenceNumber;
	}
	
	public int previousSentence() {
		if (sentenceNumber==0) return sentenceNumber=lastIndex;
    	return --sentenceNumber;
	}
	
	public int goToSentence(int n) {
		if (n<0) return sentenceNumber=0;
		if (n>lastIndex) return sentenceNumber=lastIndex;		    
    	return sentenceNumber = n;
	}
	
	public void loadFont() {
		font = new Font("SansSerif", Font.PLAIN, fontSize);
		smallFont = new Font("SansSerif", Font.BOLD, smallFontSize);
    	metrics = getFontMetrics(font);
    	fontDescendent = metrics.getDescent();
    	smallfontHight = metrics.getHeight();
    	fontHight = metrics.getHeight();    	
    	doubleFontHight = 2 * fontHight;
    	tripleFontHight = 3 * fontHight;
    	levelSize = fontHight * levelSizeFactor;	
	}
	
	public Dimension getDimension() {
		return area;
	}
	
	public abstract void loadSentence();
}

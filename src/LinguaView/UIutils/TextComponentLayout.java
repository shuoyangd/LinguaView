package LinguaView.UIutils;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;

import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class TextComponentLayout {
	private BatchDocument doc;
	private JTextPane Textcomponent;
	private Style tagStyle, attributeStyle, strStyle, normalStyle;
	ArrayList<String> wrdsArray = new ArrayList<String>();
    ArrayList<String> stysArray = new ArrayList<String>();
	
	public TextComponentLayout() {
		tagStyle = doc.addStyle("tag_Style", null);
        attributeStyle = doc.addStyle("attribute_Style", null);
        strStyle = doc.addStyle("str_Style", null);
	}
	
	public void setPane(JTextPane Textcomponent) {
		this.Textcomponent = Textcomponent;
	}
	
	public void linkUp() {
		Textcomponent.setDocument(doc);
	}
	
	/**
	 * If you initiate TextComponentLayout with this constructor, you have to set up the
	 * content of the component by yourself(using JTextPane.setText(String content)), while
	 * the coloring listener will automatically color your text.
	 * Please note that this schema will be much slower than the one used in the other constructor
	 * 
	 * @param Textcomponent
	 */
	public TextComponentLayout(JTextPane Textcomponent) {
		doc = new BatchDocument();
		
		normalStyle = doc.addStyle("normal_Style", null);
		tagStyle = doc.addStyle("tag_Style", null);
        attributeStyle = doc.addStyle("attribute_Style", null);
        strStyle = doc.addStyle("str_Style", null);
        StyleConstants.setForeground(normalStyle,
        		Color.black);
        StyleConstants.setForeground(tagStyle, 
        		Color.getHSBColor((float)0.6944, (float)0.50, (float)0.80));
        StyleConstants.setForeground(attributeStyle, 
        		Color.getHSBColor((float)0.5278, (float)0.90, (float)0.55));
        StyleConstants.setForeground(strStyle, 
        		Color.getHSBColor((float)0.8889, (float)0.60, (float)1.0));
        StyleConstants.setBold(tagStyle, true);
        StyleConstants.setBold(attributeStyle, true);
        StyleConstants.setBold(strStyle, true);
		
		doc.addDocumentListener(new ColoringListener());
		this.Textcomponent = Textcomponent;
		linkUp();
	}
	
	/**
	 * If you initiate TextComponentLayout with this constructor, the content of the component
	 * is set by the constructor.  For the first time of coloring, the constructor will directly
	 * parse the text and assign color for each token.  The color listener will only make
	 * modifications when the text is edited.
	 * This schema is efficient and capable of opening relatively large text files (About
	 * 2MB. Only 200KB if listener schema is used) without crashing.
	 * 
	 * @param Textcomponent
	 * @param filename
	 */
	public TextComponentLayout(JTextPane Textcomponent, String filename) {
		doc = new BatchDocument();
		
		normalStyle = doc.addStyle("normal_Style", null);
		tagStyle = doc.addStyle("tag_Style", null);
        attributeStyle = doc.addStyle("attribute_Style", null);
        strStyle = doc.addStyle("str_Style", null);
        StyleConstants.setForeground(normalStyle,
        		Color.black);
        StyleConstants.setForeground(tagStyle, 
        		Color.getHSBColor((float)0.6944, (float)0.50, (float)0.80));
        StyleConstants.setForeground(attributeStyle, 
        		Color.getHSBColor((float)0.5278, (float)0.90, (float)0.55));
        StyleConstants.setForeground(strStyle, 
        		Color.getHSBColor((float)0.8889, (float)0.60, (float)1.0));
        StyleConstants.setBold(tagStyle, true);
        StyleConstants.setBold(attributeStyle, true);
        StyleConstants.setBold(strStyle, true);
        
		initColouring(filename);
		this.Textcomponent = Textcomponent;
		this.Textcomponent.setEditable(true);
		linkUp();
		doc.addDocumentListener(new ColoringListener());
	}
	
	public void initColouring(String filename) {
		try {
			BufferedReader in = new BufferedReader(new FileReader(filename));
			String line = in.readLine();
			while(line != null) {
				initColouringLineParser(line);
				for(int j = 0; j < wrdsArray.size(); j++) {
					if(stysArray.get(j).equals("normal")) {
						doc.appendBatchString(wrdsArray.get(j), normalStyle);
					}
					else if(stysArray.get(j).equals("tag")) {
						doc.appendBatchString(wrdsArray.get(j), tagStyle);
					}
					else if(stysArray.get(j).equals("attribute")) {
						doc.appendBatchString(wrdsArray.get(j), attributeStyle);
					}
					else if(stysArray.get(j).equals("str")) {
						doc.appendBatchString(wrdsArray.get(j), strStyle);
					}
				}
				doc.appendBatchLineFeed(normalStyle);
				wrdsArray = new ArrayList<String>();
				stysArray = new ArrayList<String>();
				line = in.readLine();
			}
			doc.processBatchUpdates(0);
			in.close();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Assign initial word colouring schema with a finite state automata
	 * Process a line of text each time
	 * state = 0: normal state
	 * state = 1: xml tag detected
	 * state = 2: attribute name detected
	 * state = 3: attribute value detected
	 * state = 4: end of xml tag detected
	 * state = 5: normal state
	 * @param doc
	 * @param pos
	 * @return
	 * @throws BadLocationException
	 */
	public void initColouringLineParser(String line) {
		int state = 0;
        int startpos = 0, endpos = 0;
        for(int i = 0; i < line.length(); i++) {
        	char ch = line.charAt(i);
        	if((state == 0) && (ch == '<')) {
        		state = 1;
        		startpos = i;
        		if(startpos > 0) {
        			wrdsArray.add(line.substring(0, startpos));
        			stysArray.add("normal");
        		}
        		continue;
        	}
        	else if((state == 1) && !((ch == ' ') || (ch == '>'))) {
        		state = 1;
        		continue;
        	}
        	else if((state == 1) && (ch == ' ')) {
        		state = 2;
        		endpos = i + 1;
        		wrdsArray.add(line.substring(startpos, endpos));
        		stysArray.add("tag");
        		startpos = i + 1;
        		continue;
        	}
        	else if((state == 1) && (ch == '>')) {
        		state = 5;
        		endpos = i + 1;
        		wrdsArray.add(line.substring(startpos, endpos));
        		stysArray.add("tag");
        		startpos = i + 1;
        		continue;
        	}
        	else if((state == 2) && !((ch == '\"') || (ch == '>'))) {
        		state = 2;
        		continue;
        	}
        	else if((state == 2) && (ch == '\"')) {
        		state = 3;
        		endpos = i;
        		wrdsArray.add(line.substring(startpos, endpos));
        		stysArray.add("attribute");
        		startpos = i;
        		continue;
        	}
        	else if((state == 2) && (ch == '>')) {
        		state = 5;
        		endpos = i + 1;
        		wrdsArray.add(line.substring(startpos, endpos));
        		stysArray.add("tag");
        		startpos = i + 1;
        		continue;
        	}
        	else if((state == 3) && !((ch == '\"') || (ch == '>'))) {
        		state = 3;
        		continue;
        	}
        	else if((state == 3) && (ch == '\"')) {
        		state = 4;
        		endpos = i + 1;
        		wrdsArray.add(line.substring(startpos, endpos));
        		stysArray.add("str");
        		startpos = i + 1;
        		continue;
        	}
        	else if((state == 3) && (ch == '>')) {
        		state = 5;
        		endpos = i + 1;
        		wrdsArray.add(line.substring(startpos, endpos));
        		stysArray.add("tag");
        		startpos = i + 1;
        		continue;
        	}
        	else if((state == 4) && !((ch == ' ') || (ch == '\n') || (ch == '\t') || (ch == '>'))) {
        		state = 2;
        		continue;
        	}
        	else if((state == 4) && ((ch == ' ') || (ch == '\n') || (ch == '\t'))) {
        		state = 4;
        		continue;
        	}
        	else  if((state == 4) && (ch == '>')) {
        		state = 5;
        		endpos = i + 1;
        		wrdsArray.add(line.substring(startpos, endpos));
        		stysArray.add("tag");
        		startpos = i + 1;
        		continue;
        	}
        	else if((state == 5) && (ch != '<')) {
        		state = 5;
        		continue;
        	}
        	else if((state == 5) && (ch == '<')) {
        		state = 1;
        		endpos = i + 1;
        		wrdsArray.add(line.substring(startpos, endpos));
        		stysArray.add("normal");
        		startpos = i + 1;
        		continue;
        	}
        }
        if(endpos < line.length()) {
        	wrdsArray.add(line.substring(endpos, line.length()));
    		stysArray.add("normal");
        }
	}
	
	class ColoringListener implements DocumentListener {		
		@Override
		public void changedUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void insertUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub
			try {
	            colouring((StyledDocument) e.getDocument(), e.getOffset(), e.getLength());
	        } catch (BadLocationException e1) {
	            e1.printStackTrace();
	        }
		}

		@Override
		public void removeUpdate(DocumentEvent e) {
			// TODO Auto-generated method stub
			try {
	            colouring((StyledDocument) e.getDocument(), e.getOffset(), 0);
	        } catch (BadLocationException e1) {
	            e1.printStackTrace();
	        }
		}
		
		public void colouring(StyledDocument doc, int pos, int len) throws BadLocationException {
	        int start = indexOfWordStart(doc, pos);
	        int end = indexOfWordEnd(doc, pos + len);

	        char ch;
	        while (start < end) {
	            ch = getCharAt(doc, start);
	            if (ch == '<') {
	                start = colouringWord(doc, start);
	            }
	            else if (ch == '>') {
	            	++start;
	            	start = colouringWord(doc, start);
	            }
	            else {
	                ++start;
	            }
	        }
	    }
		
		/**
		 * Carries out word colouring with a finite state automata
		 * state = 0: normal state
		 * state = 1: xml tag detected
		 * state = 2: attribute name detected
		 * state = 3: attribute value detected
		 * state = 4: end of xml tag detected
		 * state = 5: normal state
		 * @param doc
		 * @param pos
		 * @return
		 * @throws BadLocationException
		 */
		public int colouringWord(StyledDocument doc, int pos) throws BadLocationException {
	        int wordEnd = indexOfWordEnd(doc, pos);
	        String word = doc.getText(pos, wordEnd - pos + 1);
	        
	        int state = 0;
	        int startpos = 0, endpos = 0;
	        for(int i = 0; i < word.length(); i++) {
	        	char ch = word.charAt(i);
	        	if((state == 0) && (ch == '<')) {
	        		state = 1;
	        		startpos = i;
	        		if(startpos > 0) {
	        			SwingUtilities.invokeLater(new ColouringTask(doc, pos,
		        				startpos, normalStyle));
	        		}
	        		continue;
	        	}
	        	else if((state == 1) && !((ch == ' ') || (ch == '>'))) {
	        		state = 1;
	        		continue;
	        	}
	        	else if((state == 1) && (ch == ' ')) {
	        		state = 2;
	        		endpos = i;
	        		SwingUtilities.invokeLater(new ColouringTask(doc, pos + startpos,
	        				endpos - startpos, tagStyle));
	        		startpos = i + 1;
	        		continue;
	        	}
	        	else if((state == 1) && (ch == '>')) {
	        		state = 5;
	        		endpos = i + 1;
	        		SwingUtilities.invokeLater(new ColouringTask(doc, pos + startpos,
	        				endpos - startpos, tagStyle));
	        		startpos = i + 2;
	        		continue;
	        	}
	        	else if((state == 2) && !((ch == '\"') || (ch == '>'))) {
	        		state = 2;
	        		continue;
	        	}
	        	else if((state == 2) && (ch == '\"')) {
	        		state = 3;
	        		endpos = i - 1;
	        		SwingUtilities.invokeLater(new ColouringTask(doc, pos + startpos,
	        				endpos - startpos, attributeStyle));
	        		startpos = i;
	        		continue;
	        	}
	        	else if((state == 2) && (ch == '>')) {
	        		state = 5;
	        		endpos = i + 1;
	        		SwingUtilities.invokeLater(new ColouringTask(doc, pos + startpos,
	        				endpos - startpos, tagStyle));
	        		startpos = i + 2;
	        		continue;
	        	}
	        	else if((state == 3) && !((ch == '\"') || (ch == '>'))) {
	        		state = 3;
	        		continue;
	        	}
	        	else if((state == 3) && (ch == '\"')) {
	        		state = 4;
	        		endpos = i + 1;
	        		SwingUtilities.invokeLater(new ColouringTask(doc, pos + startpos,
	        				endpos - startpos, strStyle));
	        		startpos = i + 1;
	        		continue;
	        	}
	        	else if((state == 3) && (ch == '>')) {
	        		state = 5;
	        		endpos = i + 1;
	        		SwingUtilities.invokeLater(new ColouringTask(doc, pos + startpos,
	        				endpos - startpos, tagStyle));
	        		startpos = i + 2;
	        		continue;
	        	}
	        	else if((state == 4) && !((ch == ' ') || (ch == '\n') || (ch == '\t') || (ch == '>'))) {
	        		state = 2;
	        		startpos = i;
	        		continue;
	        	}
	        	else if((state == 4) && ((ch == ' ') || (ch == '\n') || (ch == '\t'))) {
	        		state = 4;
	        		continue;
	        	}
	        	else  if((state == 4) && (ch == '>')) {
	        		state = 5;
	        		endpos = i + 1;
	        		SwingUtilities.invokeLater(new ColouringTask(doc, pos + startpos,
	        				endpos - startpos, tagStyle));
	        		startpos = i + 2;
	        		continue;
	        	}
	        	else if((state == 5) && (ch != '<')) {
	        		state = 5;
	        		continue;
	        	}
	        	else if((state == 5) && (ch == '<')) {
	        		state = 5;
	        		endpos = i;
	        		SwingUtilities.invokeLater(new ColouringTask(doc, pos + startpos,
	        				endpos - startpos, normalStyle));
	        		startpos = i + 1;
	        		continue;
	        	}
	        }
	        if(endpos < word.length()) {
	        	SwingUtilities.invokeLater(new ColouringTask(doc, pos + endpos,
	        			word.length() - endpos - 1, normalStyle));
	        }
	        return wordEnd;
	    }
		
		public int indexOfWordStart(StyledDocument doc, int pos) throws BadLocationException{
			for(; (pos > 0) && (getCharAt(doc, pos) != '<'); --pos);
			return pos;
		}
		
		public int indexOfWordEnd(Document doc, int pos) throws BadLocationException {
	        //for (; (pos < doc.getLength()) && (getCharAt(doc, pos) != '>'); ++pos);
			while((pos < doc.getLength()) && (getCharAt(doc, pos) != '>')) {
				++pos;
			}
	        return pos;
	    }
		
		public char getCharAt(Document doc, int pos) throws BadLocationException {
			return doc.getText(pos, 1).charAt(0);
		}
	}
	
	class ColouringTask implements Runnable {
	    private StyledDocument doc;
	    private Style style;
	    private int pos;
	    private int len;

	    public ColouringTask(StyledDocument doc, int pos, int len, Style style) {
	        this.doc = doc;
	        this.pos = pos;
	        this.len = len;
	        this.style = style;
	    }

	    public void run() {
	        try {
	            doc.setCharacterAttributes(pos, len, style, true);
	        } 
	        catch (Exception e) 
	        {
	        	e.printStackTrace();
	        }
	    }
	}
}

/**
 * DefaultDocument subclass that supports batching inserts.
 */
@SuppressWarnings("serial")
class BatchDocument extends DefaultStyledDocument {
    /**
     * EOL tag that we re-use when creating ElementSpecs
     */
    private static final char[] EOL_ARRAY = { '\n' };

    /**
     * Batched ElementSpecs
     */
    private ArrayList<ElementSpec> batch = null;

    public BatchDocument() {
        batch = new ArrayList<ElementSpec>();
    }

    /**
     * Adds a String (assumed to not contain linefeeds) for 
     * later batch insertion.
     */
    public void appendBatchString(String str, 
        AttributeSet a) {
        // We could synchronize this if multiple threads 
        // would be in here. Since we're trying to boost speed, 
        // we'll leave it off for now.

        // Make a copy of the attributes, since we will hang onto 
        // them indefinitely and the caller might change them 
        // before they are processed.
        a = a.copyAttributes();
        char[] chars = str.toCharArray();
        batch.add(new ElementSpec(
            a, ElementSpec.ContentType, chars, 0, str.length()));
    }

    /**
     * Adds a linefeed for later batch processing
     */
    public void appendBatchLineFeed(AttributeSet a) {
        // See sync notes above. In the interest of speed, this 
        // isn't synchronized.

        // Add a spec with the linefeed characters
        batch.add(new ElementSpec(
                a, ElementSpec.ContentType, EOL_ARRAY, 0, 1));

        // Then add attributes for element start/end tags. Ideally 
        // we'd get the attributes for the current position, but we 
        // don't know what those are yet if we have unprocessed 
        // batch inserts. Alternatives would be to get the last 
        // paragraph element (instead of the first), or to process 
        // any batch changes when a linefeed is inserted.
        Element paragraph = getParagraphElement(0);
        AttributeSet pattr = paragraph.getAttributes();
        batch.add(new ElementSpec(null, ElementSpec.EndTagType));
        batch.add(new ElementSpec(pattr, ElementSpec.StartTagType));
    }

    public void processBatchUpdates(int offs) throws 
        BadLocationException {
        // As with insertBatchString, this could be synchronized if
        // there was a chance multiple threads would be in here.
        ElementSpec[] inserts = new ElementSpec[batch.size()];
        batch.toArray(inserts);

        // Process all of the inserts in bulk
        super.insert(offs, inserts);
    }
}
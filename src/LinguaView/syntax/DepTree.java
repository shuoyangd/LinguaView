package LinguaView.syntax;

import java.util.*;

import org.w3c.dom.Attr;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
/**
 * DepTree is a Java implementation for dependency tree, but actually it can
 * also be used for dependency graph.
 * A DepTree consists of a series of tokens and edges linking the tokens.
 * DepTree also contains methods to read in a dependency tree / graph from certain inputs.
 * 
 * @author shuoyang
 *
 */
public class DepTree {
	/**
	 * A map from the token index to token object
	 */
	Map<Integer, Token> tokens = new HashMap<Integer, Token>();
	/**
	 * A map from the edge index to edge object
	 */
	Map<Integer, Edge> edges = new HashMap<Integer, Edge>();
	/**
	 * If the edges contain a reference to the token indexed -1,
	 * we say the dependency tree has a "root node"
	 * You don't have to add root node manually in the inputs,
	 * it will be automatically added when reference to token -1 detected.
	 */
	boolean hasRoot = false;
	
	/**
	 * load tokens from the input element "wordlist"
	 * for what "wordlist" means, check out comments in LinguaView.java 
	 * or check the input format document
	 * @param tokenls
	 */
	public void loadTokens(Element tokenls) {
		if(tokenls == null) {
			return;
		}
		
		NodeList tokls = tokenls.getChildNodes();
		// iterate through all the tokens
		for (int k = 0; k < tokls.getLength(); k++) {
			Node tok = tokls.item(k);
			if (tok instanceof Element) {
				Token token = null;
				NamedNodeMap Attributes = tok
						.getAttributes();
				// iterate through all the properties
				// first-time iteration, finding the "id" property to initialize a new token
				for (int m = 0; m < Attributes.getLength(); m++) {
					Node Attribute = Attributes.item(m);
					if (Attribute instanceof Attr) {
						String AttrName = ((Attr) Attribute)
								.getName();
						String AttrValue = ((Attr) Attribute)
								.getValue();
						if (AttrName.equals("id")) {
							token = new Token(
									Integer.parseInt(AttrValue));
							break;
						}
					}
				}
				// second-time iteration, adding the properties into the new token
				for (int m = 0; m < Attributes.getLength(); m++) {
					Node Attribute = Attributes.item(m);
					if (Attribute instanceof Attr) {
						String AttrName = ((Attr) Attribute)
								.getName();
						String AttrValue = ((Attr) Attribute)
								.getValue();
						if (token != null) {
							token.addProperty(AttrName,
									AttrValue);
						}
					}
				}
				tokens.put(token.getID(), token);
			}
		}
	}
	
	/**
	 * Loading all the edges from a string
	 * The string looks like (8, 10, A) for an edge from 8 to 10 labeled A
	 * For details of the depString format, please refer to the input format document
	 * 
	 * @param depString
	 */
	public void loadEdges(String depString) {
		hasRoot = false;
	    String[] depArray = depString.substring(1).split("\\(");
	    // first-time iteration, find whether there is a root node
	    for(String rawdep: depArray) {
	    	rawdep = rawdep.substring(0, rawdep.indexOf(')'));
	    	String[] dep = rawdep.split(", ");
	    	// if root node is found, add it
	    	if(Integer.parseInt(dep[0].trim()) == -1 || 
	    			Integer.parseInt(dep[1].trim()) == -1) {
	    		if(!hasRoot) {
	    			Token temp = new Token(-1);
	    			temp.addProperty("head", "Root");
	    			tokens.put(-1, temp);
	    			hasRoot = true;
	    		}
	    	}
	    }
	    
	    int i = 0;
	    // second-time iteration, collecting edges to the map
	    for(String rawdep: depArray) {
	    	rawdep = rawdep.substring(0, rawdep.indexOf(')'));
	   		String[] dep = rawdep.split(", ");
	   		int from = Integer.parseInt(dep[0].trim());
	   		int to = Integer.parseInt(dep[1].trim());
	   		edges.put(i, new Edge(from, to, dep[2].trim()));
	   		i++;
	   	}
	}
	
	/**
	 * Loading tokens from the raw string
	 * In LinguaView, this is only used for initialization
	 * The tokens are separated according to spaces
	 * 
	 * @param sentence
	 */
	public void loadTokens(String sentence) {
    	String[] tokenArray = sentence.split(" ");
    	int index = 0;
    	for(String token: tokenArray) {
    		Token temp = new Token(index);
    		temp.addProperty("head", token);
    		tokens.put(index, temp);
    		index ++;
    	}
    }
}
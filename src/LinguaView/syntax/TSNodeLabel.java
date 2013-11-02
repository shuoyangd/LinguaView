package LinguaView.syntax;

import java.util.ArrayList;
import java.util.List;
/**
 * This version of TSNodeLabel is basically copied from the original version in Federico Sangati's
 * constituent tree viewer.  But since much of the functions of the original implementation is not
 * needed in the viewer, we only copied methods that are used in the viewer.
 * 
 * Please note that the constructor of this implementation is completely reconstructed.
 * 
 * The TSNodeLabel represents a node in a whole constituent tree. Different nodes are connected
 * using reference variables.
 * 
 * @author shuoyang
 * @author Federico Sangati (original code)
 *
 */
public class TSNodeLabel {
	public String label;
	public TSNodeLabel parent;
	public boolean isLexical = false;
	public TSNodeLabel[] daughters;

	/**
	 * Construct the root TSNodeLabel from a PennTree-string in the given format.
	 * Will call the other TSNodeLabel constructor to recursively construct its daughters.
	 * @param PennTree
	 */
	public TSNodeLabel(String PennTree) {
		PennTree = PennTree.replaceAll("[\n\t\\ ]+", " ");
		PennTree = PennTree.trim();
		if(PennTree.matches("^\\( *\\)$") || PennTree.equals("")) {
			label = null;
			parent = null;
			daughters = null;
			return;
		}
		
		PennTree = PennTree.substring(PennTree.indexOf('(') + 1, PennTree.lastIndexOf(')')).trim();
		//from here the PennTree string cannot be revised
		//deal with label & parent & isLexical
		label = PennTree.substring(PennTree.indexOf('(') + 1, PennTree.indexOf(' '));
		parent = null;
		isLexical = false;
		//deal with daughters
		ArrayList<TSNodeLabel> L = new ArrayList<TSNodeLabel>();
		Index currentPos = new Index(findNearestNonSpace(PennTree, PennTree.indexOf(' ')));
		while(currentPos.value < PennTree.length()) {
			if (PennTree.charAt(currentPos.value) == ')') {
				break;
			}
			else if(PennTree.charAt(currentPos.value) != ' ') {
				TSNodeLabel temp = new TSNodeLabel(PennTree, currentPos);
				temp.parent = this;
				L.add(temp);
			}
			else {
				currentPos.value++;
			}
		}
		if(L.isEmpty()) {
			daughters = null;
		}
		else {
			int len = L.size();
			daughters = new TSNodeLabel[len];
			L.toArray(daughters);
		}
	}
	
	/**
	 * Construct all the non-root TSNodeLabel recursively.
	 * @param PennTree
	 * @param currentPos
	 */
	public TSNodeLabel(String PennTree, Index currentPos) {
		int startpt = 0, endpt = 0;
		currentPos.value = findNearestNonSpace(PennTree, currentPos.value);
		
		//deal with the label
		//for normal nodes
		if(PennTree.charAt(currentPos.value) == '(') {
			startpt = currentPos.value + 1;
			for(int i = currentPos.value; i < PennTree.length(); i++) {
				if(PennTree.charAt(i) == ' ' && i > startpt) {
					endpt = i;
					break;
				}
			}
		}
		//for lexical nodes
		else {
			isLexical = true;
			startpt = currentPos.value;
			for(int i = currentPos.value; i < PennTree.length(); i++) {
				if((PennTree.charAt(i) == '(' || PennTree.charAt(i) == ')') && i > startpt) {
					endpt = i;
					break;
				}
			}
		}
		label = PennTree.substring(startpt, endpt);
		
		//deal with the daughters & isLexical
		ArrayList<TSNodeLabel> L = new ArrayList<TSNodeLabel>();
		currentPos.value = findNearestNonSpace(PennTree, endpt);
		while(currentPos.value < PennTree.length() && !isLexical) {
			//for normal node
			if(PennTree.charAt(currentPos.value) == '(') {
				isLexical = false;
				TSNodeLabel temp = new TSNodeLabel(PennTree, currentPos);
				temp.parent = this;
				L.add(temp);
			}
			//for the ends(and the lexical ones)
			else if(PennTree.charAt(currentPos.value) == ')') {
				if(isLexical == false) {
					currentPos.value++;
					currentPos.value = findNearestNonSpace(PennTree, currentPos.value);
				}
				break;
			}
			//for nodes one level higher than the lexical nodes
			else {
				isLexical = false;
				TSNodeLabel temp = new TSNodeLabel(PennTree, currentPos);
				temp.parent = this;
				L.add(temp);
			}
		}
		if(L.isEmpty()) {
			daughters = null;
		}
		else {
			int len = L.size();
			daughters = new TSNodeLabel[len];
			L.toArray(daughters);
		}
	}
	
	private int findNearestNonSpace(String PennTree, int pos) {
		while(true) {
			if(PennTree.charAt(pos) == ' ' && pos < PennTree.length()) {
				pos++;
			}
			else {
				break;
			}
		}
		return pos;
	}
	
	public int countAllNodes() {
		int result = 1;
		if (this.isTerminal()) return result; 
		for(TSNodeLabel TN : this.daughters) {
			result += TN.countAllNodes();
		}
		return result;
	}
	
	public int countLexicalNodes() {
		if (this.isLexical) return 1;		
		if (this.isTerminal()) return 0;
		int result = 0;
		for(TSNodeLabel TN : this.daughters) {
			result += TN.countLexicalNodes();
		}
		return result;
	}
	
	public boolean isTerminal() {
		return this.daughters == null;
	}
	
	/**
	 * Returns a LinkedList of type TreeNodeLabel containing the lexical items of
	 * the current TreeNodeLabel. 
	 */
	public ArrayList<TSNodeLabel> collectLexicalItems() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectLexicalItems(result);
		return result;
	}
	
	/**
	 * Method to implement collectLexicalItems()
	 * @param lexItems
	 */
	private void collectLexicalItems(List<TSNodeLabel> lexItems) {
		if (this.isTerminal()) return;
		for(TSNodeLabel TN : this.daughters) {
			if (TN.isLexical) lexItems.add(TN);
			else TN.collectLexicalItems(lexItems);
		}		
	}
	
	/**
	 * Returns a LinkedList of type TreeNodeLabel containing all items of
	 * the current TreeNode in depth first. 
	 */
	public ArrayList<TSNodeLabel> collectAllNodes() {
		ArrayList<TSNodeLabel> result = new ArrayList<TSNodeLabel>();
		this.collectAllNodes(result);
		return result;
	}
	
	/**
	 * Method to implement collectTerminals()
	 * @param terminals
	 */
	private void collectAllNodes(List<TSNodeLabel> allNodes) {
		allNodes.add(this);
		if (this.isTerminal()) return;
		for(TSNodeLabel TN : this.daughters) {
			TN.collectAllNodes(allNodes);
		}		
	}
	
	public int maxDepth() {
		if (this.isTerminal()) return 0;		
		int maxDepth = 0;
		for(TSNodeLabel t : this.daughters) {
			int tDepth = t.maxDepth();
			if (tDepth > maxDepth) maxDepth = tDepth;
		}
		return maxDepth+1;
	}
	
	public String label() {
		return this.label;
	}
	
	public int prole() {
		if (this.daughters==null) return 0;
		return this.daughters.length;
	}
	
	/**
	 * Return the height of the current TreeNodeLabel in the tree (0 if root)
	 */	
	public int height() {		
		int height = 0;
		TSNodeLabel TN = this;
		
		while(TN.parent!=null) {
			TN = TN.parent;
			height++;
		}
		return height;		
	}
	
	/**
	 * @return true if the current TreeNodeLabel is a unique daughter, 
	 * i.e. it's parent has only one daughter.
	 */
	public boolean isUniqueDaughter() {
		TSNodeLabel parent = this.parent;
		if (parent==null) return false;
		return (this.parent.daughters.length==1);
	}
	
	@SuppressWarnings("unused")
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String tStr = "( (S (NP-SBJ (NP (NNP Pierre) (NNP Vinken) ) (, ,)"
				+ " (ADJP (NP (CD 61) (NNS years) ) (JJ old) ) (, ,) )"
				+ " (VP (MD will) (VP (VB join) (NP (DT the) (NN board) )"
				+ " (PP-CLR (IN as) (NP (DT a) (JJ nonexecutive) (NN director) ))"
				+ " (NP-TMP (NNP Nov.) (CD 29) ))) (. .) ))";
		TSNodeLabel t = new TSNodeLabel(tStr);
	}
}

class Index {
	public int value;
	
	public Index(int val) {
		value = val;
	}
}
package LinguaView.syntax;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;

import LinguaView.syntax.CategoryObject.CoindexedObject;
import SyntaxUtils.CCGChart;
import SyntaxUtils.CCGParseResult;
import fig.basic.Pair;
/**
 * 
 * @author wsun, c.wang
 *
 */
public abstract class CCGNode {
	public static boolean HIDE_ADJUNCT_ARGUMENT = false;
	@Deprecated
	protected CCGInternalNode parent;
	// boolean terminal; // only in pre-terminal nodes this field could be true
	// in coordination sentence, the head is more than one
	// boolean co_indexed; //indicate whether the argument is co-indexed.
	// for debug
	public String parString;
	// for debug, label from Treebank, in form of "ID=wsj_XXXX.XX ..."
	public String source = null;
	public String additionalInfo = null;
	
	CategoryObject cat;
	protected int start;
	protected int end;
	
//	List<CCGTerminalNode> leaves;
	
	public int start(){
		return start;
	}
	
	public int end(){
		return end;
	}
	
	public String getSource(){
		if (source == null)
			return null;
		String[] ss = source.split("\\s");
		ss = ss[0].split("=");
		return ss[1];
	}

	// <pair<index-in-sent, index of slot>, set<head>>
	Map<Pair<Integer, Integer>, Set<Integer>> newlyFilledSlots; // newly filled
																// slots
	// coindexedObject in new Category, and map it to the unfilled slots.
	// <co-obj-in-this, pair<index-in-sent, index of slot>>
	Map<CoindexedObject, Set<Pair<Integer, Integer>>> unfilledSlots;
	public Set<Integer>[][] slots; // all slots. say slots[1][2] = {2, 3}, that means
							// the second slot of second node is filled with {2,
							// 3}, remember to get the start index

	public abstract boolean isTerminal();

	public abstract String toTreeString();
	
	public abstract String toIndentedTreeString(int treeDepth);
	
	public String toIndentedTreeString(){
		return toIndentedTreeString(0);
	}

	public static CCGNode getCCGNodeFromString(String parString) {
		return getCCGNodeFromString(parString, null);
	}

	public static CCGNode getCCGNodeFromString(String parString, String source) {
		ParenthesesBlockCCG p = ParenthesesBlockCCG
				.getParenthesesBlocks(parString);
		CCGNode c = getCCGNodeFromParenthesesBlockCCG(p, source);
		c.parString = parString;
		return c;
		// return (getCCGNodeFromParenthesesBlockCCG(p));
	}

	public static CCGNode getCCGNodeFromParenthesesBlockCCG(
			ParenthesesBlockCCG p, String source) {
		return getCCGNodeFromParenthesesBlockCCG(p, -1, source);
	}

	// Recursively get all the nodes
	// add an argument terminals terminals indicate how much children before
	private static CCGNode getCCGNodeFromParenthesesBlockCCG(
			ParenthesesBlockCCG p, int terminals, String source) {
		if (p.subBlocks.isEmpty())
			return new CCGTerminalNode(p.label.trim(), terminals + 1);
		CCGInternalNode intNode = new CCGInternalNode(p.label);
		intNode.start = terminals + 1;
		int i = 0;
		int end = terminals;
		for (ParenthesesBlockCCG subP : p.subBlocks) {
			CCGNode d = getCCGNodeFromParenthesesBlockCCG(subP, end, source);
			end = d.end;
			
			/*remove things like NP -> NP*/
			if (d instanceof CCGInternalNode){
				CCGInternalNode test = (CCGInternalNode) d;
				if (test.prole() == 1 && test.categoryToString().equals(test.children[0].categoryToString()))
					intNode.children[i] = test.children[0];
				else intNode.children[i] = d;
			}else 
				intNode.children[i] = d;
			
			d.parent = intNode;
			d.source = source;
			i++;
		}
		intNode.end = end;
		intNode.source = source;
		return intNode;
	}
	
	@SuppressWarnings("resource")
	@Deprecated
	/**
	 * Use CCGBankReader instead.
	 * @param inputFile
	 * @param encoding
	 * @return
	 */
	public static ArrayList<CCGNode> readCCGFile(File inputFile, String encoding)  {
		ArrayList<CCGNode> result = new ArrayList<CCGNode>();
		BufferedReader br = null;
//		Scanner scan = null;
		String cache = null;
		int count = 0;
		try {
			br = new BufferedReader(new InputStreamReader(new FileInputStream(inputFile), encoding));
			String s = br.readLine();
			while (s != null){
				s = s.trim();
				if (s.length() == 0){
					s = br.readLine();
					continue;
				}
				if (s.charAt(0) != '(') {
					cache = s;
					s = br.readLine();
					continue;
				}
				CCGNode c = getCCGNodeFromString(s, cache);
				c.source = cache;
				cache = null;
				result.add(c);
				
				count++;
				if (count % 100 == 0)
					System.out.print('.');
				
				s = br.readLine();
			}
//			scan = new Scanner(inputFile, encoding);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

//		while (scan.hasNextLine()) {
//			String line = scan.nextLine();
//			if (line.charAt(0) != '(') {
//				cache = line;
//				continue;
//			}
//			try{
//				CCGNode c = getCCGNodeFromString(line, cache);
//				c.source = cache;
//				cache = null;
//				result.add(c);
//				count++;
//				if (count % 100 == 0)
//					System.out.print('.');
//			}catch (Exception e){
//				System.err.println(line);
//				e.printStackTrace();
//			}
//		}
		return result;
	}

	@Deprecated
	public CCGInternalNode parent() {
		return parent;
	}

	public abstract CCGTerminalNode headTerm();
	
	
	public String categoryToString() {
		return cat.toString();
	}
	public String categoryWithNoFeature(){
		return cat.toStringWithoutFeature();
	}

	public CategoryObject category() {
		return cat;
	}

	public boolean isConjunct() {
		return cat._isConjunctConstituent;
	}

	@Deprecated
	public boolean isUniqueDaughter() {
		return this.parent.numOfChildren == 1;
	}

	public int countAllNodes() {
		int result = 1;
		if (this.isTerminal())
			return result;
		for (CCGNode n : ((CCGInternalNode) this).children) {
			result += n.countAllNodes();
		}
		return result;
	}

	public int countTerminalNodes() {
		int result = 0;
		if (this.isTerminal())
			return 1;
		for (CCGNode n : ((CCGInternalNode) this).children) {
			result += n.countTerminalNodes();
		}
		return result;
	}

	/**
	 * Returns a LinkedList of type CCGNode containing the lexical items of the
	 * current CCGNode.
	 */
	public List<CCGTerminalNode> collectTerminalNodes() {
		List<CCGTerminalNode> result = new ArrayList<CCGTerminalNode>();
		this.collectTerminalNodes(result);
		return result;
	}

	/**
	 * Method to implement collectTerminals()
	 * 
	 * @param terminals
	 */
	private void collectTerminalNodes(List<CCGTerminalNode> terminals) {
		if (this.isTerminal()) {
			terminals.add((CCGTerminalNode) this);
			return;
		}
		for (CCGNode n : ((CCGInternalNode) this).children) {
			n.collectTerminalNodes(terminals);
		}
	}

	/**
	 * Returns a LinkedList of type CCGNode containing the lexical items of the
	 * current CCGNode.
	 */
	public ArrayList<CCGInternalNode> collectNonTerminalNodes() {
		ArrayList<CCGInternalNode> result = new ArrayList<CCGInternalNode>();
		this.collectNonTerminalNodes(result);
		return result;
	}

	/**
	 * Method to implement collectTerminals()
	 * 
	 * @param nonTerminals
	 */
	private void collectNonTerminalNodes(List<CCGInternalNode> nonTerminals) {
		if (this.isTerminal()) {
			return;
		}
		nonTerminals.add((CCGInternalNode) this);
		for (CCGNode n : ((CCGInternalNode) this).children) {
			n.collectNonTerminalNodes(nonTerminals);
		}
	}

	/**
	 * Returns a LinkedList with all the terminal and non terminal nodes in the
	 * current CCGNode.
	 * 
	 * @return
	 */
	public List<CCGNode> collectAllNodes() {
		List<CCGNode> list = new ArrayList<CCGNode>();
		collectAllNodes(list);
		return list;
	}

	private void collectAllNodes(List<CCGNode> list) {
		list.add(this);
		if (this.isTerminal())
			return;
		for (CCGNode n : ((CCGInternalNode) this).children) {
			n.collectAllNodes(list);
		}
	}

	public ArrayList<CCGInternalNode> collectConjNodes() {
		ArrayList<CCGInternalNode> list = new ArrayList<CCGInternalNode>();
		collectConjNodes(list);
		return list;
	}

	private void collectConjNodes(ArrayList<CCGInternalNode> list) {
		if (cat._isConjunctConstituent)
			list.add((CCGInternalNode) this);
		if (this.isTerminal())
			return;
		for (CCGNode n : ((CCGInternalNode) this).children) {
			n.collectConjNodes(list);
		}
	}
	

	public int maxDepth() {
		if (this.isTerminal())
			return 0;
		int maxDepth = 0;
		for (CCGNode n : ((CCGInternalNode) this).children) {
			int increase = 1;
			int depth = increase + n.maxDepth();
			if (depth > maxDepth)
				maxDepth = depth;
		}
		return maxDepth;
	}

	/**
	 * Return the hight of the current CCGNode in the tree
	 * 
	 * @return an integer corresponding to the hight of the current TreeNode
	 */
	@Deprecated
	public int height() {
		int height = 0;
		CCGNode n = this;
		while (n.parent != null) {
			n = n.parent;
			height++;
		}
		return height;
	}

	@Deprecated
	public boolean isHead() {
		return parent != null && this == parent.children[parent.headChild];
	}

	@Deprecated
	public boolean isRoot() {
		return this.parent == null;
	}
	
	//a CCGTree from derivation may have no parent
	public void updateParent(){
		if (this instanceof CCGTerminalNode)
			return;
		Stack<CCGInternalNode> parents = new Stack<CCGInternalNode>();
		parents.push((CCGInternalNode)this);
		while(parents.size() > 0){
			CCGInternalNode next = parents.pop();
			for (int i = 0; i < next.numOfChildren; ++i){
				if (next.children[i] != null){
					next.children[i].parent = next;
					if (next.children[i] instanceof CCGInternalNode)
						parents.push((CCGInternalNode)next.children[i]);
				}
			}
		}
	}

	public static String toMSTUlab(CCGNode node) {
		// word, postag, supertag, head_word
		node.updateParent();
		String words = "";
		String poss = "";
		String cat = "";
		String indexes = "";
		List<CCGTerminalNode> terminals = node.collectTerminalNodes();
		if (terminals.size() == 1) {
			CCGTerminalNode n = terminals.get(0);
			words = n.word;
			poss = n.orig_POS;
			cat = n.cat.toString(); // modified 04/19
			indexes = "0";
		} else {
			for (CCGTerminalNode leaf : terminals) {
				String dependentCat = leaf.cat.toString(); // modified 04/19
				words += leaf.word + "\t";
				poss += leaf.orig_POS + "\t";
				cat += dependentCat + "\t";
				CCGInternalNode ancestor = leaf.parent;
				if (leaf.isHead()) {
					while (ancestor.isHead() && !ancestor.isRoot())
						ancestor = ancestor.parent;
					if (ancestor.isRoot()) {
						indexes += 0 + "\t";
						continue;
					}
					ancestor = ancestor.parent;
				}
				CCGNode head = ancestor.getAnchorThroughPercolation();
				int indexHead = terminals.indexOf(head) + 1;
				indexes += indexHead + "\t";
			}
		}
		String result = "";
		String[] resultArray = new String[] { words, poss, cat, indexes };
		for (int i = 0; i < resultArray.length; i++) {
			result += resultArray[i].trim() + "\n";
		}
		return result;
	}

	public static String toCoNLL(CCGNode node) {
		// word, postag, supertag, head_word
		node.updateParent();
		StringBuffer result = new StringBuffer();
//		String result = "";
		List<CCGTerminalNode> terminals = node.collectTerminalNodes();
		if (terminals.size() == 1) {
			CCGTerminalNode n = terminals.get(0);
			result.append(1 + "\t");
			result.append(n.word + "\t");
			result.append(n.word + "\t");
			result.append(n.orig_POS + "\t");
			result.append(n.orig_POS + "\t");
			result.append("_\t");
			result.append(0 + "\t");
			result.append(n.cat.toString() + "\n");
		} else {
			int i = 0;
			for (CCGTerminalNode leaf : terminals) {
				++i;
				CCGInternalNode ancestor = leaf.parent;
				int indexHead = -1;
				if (leaf.isHead()) {
					while (ancestor.isHead() && !ancestor.isRoot())
						ancestor = ancestor.parent;
					if (ancestor.isRoot())
						indexHead = 0;
					else
						ancestor = ancestor.parent;
				}
				if (indexHead == -1) {
					CCGNode head = ancestor.getAnchorThroughPercolation();
					indexHead = terminals.indexOf(head) + 1;
				}

				result.append(i + "\t");
				result.append(leaf.word + "\t");
				result.append(leaf.word + "\t");
				result.append(leaf.orig_POS + "\t");
				result.append(leaf.orig_POS + "\t");
				result.append("_\t");
				result.append(indexHead + "\t");
				result.append(leaf.cat.toString() + "\n");
			}
		}
		return result.toString();
	}

	public static void toMSTUlab(File inputFile, File outputFile,
			String encoding) {
		ArrayList<CCGNode> treebank = CCGNode.readCCGFile(inputFile, encoding);
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(outputFile, encoding);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (CCGNode n : treebank) {
			pw.println(CCGNode.toMSTUlab(n));
		}
		pw.close();
	}

	public static void toCoNLL(File inputFile, File outputFile, String encoding) {
		ArrayList<CCGNode> treebank = CCGNode.readCCGFile(inputFile, encoding);
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(outputFile, encoding);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (CCGNode n : treebank) {
			pw.println(CCGNode.toCoNLL(n));
		}
		pw.close();
	}
	
	public static void toCoNLL08(File inputFile, File outputFile, String encoding){
		ArrayList<CCGNode> treebank = CCGNode.readCCGFile(inputFile, encoding);
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(outputFile, encoding);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (CCGNode n : treebank) {
			pw.println(CCGNode.toCoNLL08(n));
		}
		pw.close();
	}
	
	@SuppressWarnings("unchecked")
	public static String toCoNLL08(CCGNode cn){
		StringBuffer sb = new StringBuffer();
		cn.updateParent();
//		cn.fillSlots();
		//TODO
		List<Integer> hasArgColumns = new ArrayList<Integer>();
		if (cn.slots == null)
			cn.slots = new Set[0][0];
		
		for(int i = 0; i < cn.slots.length; ++i){
			if (cn.slots[i] == null)
				continue;
			for (int j=0; j<cn.slots[i].length; ++j){
				if (cn.slots[i][j] != null && cn.slots[i][j].size() >0){
					hasArgColumns.add(i);
					break;
				}
			}
		}
		
		// word, postag, supertag, head_word
		List<CCGTerminalNode> terminals = cn.collectTerminalNodes();
		if (terminals.size() == 1) {
			CCGTerminalNode n = terminals.get(0);
			sb.append(1 + "\t");
			sb.append(n.word + "\t");
			sb.append(n.word + "\t");
			sb.append(n.orig_POS + "\t");
			sb.append(n.mod_POS + "\t");
			sb.append(n.word + "\t");
			sb.append(n.word + "\t");
			sb.append(n.categoryToString() + "\t");
			sb.append("_\t");
			sb.append(0 + "\t");
		} else {
			int i = 0;
			for (CCGTerminalNode leaf : terminals) {
				CCGInternalNode ancestor = leaf.parent;
				int indexHead = -1;
				if (leaf.isHead()) {
					while (ancestor.isHead() && !ancestor.isRoot())
						ancestor = ancestor.parent;
					if (ancestor.isRoot())
						indexHead = 0;
					else
						ancestor = ancestor.parent;
				}
				if (indexHead == -1) {
					CCGNode head = ancestor.getAnchorThroughPercolation();
					indexHead = terminals.indexOf(head) + 1;
				}

				sb.append((i + 1) + "\t");
				sb.append(leaf.word + "\t");
				sb.append(leaf.word + "\t");
				sb.append(leaf.orig_POS + "\t");
				sb.append(leaf.mod_POS + "\t");
				sb.append(leaf.word + "\t");
				sb.append(leaf.word + "\t");
				sb.append(leaf.cat.toString() + "\t");
				sb.append(indexHead + "\t");
				sb.append("_\t"); //NMOD
				if (hasArgColumns.contains(i))
					sb.append(leaf.word);
				else 
					sb.append("_");
				for (int j =0; j < hasArgColumns.size(); ++j){
					int pred = hasArgColumns.get(j);
					
					if (pred == i){
						sb.append("\t_"); 
						// should not be argument of it self even if bug exists
						continue;
					}
					
					//Bug is not fixed,so it is possible that simultaneously fill more than one slots of an predeicate??
					List<Integer> ordOfSlots = new ArrayList<Integer>();
					
					for(int k = 0; k < cn.slots[pred].length; ++k)
						if (cn.slots[pred][k] != null && cn.slots[pred][k].contains(i))
							ordOfSlots.add(k + 1);
					if (ordOfSlots.size() ==0)
						sb.append("\t_");
					
					else{// if(ordOfSlots.size() == 1)
						sb.append("\t" + ordOfSlots.get(0));
						for (int k = 1; k< ordOfSlots.size(); ++k)
							sb.append("\t" + ordOfSlots.get(k));
					}
				}
				sb.append("\n");
				
				++i;
			}
		}
		

		
		return sb.toString();
	}

	public static void toPTBTree(File inputFile, File outputFile,
			String encoding) {
		ArrayList<CCGNode> ccgtrees = CCGNode.readCCGFile(inputFile, encoding);
		PrintWriter pw = null;

		try {
			pw = new PrintWriter(outputFile, encoding);
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}

		for (CCGNode ccgtree : ccgtrees) {
			pw.println("(" + ccgtree.toTreeString() + ")");
		}

		pw.close();
	}

	public static void hasNonMinimalHeads(File inputFile, String encoding) {
		ArrayList<CCGNode> treebank = CCGNode.readCCGFile(inputFile, encoding);
		int i = 0;
		for (CCGNode t : treebank) {
			i++;
			t.hasNonMinimalHeads(false, i);
		}
	}

	public void hasNonMinimalHeads(boolean countPunctuation, int sentenceNumber) {
		List<CCGInternalNode> allNodes = this.collectNonTerminalNodes();
		List<CCGTerminalNode> allTerminals = this.collectTerminalNodes();
		IdentityHashMap<CCGTerminalNode, Integer> hights = new IdentityHashMap<CCGTerminalNode, Integer>();
		for (CCGTerminalNode t : allTerminals) {
			hights.put(t, t.height());
		}
		for (CCGInternalNode n : allNodes) {
			CCGTerminalNode terminalHead = n.getAnchorThroughPercolation();
			int terminalHeadHight = hights.get(terminalHead);
			List<CCGTerminalNode> terminals = n.collectTerminalNodes();
			CCGTerminalNode minimalHead = terminalHead;
			int minimalHight = terminalHeadHight;
			for (CCGTerminalNode t : terminals) {
				if (t == terminalHead)
					continue;
				if (!countPunctuation && t.isPunctuation())
					continue;
				int tHight = hights.get(t);
				if (tHight < minimalHight) {
					minimalHight = tHight;
					minimalHead = t;
				}
			}
			if (minimalHead != terminalHead) {
				System.out.println(sentenceNumber + ":\t" + this.toString()
						+ "\n" + "\tParent: " + n.cat.toString()
						+ "  Current Head: " + terminalHead.word
						+ "  Min Head: " + minimalHead.word);
			}
		}
	}

	public String toString() {
		return "<" + cat + ">";
	}
	
	public abstract String toStringWithAdditionalField();

	@SuppressWarnings("unused")
	public void collectHead() {
		Map<CoindexedObject, Set<Integer>> collectedHead;
	}
//	
	public void fillSlots(){
		if (slots == null){
			CCGParseResult pr = CCGChart.goldParse(this);
			if (pr != null)
				slots = pr.dependency;
		}
	}
	
	public Set<Integer> collectNewInactiveNodes(){
		if (isTerminal())
			return Collections.<Integer>emptySet();
		Set<Integer> nodes = new HashSet<Integer>();
		for (Pair<Integer, Integer> p: newlyFilledSlots.keySet()){
			int index = p.getFirst();
			if (nodes.contains(index))
				continue;
			boolean b = true;
			for (int j = 0; j<slots[index - start].length; ++j){
				if (slots[index - start][j] == null){
					b = false;
					break;
				}
			}
			if (b)
				nodes.add(index);
		}
		return nodes;
	}
	
	public CCGTerminalNode getTerminalNode(int index){
		if (index < start || index > end)
			throw new IllegalArgumentException("Try to get nodes out of the tree");
		if (this instanceof CCGTerminalNode)
			return (CCGTerminalNode)this;
		CCGInternalNode node = (CCGInternalNode)this;
		if (index > node.children[0].end())
			return node.children[1].getTerminalNode(index);
		else 
			return node.children[0].getTerminalNode(index);
	}
	
	public CCGTerminalNode getRightmostTerm() {
		return getTerminalNode(end);
	}

	public CCGTerminalNode getLeftmostTerm() {
		return getTerminalNode(start);
	}
	
	public String dependencyToHtml(){
		if (slots == null)
			fillSlots();
		if (slots == null)
			return "Fail to discover dependency";
		//TODO
		List<CCGTerminalNode> terms = collectTerminalNodes();
		StringBuffer sb = new StringBuffer();

		sb.append("<table cellpadding=\"10\">\n");

		for (int i = 0; i < slots.length; ++i) {
			Object[] colors = {"<font color=\"firebrick\">", "</font>", "<font color=\"mediumblue\">", "</font>", "<font color=\"green\">", "</font>", "<font color=\"orchid\">", "</font>" ,"<font color=\"mediumblue\">", "</font>", "<font color=\"green\">", "</font>", "<font color=\"orchid\">", "</font>"};
			sb.append("<tr>\n");
			sb.append("<td>");
			sb.append((i+1));
			sb.append("</td>\n");
			sb.append("<td>");
			sb.append(String.format("<b>%s</b>\n", terms.get(i).word()));
			sb.append("</td>\n");
			sb.append("<td>");
			sb.append(String.format("<i>%s</i>\n", String.format(terms.get(i).category().toColoredString(), colors)));
			sb.append("</td>\n");
			for (int j = 0; j < slots[i].length; ++j) {
				sb.append("<td>");
				if (slots[i][j] == null)
					sb.append(String.format(
							"<font color=\"FF0000\"><b>%s</b></font>", "null"));
				else {
					sb.append(colors[(j%4)*2]);
					List<String> heads = new ArrayList<String>();
					for (int z : slots[i][j])
						heads.add(terms.get(z).word());
					sb.append(heads.get(0));
					for (int z = 1; z < heads.size(); ++z)
						sb.append(", " + heads.get(z));
					sb.append(colors[(j%4)*2+1]);
				}
				sb.append("</td>\n");
			}
			sb.append("</tr>\n");
		}
		sb.append("</table>\n");
		return sb.toString();
	}
	
	/**
	 * start must be 0
	 * @param result
	 */
	public static String toHTML(CCGNode result){
		if (result.start != 0)
			return null;
		//TODO
//		if (result.slots == null)
//			result.fillSlots();
		
		StringBuffer sb = new StringBuffer();
		if (result.source != null) {
			String ss[] = result.source.split("\\s");
			sb.append("<br><h3>" + ss[0].substring(3) + "</h3>\n");
		}
		sb.append("<pre>" + result.toIndentedTreeString() + "</pre>\n");
		sb.append(result.dependencyToHtml());
		return sb.toString();
	}
	
	public static void main(String[] args) {
		CCGNode c = getCCGNodeFromString(
				"(<T S[dcl] 0 2> (<T S[dcl] 1 2> (<L S/S NN NN 下面 S/S>) " +
				"(<T S[dcl] 0 1> (<T S[dcl]\\NP 0 2> (<L (S[dcl]\\NP)/(S[dcl]\\NP)" +
				" VV VV 请 (S[dcl]\\NP)/(S[dcl]\\NP)>) (<T S[dcl]\\NP 0 2> (<L " +
				"(S[dcl]\\NP)/NP VV VV 听 (S[dcl]\\NP)/NP>) (<T NP 1 2> (<T NP/NP" +
				" 1 2> (<T S[dcl]/NP 1 2> (<T S/(S\\NP) 0 1> (<T NP 1 2> (<T NP 1 2>" +
				" (<T NP/NP 1 2> (<T (NP/NP)/(NP/NP) 1 2> (<L NP NR NR 美国 NP>) " +
				"(<L ((NP/NP)/(NP/NP))\\NP DEG DEG 之 ((NP/NP)/(NP/NP))\\NP>) )" +
				" (<L NP/NP NN NN 音 NP/NP>) ) (<L NP NN NN 记者 NP>) ) (<T NP 1 2>" +
				" (<L NP/NP NR NR 米克 NP/NP>) (<L NP NR NR 阿尔巴斯特 NP>) ) ) ) " +
				"(<T (S[dcl]\\NP)/NP 1 2> (<T (S\\NP)/(S\\NP) 0 2>" +
				" (<L ((S\\NP)/(S\\NP))/NP P P 从 ((S\\NP)/(S\\NP))/NP>)" +
				" (<L NP NR NR 奥斯路 NP>) ) (<L (S[dcl]\\NP)/NP VV VV 发来 " +
				"(S[dcl]\\NP)/NP>) ) ) (<L (NP/NP)\\(S[dcl]/NP) DEC DEC 的 " +
				"(NP/NP)\\(S[dcl]/NP)>) ) (<L NP NN NN 报道 NP>) ) ) ) ) ) " +
				"(<L . PU PU 。 .>))" ,
				"test");
		System.out.println(c.toIndentedTreeString());
//		System.out.println(toCoNLL(c));
//		System.out.println();
//		System.out.println(toCoNLL08(c));
		System.out.println(c.getTerminalNode(2).categoryWithNoFeature());
		System.out.println(c.getTerminalNode(5).cat.removeModifier());
//		CategoryObject cat = new CategoryObject("(S[to]/NP)\\(S[b]/NP)", false);
		CategoryObject cat = CategoryObject.fromPlainCat("((S/NP)\\(S/NP))/((S/NP)\\(S/NP))");
		System.out.println(cat.removeModifier());
//		for (CCGNode cn: c.findPath(2, 4))
//			System.out.println(cn.categoryToString());
		// toCoNLL(new File("/tmp/1"), new File("/tmp/2"), "UTF-8");
		// toCoNLL(new File("/home/wsun/proj/cccg/AUTO/dev"), new
		// File("/home/wsun/proj/cccg/AUTO/dev.conll"), "UTF-8");
		// toCoNLL(new File("/home/wsun/proj/cccg/AUTO/trn"), new
		// File("/home/wsun/proj/cccg/AUTO/trn.conll"), "UTF-8");
		/*
		 * try { File inputFile = new File(args[0]); File outputFile = new
		 * File(args[1]); toMSTUlab(inputFile, outputFile, "UTF-8"); } catch
		 * (Exception e) { e.printStackTrace(); System.exit(-1); }
		 */
		System.out.println("Done!");
		System.out.println(c.getTerminalNode(5));
	}
}

package LinguaView.syntax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.Set;

import LinguaView.syntax.CategoryObject.CoindexedObject;
import SyntaxUtils.*;
import fig.basic.Pair;

/**
 * <T CCGcat head dtrs> For non-leaf nodes, the description contains the
 * following three fields: the category of the node CCGcat, the index of its
 * head daughter head (0 = left or only daughter, 1 = right daughter), and the
 * number of its children. NB. CCG derivation is binarized.
 * 
 * @author wsun, c.wang
 */
public class CCGInternalNode extends CCGNode {

	public int headChild; // the result is the head, except NP/N
	// if the grammar was forward*, then the head is the left one
	// vice versa
	int numOfChildren;
	CCGNode[] children;

	// Map<Pair<Integer, Integer>, Set<Integer>> newlyFilledSlots; // newly
	// filled
	// Map<CoindexedObject, Set<Pair<Integer, Integer>>> unfilledSlots;
	// Set<Integer>[][] slots; // all slots. say slots[1][2] = {2, 3}, that
	// means

	public CCGInternalNode(String label) {
		// <T CCGcat head dtrs [additionalInfo]>
		label = label.substring(3, label.length() - 1);
		// CCGcat head dtrs [additionalInfo]
		String[] labelSplit = label.split("\\s+");
		this.cat = CategoryObject.fromPlainCat(labelSplit[0]);
		this.headChild = Integer.parseInt(labelSplit[1]);
		this.numOfChildren = Integer.parseInt(labelSplit[2]);
		this.children = new CCGNode[numOfChildren];
		
		if (labelSplit.length > 3)
			this.additionalInfo = labelSplit[3];
	}

	private CCGInternalNode() {
	}

	@SuppressWarnings("unchecked")
	public static CCGInternalNode generateNewNode(CCGBinaryRule rule,
			CCGNode left, CCGNode right) {
		CCGInternalNode result = new CCGInternalNode();
		Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
		result.cat = rule.perform(left.category(), right.category(), indices);
		if (result.cat == null)
			return null;
		
		result.children = new CCGNode[2];
		result.children[0] = left;
//		left.parent = result;
		result.children[1] = right;
//		right.parent = result;
		
		result.numOfChildren = 2;
		result.headChild = rule.headChild;
		if (left.end + 1 != right.start)
			throw new IllegalArgumentException("left is not next to right");
		result.start = left.start;
		result.end = right.end;
		
//		result.leaves = new ArrayList<CCGTerminalNode>(left.leaves);
//		result.leaves.addAll(right.leaves);

		result.parString = left.parString;
		result.source = left.source;

		result.unfilledSlots = new HashMap<CoindexedObject, Set<Pair<Integer, Integer>>>();
		result.slots = new Set[result.end - result.start + 1][];
		result.newlyFilledSlots = new HashMap<Pair<Integer, Integer>, Set<Integer>>();

		for (int i = 0; i < left.slots.length; ++i){
			result.slots[i] = new Set[left.slots[i].length]; 
			for (int j = 0; j<left.slots[i].length; ++j)
				if (left.slots[i][j] != null)
					result.slots[i][j] = new HashSet<Integer>(left.slots[i][j]);
		}
		for (int i = 0; i < right.slots.length; ++i){
			result.slots[i + left.slots.length] = new Set[right.slots[i].length]; 
			for (int j = 0; j< right.slots[i].length; ++j)
				if (right.slots[i][j] != null)
					result.slots[i + left.slots.length][j] = new HashSet<Integer>(right.slots[i][j]);
		}

		// transfer some of unfilled Slots to newlyFilledSlot
		// XXX we assume a slot would be filled twice
		for (Entry<CoindexedObject, CoindexedObject> e : indices.entrySet()) {
			CoindexedObject cHead = e.getKey(); // c in child nodes
			CoindexedObject pHead = e.getValue();
			Map<CoindexedObject, Set<Pair<Integer, Integer>>> m;
			// appear at the same time (impossible)
			// appear at neither ()
			if (left.unfilledSlots.containsKey(cHead))
				m = left.unfilledSlots;
			else if (right.unfilledSlots.containsKey(cHead))
				m = right.unfilledSlots;
			else
				continue;

			if (pHead.head().size()>0) {
				if (cHead.head().size() == 0) {
					Set<Pair<Integer, Integer>> toFill = m.get(cHead);
					Set<Integer> heads = pHead.head();
					for (Pair<Integer, Integer> p : toFill) {
						result.newlyFilledSlots.put(p, heads);
						result.slots[p.getFirst() - result.start][p.getSecond()] = heads;
					}
				}
			} else {
				Set<Pair<Integer, Integer>> unfilled = result.unfilledSlots
						.get(pHead);
				unfilled = unfilled == null ? new HashSet<Pair<Integer, Integer>>()
						: unfilled;
				unfilled.addAll(m.get(cHead));
				result.unfilledSlots.put(pHead, unfilled);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	public static CCGInternalNode generateNewNode(CCGUnaryRule rule,
			CCGNode child) {
		CCGInternalNode result = new CCGInternalNode();
		Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
		result.cat = rule.perform(child.category(), indices);
		if (result.cat == null)
			return null;
		
		result.children = new CCGNode[1];
		result.children[0] = child;
//		child.parent = result;
		
		result.numOfChildren = 1;
		result.headChild = 0;
		result.start = child.start;
		result.end = child.end;

		result.parString = child.parString;
		result.source = child.source;

		result.unfilledSlots = new HashMap<CoindexedObject, Set<Pair<Integer, Integer>>>();
		result.slots = new Set[result.end - result.start + 1][];
		result.newlyFilledSlots = new HashMap<Pair<Integer, Integer>, Set<Integer>>();

		for (int i = 0; i < child.slots.length; ++i){
			result.slots[i] = new Set[child.slots[i].length]; 
			for (int j = 0; j<child.slots[i].length; ++j)
				if (child.slots[i][j] != null)
					result.slots[i][j] = new HashSet<Integer>(child.slots[i][j]);
		}

		// transfer some of unfilled Slots to newlyFilledSlot
		// XXX we assume a slot would be filled twice
		for (Entry<CoindexedObject, CoindexedObject> e : indices.entrySet()) {
			CoindexedObject cHead = e.getKey(); // c in child nodes
			CoindexedObject pHead = e.getValue();
			// appear at the same time (impossible)
			// appear at neither ()
			if (!child.unfilledSlots.containsKey(cHead))
				continue;

			// no slots will be filled
			Set<Pair<Integer, Integer>> unfilled = result.unfilledSlots
					.get(pHead);
			unfilled = unfilled == null ? new HashSet<Pair<Integer, Integer>>()
					: unfilled;
			unfilled.addAll(child.unfilledSlots.get(cHead));
			result.unfilledSlots.put(pHead, unfilled);
		}
		return result;
	}

	public boolean isTerminal() {
		return false;
	}
	
	/**
	 * pred and arg are 0-based.
	 * @param pred
	 * @param arg 
	 * @return integer in pair indicate the index of ancestor, 0-based; list contains a series of CCGNode in turn
	 */
	public Pair<Integer, List<CCGNode>> findPath(int pred, int arg){
		List<CCGNode> path = new ArrayList<CCGNode>();
		if (pred > end || pred < start || arg > end || arg < start)
			throw new IllegalArgumentException("Pred or arg not in this node!");
		if (pred == arg)
			return null; // TODO raise an exception if the co-index problem is solved
		
		CCGInternalNode ancestor = this;
		while(true){
			if(pred <= ancestor.children[0].end && arg <= ancestor.children[0].end)
				ancestor = (CCGInternalNode)ancestor.children[0]; 
			else if(pred > ancestor.children[0].end && arg > ancestor.children[0].end)
				ancestor = (CCGInternalNode)ancestor.children[1]; 
			else
				break;
		}
		
		Stack<CCGNode> ancestor2pred = new Stack<CCGNode>();
		
		CCGNode temp = ancestor;
		while(temp instanceof CCGInternalNode){
			CCGInternalNode _temp = (CCGInternalNode) temp;
			if(pred > _temp.children[0].end)
				temp = _temp.children[1];
			else temp = _temp.children[0];
			ancestor2pred.push(temp);
		}
		while (ancestor2pred.size()>0)
			path.add(ancestor2pred.pop());
		
		int i_ancestor = path.size();
		path.add(ancestor);
		
		temp = ancestor;
		while(temp instanceof CCGInternalNode){
			CCGInternalNode _temp = (CCGInternalNode) temp;
			if(arg > _temp.children[0].end)
				temp = _temp.children[1];
			else temp = _temp.children[0];
			path.add(temp);
		}
		return new Pair<Integer, List<CCGNode>>(i_ancestor, path);
//		List<CCGTerminalNode> terms = collectTerminalNodes();
//		CCGTerminalNode predNode = terms.get(pred - start);
//		CCGTerminalNode argNode = terms.get(arg - start);
//
////		CCGNode ancestor = predNode.parent;
//		path.add(predNode);
//		// if null pointer exception happened, must be error of parent
//		while (ancestor.start > arg || ancestor.end < arg){
//			path.add(ancestor);
//			ancestor = ancestor.parent;
//		}
//		path.add(ancestor);
//		
//		Stack<CCGNode> argHalf = new Stack<CCGNode>();
//		argHalf.push(argNode);
//		CCGNode next = argNode.parent;
//		while (next != ancestor){
//			argHalf.push(next);
//			next = next.parent;
//		}
//		while(!argHalf.isEmpty())
//			path.add(argHalf.pop());
//		return path;
	}

	public String toTreeString() {
		String str = "(" + cat + " ";
		for (CCGNode dtr : children) {
			str += dtr.toTreeString();
		}
		str += ")";
		return str;
	}
	
	@Override
	public String toIndentedTreeString(int treeDepth){
		StringBuffer sb = new StringBuffer();
		sb.append("(" + cat + " ");
		sb.append(children[0].toIndentedTreeString(treeDepth+2+cat.toString().length()));
		if (children.length ==2){
//		for (CCGNode dtr : children) {
			sb.append("\n");
			for (int i = 0; i< treeDepth + cat.toString().length() + 2; ++i)
				sb.append(" ");
			sb.append(children[1].toIndentedTreeString(treeDepth+2+cat.toString().length()));
		}
		sb.append(")");
		return sb.toString();
	}

	public int prole() {
		return numOfChildren;
	}

	public String toString() {
		String result = "(<T " + cat + " " + headChild + " " + numOfChildren
				+ "> ";
		for (CCGNode n : children) {
			result += n.toString() + " ";
		}
		result += ")";
		return result;
	}
	
	public String toStringWithAdditionalField(){
		StringBuffer sb = new StringBuffer("(<T " + cat + " " + headChild + " " + numOfChildren );
		if (additionalInfo != null)
			sb.append(" " + additionalInfo);
		sb.append("> ");
		for (CCGNode n : children) {
			sb.append(n.toStringWithAdditionalField() + " ");
		}
		sb.append(")");
		return sb.toString();
	}

	public CCGNode[] daughters() {
		return children;
	}
	
	@Override
	public CCGTerminalNode headTerm(){
		CCGNode next = children[headChild];
		while (!(next instanceof CCGTerminalNode)){
			CCGInternalNode nn = (CCGInternalNode) next;
			next = nn.children[nn.headChild];
		}
		return (CCGTerminalNode)next;
	}

	public CCGNode getHeadDaughter() {
		return this.children[this.headChild];
	}

	public CCGTerminalNode getAnchorThroughPercolation() {
		CCGNode headDaughter = getHeadDaughter();
		if (headDaughter.isTerminal())
			return (CCGTerminalNode) headDaughter;
		return ((CCGInternalNode) headDaughter).getAnchorThroughPercolation();
	}
	
	
	public static void main(String[] args){
		CCGNode c = getCCGNodeFromString(
				"(<T S[dcl] 0 2> (<T S[dcl] 1 2> (<T NP 1 2>"
						+ " (<L NP[nb]/N PRP$ PRP$ Their NP[nb]_132/N_132>) (<L N NN NN effort N>) )"
						+ " (<T S[dcl]\\NP 0 2> (<L (S[dcl]\\NP)/(S[pt]\\NP) VBZ VBZ has "
						+ "(S[dcl]\\NP_80)/(S[pt]_81\\NP_80:B)_81>) (<T S[pt]\\NP 0 2> (<T (S[pt]\\NP)/PP 0 2> "
						+ "(<L ((S[pt]\\NP)/PP)/NP VBN VBN received ((S[pt]\\NP_90)/PP_91)/NP_92>) (<T NP 1 2> "
						+ "(<L NP[nb]/N DT DT a NP[nb]_106/N_106>) (<T N 1 2> (<L N/N JJ JJ lukewarm N_101/N_101>)"
						+ " (<L N NN NN response N>) ) ) ) (<T PP 0 2> (<L PP/NP IN IN from PP/NP_111>) (<T NP 1 2> "
						+ "(<L NP[nb]/N DT DT the NP[nb]_125/N_125>) (<T N 1 2> (<L N/N NNP NNP Justice N_120/N_120>)"
						+ " (<L N NNP NNP Department N>) ) ) ) ) ) ) (<L . . . . .>) )",
				"test");
		Pair<Integer, List<CCGNode>> p = ((CCGInternalNode)c).findPath(2, 5);
		System.out.println(p.getFirst());
		for (CCGNode cxx: p.getSecond())
			System.out.println(cxx + "\n");
	}
//	public static void main(String[] args){
//		CCGGrammar gr = CCGGrammar.createNewGrammar();
//		Lexicon lex = new Lexicon();
//		CCGBinaryRule bRule = new CCGBinaryRule("S/NP", "NP/NP", "S/NP", 0);
//		CCGUnaryRule uRule = new CCGUnaryRule("N", "NP");
//		CCGBinaryRule bRule2 = new CCGBinaryRule("S/NP", "NP", "S", 1);
//		gr.increSeenCount("S/NP", "NP/NP", "S/NP", 0);
//		gr.increSeenCount("S/NP", "NP", "S", 1);
//		gr.increSeenCount("N", "NP");
//
//		CCGTerminalNode c1 = new CCGTerminalNode("eat", "VP", "S/NP_1", 5);
//		CCGTerminalNode c2 = new CCGTerminalNode("delicious", "ADJ", "NP_9/NP_9", 6);
//		CCGTerminalNode c3 = new CCGTerminalNode("apples", "N", "N", 7);
//		CCGInternalNode d1 = generateNewNode(bRule, c1, c2);
//		CCGInternalNode d2 = generateNewNode(uRule, c3);
//		CCGInternalNode d3 = generateNewNode(bRule2, d1, d2);
//		
//		lex.incrSeenCount("eat", c1.category().toPredArgCat(), 1);
//		lex.incrSeenCount("delicious", c2.category().toPredArgCat(), 1);
//		lex.incrSeenCount("apples", c3.category().toPredArgCat(), 1);
//		
//		System.out.println(d1);
//		System.out.println(d3);
		
//		CCGChart chart = new CCGChart(d3, null, null, null, gr, null);
		
//		List acts = CCGChart.getActionSequence(d3);
//		for (Object o: acts)
//			System.out.println(o);
		
//		Sentence s = Sentence.getSentenceForParsing(d3);
//		System.out.println(s);
		
//		SentenceForCCGParsing s = new SentenceForCCGParsing(c);
		
//	}
}

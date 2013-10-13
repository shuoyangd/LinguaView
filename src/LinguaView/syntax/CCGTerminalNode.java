package LinguaView.syntax;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.ardverk.collection.PatriciaTrie;

import LinguaView.syntax.CategoryObject.CoindexedObject;
import fig.basic.Pair;

/**
 * <L CCGcategorymod_POS-tag orig_POS-tag word PredArgCat> The original POS tag is
 * the tag assigned to this word in the PennTreebank. The modified POS tag might
 * differ from this tag if it was changed during the translation to CCG.
 * PredArgcategoryis another representation of the lexical category (CCGcat) which
 * encodes the underlying predicate-argument structure
 *  
 *   @author wsun, c.wang
 */
public class CCGTerminalNode extends CCGNode {

	String mod_POS, orig_POS;
	String word;
	String predArgCat; // indicate the dependency
	String markup;

	static private PatriciaTrie<String, String> _interpretation = null;
	public static void setInterpret(PatriciaTrie<String, String> interpretation){
		_interpretation = interpretation;
	}
	
	// no slots would be filled in terminal node. so just leave it;
//	CoindexedObject slots[];
	
//	int[] dependency = null;

	// from Treebank
	public CCGTerminalNode(String label, int index) {
		// <L CCGcategorymod_POS-tag orig_POS-tag word PredArgCat>
		label = label.substring(3, label.length() - 1);
		// CCGcategory mod_POS-tag orig_POS-tag word PredArgCat
		String[] labelSplit = label.split("\\s+");
		this.mod_POS = labelSplit[1];
		this.orig_POS = labelSplit[2];
		this.word = labelSplit[3];
		this.predArgCat = labelSplit[4];
		
		if (labelSplit.length > 5)
			this.additionalInfo = labelSplit[5];

		if (_interpretation != null){
			CategoryObject temp =CategoryObject.fromPlainCat(labelSplit[0]);
			this.markup = _interpretation.get(temp.toString());
			if (markup != null){
				cat = CategoryObject.fromCACCats(markup);
				cat.head(index);
			}
			else {
				cat = CategoryObject.fromPredArgCat(labelSplit[4]);
				cat.head(index);
//				cat = temp;
			}
		} else {
			cat = CategoryObject.fromPredArgCat(labelSplit[4]);
			cat.head(index);
		}
		index(index);
	}
	
	// the following come from the lexicon
	// if we need parString and source
	@SuppressWarnings("unchecked")
	public CCGTerminalNode(String word, String POS, String markup, String additionalInfro, int index){
		this.word = word;
		this.orig_POS = POS;
		this.mod_POS = POS;
		this.predArgCat = markup;
		this.additionalInfo = additionalInfro;
		cat = CategoryObject.fromCACCats(markup);
		index(index);
		cat.head(index);
		slots = new Set[1][cat._slots.length];
		
		newlyFilledSlots = Collections.<Pair<Integer, Integer>, Set<Integer>>emptyMap();
		unfilledSlots = new HashMap<CoindexedObject, Set<Pair<Integer, Integer>>>();
		
//		CategoryObject next = cat;
//		int i= slots.length - 1;
		for (int i = 0; i <slots[0].length ; ++i){
			Pair<Integer, Integer> p = new Pair<Integer, Integer>(index, i);
			CoindexedObject head = cat._slots[i]._headObj;
//			CoindexedObject head = next._argument.headObj;
			
			Set<Pair<Integer, Integer>> sets =  unfilledSlots.get(head);
			if (sets == null){
				sets = new HashSet<Pair<Integer, Integer>>();
				unfilledSlots.put(head, sets);
			}
			sets.add(p);
		}	
			//TODO HideAdjunct into CategoryObject
			//only for pred-arg-cat. some cat will be hidden
//			if(HIDE_ADJUNCT_ARGUMENT && next.isAdjunct()){
//				--i;
//				break;
//			}
//			next = next._result;
//		}
//		for ( ;i >= 0; --i)
//			slots[0][i] = Collections.emptySet();
	}

	public String word() {
		return word;
	}
	
	public String modPOS(){
		return mod_POS;
	}
	
	private void index(int i){
		start = i;
		end = i;
	}
	
	public Set<Integer> headOfSlot(int i){
		return cat._slots[i].head();
	}
	
	/**
	 * index start from 1
	 * @return
	 */
	public int index(){
		return start;
	}
	
	public int numOfSlots(){
		return cat.depth();
	}

	public String toString() {
		return "(<L " + cat+ " " + mod_POS + " " + orig_POS + " " + word + " "
				+ predArgCat+ ">)";
	}
	
	public String toStringWithAdditionalField(){
		StringBuffer sb = new StringBuffer( "(<L " + cat+ " " + mod_POS + " " + orig_POS + " " + word + " "
				+ predArgCat);
		if (additionalInfo != null)
			sb.append(" " + additionalInfo);
		sb.append(">)");
		return sb.toString();
	}
	
	public String toIndentedTreeString(int depth){
		return toTreeString();
	}

	public boolean isTerminal() {
		return true;
	}

	public String toTreeString() {
		String str = "(" + cat+ " " + word + ")";
		return str;
	}

	public boolean isPunctuation() {
		return this.word.matches("[.,:;'`?!()]+");
	}

	@Override
	public CCGTerminalNode headTerm() {
		return this;
	}
	
	public static void main (String args[]){
		CCGTerminalNode c = new CCGTerminalNode("<L (S[dcl]\\NP)/S[dcl] VBZ VBZ says (S[dcl]\\NP_187)/S[dcl]_188>", 5);
		System.out.println(c);
		CCGTerminalNode c1 = new CCGTerminalNode("eat", "VP", "S/NP_1", "_", 5);
		System.out.println(c1);
		
	}

}

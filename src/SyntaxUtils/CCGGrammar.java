package SyntaxUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Hey it has nothing to do with jigsaw.syntax.Grammar.
 * 
 * @author C. Wang
 * 
 */
public class CCGGrammar {
	// TODO here I distinguish the two kinds of CCG Rule because different
	// RuleType
	// We might construct a superclass here
	public Map<String, Set<CCGBinaryRule>> binaryRuleMap;
	public Map<String, Set<CCGUnaryRule>> unaryRuleMap;

	private CCGGrammar() {
		binaryRuleMap = new HashMap<String, Set<CCGBinaryRule>>();
		unaryRuleMap = new HashMap<String, Set<CCGUnaryRule>>();
	}

	public static CCGGrammar createNewGrammar() {
		return new CCGGrammar();
	}

	public Set<CCGBinaryRule> lookup(String left, String right) {
		return binaryRuleMap.get(left + "+" + right);
	}

	public Set<CCGUnaryRule> lookup(String unaryCat) {
		return unaryRuleMap.get(unaryCat);
	}

	public void remove(CCGRule r) {
		if (r instanceof CCGBinaryRule)
			binaryRuleMap.remove(r.key());
		else if (r instanceof CCGUnaryRule)
			unaryRuleMap.remove(r.key());
	}

	/**
	 * add the frequency of certain unary rule by 1
	 * 
	 * @param child
	 * @param res
	 */
	public void increSeenCount(String child, String res) {
		increSeenCount(child, res, 1);
	}

	/**
	 * add the frequency of certain unary rule by [freq]
	 * 
	 * @param child
	 * @param res
	 * @param freq
	 */
	public void increSeenCount(String child, String res, int freq) {
		Set<CCGUnaryRule> rules = lookup(child);
		if (rules != null) {
			boolean find = false;
			for (CCGUnaryRule r : rules)
				if (r.result.equals(res)) {
					r.increSeenCount(freq);
					find = true;
				}
			if (!find)
				rules.add(new CCGUnaryRule(child, res, freq));
		} else {
			rules = new TreeSet<CCGUnaryRule>(CCGRule.freqComparator);
			CCGUnaryRule ur = new CCGUnaryRule(child, res, freq);
			rules.add(ur);
			unaryRuleMap.put(child, rules);
		}
	}

	/**
	 * add the frequency of certain binary rule by 1
	 * 
	 * @param child
	 * @param result
	 */
	public void increSeenCount(String left, String right, String result,
			int head) {
		increSeenCount(left, right, result, head, 1);
	}

	/**
	 * add the frequency of certain unary rule by [freq]
	 * 
	 * @param child
	 * @param result
	 */
	public void increSeenCount(String left, String right, String result,
			int head, int freq) {
		Set<CCGBinaryRule> rules = lookup(left, right);
		if (rules != null) {
			boolean find = false;
			for (CCGBinaryRule r : rules)
				if (r.result.equals(result) && r.headChild == head) {
					// if (r.headChild != head){
					// System.err.println("same rule with different head: " +
					// r);
					// }
					r.increSeenCount(freq);
					find = true;
				}
			if (!find)
				rules.add(new CCGBinaryRule(left, right, result, head, freq));
		} else {
			rules = new TreeSet<CCGBinaryRule>(CCGRule.freqComparator);
			CCGBinaryRule br = new CCGBinaryRule(left, right, result, head,
					freq); // TODO
			rules.add(br);
			binaryRuleMap.put(br.key(), rules);
		}
	}

	/**
	 * @param rs
	 * @throws IOException
	 */
	public void load(BufferedReader rs) throws IOException{
		String s = rs.readLine();
		while (s != null && s.trim().length() > 0) {
			CCGBinaryRule bRule = new CCGBinaryRule(s.trim());
			Set<CCGBinaryRule> rules = binaryRuleMap.get(bRule.key());
			if (rules == null)
				rules = new TreeSet<CCGBinaryRule>(CCGRule.freqComparator);
			rules.add(bRule);
			binaryRuleMap.put(bRule.key(), rules);
			s = rs.readLine();
		}
		s = rs.readLine();
		while (s != null && s.trim().length() > 0) {
			CCGUnaryRule uRule = new CCGUnaryRule(s.trim());
			Set<CCGUnaryRule> rules = unaryRuleMap.get(uRule.left);
			if (rules == null)
				rules = new TreeSet<CCGUnaryRule>(CCGRule.freqComparator);
			rules.add(uRule);
			unaryRuleMap.put(uRule.left, rules);
			s = rs.readLine();
		}
	}
	
	public static CCGGrammar loadFromStream(BufferedReader rs) throws IOException {
		CCGGrammar gr = new CCGGrammar();
		gr.load(rs);
		return gr;
	}

	public static CCGGrammar loadFromFile(String filename) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(
				new FileInputStream(filename), "UTF-8"));
		return loadFromStream(br);
	}

	public void dump(Writer w) throws IOException {
		for (Set<CCGBinaryRule> s : binaryRuleMap.values())
			for (CCGBinaryRule rule : s)
				w.write(rule + "\n");
		w.write("\n"); // split the unary ones with binary ones
		for (Set<CCGUnaryRule> s : unaryRuleMap.values())
			for (CCGUnaryRule rule : s)
				w.write(rule + "\n");
		w.write("\n");
	}

}

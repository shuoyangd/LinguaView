package SyntaxUtils;

import java.lang.reflect.Method;
import java.util.EnumMap;
import java.util.Map;

import LinguaView.syntax.CategoryObject;
import LinguaView.syntax.CategoryObject.CoindexedObject;

/**
 * 
 * @author C. Wang
 */

public class CCGBinaryRule extends CCGRule {
	public String left;
	public String right;
	// public String res;
	// private int freq;
	public RuleType type;

	// note that order does matter
	public enum RuleType {
		coordination, forwardApply, backwardApply, simpleForwardCompose, simpleBackwardCompose, crossBackwardCompose, crossForwardCompose, generalizedForwardCompose, generalizedBackwardCompose, generalizedForwardCrossCompose, generalizedBackwardCrossCompose, forwardSubstitute, backwardSubstitute, unknown;
	};
	public static int typeHead[] = {0, 0, 1, 0, 1, 1, 0, 0, 1, 0, 1, 0, 1, 0 };

	public static EnumMap<RuleType, Method> typeMethod;
	public static EnumMap<RuleType, Method> tryAllMethod;
	static {
		typeMethod = new EnumMap<RuleType, Method>(RuleType.class);
		tryAllMethod = new EnumMap<RuleType, Method>(RuleType.class);
		RuleType[] types = RuleType.values();
		for (int i = 0; i < types.length; ++i) {
			try {
				typeMethod.put(types[i], CategoryObject.class
						.getDeclaredMethod(types[i].toString(),
								CategoryObject.class, CategoryObject.class,
								CategoryObject.class, Map.class));
				tryAllMethod.put(types[i], CategoryObject.class
						.getDeclaredMethod(types[i].toString(),
								CategoryObject.class, CategoryObject.class));
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
	}

	public CCGBinaryRule(String left, String right, String result, int head) {
		this.left = left;
		this.right = right;
		this.result = result;
		this.type = classifier(left, right, result);
		freq = 1;
		headChild = head;
	}

	public CCGBinaryRule(String left, String right, String result, int head,
			int freq) {
		this(left, right, result, head);
		this.freq = freq;
	}

	public CCGBinaryRule(String io) {
		String ss[] = io.split("\\s+");
		// rule is like "freq # type # head # res --> left right"
		freq = Integer.parseInt(ss[0].trim());
		type = RuleType.values()[Integer.parseInt(ss[2].trim())];
		headChild = Integer.parseInt(ss[4].trim());
		result = ss[6];
		left = ss[8];
		right = ss[9];
	}

	@Override
	public String key() {
		return key(left, right);
	}
	/**
	 * to keep this it consistent with the above func
	 * @param left
	 * @param right
	 * @return
	 */
	public static String key(String left, String right){
		return left + "+" + right;
	}

	protected static RuleType classifier(String left, String right, String result) {
		CategoryObject lc = CategoryObject.fromPlainCat(left);
		CategoryObject rc = CategoryObject.fromPlainCat(right);
		CategoryObject res = CategoryObject.fromPlainCat(result);
		RuleType types[] = RuleType.values();
		for (int i = 0; i < types.length; ++i) {
			Method m = typeMethod.get(types[i]);
			try {
				if ((Boolean) (m.invoke(null, lc, rc, res, null)))
					return types[i];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return RuleType.unknown;
	}

	public int arity() {
		return 2;
	}

	public static CCGBinaryRule tryAllRules(CategoryObject left, CategoryObject right){
		try{
			Object res = null;
			RuleType types[] = RuleType.values();
			for (int i = 0; i < types.length - 1; ++i){ //don't try unknown type
				Method m = tryAllMethod.get(types[i]);
				res = m.invoke(null, left, right);
				//res = tryAllMethod.get(i).invoke(null, left, right);
				if (res != null){
					CCGBinaryRule bRule = new CCGBinaryRule(left.toString(), right.toString(), res.toString(), typeHead[i]);
					return bRule;
				}
			}
			return null;
		}catch (Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	public CategoryObject perform(CategoryObject left, CategoryObject right,
			Map<CoindexedObject, CoindexedObject> indices) {
//		if (type == RuleType.unknown)
//			return null;
		CategoryObject res = CategoryObject.fromPlainCat(result);
		Method m = typeMethod.get(type);
//		System.err.print(type+" "+m);
		try {
			if (m != null
					&& (Boolean) m.invoke(null, left, right, res, indices)) {
//				System.err.println(left+"+"+right+"=>"+res);
				return res;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof CCGBinaryRule))
			return false;
		CCGBinaryRule that = (CCGBinaryRule) o;
		if (that.type != type)
			return false;
		if (!that.left.equals(left) || !that.right.equals(right)
				|| !that.result.equals(result) || that.headChild != headChild)
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return result.hashCode() + headChild;
	}

	@Override
	public int typeOrdinal() {
		return type.ordinal();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(freq + " # ");
		sb.append(typeOrdinal() + " # ");
		sb.append(headChild + " # ");
		sb.append(result + " --> ");
		sb.append(left + " " + right);
		return sb.toString();
	}

	public static void main(String[] args) {
		CCGBinaryRule test = new CCGBinaryRule("NP", "S\\NP", "S", 1, 5);
		System.out.println(test);
		System.out.println(test.type);

		test = new CCGBinaryRule("NP", "NP", "S", 0, 8);
		System.out.println(test);
		System.out.println(test.type);

		String s = test.toString();
		CCGBinaryRule newRule = new CCGBinaryRule(s);
		System.out.println(newRule.toString());
	}
}

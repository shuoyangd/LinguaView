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
public class CCGUnaryRule extends CCGRule{
	public String left;
//	public String result;
//	private int freq;
	public RuleType type;

	public enum RuleType {
		typeRaising, unaryTypeChangingSimple, unaryTypeChangingN, unaryTypeChangingV, unknown
	};

	public static EnumMap<RuleType, Method> typeMethod;
	static {
		typeMethod = new EnumMap<RuleType, Method>(RuleType.class);
		RuleType[] types = RuleType.values();
		for (int i = 0; i < types.length; ++i) {
			try {
				typeMethod.put(types[i], CategoryObject.class.getDeclaredMethod(types[i].toString(),
								CategoryObject.class, CategoryObject.class, Map.class));
			} catch (SecurityException e) {
				e.printStackTrace();
			} catch (NoSuchMethodException e) {
				e.printStackTrace();
			}
		}
	}

	public CCGUnaryRule(String left, String result) {
		this.left = left;
		this.result = result;
		this.type = classifier(left, result);
		freq = 1;
		headChild = 0;
	}
	
	public CCGUnaryRule(String io){
		String ss[] = io.split("\\s+");
		// rule is like "freq # type # res --> left"
		freq = Integer.parseInt(ss[0].trim());
		type = RuleType.values()[Integer.parseInt(ss[2].trim())];
		result = ss[4];
		left = ss[6];
		headChild = 0;
	}

	public CCGUnaryRule(String left, String result, int freq) {
		this(left, result);
		this.freq = freq;
	}


	protected static RuleType classifier(String left, String result) {
		CategoryObject lc = CategoryObject.fromPlainCat(left);
		CategoryObject res = CategoryObject.fromPlainCat(result);
		RuleType types[] = RuleType.values();
		for (int i = 0; i < types.length; ++i) {
			Method m = typeMethod.get(types[i]);
			try {
				if ((Boolean) (m.invoke(null, lc, res, null)))
					return types[i];
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return RuleType.unknown;
	}
	
	public CategoryObject perform(CategoryObject child, Map<CoindexedObject, CoindexedObject> indices){
//		if (type == RuleType.unknown)
//			return null;
//		if (type == RuleType.manual)
		//TODO
//		System.err.println(type);
		CategoryObject res = CategoryObject.fromPlainCat(result);
		Method m = typeMethod.get(type);
		try {
			if (m!= null &&(Boolean) m.invoke(null, child, res, indices))
				return res;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public int arity() {
		return 1;
	}
	
	@Override
	public String key(){
		return key(left);
	}
	
	public static String key(String unaryCat){
		return unaryCat;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (this == o)
			return true;
		if (!(o instanceof CCGUnaryRule))
			return false;
		CCGUnaryRule that = (CCGUnaryRule) o;
		if (that.type != type)
			return false;
		if (!that.left.equals(left) || !that.result.equals(result))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		return result.hashCode();
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append(freq + " # ");
		sb.append(type.ordinal() + " # ");
		sb.append(result + " --> ");
		sb.append(left);
		return sb.toString();
	}

	public static void main(String[] args) {
		CCGUnaryRule test = new CCGUnaryRule("NP", "S\\(S/NP)");
		System.out.println(test);
		System.out.println(test.type);

		test = new CCGUnaryRule("N", "NP", 2);
		System.out.println(test);
		System.out.println(test.type);
		
		test = new CCGUnaryRule("S[pss]\\NP", "S/S");
		System.out.println(test);
		System.out.println(test.type);
	}
	
	@Override
	public int typeOrdinal(){
		return type.ordinal();
	}
}

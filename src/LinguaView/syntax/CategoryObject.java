package LinguaView.syntax;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * There are two versions of methods to perform CCG Rule. The first type is when
 * we have two categories, then we decide what kind of category we could get
 * upon certain rule. The second type is when we have a derivation of CCG tree,
 * tell if the derivation satisfy certain rules, and it needed, find the
 * dependency upon certain rules. Indices is used to build the dependency; just
 * leave it null when we don't care about the dependency. If the operation
 * failed, indices would not change.
 * 
 * @author C.Wang
 */
public class CategoryObject { // extends CategoryObject{
	static enum Direction { FORWARD, BACKWARD, BASIC };
	/**
	 * _headObject contains the head of the cat, represented by the index in a
	 * sentence, and a list of cats with equivalent heads. For Terminal CCG
	 * Node, it indicates the co-index read from the treebank; for non-terminal
	 * node, it contains all the nodes in the derivation that are equivalent.
	 * 
	 * @author C.Wang
	 * 
	 */
	@SuppressWarnings("serial")
	public static class CoindexedObject extends HashSet<CategoryObject> {
		Set<Integer> head; // coordination might brought more than one head
		Set<CoindexedObject> children; // it could be more than two since we have co-indexed object.
//		Set<Pair<Integer, Integer>> slots;
		
		CoindexedObject() {
			head = new HashSet<Integer>();
			children = new HashSet<CoindexedObject>();
		}

		public void clear() {
			// feature = null;
			head.clear();
			children.clear();
			super.clear();
		}

		/**
		 * r should be in the same category
		 */
		@Override
		public boolean add(CategoryObject r) {
			if (r._headObj == null) {
				r._headObj = this;
				return super.add(r);
			} else if (this != r._headObj) {
				CoindexedObject x = this.mergeWith(r._headObj);
				if (x != null) {
					r._headObj = x;
					return true;
				} else
					return false;
			}
			return true;
			// r._headObj = this;
			// return super.add(r);
		}

		/**
		 * Get the head from the child nodes.
		 * 
		 * @param r
		 * @return
		 */
		public boolean attach(CoindexedObject r) {
			// try {
			head.addAll(r.head);
			// } catch (Exception e) {
			// System.out.println();
			// }
			return children.add(r);
		}

		/**
		 * When combining two category, we might find two cats in a certain
		 * recursive cat are equivalent, thus we have to merge the two cats.
		 * 
		 * @param r
		 * @return this after merged or null
		 */
		public CoindexedObject mergeWith(CoindexedObject r) {
			children.addAll(r.children);
			for (CategoryObject co : r)
				super.add(co);
			// NOTE: addAll call the add function of the subclass
			// super.addAll(r);
			this.head.addAll(r.head);
			return this;
		}

		/**
		 * see mergeWith(CoindexedObject r) method
		 * 
		 * @param r
		 * @return
		 */
		public CoindexedObject addAll(CoindexedObject r) {
			return mergeWith(r);
		}

		public boolean coindexedWith(CategoryObject r) {
			return super.contains(r);
		}

		public void head(int h) { head.add(h); }
		public Set<Integer> head() { return head; }

		public boolean emptyHead() {
			return head == null ? true : head.isEmpty();
		}
	}

	String _category; // category in String format
	String _feature;
	
	CategoryInterpretation _interpret;
	
	public CategoryObject[] _slots = null;
	//int _numOfSlots;

	Direction _dir;

	boolean _isConjunctConstituent = false; // here to indicate things like NP[conj]

	// the following field is not null only when direction is not basic
	CategoryObject _result = null;
	CategoryObject _argument = null;

	int _depth; // recursive depth is needed to record dependency num

	public boolean _longRange = false;

	public CoindexedObject _headObj = null;

	protected CategoryObject(CategoryObject res, Direction dir, CategoryObject arg) {
		if (dir == Direction.BASIC)
			throw new IllegalArgumentException("Combine with direction basic!");
		this._dir = dir;
		_result = res;
		_argument = arg;
		_depth = _result._depth + 1;
		//_numOfSlots = _result._numOfSlots + _argument._numOfSlots;
		_category = toString();
	}

	public CategoryObject() { ; }

	public static CategoryObject fromPlainCat(String label) {
		boolean isConjunction = false;
		CategoryObject cat = null;
		
		if (label.endsWith("[conj]")) {
			isConjunction = true;
			label = label.substring(0, label.length() - 6);
		}
//		try {
			cat = fromPredArgCat(label, null);
			cat._isConjunctConstituent = isConjunction;
//		} catch (Exception e) {
//			e.printStackTrace();
//			throw new IllegalArgumentException("Wrong Cat: " + label);
//		}
		return cat;
	}
	
	/**
	 * move this to terminal node
	 * 
	 * @param label
	 * @param isPredArgCat
	 */
	public static CategoryObject fromPredArgCat(String label) {
		boolean isConjunction = false;
		CategoryObject cat = null;
//		CoindexedObject[] slots = null;
		
		if (label.endsWith("[conj]")) {
			isConjunction = true;
			label = label.substring(0, label.length() - 6);
		}
		try {
//			if (isPredArgCat) {
			Map<Integer, CoindexedObject> indexCache = new HashMap<Integer, CoindexedObject>();
			cat = fromPredArgCat(label, indexCache);
			cat.coindexWith(indexCache.get(0));
			cat._slots = new CategoryObject[cat.depth()];
			cat._isConjunctConstituent = isConjunction;
			CategoryObject next = cat;
			for (int i = cat._slots.length - 1; i >= 0; --i) {
				cat._slots[i] = next._argument;
				next = next._result;
			}
//				indexCache.put(0, _headObj);
//			} else
//				fromPredArgCat(label, null);
		} catch (Exception e) {
			e.printStackTrace();
			throw new IllegalArgumentException("Wrong Cat: " + label);
			// System.err.println("Wrong Cat: " + label); // Debug
		}
		return cat;
	}

	private static CategoryObject fromPredArgCat(String label,
			Map<Integer, CoindexedObject> indices) {

		CategoryObject cat = new CategoryObject();
		
		label = label.replace(":B", "");
		label = label.replace(":U", "");
		label = label.replace("(", "{");
		label = label.replace(")", "}");
		// k points at '_' or the same with l, l points at '\\' or '/'
		if (label.contains("/") || label.contains("\\")) { //complex category
			int k = -1;
			int l = -1;
			if (label.charAt(0) == '{') {
				boolean needAgain = true;
				while (needAgain){
					l = 0;
					k = -1;
					if (label.charAt(0) != '{')
						break;
					int numOfOpenParen = 1;
					for (k = 1; k < label.length(); k++) {
						if (label.charAt(k) == '{')
							numOfOpenParen++;
						if (label.charAt(k) == '}')
							numOfOpenParen--;
						if (numOfOpenParen == 0)
							break;
					}
					l = ++k;
					if (l == label.length())
						label = label.substring(1, label.length() -1);
					else 
						needAgain = false;
				}
				while (l < label.length()) {
					if (label.charAt(l) == '_')
						k = l;
					if (label.charAt(l) == '/' || label.charAt(l) == '\\')
						break;
					++l;
				}
				if (k == -1)
					k = l;
			} else {
				for (l = 1; l < label.length(); l++) {
					if (label.charAt(l) == '_')
						k = l;
					if (label.charAt(l) == '/' || label.charAt(l) == '\\')
						break;
				}
				if (k == -1)
					k = l;
			}
			if (l == label.length() - 1) // here's only a parenthesis
				throw new IllegalArgumentException(label);

			cat._dir = (label.charAt(l) == '\\' ? Direction.BACKWARD
					: Direction.FORWARD);

//			cat._result = new CategoryObject();
			if (label.charAt(0) == '{')
				cat._result = fromPredArgCat(label.substring(1, k - 1), indices);
			else
				cat._result = fromPredArgCat(label.substring(0, k), indices);

			if (indices != null) {
				int indexResult = 0;
				if (l != k)
					indexResult = Integer.parseInt(label.substring(k + 1, l));
				cat._result.coindexWith(indices.get(indexResult));
				indices.put(indexResult, cat._result._headObj);
			}

			cat._depth = cat._result._depth + 1;
			// x points to '_', after '}' or after the last character of
			// atomic category
			if (indices != null) {
				int indexArgument = 0;
				int x = label.length();
				if (label.charAt(label.length() - 1) != '}')
					for (int i = label.length() - 1; i > l; --i) {
						if (label.charAt(i) == '_') {
							x = i;
							indexArgument = Integer.parseInt(label.substring(i + 1, label.length()));
							break;
						}
					}
//				cat._argument = new CategoryObject();
				if (label.charAt(l + 1) == '{')
					cat._argument = fromPredArgCat(label.substring(l + 2, x - 1), indices);
				else
					cat._argument = fromPredArgCat(label.substring(l + 1, x), indices);
				cat._argument.coindexWith(indices.get(indexArgument));
				indices.put(indexArgument, cat._argument._headObj);
			} else {
//				cat._argument = new CategoryObject();
				if (label.charAt(l + 1) == '{')
					cat._argument = fromPredArgCat(label.substring(l + 2, label.length() - 1), null);
				else
					cat._argument = fromPredArgCat(label.substring(l + 1, label.length()), null);
			}
			cat._category = cat.toString();
		} else {
			// category = label;
			cat._depth = 0;

			Pattern p = Pattern.compile("\\[\\w+\\]");
			Matcher m1 = p.matcher(label);
			if (m1.find()) {
				String feat = m1.group();
				cat._feature = (feat.substring(1, feat.length() - 1));
				// feature = feature.substring(1, feature.length() - 1);
				cat._category = m1.replaceAll("");
			} else
				cat._category = label;
			cat._dir = Direction.BASIC;
		}
		return cat;
	}
	
	//it looks like 
	//  1 ((S[X]{Y}\NP{Z}){Y}/(S[X]{Y}\NP{Z}){Y}<1>){_}
	public static CategoryObject fromCACCats(String label){
		CategoryObject cat = null;
//		try {
			label = label.replaceAll("#.*$", "").trim();
			String ss[] = label.split(" ", 2);
			int slotLength = Integer.parseInt(ss[0]);
			CategoryObject[] slotIndices = new CategoryObject[slotLength];
			/**
			if (label.endsWith("[conj]")) {
				isConjunction = true;
				label = label.substring(0, label.length() - 6);
			}**/
			Map<Character, CoindexedObject> indexCache = new HashMap<Character, CoindexedObject>();
			cat = fromCACCats(ss[1], indexCache, slotIndices);
			cat.coindexWith(indexCache.get('_'));
			cat._slots = slotIndices;
//		} catch (Exception e) {
//			e.printStackTrace();
//			System.err.println("Wrong Cat: " + label); // Debug
//		}
		return cat;
	}
	
	//things look like this: ((S[X]{Y}\NP{Z}){Y}/(S[X]{Y}\NP{Z}){Y}<1>){_}
	public static CategoryObject fromCACCats(String label, Map<Character, CoindexedObject> indices, CategoryObject[] slotIndices){
		CategoryObject cat = new CategoryObject();
		label = label.replace("[X]", "");

		
		int slotIndex = -1;
		if (label.charAt(label.length() - 1) == '>'){
			int temp = label.lastIndexOf('<');
			slotIndex = Integer.parseInt(label.substring(temp + 1, label.length()-1));
//			slotIndex = label.charAt(label.length() - 2) - '0';
			label = label.substring(0, temp);
		}
		char charIndex;
		char x = label.charAt(label.length() - 2);
		if (x == '*'){
			cat._longRange = true;
			charIndex = label.charAt(label.length() - 3);
			label = label.substring(0, label.length() - 4);
		}else{
			cat._longRange = false;
			charIndex = x;
			label = label.substring(0, label.length() - 3);
		}
		
		if (label.contains("/") || label.contains("\\")) {
			int l = -1;
			//the result is complex. We need to find out which / or \ is the dir
			if (label.charAt(0) == '(') {
				// let l points to the character after the last ')'
				boolean needAgain = true;
				while(needAgain){
					l = 1;
					if (label.charAt(0) != '(')
						break;
					int numOfOpenParen = 1;
					for (; l < label.length(); l++) {
						if (label.charAt(l) == '(')
							numOfOpenParen++;
						if (label.charAt(l) == ')')
							numOfOpenParen--;
						if (numOfOpenParen == 0)
							break;
					}
					++l; 
					if (l == label.length())
						label = label.substring(1, label.length()-1);
					else 
						needAgain = false;
				}
				// now let l points at '\\' or '/'
				while (l < label.length()) {
					if (label.charAt(l) == '/' || label.charAt(l) == '\\')
						break;
					++l;
				}
			}else{
				for (l = 1; l < label.length(); l++) {
					if (label.charAt(l) == '/' || label.charAt(l) == '\\')
						break;
				}
			}
			cat._result = fromCACCats(label.substring(0, l), indices, slotIndices);
			cat._argument = fromCACCats(label.substring(l+1), indices, slotIndices);
			cat._dir = (label.charAt(l) == '\\' ? Direction.BACKWARD	: Direction.FORWARD);
			cat._depth = cat._result._depth + 1;
			cat._category = cat.toString();
			
		} else{ // it is an atomic category
			cat._depth = 0;
			Pattern p = Pattern.compile("\\[\\w+\\]");
			Matcher m1 = p.matcher(label);
			if (m1.find()) {
				String feat = m1.group();
				cat._feature = (feat.substring(1, feat.length() - 1));
				// feature = feature.substring(1, feature.length() - 1);
				cat._category = m1.replaceAll("");
			} else
				cat._category = label;
			cat._dir = Direction.BASIC;
		}

		CoindexedObject head = indices.get(charIndex);
		cat.coindexWith(head);
		indices.put(charIndex, cat._headObj);
		
		if(slotIndex > 0)
			slotIndices[slotIndex-1] = cat;
		
		return cat;
	}

	public int depth() { return _depth; }
	
	// wsun: Add more methods to support tree2dag implementation
	public boolean isBasic() 	{ return _dir == Direction.BASIC; }
	public boolean isForward()	{ return _dir == Direction.FORWARD; }
	public boolean isBackward()	{ return _dir == Direction.BACKWARD; }
//	public boolean isAdjunct()  { return _result.toString().equals(_argument.toString()); }
	
	/**
	 * Generate a simplified category. Get rid of feature, NP=>N, etc.
	 * 
	 * @return
	 * @author wsun
	 */
	public String getSimplifiedCategoryString() {
		StringBuffer sb = new StringBuffer();
		if (_dir == Direction.BASIC) {
			sb.append(_category.charAt(0));
		} else {
			if (_result._depth > 0) sb.append("{");
			sb.append(_result.getSimplifiedCategoryString());
			if (_result._depth > 0) sb.append("}");
			
			sb.append(_dir == Direction.BACKWARD ? '\\' : '/');

			if (_argument._depth > 0) sb.append("{");
			sb.append(_argument.getSimplifiedCategoryString());
			if (_argument._depth > 0) sb.append("}");
		}
		return sb.toString();
	}

	/**
	 * See if cat x1 and cat x2 are unified, unification considered.
	 * 
	 * @param x1
	 * @param x2
	 * @return the result category of the unification. null if failed.
	 */
	static CategoryObject unify(CategoryObject x1, CategoryObject x2) {
		return unify(x1, x2, null);
	}

	/**
	 * Try to unify the two cats, and see what we could get. When doing reduce,
	 * the return value could be put into the CCGInternalNode. We expect to get
	 * a result category, a map of co-indexed objects.
	 * 
	 * @param x1
	 * @param x2
	 * @param indices
	 *            Where we could map the co-indexed object in the child node and
	 *            those in parent node. Just set it null if we don't care.
	 * @return Category generated from the unification. Null if the two cats
	 *         fail to unify.
	 */
	static CategoryObject unify(CategoryObject x1, CategoryObject x2,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (x1 == null || x2 == null)                       return null;
		if (x1.depth() != x2.depth() || x1._dir != x2._dir) return null;

		if (x1._dir == Direction.BASIC) {
			// XXX here we regard NP and N are the same.
			if (!(x1._category.equals(x2._category) 
            || (x1._category.equals("NP") && x2._category.equals("N") 
                || (x2._category.equals("NP") && x1._category.equals("N")))))
				return null;
			if (x1.feature() != null && x2.feature() != null && !x1.feature().equals(x2.feature()))
				return null;

			if (indices != null)
				merge(x1._headObj, x2._headObj, indices);

			CategoryObject res = fromPlainCat(x1.feature() == null ? x2.toString() : x1.toString());
			//new CategoryObject(x1.feature() == null ? x2.toString() : x1.toString(), false);
			if (indices != null) {
				res.attachHead(x1, indices);
				res.attachHead(x2, indices);
			}
			// attach head to it
			return res;
		} else {
			CategoryObject res = unify(x1._result, x2._result, indices);
			CategoryObject arg = unify(x1._argument, x2._argument, indices);
			if (res != null && arg != null) {
				if (indices != null)
					merge(x1._headObj, x2._headObj, indices);
				res = new CategoryObject(res, x1._dir, arg);
				if (indices != null) {
					res.attachHead(x1, indices);
					res.attachHead(x2, indices);
				}
				return res;
			} else
				return null;
		}
	}

	private static CategoryObject generateFromChild(CategoryObject cat) {
		CategoryObject obj = new CategoryObject();

		obj._dir = cat._dir;
		obj._depth = cat.depth();

		Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();

		if (obj._depth != 0) {
			obj._result = generateFromChild(cat._result);// , indices);
			obj._argument = generateFromChild(cat._argument);// , indices);
			obj._category = obj.toString();
		} else {
			// category could change after feature is added
			obj._category = cat._category;
			obj._feature = cat._feature;
		}
		obj.coindexWith(null);
		obj._headObj.attach(cat._headObj);
		indices.put(cat._headObj, obj._headObj);
		return obj;
	}

	private static CategoryObject generateFromChild(CategoryObject cat,
			Map<CoindexedObject, CoindexedObject> indices) {
		CategoryObject obj = new CategoryObject();
		obj._dir = cat._dir;
		obj._depth = cat.depth();

		if (obj._depth != 0) {
			obj._result = generateFromChild(cat._result, indices);
			obj._argument = generateFromChild(cat._argument, indices);
			obj._category = obj.toString();
		} else {
			obj._category = cat._category;
			obj._feature = cat._feature;
		}
		obj.coindexWith(indices.get(cat._headObj));
		obj._headObj.attach(cat._headObj);
		indices.put(cat._headObj, obj._headObj);
		return obj;
	}

	public String toStringWithoutFeature(){
		StringBuffer sb = new StringBuffer();
		if (_dir == Direction.BASIC) {
			sb.append(_category);
		}
		else {
			if (_result._depth > 0)
				sb.append("{");
			sb.append(_result.toStringWithoutFeature());
			if (_result._depth > 0)
				sb.append("}");
			sb.append(_dir == Direction.BACKWARD ? '\\' : '/');

			if (_argument._depth > 0)
				sb.append("{");
			sb.append(_argument.toStringWithoutFeature());
			if (_argument._depth > 0)
				sb.append("}");
		}
		return sb.toString();
	}
	
	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		if (_dir == Direction.BASIC) {
			sb.append(_category);
			if (feature() != null)
				sb.append("[" + feature() + "]");
			if (_isConjunctConstituent)
				sb.append("[conj]");
//		} else if (_category != null){
//			sb.append(_category);
		} else {
			if (_result._depth > 0)
				sb.append("{");
			sb.append(_result);
			if (_result._depth > 0)
				sb.append("}");
			sb.append(_dir == Direction.BACKWARD ? '\\' : '/');

			if (_argument._depth > 0)
				sb.append("{");
			sb.append(_argument);
			if (_argument._depth > 0)
				sb.append("}");
			_category = sb.toString();
			if (_isConjunctConstituent)
				sb.append("[conj]");
		}
		return sb.toString();
	}
	
	public static String[] HTMLColors = {"firebrick", "mediumblue", "green", "orchid"};
	
	public String toColoredString(){
		return toColoredString(_slots);
	}
	
	public String toColoredString(CategoryObject[] slots){
		StringBuffer sb = new StringBuffer();
		boolean colored = false;
		for (int i = 0; i< slots.length; ++i){
			if (slots[i] == this){
				colored = true;
				sb.append(String.format("<font color=\"%s\">", CategoryObject.HTMLColors[i%4]));
				break;
			}
		}
		if (_dir == Direction.BASIC) {
			sb.append(_category);
			if (feature() != null)
				sb.append("[" + feature() + "]");
			if (_isConjunctConstituent)
				sb.append("[conj]");
		} else {
			if (_result._depth > 0)
				sb.append("{");
			sb.append(_result.toColoredString(slots));
			if (_result._depth > 0)
				sb.append("}");
			sb.append(_dir == Direction.BACKWARD ? '\\' : '/');

			if (_argument._depth > 0)
				sb.append("{");
//			sb.append("%s");
			sb.append(_argument.toColoredString(slots));
//			sb.append("%s");
			if (_argument._depth > 0)
				sb.append("}");
			_category = sb.toString();
			if (_isConjunctConstituent)
				sb.append("[conj]");
		}
		if(colored)
			sb.append("</font>");
		return sb.toString();
	}

	public String toPredArgCat() {
		if (_headObj == null)
			throw new NullPointerException("_headObj null!");
		if (_dir == Direction.BASIC)
			return toString();
		else {
			Map<CoindexedObject, Integer> indices = new HashMap<CoindexedObject, Integer>();
			StringBuffer sb = new StringBuffer();
			indices.put(_headObj, 0);
			sb.append(_result.toPredArgCat(indices));
			sb.append(_dir == Direction.BACKWARD ? '\\' : '/');
			sb.append(_argument.toPredArgCat(indices));
			return sb.toString();
		}
	}
	
	/**
	 * recursive get the predArgCat string
	 * 
	 * @param indices
	 * @return
	 */
	private String toPredArgCat(Map<CoindexedObject, Integer> indices) {
		if(_headObj == null)
			throw new NullPointerException("_headObj null!");
		StringBuffer sb = new StringBuffer();
		int index;
		if (indices.containsKey(_headObj))
			index = indices.get(_headObj);
		else {
			index = indices.size();
			indices.put(_headObj, index);
		}
		if (_dir == Direction.BASIC)
			sb.append(toString());
		else {
			sb.append("{");
			sb.append(_result.toPredArgCat(indices));
			sb.append(_dir == Direction.BACKWARD ? '\\' : '/');
			sb.append(_argument.toPredArgCat(indices));
			sb.append("}");
		}
		if (index != 0)
			sb.append("_" + index);
		return sb.toString();
	}
	
  /** To handle C and C style markedup file **/
	static char[] markedupChars = {'_', 'Y', 'Z', 'W', 'V', 'U', 'T', 'S', 'R', 'Q', 'P', 'O', 'N', 'M', 'L', 'K', 'J', 'I', 'H', 'G', 'F', 'E', 'D', 'C', 'B', 'A'};
	public String toMarkedupString(){
		if (_headObj == null || _slots == null)
			throw new NullPointerException("_headObj or slots null!");
		if (_dir == Direction.BASIC)
			return "0 " + toString()+"{_}";
		else {
			Map<CoindexedObject, Integer> indices = new HashMap<CoindexedObject, Integer>();
			// slot indices not work. same _headObj could be more than slots
//			Map<CoindexedObject, Integer> slotsIndices = new HashMap<CoindexedObject, Integer>();
			CategoryObject unmarkedSlot[] = new CategoryObject[_slots.length];
			for (int i = 0; i< _slots.length;++i)
				unmarkedSlot[i] = _slots[i];
			indices.put(_headObj, 0);

			try{
				StringBuffer sb = new StringBuffer(_slots.length + " ");
				sb.append('(');
				sb.append(_result.toMarkedupString(indices, unmarkedSlot));
				sb.append(_dir == Direction.BACKWARD ? '\\' : '/');
				sb.append(_argument.toMarkedupString(indices, unmarkedSlot));
				sb.append(')');
				sb.append("{_}");// hey hey hey slot could not be it self;
				return sb.toString();
			}catch(Exception e){
				System.err.println(toString());
				System.err.println(toPredArgCat());
//				System.err.println();
				System.err.println("too much slots...26 letters not enough to hold this");
//				e.printStackTrace();
			}
			return null;
		}
	}

	private String toMarkedupString(Map<CoindexedObject, Integer> indices, CategoryObject[] unmarkedSlot){
		if(_headObj == null)
			throw new NullPointerException("_headObj null!");
		StringBuffer sb = new StringBuffer();
		int index;
		if (indices.containsKey(_headObj))
			index = indices.get(_headObj);
		else {
			index = indices.size();
			indices.put(_headObj, index);
		}
		if (_dir == Direction.BASIC)
			sb.append(toString());
		else {
			sb.append("(");
			sb.append(_result.toMarkedupString(indices, unmarkedSlot));
			sb.append(_dir == Direction.BACKWARD ? '\\' : '/');
			sb.append(_argument.toMarkedupString(indices, unmarkedSlot));
			sb.append(")");
		}
		sb.append("{" + markedupChars[index]);
		if (_longRange)
			sb.append('*');
		sb.append('}');
		
		for (int i=0; i< unmarkedSlot.length; ++i){
			if (unmarkedSlot[i] == this){
				sb.append("<" + (i+1) + '>');
//				unmarkedSlot[i] = null;
				break;
			}
		}
		return sb.toString();
	}
	

	/**
	 * @param x1
	 * @param x2
	 * @return true if the two cats have same functor and same co-index relation
	 */
	public static boolean compareIndices(CategoryObject x1, CategoryObject x2) {
		Pattern p = Pattern.compile("[^{_}/\\d\\\\]");

		String s1 = x1.toPredArgCat();
		Matcher m1 = p.matcher(s1);
		s1 = m1.replaceAll("");

		String s2 = x2.toPredArgCat();
		Matcher m2 = p.matcher(s2);
		s2 = m2.replaceAll("");

		return s1.equals(s2);
	}

	private static void merge(CoindexedObject x1, CoindexedObject x2,
			Map<CoindexedObject, CoindexedObject> indices) {
		// map _headObj in the child category to the new _headObj in the parent
		// node.
		CoindexedObject o1 = indices.get(x1);
		CoindexedObject o2 = indices.get(x2);

		if (o1 != null && o2 != null) {
			/*
			 * for case like the following, we might have to merge the two
			 * pairs:(<T N/N 1 2> (<L (N/N)/(N/N) IN IN about
			 * (N_98/N_92)_98/(N_98/N_92)_98>) (<L N/N CD CD 1,200 N_84/N_84>) )
			 */
			if (o1 != o2)
				indices.put(x2, o1.mergeWith(o2));
		} else {
			if (o1 == null && o2 == null) {
				CoindexedObject obj = new CoindexedObject();
				obj.attach(x1);
				obj.attach(x2);
				indices.put(x1, obj);
				indices.put(x2, obj);
			} else if (o1 == null) {
				o2.attach(x1);
				indices.put(x1, o2);
			} else {
				o1.attach(x2);
				indices.put(x2, o1);
			}
		}
	}

	/**
	 * Add head information for the new parent node with the map. For example
	 * A1/B1 B2/C1 -> A2/C2, here we attach the head information from A1 to A2
	 * 
	 * @param prev
	 * @param indices
	 */
	public void recursiveAttachHead(CategoryObject prev, Map<CoindexedObject, CoindexedObject> indices) {
		if (depth() != prev.depth())
			throw new IllegalArgumentException(
					"Attach cats with different depth:" + prev + " " + this);

		attachHead(prev, indices);
		// coindexWith(indices.get(prev._headObj));
		// _headObj.attach(prev._headObj);
		// indices.put(prev._headObj, _headObj);

		if (_dir != Direction.BASIC) {
			try {
				_result.recursiveAttachHead(prev._result, indices);
				_argument.recursiveAttachHead(prev._argument, indices);
			} catch (IllegalArgumentException e) {
				throw new IllegalArgumentException(
						"Attach cats with different depth:" + prev + " " + this);
			}
		}
	}

	public void recursiveAttachHead(CategoryObject prev) {
		recursiveAttachHead(prev,
				new HashMap<CoindexedObject, CoindexedObject>());
	}

	/**
	 * attachHead function attach the _headObj of a child to the parent. Note
	 * that any internal category are not attached a head; otherwise use
	 * recursiveAttachHead function
	 * 
	 * @param prev
	 * @param indices
	 */
	public boolean attachHead(CategoryObject prev,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (prev._headObj == null)
			return false;
		coindexWith(indices.get(prev._headObj));
		_headObj.attach(prev._headObj);
		indices.put(prev._headObj, _headObj);
		return true;
	}

	public void clearCoindex() {
		_headObj = null;
		if (_dir != Direction.BASIC) {
			_result.clearCoindex();
			_argument.clearCoindex();
		}
	}

	public Set<Integer> head() {
		return _headObj == null ? null : _headObj.head();
	}

	public String feature() {
		if (_dir != Direction.BASIC)
			return null;
		return _feature;
	}

	private void setFeature(Set<CoindexedObject> heads) {
		for (CoindexedObject x : heads) {
			String cat = null;
			String feat = null;
			// Set<Integer> hhs = new HashSet<Integer>();
			for (CoindexedObject c : x.children) {
				// x.head.addAll(c.head());
				for (CategoryObject o : c)
					if (o._feature != null) {
						// here for debug:
						// if (cat != null || feat != null) {
						// assert (cat.equals(o.category));
						// assert (feat.equals(o.feature));
						// } else {
						cat = o._category;
						feat = o._feature;
						break;
						// }
					}
			}
			if (feat != null)
				// things like NP[nb]_147/N_147 ... atomic is not enough
				// by default co-indexed object have only one feature
				for (CategoryObject c : x) {
					if (c._dir == Direction.BASIC) {
						if (c._category.equals(cat))
							c._feature = feat;
					} else
						c._category = null; // cache should be cleared
				}
		}
	}

	public void head(int head) {
		if (_headObj != null)
			_headObj.head(head);
		else
			System.err.println("head not initailized");
	}

	/**
	 * initialize the _headObj
	 * 
	 * @param objs
	 */
	public void coindexWith(CoindexedObject objs) {
		if (objs != null)
			if(objs == _headObj)
				return;
			else if (_headObj != null){
				replaceCoindexObject(objs);
				return;
			}
		if (objs == null)
			objs = new CoindexedObject();
		objs.add(this);
	}
	
	private void replaceCoindexObject(CoindexedObject objs){
		if (_headObj != null)
			objs.mergeWith(_headObj);
		for (CategoryObject cats: _headObj){
			objs.add(cats);
			cats._headObj = objs;
		}
	}

	public void recursiveCoindexWith(CategoryObject cat) {
		if (cat.depth() != depth()) {
			System.err.println("coindex with something with different depth");
			return;
		}
		coindexWith(cat._headObj);
		cat.coindexWith(_headObj); // even if the two are null...sigh
		if (cat.depth() > 0) {
			_result.recursiveCoindexWith(cat._result);
			_argument.recursiveCoindexWith(cat._argument);
		}
	}

	/**
	 * @SupportedOperation Forward Apply: A/B + B -> A
	 * @param argument
	 * @param _dir
	 * @return
	 */
	public static CategoryObject forwardApply(CategoryObject left, CategoryObject arg) {
		if (left._dir == Direction.FORWARD) {
			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (unify(left._argument, arg, indices) != null) {
				CategoryObject res = generateFromChild(left._result, indices);
				res.setFeature(new HashSet<CoindexedObject>(indices.values()));
				return res;
			}
		}
		return null;
	}

	/**
	 * @SupportedOperation Forward Apply: A/B + B -> A
	 * @return if the operation succeed
	 */
	public static boolean forwardApply(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (left._dir != Direction.FORWARD)
			return false;
		if (null == unify(res, left._result))
			return false;

		if (indices != null) {
			// Map<CoindexedObject, CoindexedObject> indices = new
			// HashMap<CoindexedObject, CoindexedObject>();
			if (null == unify(left._argument, right, indices))
				return false;
			res.recursiveAttachHead(left._result, indices);
		} else if (null == unify(left._argument, right))
			return false;
		return true;
	}

	/**
	 * @SupportedOperation Backward Apply: B + A\B -> A
	 * @param arg
	 * @return res
	 */
	public static CategoryObject backwardApply(CategoryObject arg, CategoryObject right) {
		if (right._dir == Direction.BACKWARD) {
			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (unify(right._argument, arg, indices) != null) {
				CategoryObject res = generateFromChild(right._result, indices);
				res.setFeature(new HashSet<CoindexedObject>(indices.values()));
				return res;
			}
		}
		return null;
	}

	/**
	 * @SupportedOperation Backward Apply: B + A\B -> A
	 * @return if the operation succeed
	 */
	public static boolean backwardApply(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (right._dir != Direction.BACKWARD)
			return false;
		if (null == unify(res, right._result))
			return false;

		if (indices != null) {
			// Map<CoindexedObject, CoindexedObject> indices = new
			// HashMap<CoindexedObject, CoindexedObject>();
			if (null == unify(right._argument, left, indices))
				return false;
			res.recursiveAttachHead(right._result, indices);
		} else if (null == unify(right._argument, left))
			return false;
		return true;
	}

	/**
	 * @SupportedOperation Forward Compose: X/Y Y/Z -> X/Z
	 * @param arg
	 * @return
	 */
	public static CategoryObject simpleForwardCompose(CategoryObject left, CategoryObject arg) {
		if (left._dir == Direction.FORWARD && arg._dir == Direction.FORWARD) {
			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (null != unify(left._argument, arg._result, indices)) {
				CategoryObject res = new CategoryObject(generateFromChild(
						left._result, indices), Direction.FORWARD,
						generateFromChild(arg._argument, indices));
				res.coindexWith(res._result._headObj);
				res.setFeature(new HashSet<CoindexedObject>(indices.values()));
				return res;
			}
		}
		return null;
	}

	/**
	 * @SupportedOperation forward Compose: X/Y Y/Z -> X/Z
	 * @return if the operation succeed
	 */
	public static boolean simpleForwardCompose(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		// branches return false could be removed, when doing parsing, we might
		// not perform the check
		if (left._dir != Direction.FORWARD || right._dir != Direction.FORWARD
				|| res._dir != Direction.FORWARD)
			return false;
		if (null == unify(res._result, left._result)
				|| null == unify(res._argument, right._argument))
			return false;
		if (indices != null) {
			// Map<CoindexedObject, CoindexedObject> indices = new
			// HashMap<CoindexedObject, CoindexedObject>();
			if (null == unify(left._argument, right._result, indices))
				return false;
			res._result.recursiveAttachHead(left._result, indices);
			res._argument.recursiveAttachHead(right._argument, indices);
			res.coindexWith(res._result._headObj);
		} else if (null == unify(left._argument, right._result))
			return false;
		return true;
	}

	/**
	 * @SupportedOperation backward Compose: Y\X Z\Y -> Z\X
	 * @param arg
	 *            Y\X
	 * @return
	 */
	public static CategoryObject simpleBackwardCompose(CategoryObject arg, CategoryObject right) {
		if (right._dir == Direction.BACKWARD && arg._dir == Direction.BACKWARD) {
			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (null != unify(right._argument, arg._result, indices)) {
				CategoryObject res = new CategoryObject(generateFromChild(
						right._result, indices), Direction.BACKWARD,
						generateFromChild(arg._argument, indices));
				res.coindexWith(res._result._headObj);
				res.setFeature(new HashSet<CoindexedObject>(indices.values()));
				return res;
			}
		}
		return null;
	}

	/**
	 * @SupportedOperation backward Compose: Y\X Z\Y -> Z\X
	 * @return if the operation succeed
	 */
	public static boolean simpleBackwardCompose(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (left._dir != Direction.BACKWARD || right._dir != Direction.BACKWARD
				|| res._dir != Direction.BACKWARD)
			return false;
		if (null == unify(res._result, right._result)
				|| null == unify(res._argument, left._argument))
			return false;
		if (indices != null) {
			// Map<CoindexedObject, CoindexedObject> indices = new
			// HashMap<CoindexedObject, CoindexedObject>();
			if (null == unify(right._argument, left._result, indices))
				return false;
			res._result.recursiveAttachHead(right._result, indices);
			res._argument.recursiveAttachHead(left._argument, indices);
			res.coindexWith(res._result._headObj); // XXX I'm not clear on
													// that..
													// here the head might
													// be
													// consistent with the
		} else if (null == unify(right._argument, left._result))
			return false;
		return true;
	}

	/**
	 * @SupportedOperation cross backward Compose: Y/Z X\Y -> X/Z
	 * @param arg
	 *            Y/Z
	 * @return X/Z
	 */
	public static CategoryObject crossBackwardCompose(CategoryObject arg, CategoryObject right) {
		if (right._dir == Direction.BACKWARD && arg._dir == Direction.FORWARD) {
			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (null != unify(right._argument, arg._result, indices)) {
				CategoryObject res = new CategoryObject(generateFromChild(
						right._result, indices), Direction.FORWARD,
						generateFromChild(arg._argument, indices));
				res.coindexWith(res._result._headObj);
				res.setFeature(new HashSet<CoindexedObject>(indices.values()));
				return res;
			}
		}
		return null;
	}

	/**
	 * @SupportedOperation cross backward Compose: Y/Z X\Y -> X/Z
	 * @return if the operation succeed
	 */
	public static boolean crossBackwardCompose(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (left._dir != Direction.FORWARD || right._dir != Direction.BACKWARD
				|| res._dir != Direction.FORWARD)
			return false;
		if (null == unify(res._result, right._result)
				|| null == unify(res._argument, left._argument))
			return false;

		if (indices != null) {
			// Map<CoindexedObject, CoindexedObject> indices = new
			// HashMap<CoindexedObject, CoindexedObject>();
			if (null == unify(right._argument, left._result, indices))
				return false;
			res._result.recursiveAttachHead(right._result, indices);
			res._argument.recursiveAttachHead(left._argument, indices);
			res.coindexWith(res._result._headObj);
		} else if (null == unify(right._argument, left._result))
			return false;
		return true;
	}

	/**
	 * @SupportedOperation cross forward compose: X/Y Y\Z -> X\Z
	 * @param arg
	 *            Y\Z
	 * @return
	 */
	public static CategoryObject crossForwardCompose(CategoryObject left, CategoryObject arg) {
		if (left._dir == Direction.FORWARD && arg._dir == Direction.BACKWARD) {
			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (null != unify(left._argument, arg._result, indices)) {
				CategoryObject res = new CategoryObject(generateFromChild(
						left._result, indices), Direction.BACKWARD,
						generateFromChild(arg._argument, indices));
				res.coindexWith(res._result._headObj);
				res.setFeature(new HashSet<CoindexedObject>(indices.values()));
				return res;
			}
		}
		return null;
	}

	/**
	 * @SupportedOperation cross forward compose: X/Y Y\Z -> X\Z
	 * @return if the operation succeed
	 */
	public static boolean crossForwardCompose(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (left._dir != Direction.FORWARD || right._dir != Direction.BACKWARD
				|| res._dir != Direction.BACKWARD)
			return false;
		if (null == unify(res._result, left._result)
				|| null == unify(res._argument, right._argument))
			return false;

		if (indices != null) {
			// Map<CoindexedObject, CoindexedObject> indices = new
			// HashMap<CoindexedObject, CoindexedObject>();
			if (null == unify(right._result, left._argument, indices))
				return false;
			res._result.recursiveAttachHead(left._result, indices);
			res._argument.recursiveAttachHead(right._argument, indices);
			res.coindexWith(res._result._headObj);
		} else if (null == unify(right._result, left._argument))
			return false;
		return true;
	}

	// -1 means no restriction
	public static int MaxSecondaryFunctorArity = -1;

	/**
	 * @SupportedOperation Generalized Forward Compose: A/B + (B/C)/$1 ->(A/C)/$
	 * @param (B/C)/D
	 * @return
	 */
	public static CategoryObject generalizedForwardCompose(CategoryObject left, CategoryObject arg) {
		if (left._dir == Direction.FORWARD && arg._dir == Direction.FORWARD) {
			if (left._argument.depth() >= arg._result.depth())
				return null;
			if (MaxSecondaryFunctorArity > 0
					&& arg._result.depth() - left._argument.depth() > MaxSecondaryFunctorArity)
				return null;
			Stack<CategoryObject> stk = new Stack<CategoryObject>();
			CategoryObject next = arg;

			while (left._argument.depth() < next._result.depth()) {
				// if (next._result.dir != Direction.FORWARD)
				// return null;
				stk.push(next);
				next = next._result;
			}

			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			// next should look like B/C
			if (null == unify(left._argument, next._result, indices))
				return null;

			CategoryObject res = new CategoryObject(generateFromChild(left._result,
					indices), Direction.FORWARD, generateFromChild(
					next._argument, indices));
			res.coindexWith(res._result._headObj);

			// CategoryObject res = generateFromChild(_result, indices);
			while (stk.size() > 0) {
				CategoryObject n = stk.pop();
				res = new CategoryObject(res, n._dir, // Direction.FORWARD,
						generateFromChild(n._argument, indices));
				res.attachHead(n, indices);
			}
			res.setFeature(new HashSet<CoindexedObject>(indices.values()));
			return res;
		}
		return null;
	}

	/**
	 * @SupportedOperation Generalized Forward Compose: A/B + (B/C)/$ -> (A/C)/$
	 * @return if the operation succeed
	 */
	public static boolean generalizedForwardCompose(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (left._dir != Direction.FORWARD || right.depth() < 2)
			return false;
		if (left._argument.depth() >= right._result.depth()
				|| res.depth() != left._result.depth() + right.depth()
						- left._argument.depth())
			return false;
		if (MaxSecondaryFunctorArity > 0
				&& right._result.depth() - left._argument.depth() > MaxSecondaryFunctorArity)
			return false;

		Stack<CategoryObject> resStack = new Stack<CategoryObject>();
		CategoryObject resNext = res;
		Stack<CategoryObject> rightStack = new Stack<CategoryObject>();
		CategoryObject rightNext = right;

		while (left._argument.depth() < rightNext._result.depth()) {
			// rightNext.result: don't worry about null pointer
			// if (rightNext.dir != Direction.FORWARD
			// || resNext.dir != Direction.FORWARD)
			// return false;
			// XXX I think all the functor should be Forward as indicated in
			// manual,
			// but actually in Treebank, though in low frequency, such rules
			// appeard.
			if (rightNext._dir != resNext._dir)
				return false;
			if (null == unify(resNext._argument, rightNext._argument))
				return false;
			resStack.push(resNext);
			rightStack.push(rightNext);
			resNext = resNext._result;
			rightNext = rightNext._result;
		}
		// rightNext looks like B/C, and resNext looks like A/C , left is A/B
		if (rightNext._dir != Direction.FORWARD
				|| resNext._dir != Direction.FORWARD)
			return false;
		if (null == unify(resNext._result, left._result))
			return false;

		// Map<CoindexedObject, CoindexedObject> indices = attachHead ? new
		// HashMap<CoindexedObject, CoindexedObject>()
		// : null;
		if (null == unify(rightNext._result, left._argument, indices))
			return false;

		if (indices != null) {
			resNext._result.recursiveAttachHead(left._result, indices);
			resNext._argument.recursiveAttachHead(rightNext._argument, indices);
			resNext.attachHead(left._result, indices);

			while (resStack.size() > 0) {
				resNext = resStack.pop();
				rightNext = rightStack.pop();
				resNext._argument.recursiveAttachHead(rightNext._argument,
						indices);
				resNext.attachHead(rightNext, indices);
			}
		}
		return true;
	}

	/**
	 * @SupportedOperation Generalized Backward Compose: (B\C)\$ A\B -> (A\C)\$
	 * @param (B\C)\$
	 * @return
	 */
	public static CategoryObject generalizedBackwardCompose(CategoryObject arg, CategoryObject right) {
		if (right._dir == Direction.BACKWARD && arg._dir == Direction.BACKWARD) {
			if (right._argument.depth() >= arg._result.depth())
				return null;
			if (MaxSecondaryFunctorArity > 0
					&& arg._result.depth() - right._argument.depth() > MaxSecondaryFunctorArity)
				return null;
			Stack<CategoryObject> stk = new Stack<CategoryObject>();
			CategoryObject next = arg;

			// next should look like B/C
			while (right._argument.depth() < next._result.depth()) {
				// XXX I think all the functor should be backward as indicated
				// in manual,
				// but actually in Treebank, though in low frequency, such rules
				// appeard.
				// if (next._result.dir != Direction.BACKWARD)
				// return null;
				stk.push(next);
				next = next._result;
			}
			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (null == unify(right._argument, next._result, indices))
				return null;

			CategoryObject res = new CategoryObject(generateFromChild(right._result,
					indices), Direction.BACKWARD, generateFromChild(
					next._argument, indices));
			res.coindexWith(res._result._headObj);

			while (stk.size() > 0) {
				CategoryObject n = stk.pop();
				res = new CategoryObject(res, n._dir,// Direction.BACKWARD,
						generateFromChild(n._argument, indices));
				res.attachHead(n, indices);
			}
			res.setFeature(new HashSet<CoindexedObject>(indices.values()));
			return res;
		}
		return null;
	}

	/**
	 * @SupportedOperation GeneralizedBackwardCompose: (B\C)\$ A\B -> (A\C)\$
	 * @return if the operation succeed
	 */
	public static boolean generalizedBackwardCompose(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (right._dir != Direction.BACKWARD || left.depth() < 2)
			return false;
		if (right._argument.depth() >= left._result.depth()
				|| res.depth() != right._result.depth() + left.depth()
						- right._argument.depth())
			return false;
		if (MaxSecondaryFunctorArity > 0
				&& left._result.depth() - right._argument.depth() > MaxSecondaryFunctorArity)
			return false;

		Stack<CategoryObject> resStack = new Stack<CategoryObject>();
		CategoryObject resNext = res;
		Stack<CategoryObject> rightStack = new Stack<CategoryObject>();
		CategoryObject rightNext = left;

		while (right._argument.depth() < rightNext._result.depth()) {
			// rightNext.result: don't worry about null pointer
			// if (rightNext.dir != Direction.BACKWARD
			// || resNext.dir != Direction.BACKWARD)
			// return false;

			// XXX I think all the functor should be backward as indicated in
			// manual,
			// but actually in Treebank, though in low frequency, such rules
			// appeard.
			if (rightNext._dir != resNext._dir)
				return false;
			if (null == unify(resNext._argument, rightNext._argument))
				return false;
			resStack.push(resNext);
			rightStack.push(rightNext);
			resNext = resNext._result;
			rightNext = rightNext._result;
		}
		// rightNext looks like B/C, and resNext looks like A/C , left is A/B
		if (rightNext._dir != Direction.BACKWARD
				|| resNext._dir != Direction.BACKWARD)
			return false;
		if (null == unify(resNext._result, right._result))
			return false;

		// Map<CoindexedObject, CoindexedObject> indices = attachHead ? new
		// HashMap<CoindexedObject, CoindexedObject>()
		// : null;
		if (null == unify(rightNext._result, right._argument, indices))
			return false;

		if (indices != null) {
			resNext._result.recursiveAttachHead(right._result, indices);
			resNext._argument.recursiveAttachHead(rightNext._argument, indices);
			resNext.attachHead(right._result, indices);

			while (resStack.size() > 0) {
				resNext = resStack.pop();
				rightNext = rightStack.pop();
				resNext._argument.recursiveAttachHead(rightNext._argument,
						indices);
				resNext.attachHead(rightNext, indices);
			}
		}
		return true;
	}

	/**
	 * @SupportedOperation Generalized Forward Cross Compose: A/B + (B\C)$ ->
	 *                     (A\C)$
	 * @param arg
	 * @return
	 */
	public static CategoryObject generalizedForwardCrossCompose(CategoryObject left, CategoryObject arg) {
		if (left._dir == Direction.FORWARD && arg._dir != Direction.BASIC) {
			// && arg._result.dir == Direction.FORWARD) {
			if (left._argument.depth() >= arg._result.depth())
				return null;
			if (MaxSecondaryFunctorArity > 0
					&& arg._result.depth() - left._argument.depth() > MaxSecondaryFunctorArity)
				return null;
			Stack<CategoryObject> stk = new Stack<CategoryObject>();
			CategoryObject next = arg;

			// if success, next should look like B/C
			while (left._argument.depth() < next._result.depth()) {
				stk.push(next);
				next = next._result; // no exception will happen here since its
										// depth is guaranteed
			}

			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (next._dir != Direction.BACKWARD
					|| null == unify(left._argument, next._result, indices))
				return null;

			CategoryObject res = new CategoryObject(generateFromChild(left._result,
					indices), Direction.BACKWARD, generateFromChild(
					next._argument, indices));
			res.coindexWith(res._result._headObj);

			while (stk.size() > 0) {
				CategoryObject n = stk.pop();
				res = new CategoryObject(res, n._dir, generateFromChild(
						n._argument, indices));
				res.attachHead(n, indices);
			}
			res.setFeature(new HashSet<CoindexedObject>(indices.values()));
			return res;
		}
		return null;
	}

	/**
	 * @SupportedOperation Generalized Forward Cross Compose: A/B + (B\C)$ ->
	 *                     (A\C)$
	 * @return if the operation succeed
	 */
	public static boolean generalizedForwardCrossCompose(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (left._dir != Direction.FORWARD || right.depth() < 2)
			return false;
		if (left._argument.depth() >= right._result.depth()
				|| res.depth() != left._result.depth() + right.depth()
						- left._argument.depth())
			return false;
		if (MaxSecondaryFunctorArity > 0
				&& right._result.depth() - left._argument.depth() > MaxSecondaryFunctorArity)
			return false;

		Stack<CategoryObject> resStack = new Stack<CategoryObject>();
		CategoryObject resNext = res;
		Stack<CategoryObject> rightStack = new Stack<CategoryObject>();
		CategoryObject rightNext = right;

		while (left._argument.depth() < rightNext._result.depth()) {
			// rightNext.result: don't worry about null pointer
			if (rightNext._dir != resNext._dir)
				return false;
			if (null == unify(resNext._argument, rightNext._argument))
				return false;
			resStack.push(resNext);
			rightStack.push(rightNext);
			resNext = resNext._result;
			rightNext = rightNext._result;
		}
		// rightNext looks like B\C, and resNext looks like A\C , left is A/B
		if (rightNext._dir != Direction.BACKWARD
				|| resNext._dir != Direction.BACKWARD)
			return false;
		if (null == unify(resNext._result, left._result))
			return false;

		// Map<CoindexedObject, CoindexedObject> indices = attachHead ? new
		// HashMap<CoindexedObject, CoindexedObject>()
		// : null;
		if (null == unify(rightNext._result, left._argument, indices))
			return false;

		if (indices != null) {
			resNext._result.recursiveAttachHead(left._result, indices);
			resNext._argument.recursiveAttachHead(rightNext._argument, indices);
			resNext.attachHead(left._result, indices);

			while (resStack.size() > 0) {
				resNext = resStack.pop();
				rightNext = rightStack.pop();
				resNext._argument.recursiveAttachHead(rightNext._argument,
						indices);
				resNext.attachHead(rightNext, indices);
			}
		}
		return true;
	}

	/**
	 * @SupportedOperation Generalized Backward Cross Compose: (B/C)$ + A\B ->
	 *                     (A/C)$
	 * @param (B/C)_D
	 * @return
	 */
	public static CategoryObject generalizedBackwardCrossCompose(CategoryObject arg, CategoryObject right) {
		if (right._dir == Direction.BACKWARD && arg._dir != Direction.BASIC) {
			// && arg._result.dir == Direction.FORWARD) {
			if (right._argument.depth() >= arg._result.depth())
				return null;
			if (MaxSecondaryFunctorArity > 0
					&& arg._result.depth() - right._argument.depth() > MaxSecondaryFunctorArity)
				return null;
			Stack<CategoryObject> stk = new Stack<CategoryObject>();
			CategoryObject next = arg;

			// if success, next should look like B/C
			while (right._argument.depth() < next._result.depth()) {
				stk.push(next);
				next = next._result;
			}

			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (next._dir != Direction.FORWARD
					|| null == unify(right._argument, next._result, indices))
				return null;

			CategoryObject res = new CategoryObject(generateFromChild(right._result,
					indices), Direction.FORWARD, generateFromChild(
					next._argument, indices));
			res.coindexWith(res._result._headObj);
			while (stk.size() > 0) {
				CategoryObject n = stk.pop();
				res = new CategoryObject(res, n._dir, generateFromChild(
						n._argument, indices));
				res.attachHead(n, indices);
			}
			res.setFeature(new HashSet<CoindexedObject>(indices.values()));
			return res;
		}
		return null;
	}

	/**
	 * @SupportedOperation Generalized Backward Cross Compose: (B/C)$ + A\B ->
	 *                     (A/C)$
	 * @return if the operation succeed
	 */
	public static boolean generalizedBackwardCrossCompose(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (right._dir != Direction.BACKWARD || left.depth() < 2)
			return false;
		if (right._argument.depth() >= left._result.depth()
				|| res.depth() != right._result.depth() + left.depth()
						- right._argument.depth())
			return false;
		if (MaxSecondaryFunctorArity > 0
				&& left._result.depth() - right._argument.depth() > MaxSecondaryFunctorArity)
			return false;

		Stack<CategoryObject> resStack = new Stack<CategoryObject>();
		CategoryObject resNext = res;
		Stack<CategoryObject> rightStack = new Stack<CategoryObject>();
		CategoryObject rightNext = left;

		while (right._argument.depth() < rightNext._result.depth()) {
			// rightNext.result: don't worry about null pointer
			if (rightNext._dir != resNext._dir)
				return false;
			if (null == unify(resNext._argument, rightNext._argument))
				return false;
			resStack.push(resNext);
			rightStack.push(rightNext);
			resNext = resNext._result;
			rightNext = rightNext._result;
		}
		// rightNext looks like B\C, and resNext looks like A\C , left is A/B
		if (rightNext._dir != Direction.FORWARD
				|| resNext._dir != Direction.FORWARD)
			return false;
		if (null == unify(resNext._result, right._result))
			return false;

		// Map<CoindexedObject, CoindexedObject> indices = attachHead ? new
		// HashMap<CoindexedObject, CoindexedObject>()
		// : null;
		if (null == unify(rightNext._result, right._argument, indices))
			return false;

		if (indices != null) {
			resNext._result.recursiveAttachHead(right._result, indices);
			resNext._argument.recursiveAttachHead(rightNext._argument, indices);
			resNext.attachHead(right._result, indices);

			while (resStack.size() > 0) {
				resNext = resStack.pop();
				rightNext = rightStack.pop();
				resNext._argument.recursiveAttachHead(rightNext._argument,
						indices);
				resNext.attachHead(rightNext, indices);
			}
		}
		return true;
	}

	/**
	 * @SupportedOperation Forward Crossing Substitution: (X/Y)\Z + Y\Z -> X\Z
	 * @SupportedOperation Forward Substitution: (X/Y)/Z + Y/Z -> X/Z
	 * 
	 * @param arg
	 *            Y_Z
	 * @result X_Z
	 */
	public static CategoryObject forwardSubstitute(CategoryObject left, CategoryObject arg) {
		if (left._dir == arg._dir && left._dir != Direction.BASIC
				&& left._result._dir == Direction.FORWARD) {
			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (null != unify(left._result._argument, arg._result, indices)
					&& null != unify(left._argument, arg._argument, indices)) {
				CategoryObject res = new CategoryObject(generateFromChild(
						left._result._result, indices), arg._dir, generateFromChild(
						arg._argument, indices));
				res.coindexWith(res._result._headObj);
				res._argument.recursiveAttachHead(left._argument, indices);
				res.setFeature(new HashSet<CoindexedObject>(indices.values()));
				return res;
			}
		}
		return null;
	}

	/**
	 * @SupportedOperation: Forward Crossing Substitution: (X/Y)\Z + Y\Z -> X\Z
	 * @SupportedOperation: Forward Substitution: (X/Y)/Z + Y/Z -> X/Z
	 * @return if the operation succeed
	 */
	public static boolean forwardSubstitute(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (res._dir == Direction.BASIC || left._dir != res._dir
				|| right._dir != res._dir
				|| left._result._dir != Direction.FORWARD)
			return false;

		if (null == unify(res._result, left._result._result)
				|| null == unify(res._argument, right._argument)
				|| null == unify(res._argument, left._argument))
			return false;

		// Map<CoindexedObject, CoindexedObject> indices = attachHead ? new
		// HashMap<CoindexedObject, CoindexedObject>()
		// : null;
		if (null == unify(left._result._argument, right._result, indices)
				|| null == unify(left._argument, right._argument, indices))
			return false;

		if (indices != null) {
			res._result.recursiveAttachHead(left._result._result, indices);
			res._argument.recursiveAttachHead(left._argument, indices);
			res._argument.recursiveAttachHead(right._argument, indices);
			res.coindexWith(res._result._headObj);
		}
		return true;
	}

	/**
	 * @SupportedOperation: Backward Crossing Substitution: Y/Z + (X\Y)/Z -> X/Z
	 * @SupportedOperation: Backward Substitution: Y\Z + (X\Y)\Z + -> X\Z
	 * @param arg
	 *            Y_Z
	 * @result X_Z
	 */
	public static CategoryObject backwardSubstitute(CategoryObject arg, CategoryObject right) {
		if (right._dir == arg._dir && right._dir != Direction.BASIC
				&& right._result._dir == Direction.BACKWARD) {
			Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
			if (null != unify(right._result._argument, arg._result, indices)
					&& null != unify(right._argument, arg._argument, indices)) {
				CategoryObject res = new CategoryObject(generateFromChild(
						right._result._result, indices), arg._dir, generateFromChild(
						arg._argument, indices));
				res.coindexWith(res._result._headObj);
				res._argument.recursiveAttachHead(right._argument, indices);
				res.setFeature(new HashSet<CoindexedObject>(indices.values()));
				return res;
			}
		}
		return null;
	}

	/**
	 * @SupportedOperation: Backward Crossing Substitution: Y/Z + (X\Y)/Z -> X/Z
	 * @SupportedOperation: Backward Substitution: Y\Z + (X\Y)\Z + -> X\Z
	 * @return if the operation succeed
	 */
	public static boolean backwardSubstitute(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (res._dir == Direction.BASIC || left._dir != res._dir
				|| right._dir != res._dir
				|| right._result._dir != Direction.BACKWARD)
			return false;

		if (null == unify(res._result, right._result._result)
				|| null == unify(res._argument, right._argument)
				|| null == unify(res._argument, left._argument))
			return false;

		// Map<CoindexedObject, CoindexedObject> indices = attachHead ? new
		// HashMap<CoindexedObject, CoindexedObject>()
		// : null;
		if (null == unify(right._result._argument, left._result, indices)
				|| null == unify(left._argument, right._argument, indices))
			return false;
		if (indices != null) {
			res._result.recursiveAttachHead(right._result._result, indices);
			res._argument.recursiveAttachHead(left._argument, indices);
			res._argument.recursiveAttachHead(right._argument, indices);
			res.coindexWith(res._result._headObj);
		}
		return true;
	}

	/**
	 * @SupportedOperation: X -> T/(T\X)
	 * @FromManual: Type-raising a constituent with category w and lexical head
	 *              H to T/(T\X).results in a category whose head is H. The two
	 *              T categories unify, and have the same head as the argument
	 *              T\X.
	 * @param target
	 *            (T\X) in the above operation
	 * @return
	 */
	public CategoryObject forwardTypeRaising(CategoryObject target) {
		// target should be the case like: T\X
		if (target._dir == Direction.BACKWARD
				&& null != unify(this, target._argument)) {
			// Let's indicate it in this way: T1/(T2\X)
			CategoryObject T1 = fromPredArgCat(target._result.toPredArgCat());
			CategoryObject T2 = fromPredArgCat(target._result.toPredArgCat());
					//new CategoryObject(target._result.toPredArgCat(), true);

			CategoryObject X = generateFromChild(this);
			// (T2\X) has the head with T2, then have the same head with T1.
			CategoryObject TX = new CategoryObject(T2, Direction.BACKWARD, X);
			// The new cat has the same head with T1, thus have the head with
			// (T2\X)
			TX.recursiveAttachHead(target);
			// T1 and T2 are co-indexed
			T1.coindexWith(T2._headObj);
			CategoryObject res = new CategoryObject(T1, Direction.FORWARD, TX);
			res.coindexWith(res._result._headObj);
			return res;
		}
		return null;
	}

	/**
	 * @SupportedOperation: X -> T\(T/X).
	 * @FromManual: Type-raising a constituent with category w and lexical head
	 *              H to T\(T/X).results in a category whose head is H. The two
	 *              T categories unify, and have the same head as the argument
	 *              T/X.
	 * @param target
	 *            (T/X) in the above operation
	 * @return
	 */
	public CategoryObject backwardTypeRaising(CategoryObject target) {
		if (target._dir == Direction.FORWARD
				&& null != unify(this, target._argument)) {
			// Let's indicate it in this way: T1/(T2\X)
			CategoryObject T1 = CategoryObject.fromPredArgCat(target._result.toPredArgCat());
			CategoryObject T2 = CategoryObject.fromPredArgCat(target._result.toPredArgCat());

			CategoryObject X = generateFromChild(this);
			// (T2\X) has the head with T2, then have the same head with T1.
			CategoryObject TX = new CategoryObject(T2, Direction.FORWARD, X);
			// The new cat has the same head with T1, thus have the head with
			// (T2\X)
			TX.recursiveAttachHead(target);
			// T1 and T2 are co-indexed
			T1.coindexWith(T2._headObj);
			CategoryObject res = new CategoryObject(T1, Direction.BACKWARD, TX);
			res.coindexWith(res._result._headObj);
			return res;
		}
		return null;
	}

	public void GenerateBlankCoobj(){
		List<CategoryObject> objs = new ArrayList<CategoryObject>();
		objs.add(this);
		while (!objs.isEmpty()){
			CategoryObject next = objs.remove(0);
			next.coindexWith(null);
			if (next.depth() > 0){
				objs.add(next._argument);
				objs.add(next._result);
			}
		}
	}
	
	public static boolean unknown(CategoryObject child, CategoryObject res, Map<CoindexedObject, CoindexedObject> indices){
		if (indices != null)
			res.GenerateBlankCoobj();
		return true;
	}
	
	public static CategoryObject unknown(CategoryObject child){
		return null;
	}
	
	public static boolean unknown(CategoryObject left, CategoryObject right, CategoryObject res, Map<CoindexedObject, CoindexedObject> indices){
		if (indices != null)
			res.GenerateBlankCoobj();
		return true;
	}
	
	public static CategoryObject unknown(CategoryObject left, CategoryObject right){
		return null;
	}
	
	private static Map<String, String> extraCats = new HashMap<String, String>();
	static {
		// put only coindex object
		extraCats.put("{S\\NP}\\{S\\NP}", "{S_1\\NP_2}_1\\{S_1\\NP_2}_1");
	}

	/**
	 * Cats from nowhere indicate things
	 * like T in [X -> T\(T/X)], or S_x in the [S_y\NP -> (S_x\NP)\(S_x\NP)].
	 * these cats never appear in child node. If they are complex, they have to
	 * assigned a head by extraCats map!!!
	 * 
	 * @TuCao hehe, your code is too simple, sometimes naive!
	 * @return
	 */
	public static boolean makeHeadFromNowhere(CategoryObject cat) {
		// for each part, if no head is attached
		if (cat._depth == 0)
			cat.coindexWith(null);
		else {
			String predArgCat = extraCats.get(cat.toString());
			if (predArgCat == null)
				return false;
			else
				makeCoindexFromString(predArgCat, cat);
		}
		return true;
	}

	/**
	 * Assume we have categoryObject name S/NP with no _headObj, and a string
	 * "S/NP_1", we try to appoint proper co-index for the category. If some
	 * part of the cat already carry _headObj, nothing on this part will be done
	 * 
	 * @param predArgCat
	 * @param catWithNoHead
	 * @return false if the cat and the string not match
	 */
	public static boolean makeCoindexFromString(String predArgCat,
			CategoryObject catWithNoHead) {
		// Map<Integer, CoindexedObject> indices;
		CategoryObject indexedObj = CategoryObject.fromPredArgCat(predArgCat);
		Map<CoindexedObject, Integer> referredIndices = new HashMap<CoindexedObject, Integer>();
		Map<Integer, CoindexedObject> todoIndices = new HashMap<Integer, CoindexedObject>();
		
		List<CategoryObject> indexedCache = new ArrayList<CategoryObject>();
		List<CategoryObject> todoCache = new ArrayList<CategoryObject>();
		
		indexedCache.add(indexedObj);
		todoCache.add(catWithNoHead);
		
		while (indexedCache.size() > 0) {
			CategoryObject next = indexedCache.remove(0);
			CategoryObject todoNext = todoCache.remove(0);
			
			if (!referredIndices.containsKey(next._headObj)) {
				referredIndices.put(next._headObj, referredIndices.size());
				if (todoNext._headObj == null) //XXX here reserve the original head
					todoNext.coindexWith(null);
				todoIndices.put(todoIndices.size(), todoNext._headObj);
			}else{
				int index = referredIndices.get(next._headObj);
				todoNext.coindexWith(todoIndices.get(index));
			}
			if (next._depth > 0) {
				indexedCache.add(next._argument);
				indexedCache.add(next._result);
				todoCache.add(todoNext._argument);
				todoCache.add(todoNext._result);
			}
		}
		return false;
	}

	/**
	 * @SupportedOperation: X -> T\(T/X)
	 * @SupportedOperation: X -> T/(T\X)
	 * @return if the operation succeed TODO T complex category will make
	 *         trouble!
	 */
	public static boolean typeRaising(CategoryObject x, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		if (res._dir == Direction.BASIC || res._argument._dir == Direction.BASIC
				|| res._dir == res._argument._dir)
			return false;

		if (null == unify(res._argument._argument, x))
			return false;
		if (null == unify(res._result, res._argument._result))
			return false;

		if (indices != null) {

			makeHeadFromNowhere(res._argument._result);
			res._result.recursiveCoindexWith(res._argument._result);
			res._argument.coindexWith(res._argument._result._headObj);

			res._argument._argument.recursiveAttachHead(x, indices);
			res.coindexWith(res._argument._argument._headObj);
		}
		return true;
	}

	// TypeChangingSimple
	// TypeChangingN
	// TypeChangingV
	// BinaryTypeChanging
	// Manual

	public static CategoryObject unaryTypeChanging(CategoryObject child) {
		// NP -> N is the most common one
		if (child._category.equals("N")) {
			CategoryObject NP = generateFromChild(child);
			NP._category = "NP"; // I think this way best perform changing rule
			return NP;
		}
		// TODO here's a trouble: S[ng]\NP could generate more than one cat
		return null;
	}

	/**
	 * @SupportedOperation N -> NP and other atomic category changing (just to
	 *                     cover the grammar)
	 * @param res
	 * @param child
	 * @param indices
	 * @return
	 */
	public static boolean unaryTypeChangingSimple(CategoryObject child,
			CategoryObject res, Map<CoindexedObject, CoindexedObject> indices) {
		if (res._dir == Direction.BASIC && child._dir == Direction.BASIC) {
			if (indices != null)
				res.attachHead(child, indices);
			return true;
		}
		if (child._dir != Direction.BASIC && null != unify(child._result, res)){
			if (indices != null)
				res.recursiveAttachHead(child._result, indices);
			return true;
		}
		return false;
	}

	/**
	 * @SupportedOperation S[??]$NP_x -> NP_x\NP_x
	 * @param res
	 * @param child
	 * @param indices
	 * @return
	 */
	public static boolean unaryTypeChangingN(CategoryObject child,
			CategoryObject res, Map<CoindexedObject, CoindexedObject> indices) {
		if (child._dir != Direction.BASIC
				&& null != unify(res, fromPlainCat("NP\\NP"))
				&& null != unify(child._argument, fromPlainCat("NP"))
				&& null != unify(child._result, fromPlainCat("S"))) {
			if (indices != null) {
				res.attachHead(child, indices);
				res._argument.attachHead(child._argument, indices);
				res._result.attachHead(child._argument, indices);
			}
			return true;
		}
		return false;
	}

	/**
	 * @SupportedOperation S[X]$NP_x -> (S_y\NP_x)_y\(S_y\NP_x)_y
	 * @param res
	 * @param child
	 * @param indices
	 * @return
	 */
	public static boolean unaryTypeChangingV(CategoryObject child,
			CategoryObject res, Map<CoindexedObject, CoindexedObject> indices) {
		if (child._dir != Direction.BASIC
				&& null != unify(res, fromPlainCat("(S\\NP)\\(S\\NP)"))
				&& null != unify(child._argument, fromPlainCat("NP"))
				&& null != unify(child._result, fromPlainCat("S"))) {
			if (indices != null) {
				res.attachHead(child, indices);
//				makeHeadFromNowhere(res);
				res._result.coindexWith(null);
				res._argument.coindexWith(res._result._headObj);
				res._result._result.coindexWith(res._result._headObj);
				res._result._argument.attachHead(child._argument, indices);
				res._argument._result.coindexWith(res._result._headObj);
				res._argument._argument.attachHead(child._argument, indices);
			}
			return true;
		}
		return false;
	}

	/**
	 * if X is to do type changing, let's make it two step.
	 * 
	 * @SupportedOperation X + conj(or punc) -> X[conj]
	 * @SupportedOperation conj(or punc) + X -> X[conj]
	 * @SupportedOperation X[conj] + X -> X
	 * @SupportedOperation X + X[conj] -> X
	 * @param left
	 * @param right
	 * @return
	 */
	public static CategoryObject coordination(CategoryObject left,
			CategoryObject right) {
		Map<CoindexedObject, CoindexedObject> indices = new HashMap<CoindexedObject, CoindexedObject>();
		CategoryObject res = null;
		// X[conj] + X -> X
		if (//(left.isConjunctConstituent || right.isConjunctConstituent)
				null != unify(left, right, indices)) {
			res = generateFromChild(left, indices);
			res.attachHead(right, indices);
			res.setFeature(new HashSet<CoindexedObject>(indices.values()));
		} else if (left._category.equals("conj") || left.isPunctuation()) {
			res = generateFromChild(right);
			res._isConjunctConstituent = true;
		} else if (right._category.equals("conj") || right.isPunctuation()) {
			res = generateFromChild(left);
			res._isConjunctConstituent = true;
//		} else if (null != unify(left, right, indices)){
//			res = generateFromChild(left, indices);
//			res.attachHead(right, indices);
//			res.setFeature(new HashSet<CoindexedObject>(indices.values()));
		}
		return res;
	}

	/**
	 * @SupportedOperation X + conj (or punc) -> X[conj]
	 * @SupportedOperation conj (or punc) + X -> X[conj]
	 * @SupportedOperation X[conj] + X -> X
	 * @SupportedOperation X + X[conj] -> X
	 * @param left
	 * @param right
	 * @param attachHead
	 * @return
	 */
	public static boolean coordination(CategoryObject left,
			CategoryObject right, CategoryObject res,
			Map<CoindexedObject, CoindexedObject> indices) {
		// Map<CoindexedObject, CoindexedObject> indices = attachHead ? new
		// HashMap<CoindexedObject, CoindexedObject>()
		// : null;
		if (//(left.isConjunctConstituent || right.isConjunctConstituent) &&
				null != unify(left, right, indices)
				&& null != unify(left, res) && null != unify(right, res)) {
			if (indices != null) {
				res.recursiveAttachHead(left, indices);
				res.recursiveAttachHead(right, indices);
			}
			return true;
			// XXX though the result should be X[conj], but lots of grammar in
			// the tree bank does not has the [conj] part
		} else if (// null != unify(right, res)
		(left._category.equals("conj") || left.isPunctuation())) {
			if (null != unify(right, res)) {
				if (indices != null)
					res.recursiveAttachHead(right, indices);
				return true;
			} else if (unaryTypeChangingSimple(right, res, indices))
				return true;
			else if (unaryTypeChangingN(right, res, indices))
				return true;
			else if (unaryTypeChangingV(right, res, indices))
				return true;
		} else if (// null != unify(left, res)
		(right._category.equals("conj") || right.isPunctuation())) {
			if (null != unify(left, res)) {
				if (indices != null)
					res.recursiveAttachHead(left, indices);
				return true;
			} else if (unaryTypeChangingSimple(left, res, indices))
				return true;
			else if (unaryTypeChangingN(left, res, indices))
				return true;
			else if (unaryTypeChangingV(left, res, indices))
				return true;
		}
		return false;
	}
	
	public boolean isAdjunct(){
		if (toString().equals("{{S[to]\\NP}/{S[b]\\NP}}"))
			return true;
		if (_dir != Direction.BASIC ){//&& _result.toPredArgCat().equals(_argument.toPredArgCat())){
			// wsun: Why not
			// boolean equal? = _headObj==null ? _result.toString().equals(_argument.toString()) : _result.toPredArgCat()....
			boolean test;
			if (_headObj != null)
				test = _result.toPredArgCat().equals(_argument.toPredArgCat());
			else test = _result.toString().equals(_argument.toString());
			if (!test)
				return false;
			String catString = toString();
			Pattern p = Pattern.compile("\\[\\w+\\]");
			Matcher m1 = p.matcher(catString);
			int z = 0;
			while (m1.find(z)) {
				String feat = m1.group();
				if (!feat.equals("[adj]"))
					return false;
				z = m1.end();
			}
			return true;
		}
		return false;
	}
	
	public CategoryObject removeModifier(){
		if (isAdjunct())
			return _result.removeModifier();
		else return this;
	}

	private boolean isPunctuation() {
		if (_category.equals("LRB") || _category.equals("RRB"))
			return true;
		if (_category.equals("LQU") || _category.equals("RQU"))
			return true;
		return _category.matches("[.,:;'`?!()]+");
	}

	public static void main(String[] args){
//		String s = "(S[dcl]\\NP)[conj]";
//		String s = "  4 (((NP{Y}\\NP{Y}<1>){_}/((S[to]{Z}<2>\\NP{W*}){Z}/NP{Y*}<3>){Z}){_}/NP{W}<4>){_}";
//		String s = "2 (((PP{_}/PP{_}){_}/(PP{_}/PP{_}){_}<1>){_}\\(S[adj]{Y}\\NP{Z}){Y}<2>){_}";
		String s = "16 ((((((((((((((((QP{_}<1>/QP{_}<2>){_}<3>/QP{_}<4>){_}<5>/QP{_}<6>){_}<7>/QP{_}<8>){_}<9>/QP{_}<10>){_}<11>/QP{_}<12>){_}<13>/QP{_}<14>){_}<15>/QP{_}<16>){_}/conj{_}){_}/QP{_}){_}/QP{_}){_}/QP{_}){_}/QP{_}){_}/QP{_}){_}/QP{_}){_}/QP{_}){_}";
		CategoryObject cat = fromCACCats(s);
		System.out.println(cat);
		System.out.println(cat.toPredArgCat());
		System.out.println(cat.toMarkedupString());
		System.exit(0);
		
	}

	/*
	public static CategoryObject fromCACCats(String s, Map<Integer, Integer> marked2slot){
		
		Map<Character, Integer> tempIndices = new HashMap<Character, Integer>();
		Map<Integer, Character> markedSlots = new HashMap<Integer, Character>();
		int tempIndex = 100;
		String convert = null;
		if (s.charAt(0) == '(' || s.charAt(0) =='{')
			convert = s.substring(1, s.length() - 4);
		else convert = s.substring(0, s.length() - 3);
		//[X] indicate free feature. C&C seems to distinguish "no feature" with "free feature"
		convert = convert.replace("[X]", ""); 
		tempIndices.put('_', 0);
		
		Pattern p = Pattern.compile("\\{([_A-Z])\\**\\}<([0-9])>");
		Matcher m = p.matcher(convert);
		int z =0;
		while (m.find(z)){
			Character c = m.group(1).charAt(0);
			Integer i = Integer.parseInt(m.group(2));
			markedSlots.put(i, c);
			z = m.end();
		}
		convert = convert.replaceAll("<[0-9]>", "");
		convert = convert.replaceAll("\\{_\\}", "");
		
		p = Pattern.compile("\\{([A-Z])\\**\\}");
		m = p.matcher(convert);
		while (m.find()) {
			char character = m.group(1).charAt(0);
			Integer i = tempIndices.get(character);
			if (i == null)
				i = ++tempIndex;
			tempIndices.put(character, i);
			convert = m.replaceFirst("_" + i);
			m = p.matcher(convert);
		}
		Map<Integer, Integer> MarkedSlot2TempIndex = new HashMap<Integer, Integer>();
		for (Entry <Integer, Character> e: markedSlots.entrySet())
			MarkedSlot2TempIndex.put(e.getKey(), tempIndices.get(e.getValue()));
		markedSlots = null;
		tempIndices = null;
		
		Map<Integer, CoindexedObject> tempIndexToObj = new HashMap<Integer, CoindexedObject>();
		CategoryObject cat = new CategoryObject();
		cat.fromPredArgCat(convert, tempIndexToObj);
		cat.coindexWith(tempIndexToObj.get(0));
		
//		Map<Integer, CoindexedObject> markedSlot2obj = new HashMap<Integer, CoindexedObject>();
//		for (Entry<Integer, Integer> e: tempIndexToMarkedSlot.entrySet())
//			markedSlot2obj.put(e.getValue(), tempIndexToObj.get(e.getKey()));
////		for (Entry<Integer, CoindexedObject> e: tempIndexToObj.entrySet())
////			markedSlot2obj.put(e.getKey(), tempIndexToMarkedSlot.get(e.getValue()));
//		tempIndexToObj = null;
		
		CategoryObject next = cat;
		
		Map<CoindexedObject, Integer> obj2Slot = new HashMap<CoindexedObject, Integer>();
		for (int i = cat.depth() - 1; i >=0; --i){
			obj2Slot.put(next._argument._headObj, i);
//			Integer markedSlot = obj2markedSlot.get(next._argument._headObj);
//			for (Entry<Integer, CoindexedObject> e: tempIndexToObj.entrySet()){
//				if (e.getValue() == next._argument._headObj)
//					index2slot.put(MarkedSlot2TempIndex.get(e.getKey()), i);
//			}
//			if (markedSlot != null)
//				index2slot.put(markedSlot, i);
			next = next._result;
		}
		for (Entry<Integer, Integer> e: MarkedSlot2TempIndex.entrySet())
			marked2slot.put(e.getKey(), obj2Slot.get(tempIndexToObj.get(e.getValue())));
		return cat;
	}*/

//	public CategoryObject(String label, boolean isCoindexed) {
//		// for test
//		fromPredArgCat(label, isCoindexed);
//	}

}

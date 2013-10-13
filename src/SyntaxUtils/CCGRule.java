package SyntaxUtils;

import java.util.Comparator;

public abstract class CCGRule {
	public String result;
	protected int freq;
	public abstract int arity();
	
	public int headChild;
	
	public int freq() {
		// return freq.get(key);
		return freq;
	}
	
	public int increSeenCount(int f) {
		return freq += f;
	}
	
	public int increSeenCount() {
		return ++freq;
	}
	
	/**
	 * the key for a rule in a map
	 * @return
	 */
	public abstract String key();
		
	public abstract int typeOrdinal();
	
	public static Comparator<CCGRule> freqComparator = new Comparator<CCGRule>() {
		@Override
		public int compare(CCGRule o1, CCGRule o2) {

			if (o1.freq != o2.freq)
				return o1.freq > o2.freq ? -1 : 1;
			if (o1.typeOrdinal() == 0)
				return 1;
			if (o2.typeOrdinal() == 0)
				return -1;
			return o1.typeOrdinal() > o2.typeOrdinal() ? 1
					: o1.typeOrdinal() == o2.typeOrdinal() ? 0 : -1;
		}
	};
}

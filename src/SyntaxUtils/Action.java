package SyntaxUtils;

/**
 * 
 * @author C. Wang, wsun
 */
public class Action {
	private static enum ActionType {
		SHIFT, REDUCE, UNARY// , FINISH
	};

	// { SHIFT, FORWARDAPPLY, BACKWARDAPPLY, TYPERAISING, FORWARDCOMPOSE,
	// BACKWARDCOMPOSE };
	
	final ActionType _type;
	final CCGUnaryRule _uRule; // replace it with index in grammar
	final CCGBinaryRule _bRule;
	final String _tag;

	private Action(ActionType type, CCGUnaryRule u, CCGBinaryRule b, String tag) {
		_type = type;
		_uRule = u;
		_tag = tag;
		_bRule = b;
	}

	public static Action getShiftAction(String tag) {
		return new Action(ActionType.SHIFT, null, null, tag);
	}

	public static Action getUnaryAction(CCGUnaryRule uRule) {
		return new Action(ActionType.UNARY, uRule, null, null);
	}

	public static Action getReduceAction(CCGBinaryRule bRule) {
		return new Action(ActionType.REDUCE, null, bRule, null);
	}

	public boolean isShiftAction() {
		return _type == ActionType.SHIFT;
	}
	
	public boolean isUnaryReduceAction() {
		return _type == ActionType.UNARY;
	}
	
	public boolean isBinaryReduceAction() {
		return _type == ActionType.REDUCE;
	}

	@Override
	public String toString() {
		switch (_type) {
		case SHIFT:
			return _type.toString() + ":" + _tag;
		case REDUCE:
			return _type.toString() + ": " + _bRule;
		case UNARY:
			return _type.toString() + ": " + _uRule;
		default:
			return "Finish";
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o == this)
			return true;
		if (o == null)
			return false;
		if (!(o instanceof Action))
			return false;
		Action another = (Action) o;
		if (another._type != this._type)
			return false;
		switch (_type) {
		case SHIFT:
			String tag1 = _tag.replaceAll("_[0-9]+", "");
			String tag2 = another._tag.replaceAll("_[0-9]+", "");
			tag1 = tag1.replaceAll("\\{[_*A-Z]+\\}", "");
			tag2 = tag2.replaceAll("\\{[_*A-Z]+\\}", "");
			tag1 = tag1.replaceAll("<[0-9]+>", "");
			tag2 = tag2.replaceAll("<[0-9]+>", "");
			return tag1.equals(tag2);
			// if more than one co-indexed way exists for a category,
			// then we should use the Category.equals method
		case REDUCE:
			return _bRule.equals(another._bRule);
		case UNARY:
			return _uRule.equals(another._uRule);
		}
		return true; // for finish
	}

	// TODO not necessary currently
	// public int hashCode(){
	// }
}
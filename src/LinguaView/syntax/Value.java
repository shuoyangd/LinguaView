package LinguaView.syntax;

public class Value {
	public enum ValueType {
		SEM_FORM,
		AVM,
		SET_OF_AVM
	}
	
	public ValueType type() {
		if (this instanceof SetOfAttributeValueMatrix)
			return ValueType.SET_OF_AVM;
		else if (this instanceof AttributeValueMatrix)
			return ValueType.AVM;
		else if (this instanceof SemanticForm)
			return ValueType.SEM_FORM;
		else 
			return null;
	}
}
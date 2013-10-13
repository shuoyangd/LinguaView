package LinguaView.syntax;

public class Atomic extends Value {
	private String value;
	
	public Atomic() {
		
	}
	
	public Atomic(String val) {
		value = val;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String val) {
		value = val;
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof Atomic)) {
			return false;
		}
		else {
			return value.equals(((Atomic)other).value);
		}
	}
}
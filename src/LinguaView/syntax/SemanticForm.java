package LinguaView.syntax;

import java.util.HashSet;
import java.util.Set;

public class SemanticForm extends Value{
	private String predicate = new String();
	private Set<Attribute> arguments = new HashSet<Attribute>();
	
	public SemanticForm() {
		
	}
	
	public SemanticForm(String pred, String[] args) {
		setPred(pred);
		for(String arg: args) {
			Attribute attr = new Attribute(arg);
			addArgs(attr);
		}
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof SemanticForm)) {
			return false;
		}
		if(!predicate.equals(((SemanticForm)other).predicate)) {
			return false;
		}
		else {
			return argsEqual((SemanticForm)other);
			
		}
	}
	
	private boolean argsEqual(SemanticForm other) {
		if(this.argsContainAll(other.arguments) && other.argsContainAll(this.arguments)) {
			return true;
		}
		else {
			return false;
		}
	}
	
	private boolean argsContainAll(Set<Attribute> otherargs) {
		for(Attribute otherarg: otherargs) {
			if(!argContains(otherarg)) {
				return false;
			}
		}
		return true;
	}
	
	private boolean argContains(Attribute otherarg) {
		for(Attribute arg: arguments) {
			if(arg.equals(otherarg)) {
				return true;
			}
		}
		return false;
	}
	
	public String getPred() {
		return predicate;
	}
	
	public Set<Attribute> getArgs() {
		return arguments;
	}
	
	public String[] getStringArgs() {
		String[] args = new String[arguments.size()];
		int i = 0;
		for(Attribute argInAttr: arguments) {
			args[i] = argInAttr.getName();
			i++;
		}
		return args;
	}
	
	public void setPred(String pred) {
		predicate = pred;
	}
	
	public void addArgs(Attribute attr) {
		arguments.add(attr);
	}
	
	public void addArgs(String arg) {
		Attribute attr = new Attribute(arg);
		arguments.add(attr);
	}
}
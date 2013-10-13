package LinguaView.syntax;

import java.util.HashSet;
import java.util.Set;

public class SetOfAttributeValueMatrix extends Value{
	private Set<AttributeValueMatrix> avms = new HashSet<AttributeValueMatrix>();
	
	public Set<AttributeValueMatrix> getSet() {
		return avms;
	}
	
	public void setSet(Set<AttributeValueMatrix> newset) {
		avms = newset;
	}
	
	public void addAVM(AttributeValueMatrix avm) {
		avms.add(avm);
	}
	
	public void addAll(SetOfAttributeValueMatrix set) {
		avms.addAll(set.avms);
	}
	
	public boolean contain(AttributeValueMatrix other) {
		for(AttributeValueMatrix avm: avms) {
			if(avm.equals(other)) {
				return true;
			}
		}
		return false;
	}
	
	public boolean containsAll(SetOfAttributeValueMatrix otherSet) {
		for(AttributeValueMatrix avm: otherSet.avms) {
			if(!contain(avm)) {
				return false;
			}
		}
		return true;
	}
	
	public boolean equals(Object other) {
		if(!(other instanceof SetOfAttributeValueMatrix)) {
			return false;
		}
		else {
			SetOfAttributeValueMatrix otherSet = (SetOfAttributeValueMatrix)other;
			if(this.containsAll(otherSet) && otherSet.containsAll(this)) {
				return true;
			}
			else {
				return false;
			}
		}
	}
	
	public static void union(SetOfAttributeValueMatrix s1, SetOfAttributeValueMatrix s2) {
		Set<AttributeValueMatrix> avms1 = s1.avms;
		Set<AttributeValueMatrix> avms2 = s2.avms;
		avms1.addAll(avms2);
		avms2.addAll(avms1);
	}
}
package LinguaView.syntax;

import java.util.*;

public class Token {
	
	private Map<String, String> Properties = new HashMap<String, String>();
	private int id;
	private String head;
	
	public Token(int id) {
		this.id = id;
	}
	
	public void addProperty(String Attribute, String Value) {
		if(Attribute.equalsIgnoreCase("id")) {
			id = Integer.parseInt(Value);
		}
		else if(Attribute.equalsIgnoreCase("head")) {
			head = Value;
		}
		else {
			Properties.put(Attribute, Value);
		}
	}
	
	public String getProperty(String Attribute) {
		if(Attribute.equalsIgnoreCase("id")) {
			return String.valueOf(id);
		}
		else if(Attribute.equalsIgnoreCase("head")) {
			return head;
		}
		else {
			return Properties.get(Attribute);
		}
	}
	
	public Set<String> getPropertyList() {
		return Properties.keySet();
	}
	
	public int getID() {
		return id;
	}
	
	public String gethead() {
		return head;
	}
}
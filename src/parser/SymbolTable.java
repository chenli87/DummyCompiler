package parser;

import java.util.*;


public class SymbolTable {
	private Stack<String> stack;
	private LinkedHashMap<String, Instruction> table;
	
	public SymbolTable() {
		stack = new Stack<String>();
		table = new LinkedHashMap<String, Instruction>(109);
	}
	
	public Object[] symbolList() {
		return table.values().toArray();
	}
	
	public void put(String key, Instruction value) {
		table.put(key, value);
		stack.push(key);
	}
	
	public Instruction get(String key) {
		return table.get(key);
	}
	
	public void enterNewScope() {
		stack.push("-1");
	}
	
	public void exitCurrentScope() {
		while(stack.peek().compareTo("-1") != 0) {
			table.remove(stack.peek());
			stack.pop();
		}
		stack.pop();
	}
}

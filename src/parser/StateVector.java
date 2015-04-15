package parser;

import java.util.ArrayList;

public class StateVector {
	static class Link {
		public String var;
		public Instruction instruction;
		
		public Link(String name, Instruction in) {
			var = name;
			instruction = in;
		}
		
		public Link(Link link) {
			var = link.var;
			instruction = link.instruction;
		}
		public String toString() {
			return var + " " + instruction;
		}
	}
	
	public ArrayList<Link> localVars;
	
	public StateVector() {
		localVars = new ArrayList<Link>();
	}
	
	public StateVector(StateVector stateVector) {
		localVars = new ArrayList<Link>();
		for(int i=0; i<stateVector.localVars.size(); i++) {
			localVars.add(new Link(stateVector.localVars.get(i)));
		}
	}
	
	public void addLocalVar(String var, Instruction instruction) {
		Link link = new Link(var, instruction);
		localVars.add(link);
	}
	
	public void updateLocalVar(String var, Instruction instruction) {
		getLocalVar(var).instruction = instruction;
	}
	
	public Link getLocalVar(String name) {
		for(int i = 0; i < localVars.size(); i++) {
			if(localVars.get(i).var.compareTo(name) == 0) {
				return localVars.get(i);
			}
		}
		return null;
	}
}

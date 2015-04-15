package parser;

import java.util.*;

public class Function extends Instruction {
	public List<BasicBlock> basicBlocks;
	public int basicBlockSize;
	public String functionName;
	public int varCounter;
	public SymbolTable symbolTable;
	public int paramsNum;
	public boolean isReturn;
	
	public static class globalWithTag {
		Instruction var;
		boolean isUpdated;
		
		public globalWithTag(Instruction newVar, boolean update) {
			var = newVar;
			isUpdated = update;
		}
	}
	
	public Function() {
		symbolTable = new SymbolTable();
		name = "function";
		functionName = "";
		basicBlocks = new ArrayList<BasicBlock> ();
		basicBlockSize = -1;
		varCounter = 1;
		paramsNum = 0;
		
		instance_id--;
	}
	
	public Function(String function) {
		symbolTable = new SymbolTable();
		name = "function";
		functionName = function;
		basicBlocks = new ArrayList<BasicBlock> ();
		basicBlockSize = -1;
		varCounter = 1;
		paramsNum = 0;
		
		instance_id--;
	}
	
	public void updateSymbolTable() {
		for(int i=0; i<currentBasicBlock().stateVector.localVars.size(); i++) {
			symbolTable.put(currentBasicBlock().stateVector.localVars.get(i).var, 
			    currentBasicBlock().stateVector.localVars.get(i).instruction);
		}
	}
	
	public void addBasicBlock(BasicBlock basicBlock) {
		basicBlocks.add(basicBlock);
		basicBlockSize = basicBlocks.size() - 1;
		updateSymbolTable();
	}
	
	public BasicBlock currentBasicBlock() {
		return basicBlocks.get(basicBlockSize);
	}
}

package parser;

import java.util.*;

public class BasicBlock {
	public ArrayList<BasicBlock> predecessors, successors;
	public ArrayList<Instruction> instructions; 
	public ArrayList<Instruction> liveness;
	public int conditionalSuccessorNum, unconditionalSuccessorNum;
	public StateVector stateVector;
	
	public int blockID;
	
	private static int id = 0;
	
	public BasicBlock(ArrayList<BasicBlock> inputPredecessors) {
		predecessors = inputPredecessors;
		successors = new ArrayList<BasicBlock>();
		instructions = new ArrayList<Instruction>();
		liveness = new ArrayList<Instruction>();
		stateVector = new StateVector();
		blockID = id++;
	}
	
	public void addPredecessor(BasicBlock predecessor) {
		predecessors.add(predecessor);
	}
	
	public void addInstruction(Instruction i) {
		instructions.add(i);
	}
	
	public void addSuccessor(BasicBlock successor) {
		successors.add(successor);
	}
	
	public void setConditionalBranch(BasicBlock conditionalBranch) {
		successors.add(conditionalBranch);
		conditionalSuccessorNum = successors.size()-1;
	}
	
	public void setUnconditionalBranch(BasicBlock unconditionalBranch) {
			successors.add(unconditionalBranch);
			unconditionalSuccessorNum = successors.size()-1;
	}
	
	public BasicBlock getConditionalBranch() {
		return successors.get(conditionalSuccessorNum);
	}
	
	public BasicBlock getUnconditionalBranch() {
		return successors.get(unconditionalSuccessorNum);
	}
}


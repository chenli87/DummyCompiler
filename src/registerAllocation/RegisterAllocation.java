package registerAllocation;

import java.io.IOException;
import java.util.*;

import parser.*;
import parser.Instruction.Phi;
import parser.Instruction.Phi.FiPair;
import parser.Instruction.*;

public class RegisterAllocation {
	private Parser parser;
	private ArrayList<Function> functions;
	private ArrayList<Interference> interferenceGraph;	
	private ArrayList<Phi> fiList;			
	private ArrayList<Instruction> spills;	
	private Stack<Instruction> selectStack;	
	private static boolean checkPoint;
	
	public ArrayList<Instruction> globalMallocs;
	
	public RegisterAllocation(String fileName) throws IOException {
		parser = new Parser(fileName);
		functions = parser.parsing();
		globalMallocs = parser.globalMallocs;
		interferenceGraph = new ArrayList<Interference>();
		fiList = new ArrayList<Phi>();
		selectStack = new Stack<Instruction>();
		spills = new ArrayList<Instruction>();
		
	}
	
	public static class Interference {
		private Instruction name;
		private ArrayList<Instruction> conflictList;
		private ArrayList<Integer> availableReg;
		
		public Interference(Instruction instruction, ArrayList<Instruction> list) {
			name = instruction;
			conflictList = list;
			availableReg = new ArrayList<Integer>();
			for(int i=10; i<16; i++) {
				availableReg.add(i);
			}
		}
		
		public Interference(Interference interference) {
			name = interference.name;
			conflictList = new ArrayList<Instruction>();
			for(Instruction instruction: interference.conflictList) {
				conflictList.add(instruction);
			}
			
			availableReg = new ArrayList<Integer>();
			for(int i=0; i<interference.availableReg.size(); i++) {
				availableReg.add(interference.availableReg.get(i));
			}
		}
		
		public Instruction getName() {
			return name;
		}
		
		public boolean contains(Instruction instruction) {
			return conflictList.contains(instruction);
		}
		
		public void addInterference(Instruction instruction) {
			if(!conflictList.contains(instruction) && name != instruction) {
				conflictList.add(instruction);
				checkPoint = true;
			}
		}
		
		public String toString() {
			System.out.print(conflictList.size() + "     ");
			String result = name.toString() + ": (";
			int i;
			for(i=0; i<conflictList.size()-1; i++) {
				result += conflictList.get(i).toString() + ", ";
			}
			if(conflictList.size() > 0)
				result += conflictList.get(i);
			result += ")";
			return result;
		}
	}
	
	public Interference inGraph(Instruction instruction) {
		for(Interference interference: interferenceGraph) {
			if(interference.name == instruction) 
				return interference;
		}
		return null;
	}
	
	public void updateInterferenceGraph(ArrayList<Instruction> instructionList, Instruction newInstruction) {
		Interference temp;
		ArrayList<Instruction> newList = new ArrayList<Instruction>();
		
		for(Instruction instruction: instructionList) {
			temp = inGraph(instruction);
			if(temp != null) {
				temp.addInterference(newInstruction);
			
			} else {
				
				ArrayList<Instruction> tempList = new ArrayList<Instruction>();
				if(instruction != newInstruction) {
					tempList.add(newInstruction);
				}
				
				interferenceGraph.add(new Interference(instruction, tempList));
				checkPoint = true;
			}
			
			if(instruction != newInstruction) {
				
				newList.add(instruction);
			}
		}
		
		if(inGraph(newInstruction) == null) {
			interferenceGraph.add(new Interference(newInstruction, newList));
			checkPoint = true;
		} else {
			for(Instruction in: newList) {
				if(!inGraph(newInstruction).conflictList.contains(in)) {
					inGraph(newInstruction).conflictList.add(in);
				}
			}
		}
	}
	
	public void appendList(ArrayList<Instruction> target, ArrayList<Instruction> appending) {
		for(Instruction instruction: appending) {
			updateInterferenceGraph(target, instruction);
			if(!target.contains(instruction)) {
				target.add(instruction);
			}
		}
	}
	
	public void buildInterferenceGraph(Function function) {
		do {
			checkPoint = false;
			for(int i=function.basicBlocks.size()-1; i>=0; i--) {
				for(int j=function.basicBlocks.get(i).instructions.size()-1; j>=0; j--) {	
					if(Instruction.isUnaryInstruction(function.basicBlocks.get(i).instructions.get(j))) {	
						UnaryInstruction temp = (UnaryInstruction) function.basicBlocks.get(i).instructions.get(j);
												
						function.basicBlocks.get(i).liveness.remove(temp);
						
						if(temp.op.name.compareTo("constant") != 0 && temp.op.name.compareTo("array") != 0 && !temp.op.name.equals("var")) {
							
							updateInterferenceGraph(function.basicBlocks.get(i).liveness, temp.op);
							
							if(!function.basicBlocks.get(i).liveness.contains(temp.op)) {
								function.basicBlocks.get(i).liveness.add(temp.op);
							}
						}
					} else if(Instruction.isBinaryInstruction(function.basicBlocks.get(i).instructions.get(j))) {
						BinaryInstruction temp = (BinaryInstruction) function.basicBlocks.get(i).instructions.get(j);
					
						function.basicBlocks.get(i).liveness.remove(temp);
						
						if(temp.op1.name.compareTo("constant") != 0 && temp.op1.name.compareTo("array") != 0 && !temp.op1.name.equals("var")) {							
							updateInterferenceGraph(function.basicBlocks.get(i).liveness, temp.op1);
							
							if(!function.basicBlocks.get(i).liveness.contains(temp.op1))
								function.basicBlocks.get(i).liveness.add(temp.op1);
						}
						
						if(temp.op2.name.compareTo("constant") != 0 && temp.op2.name.compareTo("array") != 0 && !temp.op2.name.equals("var")) {							
							updateInterferenceGraph(function.basicBlocks.get(i).liveness, temp.op2);
							if(!function.basicBlocks.get(i).liveness.contains(temp.op2))
								function.basicBlocks.get(i).liveness.add(temp.op2);
						}
						
					} else if(function.basicBlocks.get(i).instructions.get(j).name.compareTo("call") == 0) {
						Call temp = (Call) function.basicBlocks.get(i).instructions.get(j);
						
						function.basicBlocks.get(i).liveness.remove(temp);
						
						for(Instruction instruction: temp.formalParams) {
							if(!instruction.name.equals("var") && !instruction.name.equals("constant") && !instruction.name.equals("array")) {
								updateInterferenceGraph(function.basicBlocks.get(i).liveness, instruction);
								if(!function.basicBlocks.get(i).liveness.contains(instruction))
									function.basicBlocks.get(i).liveness.add(instruction);
							}
						}		
					} else if(function.basicBlocks.get(i).instructions.get(j).name.compareTo("phi") == 0) {
						Phi temp = (Phi) function.basicBlocks.get(i).instructions.get(j);
						function.basicBlocks.get(i).liveness.remove(temp);
						
						if(!fiList.contains(temp))
							fiList.add(temp);
						
						int k = 0;
						for(FiPair pair: temp.fiPair) {	
							if(pair.instruction.name.compareTo("constant") != 0 && pair.instruction.name.compareTo("array") != 0 && !pair.instruction.name.equals("var")) {							
								updateInterferenceGraph(function.basicBlocks.get(i).predecessors.get(k).liveness, pair.instruction);
								
								if(!function.basicBlocks.get(i).predecessors.get(k).liveness.contains(pair.instruction)) {
									function.basicBlocks.get(i).predecessors.get(k).liveness.add(pair.instruction);
								}
							}
							k++;
						}
					} 
				}
				
				if(function.basicBlocks.get(i).predecessors != null) {
					for(BasicBlock predecessor: function.basicBlocks.get(i).predecessors) {
						if(predecessor != null) 
							appendList(predecessor.liveness, function.basicBlocks.get(i).liveness);
					}
				}
				function.basicBlocks.get(i).liveness.clear();				
			}
			function.basicBlocks.get(function.basicBlocks.size()-1).liveness.clear();
		} while(checkPoint);
	}
	
	private static Instruction pick(ArrayList<Interference> tempList) {
		for(Interference interference: tempList) {
			if(interference.conflictList.size() <= (interference.availableReg.size()-1)) 
				return interference.name;
		}
		return null;
	}
	
	private static Instruction spill(ArrayList<Interference> tempList) {
		Interference highestCost = tempList.get(0);
		
		for(int i=0; i<tempList.size(); i++) {
			if(highestCost.conflictList.size() < tempList.get(i).conflictList.size()) {
				highestCost = tempList.get(i);
			}
		}
		
		return highestCost.name;
	}
	
	private static void copy(ArrayList<Interference> target, ArrayList<Interference> source) {
		target.clear();
		for(Interference interference: source) {
			Interference temp = new Interference(interference);
			target.add(temp);
		}
	}
	
	private static void remove(ArrayList<Interference> list, Instruction instruction) {
		Interference temp = null;
		
		for(Interference interference: list) {
			if(interference.name != instruction) {
				interference.conflictList.remove(instruction);
			} else {
				temp = interference;
			}
		}
		list.remove(temp);
	}
	
	private void removeConflictReg(Instruction instruction, int regNum) {
		Interference temp = null;
		for(Interference interference: interferenceGraph) {
			if(interference.conflictList.contains(instruction)) {
				if(interference.availableReg.contains(regNum)) {
					interference.availableReg.remove(interference.availableReg.lastIndexOf(regNum));
				}
				
				interference.conflictList.remove(instruction);
			}
			
			if(interference.name == instruction) {
				temp = interference;
			}
		}
		interferenceGraph.remove(temp);
	}
	
	private ArrayList<Instruction> lookupConflicts(Instruction instruction) {
		for(Interference interference: interferenceGraph) {
			if(interference.name == instruction) {
				return interference.conflictList;
			}
		}
		return new ArrayList<Instruction>();
	}
	
	private ArrayList<Integer> lookupRegs(Instruction instruction) {
		for(Interference interference: interferenceGraph) {
			if(interference.name == instruction) {
				return interference.availableReg;
			}
		}
		
		ArrayList<Integer> availableReg = new ArrayList<Integer>();
		
		return availableReg;
	}
	
	public void eliminatePhis() {
		Phi fi;
		
		for(int i=fiList.size()-1; i>=0; i--) {
			fi = fiList.get(i);
			
			//treat constant as an instruction with no available register so that only move could happen
			if(fi.regID != 0) {					
				//assign the first attribute with the same register
				if(lookupRegs(fi.getArgument(0)).contains(fi.regID) && fi.getArgument(0).regID == 0) {
					fi.getArgument(0).regID = fi.regID;
					removeConflictReg(fi.getArgument(0), fi.regID);
				} else {
					//constant and global var should fall into this branch because they dont have any available register
					Register register = new Register(fi.regID);
					Move move = new Move(fi.getArgument(0), register);
					
					//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
					fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move);
				}
				
				//assign the second attribute with the same reg
				if(lookupRegs(fi.getArgument(1)).contains(fi.regID) && fi.getArgument(1).regID == 0) {
					fi.getArgument(1).regID = fi.regID;
					removeConflictReg(fi.getArgument(1), fi.regID);
				} else {
					//constant and global var should fall into this branch because they dont have any available register
					Register register = new Register(fi.regID);
					Move move = new Move(fi.getArgument(1), register);
					
					//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
					fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move);
				}
				
			} else if(fi.getArgument(0).regID != 0) {
				//fi does not have a register assigned
				if(lookupRegs(fi).contains(fi.getArgument(0).regID)) {
					fi.regID = fi.getArgument(0).regID;
					removeConflictReg(fi, fi.regID);
					
					//assign the second attribute with the same reg
					if(lookupRegs(fi.getArgument(1)).contains(fi.regID) && fi.getArgument(1).regID == 0) {
						fi.getArgument(1).regID = fi.regID;
						removeConflictReg(fi.getArgument(1), fi.regID);
					} else {
						Register register = new Register(fi.regID);
						Move move = new Move(fi.getArgument(1), register);
						
						//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
						fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move);
					}
				} else {
					if(fi.getArgument(1).regID == 0) {
						int reg = returnCommonRegFrom2(lookupRegs(fi), lookupRegs(fi.getArgument(1)));
						if(reg == 0) {
							if(lookupRegs(fi).size() > 0) {
								fi.regID = lookupRegs(fi).get(0);
								removeConflictReg(fi, fi.regID);
									
								Register register = new Register(fi.regID);
								
								Move move1 = new Move(fi.getArgument(0), register);
								//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
								fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move1);
								
								Move move2 = new Move(fi.getArgument(1), register);
								//add a move to the basicblock that contains fi's 2nd attribute and replace it with move in fi
								fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move2);
							} else {								
								Store store1 = new Store(fi.getArgument(0), fi);
								fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, store1);
								
								Store store2 = new Store(fi.getArgument(1), fi);
								fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, store2);
							}
						} else {
							fi.regID = reg;
							removeConflictReg(fi, fi.regID);
							
							fi.getArgument(1).regID = reg;
							removeConflictReg(fi.getArgument(1), reg);
							
							Register register = new Register(fi.regID);
							
							Move move1 = new Move(fi.getArgument(0), register);
							//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
							fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move1);
						}
					} else {
						if(lookupRegs(fi).contains(fi.getArgument(1).regID)) {
							//assign fi the same register as the second atribute
							fi.regID = fi.getArgument(1).regID;
							removeConflictReg(fi, fi.regID);
							
							Register register = new Register(fi.regID);
							
							Move move1 = new Move(fi.getArgument(0), register);
							//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
							fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move1);
						} else {
							//no common register available
							if(lookupRegs(fi).size() > 0) {
								fi.regID = lookupRegs(fi).get(0);
								removeConflictReg(fi, fi.regID);
								
								Register register = new Register(fi.regID);
								
								Move move1 = new Move(fi.getArgument(0), register);
								//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
								fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move1);
								
								Move move2 = new Move(fi.getArgument(1), register);
								//add a move to the basicblock that contains fi's 2nd attribute and replace it with move in fi
								fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move2);
							} else {
								Store store1 = new Store(fi.getArgument(0), fi);
								fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, store1);
								
								Store store2 = new Store(fi.getArgument(1), fi);
								fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, store2);
							}
						}
					}
				}
			} else if(fi.getArgument(1).regID != 0) {
				if(lookupRegs(fi).contains(fi.getArgument(1).regID)) {
					fi.regID = fi.getArgument(1).regID;
					removeConflictReg(fi, fi.regID);
					
					//assign the first attribute with the same reg
					if(lookupRegs(fi.getArgument(0)).contains(fi.regID)) {
						fi.getArgument(0).regID = fi.regID;
						removeConflictReg(fi.getArgument(0), fi.regID);
					} else {
						Register register = new Register(fi.regID);
						Move move = new Move(fi.getArgument(0), register);
						//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
						fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move);
					}
				} else {
					int reg = returnCommonRegFrom2(lookupRegs(fi), lookupRegs(fi.getArgument(0)));
					if(reg == 0) {
						if(lookupRegs(fi).size() > 0) {
							fi.regID = lookupRegs(fi).get(0);
							removeConflictReg(fi, fi.regID);
							
							Register register = new Register(fi.regID);
							
							Move move1 = new Move(fi.getArgument(0), register);
							//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
							fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move1);							
							
							Move move2 = new Move(fi.getArgument(1), register);
							//add a move to the basicblock that contains fi's 2nd attribute and replace it with move in fi
							fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move2);
						} else {							
							Store store1 = new Store(fi.getArgument(0), fi);
							fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, store1);
							
							Store store2 = new Store(fi.getArgument(1), fi);
							fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, store2);
						}
					} else {
						fi.regID = reg;
						removeConflictReg(fi, fi.regID);
						
						fi.getArgument(0).regID = reg;
						removeConflictReg(fi.getArgument(0), reg);
						
						Register register = new Register(fi.regID);
						Move move2 = new Move(fi.getArgument(1), register);
						//add a move to the basicblock that contains fi's 1st attribute and replace it with move in fi
						fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move2);
					}
				}
			} else {
				assignPhiReg(fi);
			}
		}
	}
	
	//return true if conflicting
	private boolean isConflict(Instruction in1, Instruction in2) {
		return lookupConflicts(in1).contains(in2);
	}
	
	private static int returnCommonRegFrom3(ArrayList<Integer> list1, ArrayList<Integer> list2, ArrayList<Integer> list3) {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		for(int i: list1) {
			temp.add(i);
		}
		
		temp.retainAll(list2);
		temp.retainAll(list3);
		
		if(temp.size() != 0) {
			return temp.get(0);
		} 
		
		return 0;
	}
	
	
	private static int returnCommonRegFrom2(ArrayList<Integer> list1, ArrayList<Integer> list2) {
		ArrayList<Integer> temp = new ArrayList<Integer>();
		for(int i: list1) {
			temp.add(i);
		}
		temp.retainAll(list2);
		
		if(temp.size() != 0) {
			return temp.get(0);
		} 
		
		return 0;
	}
	
	
	//assign Phi to the same register
	private void assignPhiReg(Phi fi) {
		if(!isConflict(fi, fi.getArgument(0)) && !isConflict(fi, fi.getArgument(1)) && !isConflict(fi.getArgument(0), fi.getArgument(1))) {
			int reg = returnCommonRegFrom3(lookupRegs(fi), lookupRegs(fi.getArgument(0)), lookupRegs(fi.getArgument(1)));
			//if they have common reg, assgn them with this reg
			if(reg != 0) {
				fi.getArgument(0).regID = reg;
				removeConflictReg(fi.getArgument(0), reg);
				fi.getArgument(1).regID = reg;
				removeConflictReg(fi.getArgument(1), reg);
				fi.regID = reg;
				removeConflictReg(fi, reg);
			} else {
				//pick a available reg from the second phi attribute
				if(lookupRegs(fi.getArgument(1)).size() > 0) {
					fi.getArgument(1).regID = lookupRegs(fi.getArgument(1)).get(0);
					removeConflictReg(fi.getArgument(1), fi.getArgument(1).regID);
					
					fi.regID = fi.getArgument(1).regID;
					removeConflictReg(fi, fi.regID);
					
					Register register = new Register(fi.getArgument(1).regID);
					Move move = new Move(fi.getArgument(0), register);
					
					//add a move to the basicblock that contains phi's 1st attribute and replace it with move in phi
					fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move);
				} else if(lookupRegs(fi.getArgument(0)).size() > 0) {	//pick a available reg from the 1st phi attribute
					
					fi.getArgument(0).regID = lookupRegs(fi.getArgument(0)).get(0);
					removeConflictReg(fi.getArgument(0), fi.getArgument(0).regID);
					
					fi.regID = fi.getArgument(0).regID;
					removeConflictReg(fi, fi.regID);
					
					Register register = new Register(fi.getArgument(0).regID);
					Move move = new Move(fi.getArgument(1), register);
					
					//add a move to the basicblock that contains phi's 1st attribute and replace it with move in phi
					fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move);				
				} else {	
					//they all have no available reg and need to store them in the same memory of phi
					if(lookupRegs(fi).size() > 0) {
						fi.regID = lookupRegs(fi).get(0);
						removeConflictReg(fi, fi.regID);
						
						Register register = new Register(fi.regID);
						
						Move move0 = new Move(fi.getArgument(0), register);
						fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move0);
						
						Move move1 = new Move(fi.getArgument(1), register);
						fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move1);
					} else {						
						Store store0 = new Store(fi.getArgument(0), fi);
						fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, store0);
						
						Store store1 = new Store(fi.getArgument(1), fi);
						fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, store1);
					}
				}
			}
		} else {
			if(!isConflict(fi, fi.getArgument(0))) {
				int reg = returnCommonRegFrom2(lookupRegs(fi), lookupRegs(fi.getArgument(0)));
				if(reg != 0) {
					fi.regID = reg;
					removeConflictReg(fi, reg);
					fi.getArgument(0).regID = reg;
					removeConflictReg(fi.getArgument(0), reg);
				} else {
					if(lookupRegs(fi).size() > 0) {
						int optionReg = returnCommonRegFrom2(lookupRegs(fi), lookupRegs(fi.getArgument(1)));
						if(optionReg == 0)
							optionReg = lookupRegs(fi).get(0);
						
						fi.regID = optionReg;
						removeConflictReg(fi, fi.regID);
						
						Register register = new Register(fi.regID);
						Move move = new Move(fi.getArgument(0), register);
						
						fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move);
					} else {						
						Store store = new Store(fi.getArgument(0), fi);
						fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, store);
					}
				}
				
				if(fi.regID != 0) {
					if(!isConflict(fi, fi.getArgument(1)) && fi.getArgument(0).regID == 0 && lookupRegs(fi.getArgument(1)).contains(fi.regID)) {
						fi.getArgument(1).regID = fi.regID;
						removeConflictReg(fi.getArgument(1), fi.regID);
					} else {
						Register register = new Register(fi.regID);
						Move move = new Move(fi.getArgument(1), register);
						
						fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move);
					}
				} else {
					Store store = new Store(fi.getArgument(1), fi);
					fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, store);
				}
			} else {
				int reg = returnCommonRegFrom2(lookupRegs(fi), lookupRegs(fi.getArgument(1)));
				if(reg != 0) {
					fi.regID = reg;
					removeConflictReg(fi, reg);
					fi.getArgument(1).regID = reg;
					removeConflictReg(fi.getArgument(1), reg);
				} else {
					if(lookupRegs(fi).size() > 0) {
						
						int optionReg = returnCommonRegFrom2(lookupRegs(fi), lookupRegs(fi.getArgument(0)));
						if(optionReg == 0)
							optionReg = lookupRegs(fi).get(0);
						
						fi.regID = optionReg;
						
						removeConflictReg(fi, fi.regID);
						
						Register register = new Register(fi.regID);
						Move move = new Move(fi.getArgument(1), register);
						
						fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, move);
					} else {						
						Store store = new Store(fi.getArgument(1), fi);
						fi.fiPair.get(1).basicBlock.instructions.add(fi.fiPair.get(1).basicBlock.instructions.size()-1, store);
					}
				}
				
				if(fi.regID != 0) {
					if(!isConflict(fi, fi.getArgument(0)) && fi.getArgument(1).regID == 0 && lookupRegs(fi.getArgument(0)).contains(fi.regID)) {
						fi.getArgument(0).regID = fi.regID;
						removeConflictReg(fi.getArgument(0), fi.regID);
					} else {
						Register register = new Register(fi.regID);
						Move move = new Move(fi.getArgument(0), register);
						
						fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, move);
					}
				} else {					
					Store store = new Store(fi.getArgument(0), fi);
					fi.fiPair.get(0).basicBlock.instructions.add(fi.fiPair.get(0).basicBlock.instructions.size()-1, store);
				}
			}
		}
	}

	public void simplify() {
		//use a tempCopy of the actual interferenceGraph to push instructions into stack
		ArrayList<Interference> tempCopy = new ArrayList<Interference>();
		copy(tempCopy, interferenceGraph);
		
		//temp instruction to hold the instruction that would be put into the stack
		Instruction temp;
		
		while(tempCopy.size() != 0) {
			temp = pick(tempCopy);
			if(temp == null) {
				temp = spill(tempCopy);
			}
			selectStack.add(temp);
			remove(tempCopy, temp);
		}
	}
	
	//assign a register to all instructions
	public void select() {
		Instruction temp;
		
		while(selectStack.size() != 0) {
			temp = selectStack.pop();
			if(lookupRegs(temp).size() > 0) {
				temp.regID = lookupRegs(temp).get(0);
				removeConflictReg(temp, temp.regID);
			} else {
				//no available reg, spill it
				spills.add(temp);
			}
		}
	}
	
	private void loadSpill(Function function) {
			int offset = -1;
			for(Instruction instruction: spills) {
				AllocateStack malloc = new AllocateStack(instruction);
				function.basicBlocks.get(0).instructions.add(0, malloc);
				instruction.regID = offset--;
			}
			
			BasicBlock block = function.basicBlocks.get(function.basicBlocks.size()-1);
			
			for(Instruction instruction: spills) {
				FreeStack freestack = new FreeStack(instruction);
				block.instructions.add(block.instructions.size(), freestack);
			}		
	}
	
	public void filter(Function function) {
		//Function main = functions.get(functions.size() - 1);
		ArrayList<Register> regList = new ArrayList<Register>();
		
		for(BasicBlock block: function.basicBlocks) {
			for(Instruction instruction: block.instructions) {
				if(instruction.regID != 0) {
					removeConflictReg(instruction, instruction.regID);
				}
			}
		}
		
		for(Interference interfer: interferenceGraph) {
			if(interfer.name.name.equals("register")) {
				regList.add((Register)interfer.name);
			}
		}
		
		for(Register reg: regList) {
			removeConflictReg(reg, reg.regID);
		}
	}
	
	public void allocation() {
		for(Function function: functions) {
			buildInterferenceGraph(function);
			eliminatePhis();
			buildInterferenceGraph(function);
			filter(function);
			simplify();
			select();
			loadSpill(function);
			
			//reset for the next function
			interferenceGraph = new ArrayList<Interference>();
			fiList = new ArrayList<Phi>();
			selectStack = new Stack<Instruction>();
			spills = new ArrayList<Instruction>();	
		}
	}
	
	public ArrayList<Function> allocatedInstructions() {
		allocation();
		return functions;
	}
	
	public void printBasicBlock() {
		for(int i = 0; i < functions.size(); i++) {
			System.out.println(functions.get(i).functionName);
			
			for(int j = 0 ; j < functions.get(i).basicBlocks.size(); j++) {
				System.out.print("BasicBlock" + functions.get(i).basicBlocks.get(j).blockID + "[label=\"");
				for(int k = 0; k < functions.get(i).basicBlocks.get(j).instructions.size(); k++) {					
						System.out.print(functions.get(i).basicBlocks.get(j).instructions.get(k).toString() + " \\n");
				}
				System.out.println("\"]");
			}
			
			for(int l = 0; l < functions.get(i).basicBlocks.size(); l++) {
				for(int m = 0; m < functions.get(i).basicBlocks.get(l).successors.size(); m++) {
					System.out.println("BasicBlock" + functions.get(i).basicBlocks.get(l).blockID + " -> " + "BasicBlock" + functions.get(i).basicBlocks.get(l).successors.get(m).blockID);
				}
			}
		}
	}
	
	public static void main(String args[]) throws IOException {
		RegisterAllocation ra = new RegisterAllocation("/Users/chenli/git/Compiler/PL241/test.txt");
	
		ra.allocation();
		
		ra.printBasicBlock();
	}
}

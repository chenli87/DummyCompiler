package parser;
import java.io.IOException;
import java.util.*;
import parser.Instruction.*;


import scanner.*;
import scanner.Scanner;

public class Parser {
	private Token currentToken, previousToken;
	private Scanner scanner;
	private ArrayList<Function> functions;
	public ArrayList<Instruction> globalMallocs;
	public ArrayList<Phi> fiCreated;
	
	private int functionSize;
	private SymbolTable globalSymbolTable;
	private Stack<Instruction> instructionStack;
	
	static class InstructionPair {
		public Phi fiInstruction;
		public Instruction replacementInstruction;
		
		public InstructionPair(Phi fi, Instruction instruction) {
			fiInstruction = fi;
			replacementInstruction = instruction;
		}
	}
	
	public ArrayList<InstructionPair> unnecessaryFis;
	
	private Function currentFunction() {
		return functions.get(functionSize);
	}
	
	private void addInstructionToStack(Instruction instruction) {
		instructionStack.add(instruction);
	}
	private Instruction popInstructionFromStack() {
		return instructionStack.pop();
	}
	
	private void addFunction(Function function) {
		functions.add(function);
		functionSize = functions.size() - 1;
	}
	private void next() {
		previousToken = currentToken;
		currentToken = scanner.getSym();
	}
	private void error(String errMsg) {
		System.err.println(String.valueOf(scanner.lineNumber) + " " + errMsg);
		System.exit(0);
	}
	
	public Parser(String fileName) throws IOException {
		scanner = new Scanner(fileName);
		currentToken = scanner.getSym();
		previousToken = null;
		globalSymbolTable = new SymbolTable();

		instructionStack = new Stack<Instruction>();
		functions = new ArrayList<Function> ();
		functionSize = -1;
		unnecessaryFis = new ArrayList<InstructionPair>();
		
		fiCreated = new ArrayList<Phi>();
		
		globalMallocs = new ArrayList<Instruction>();
	}
	
	private boolean peek(Token.Type inputType) {
		return currentToken.type == inputType;
	}
	private void expect(Token.Type inputType, String err) {
		if(peek(inputType)) {
			next();
		} else {
			error(err);
		}
	}
	
	public void printBasicBlock() {
		for(int i = 0; i < functions.size(); i++) {
			System.out.println(functions.get(i).functionName);
			
			for(int j = 0 ; j < functions.get(i).basicBlocks.size(); j++) {
				System.out.print("BasicBlock" + String.valueOf(j) + "[label=\"");
				for(int k = 0; k < functions.get(i).basicBlocks.get(j).instructions.size(); k++) {
					System.out.print(functions.get(i).basicBlocks.get(j).instructions.get(k).toString() + " \\n");
				}
				System.out.println("\"]");
			}
			
			for(int l = 0; l < functions.get(i).basicBlocks.size(); l++) {
				for(int m = 0; m < functions.get(i).basicBlocks.get(l).successors.size(); m++) {
					System.out.println("BasicBlock" + String.valueOf(l) + " -> " + "BasicBlock" + String.valueOf(functions.get(i).basicBlocks.indexOf(functions.get(i).basicBlocks.get(l).successors.get(m))));
				}
			}
		}		
	}
	
	private boolean varCheckLocal(String name) {
		return (currentFunction().symbolTable.get(name) != null && !compareName(currentFunction().symbolTable.get(name), "array")); 
	}
	
	private boolean varCheckGlobal(String name) {
		if (globalSymbolTable.get(name) != null && !compareName(globalSymbolTable.get(name), "array")) {
			return true;
		} else {
			return false;
		}
	}
	
	private boolean arrayCheckLocal(String name) {
		return (currentFunction().symbolTable.get(name) != null && currentFunction().symbolTable.get(name).name.compareTo("array") == 0);
	}
	
	private boolean arrayCheckGlobal(String name) {
			return (globalSymbolTable.get(name) != null && globalSymbolTable.get(name).name.compareTo("array") == 0);
	}
	
	private int functionCheck(String name) {
		if (globalSymbolTable.get(name) != null && globalSymbolTable.get(name).name.compareTo("function") == 0) {
			Function temp = (Function) globalSymbolTable.get(name);
			return temp.paramsNum;
		} else if(name.compareTo("InputNum") == 0 || name.compareTo("OutputNewLine") == 0) {
			return 0;
		} else if(name.compareTo("OutputNum") == 0) {
			return 1;
		} else {
			error("Undefined function: " + name);
			return -1;
		}
	}
	
	private Array getArrayFromSymbolTable(String name) {
		if(currentFunction().symbolTable.get(name) != null) {
			return (Array) currentFunction().symbolTable.get(name);
		} else {
			return (Array) globalSymbolTable.get(name);
		}
	}
	
	public void designator() {
		
		expect(Token.Type.ID, "designator: miss ID");
		
		String tempName = previousToken.idName;
		boolean isArray = false;
		
		ArrayList<Instruction> size = new ArrayList<Instruction>();
		Array array = new Array();
		
		while (peek(Token.Type.LSB)) {
			isArray = true;
			
			if(!arrayCheckLocal(tempName) && !arrayCheckGlobal(tempName)) {
				error("undefined array: " + tempName); 
			}
			
			array = getArrayFromSymbolTable(tempName);
			next();
			expression();
			size.add(popInstructionFromStack());
			expect(Token.Type.RSB, "designator: miss ]");
		}
		
		if(!isArray) {
			if(!varCheckLocal(tempName) && !varCheckGlobal(tempName)) {
				error("undefined var: " + tempName);
			}
			
			addInstructionToStack(new Var(tempName));
		
		} else {
			if(size.size() != array.arraySize.size()) {
				error("array: " + tempName + " has wrong dimension.");
			}
			
			if(size.size() == 1) {
				addInstructionToStack(forwardReferring(size.get(0)));
			} else {
				Add add;
				for(int i = 0; i < size.size(); i++) {
					int tempInt = 1;
					for(int j = 1+i; j < size.size(); j++) {
						tempInt = tempInt * array.arraySize.get(j);
					}
					Mul mul = new Mul(forwardReferring(size.get(i)), new Constant(tempInt));
					currentFunction().currentBasicBlock().addInstruction(mul);
					if(i == 0) {
						addInstructionToStack(mul);
					} else {
						add = new Add(popInstructionFromStack(), mul);
						addInstructionToStack(add);
						currentFunction().currentBasicBlock().addInstruction(add);
					}
				}
			}
			//need to check array is global or local during codegen to decide base pointer
			Mul mul8 = new Mul(popInstructionFromStack(), new Constant(8));
			currentFunction().currentBasicBlock().addInstruction(mul8);
			boolean isGlobalArray = (currentFunction().symbolTable.get(array.arrayName) == null);
			Adda addBase = new Adda(array, mul8, isGlobalArray);
			currentFunction().currentBasicBlock().addInstruction(addBase);
			addInstructionToStack(addBase);
		}
	}
	
	public void factor() {
		if(peek(Token.Type.ID)) {
			designator();
		
			if(instructionStack.peek().name != "var") {
				//load a array
				Load load = new Load(popInstructionFromStack());
				addInstructionToStack(load);
				currentFunction().currentBasicBlock().addInstruction(load);
			}
		} else if (peek(Token.Type.NUM)) {
			next();
			
			Constant num = new Constant(previousToken.intVal);
			addInstructionToStack(num);
			
		} else if (peek(Token.Type.LPAREN)) {
			next();
			expression();
			expect(Token.Type.RPAREN, "factor: miss )");
		} else if (peek(Token.Type.CALL)) {
			funcCall();
		} else {
			error("Syntax Error in Factor.");
		}
	}
	
	
	private boolean isGlobal(Var var) {
		return !varCheckLocal(var.varName);
	}
	
	private void isInitialized(Var var) {
		if(!isGlobal(var) && compareName(currentFunction().symbolTable.get(var.varName), "var")) {
			error("Uninitialized var: " + var.varName);
		} 
	}
	
	public void term() {
		factor();
		while(peek(Token.Type.TIMES) || peek(Token.Type.DIVIDE)) {
			next();
			Instruction temp1 = popInstructionFromStack();
			
			if(compareName(temp1, "var")) {
				Var tempVar = (Var) temp1;
				
				isInitialized(tempVar);
				
				temp1 = forwardReferring(temp1);
			}
								
			if(previousToken.type == Token.Type.TIMES) {
				factor();
				Instruction temp2 = popInstructionFromStack();
				if(compareName(temp2, "var")) {
					Var tempVar = (Var) temp2;
					
					isInitialized(tempVar);

					temp2 = forwardReferring(temp2);
				}
				
				if(compareName(temp1, "constant") && compareName(temp2, "constant")) {
					Constant constant1 = (Constant) temp1;
					Constant constant2 = (Constant) temp2;
					
					addInstructionToStack(new Constant(constant1.constantValue*constant2.constantValue));
				} else {
					Mul mul = new Mul(temp1, temp2);
					addInstructionToStack(mul);
					currentFunction().currentBasicBlock().addInstruction(mul);
				}
			} else {
				factor();
				Instruction temp2 = popInstructionFromStack();
				if(compareName(temp2, "var")) {
					Var tempVar = (Var) temp2;
					
					isInitialized(tempVar);

					temp2 = forwardReferring(temp2);
				}
				
				if(compareName(temp1, "constant") && compareName(temp2, "constant")) {
					Constant constant1 = (Constant) temp1;
					Constant constant2 = (Constant) temp2;
					if(constant2.constantValue == 0) {
						error("Division: divisor is 0");
					} else {
						addInstructionToStack(new Constant(constant1.constantValue/constant2.constantValue));
					}
				} else {
					Div div = new Div(temp1, temp2);
					addInstructionToStack(div);
					currentFunction().currentBasicBlock().addInstruction(div);
				}
			}
		}
	}
	
	private boolean compareName(Instruction instruction, String name) {
		if(instruction != null) {
			return instruction.name.equals(name);
		} else{
			return false;
		}
	}
	
	public void expression() {
		term();
		while(peek(Token.Type.PLUS) || peek(Token.Type.MINUS)) {
			next();
			
			Instruction temp1 = popInstructionFromStack();
			if(compareName(temp1, "var")) {
				Var tempVar = (Var) temp1;
				isInitialized(tempVar);

				temp1 = forwardReferring(temp1);
			}
			
			if(previousToken.type == Token.Type.PLUS) {
				term();
				
				Instruction temp2 = popInstructionFromStack();
				if(compareName(temp2, "var")) {
					Var tempVar = (Var) temp2;
					
					isInitialized(tempVar);

					temp2 = forwardReferring(temp2);
				}
				
				if(compareName(temp1,"constant") && compareName(temp2, "constant")) {
					Constant constant1 = (Constant) temp1;
					Constant constant2 = (Constant) temp2;
					addInstructionToStack(new Constant(constant1.constantValue+constant2.constantValue));
				} else {
				
					Add add = new Add(temp1, temp2);
					addInstructionToStack(add);
					currentFunction().currentBasicBlock().addInstruction(add);
				}
			} else {
				term();
				
				Instruction temp2 = popInstructionFromStack();
				if(compareName(temp2, "var")) {
					Var tempVar = (Var) temp2;
					
					isInitialized(tempVar);

					temp2 = forwardReferring(temp2);
				}
				
				if(compareName(temp1, "constant") && compareName(temp2, "constant")) {
					Constant constant1 = (Constant) temp1;
					Constant constant2 = (Constant) temp2;
					addInstructionToStack(new Constant(constant1.constantValue-constant2.constantValue));
				} else {
					Sub sub = new Sub(temp1, temp2);
					addInstructionToStack(sub);
					currentFunction().currentBasicBlock().addInstruction(sub);
				}
			}
		}
	}
	
	public void relation() {
		expression();
		
		Instruction temp1 = popInstructionFromStack();
		if(compareName(temp1, "var")) {
			Var tempVar = (Var) temp1;
			
			isInitialized(tempVar);
			
			temp1 = forwardReferring(temp1);
		}
		if(peek(Token.Type.EQ) || peek(Token.Type.NEQ) || peek(Token.Type.LT) || peek(Token.Type.LE) || peek(Token.Type.GT) || peek(Token.Type.GE)) {
			next();
			Token tempToken = previousToken;
			
			Instruction relation;
			expression();
			Instruction temp2 = popInstructionFromStack();
			if(compareName(temp2, "var")) {
				Var tempVar = (Var) temp2;
				
				isInitialized(tempVar);

				temp2 = forwardReferring(temp2);
			}
			
			relation = new Cmp(temp1, temp2);
			currentFunction().currentBasicBlock().addInstruction(relation);
			
			switch (tempToken.type) {
				case EQ: 
					relation = new Bne(relation);
					currentFunction().currentBasicBlock().addInstruction(relation);
					break;
				case NEQ: 
					relation = new Beq(relation);
					//relation.setID(currentFunction().instructionCounter++);
					currentFunction().currentBasicBlock().addInstruction(relation);
					break;
				case LT: 
					relation = new Bge(relation);
					//relation.setID(currentFunction().instructionCounter++);
					
					currentFunction().currentBasicBlock().addInstruction(relation);
					break;
				case LE: 
					relation = new Bgt(relation);
					//relation.setID(currentFunction().instructionCounter++);
					currentFunction().currentBasicBlock().addInstruction(relation);
					break;
				case GT: 
					relation = new Ble(relation);
					//relation.setID(currentFunction().instructionCounter++);
					currentFunction().currentBasicBlock().addInstruction(relation);
					break;
				case GE: 
					relation = new Blt(relation);
					//relation.setID(currentFunction().instructionCounter++);
					currentFunction().currentBasicBlock().addInstruction(relation);
					break;
				default:
					error("Syntax Error in Relation.");
			}
		} else {
			error("Syntax Error in Relation.");
		}
	}
	
	private void markFiLiveness(Instruction in) {
		if(compareName(in, "phi")) {
			Phi temp = (Phi) in;
			temp.liveness = true;
		}
	}
	
	//forward referring
	private Instruction forwardReferring(Instruction value) {
		if(!compareName(value, "var")) {
			markFiLiveness(value);
			return  value;
		} else {
			Var var = (Var) value;
			if(!isGlobal(var)) {
				markFiLiveness(currentFunction().symbolTable.get(var.varName));
				return currentFunction().symbolTable.get(var.varName);
			} else {
				return value;
			}
		}
		
	}
	
	public void assignment() {
		expect(Token.Type.LET, "assignment: miss let");
		designator();
		
		Var var = null;
		Store store = null;
		
		if(instructionStack.peek().name == "var") {
			var = (Var) popInstructionFromStack();		
		} else {
			//store an array
			store = new Store(null, popInstructionFromStack());
		}
		
		expect(Token.Type.ASSIGN, "assignment: miss <-"); 
		expression();
		
		if(var != null) {
			if(isGlobal(var) ) {
				//have to store the new global value to memory
				Move move = new Move(forwardReferring(popInstructionFromStack()), var);
				currentFunction().currentBasicBlock().addInstruction(move);
			} else {
				currentFunction().symbolTable.put(var.varName, forwardReferring(popInstructionFromStack()));
				currentFunction().currentBasicBlock().stateVector.updateLocalVar(var.varName, currentFunction().symbolTable.get(var.varName));
			}
		} else {
			store.op1 = forwardReferring(popInstructionFromStack());
			currentFunction().currentBasicBlock().addInstruction(store);
		}
	}
	
	public void funcCall() {
		expect(Token.Type.CALL, "funcCall: miss call"); 
		expect(Token.Type.ID, "funcCall: miss ID");
		
		
		int paramSize = functionCheck(previousToken.idName);
		
		String tempName = previousToken.idName;
		ArrayList<Instruction> paramList = new ArrayList<Instruction>();
		
		if(peek(Token.Type.LPAREN)) {
			next();
			if(!peek(Token.Type.RPAREN)) {
				expression();
								
				if(compareName(instructionStack.peek(), "var")) {
					paramList.add(forwardReferring(popInstructionFromStack()));
				} else {
					paramList.add(popInstructionFromStack());
				}
				while(peek(Token.Type.COMMA)) {
					next();
					expression();
					if(compareName(instructionStack.peek(), "var")) {
						paramList.add(forwardReferring(popInstructionFromStack()));
					} else {
						paramList.add(popInstructionFromStack());
					}
				}
				expect(Token.Type.RPAREN, "funcCall: miss )");
			} else {
				next();
			}
		} 
		
		if(tempName.compareTo("InputNum") == 0) {
			if(paramList.size() == 0) {
				Read read = new Read();
				currentFunction().currentBasicBlock().addInstruction(read);
				addInstructionToStack(read);
			} else {
				error(tempName + " Wrong parameters!");
			}
		} else if(tempName.compareTo("OutputNewLine") == 0) {
			if(paramList.size() == 0) {
				Wln wln = new Wln();
				currentFunction().currentBasicBlock().addInstruction(wln);
				addInstructionToStack(wln);
			} else {
				error(tempName + " Wrong parameters!");
			}
		} else if(tempName.compareTo("OutputNum") == 0) {
			if(paramList.size() == 1) {				
				Write write = new Write(paramList.get(0));
				currentFunction().currentBasicBlock().addInstruction(write);
				addInstructionToStack(write);
			} else {
				error(tempName + " Wrong parameters!");
			}
		} else {
			if(paramList.size() == paramSize) {
				if(((Function)globalSymbolTable.get(tempName)).isReturn) {
					PassArg passarg = new PassArg(null);
					currentFunction().currentBasicBlock().addInstruction(passarg);
				}
				for(int i=0; i<paramList.size(); i++) {
					PassArg passarg = new PassArg(paramList.get(paramList.size()-1-i));
					currentFunction().currentBasicBlock().addInstruction(passarg);
				}
				
				//save callee registers
				SaveCalleeReg save = new SaveCalleeReg();
				currentFunction().currentBasicBlock().addInstruction(save);
				
				if(currentFunction().functionName.equals("main")) {
					storeGlobalVars();
				}
				
				Call call = new Call(tempName, paramList);
				currentFunction().currentBasicBlock().addInstruction(call);
				addInstructionToStack(call);
				
				//restore callee register
				RestoreCalleeReg restore = new RestoreCalleeReg();
				currentFunction().currentBasicBlock().addInstruction(restore);
				
				for(int i=0; i<paramList.size(); i++) {
					RemoveArg removearg = new RemoveArg(paramList.get(paramList.size()-1-i));
					currentFunction().currentBasicBlock().addInstruction(removearg);
				}
				
				if(((Function)globalSymbolTable.get(tempName)).isReturn) {
					GetReturn getReturn = new GetReturn(call);
					currentFunction().currentBasicBlock().addInstruction(getReturn);
				}
				
				if(currentFunction().functionName.equals("main")) {
					loadGlobalVars();
				}
			} else {
				error(tempName + " Wrong parameters!");
			}
		}
	}
	
	//store all global vars before function calls in main
	private void storeGlobalVars() {
		for(int i=0; i<globalSymbolTable.symbolList().length; i++) {
			if (((Instruction) globalSymbolTable.symbolList()[i]).name.equals("var")) {
				Store store = new Store(currentFunction().symbolTable.get(((Var) globalSymbolTable.symbolList()[i]).varName), (Instruction) globalSymbolTable.symbolList()[i]);
				currentFunction().currentBasicBlock().addInstruction(store);
			}
		}
	}
	
	//load all global vars after function calls in main
	private void loadGlobalVars() {
		for(int i=0; i<globalSymbolTable.symbolList().length; i++) {
			if (((Instruction) globalSymbolTable.symbolList()[i]).name.equals("var")) {
				Load load = new Load((Instruction) globalSymbolTable.symbolList()[i]);
				currentFunction().currentBasicBlock().addInstruction(load);
					
				currentFunction().symbolTable.put(((Var) globalSymbolTable.symbolList()[i]).varName, load);
					
				currentFunction().currentBasicBlock().stateVector.updateLocalVar(((Var) globalSymbolTable.symbolList()[i]).varName, load);
			}
		}
	}
	
	public void ifStatement() {
		expect(Token.Type.IF, "ifStatement: miss if");
		relation(); 
		expect(Token.Type.THEN, "ifStatement: miss then"); 
		
		ArrayList<BasicBlock> predecessors = new ArrayList<BasicBlock>();
		predecessors.add(currentFunction().currentBasicBlock());
		
		BasicBlock thenBlock = new BasicBlock(predecessors);
		thenBlock.stateVector = new StateVector(predecessors.get(0).stateVector);
		
		
		currentFunction().currentBasicBlock().setUnconditionalBranch(thenBlock);
		currentFunction().currentBasicBlock().addInstruction(new Jump());
		
		currentFunction().addBasicBlock(thenBlock);
		
		statSequence();
		
		BasicBlock elseBlock = new BasicBlock(predecessors);
		elseBlock.stateVector = new StateVector(predecessors.get(0).stateVector);
		
		thenBlock.predecessors.get(0).setConditionalBranch(elseBlock);
		
		currentFunction().currentBasicBlock().addInstruction(new Jump());
		currentFunction().addBasicBlock(elseBlock);
		
		if(peek(Token.Type.ELSE)) {
			next();
			statSequence();
			expect(Token.Type.FI, "ifStatement: miss fi"); 
		} else {
			expect(Token.Type.FI, "ifStatement: miss fi"); 
		}
		
		predecessors = new ArrayList<BasicBlock>();
		predecessors.add(currentFunction().basicBlocks.get(currentFunction().basicBlocks.indexOf(elseBlock)-1));
		predecessors.add(currentFunction().currentBasicBlock());
		BasicBlock jointBlock = new BasicBlock(predecessors);
		
		jointBlock.stateVector = new StateVector(predecessors.get(0).stateVector);
		
		currentFunction().currentBasicBlock().addInstruction(new Jump());
		
		for(int i=0; i < jointBlock.stateVector.localVars.size(); i++) {
			Phi fi = new Phi(null,jointBlock.stateVector.localVars.get(i).var);
			
			fi.fiPair = new ArrayList<Phi.FiPair>();
			
			markFiLiveness(predecessors.get(0).stateVector.localVars.get(i).instruction);
			fi.fiPair.add(new Phi.FiPair(predecessors.get(0), predecessors.get(0).stateVector.localVars.get(i).instruction));
			
			markFiLiveness(predecessors.get(1).stateVector.localVars.get(i).instruction);
			fi.fiPair.add(new Phi.FiPair(predecessors.get(1), predecessors.get(1).stateVector.localVars.get(i).instruction));
			
			if(fi.fiPair.get(0).instruction != fi.fiPair.get(1).instruction) {
				currentFunction().symbolTable.put(fi.varName, fi);
				jointBlock.stateVector.updateLocalVar(fi.varName, fi);
				jointBlock.addInstruction(fi);
		
				fiCreated.add(fi);		
			}
		}		
		
		currentFunction().basicBlocks.get(currentFunction().basicBlocks.indexOf(elseBlock)-1).setConditionalBranch(jointBlock);
		currentFunction().currentBasicBlock().setUnconditionalBranch(jointBlock);
		
		currentFunction().addBasicBlock(jointBlock);	
	}
	
	public void whileStatement() {
		expect(Token.Type.WHILE, "whileStatement: miss while");
		
		ArrayList<BasicBlock> predecessors = new ArrayList<BasicBlock>();
		predecessors.add(currentFunction().currentBasicBlock());
		
		currentFunction().currentBasicBlock().addInstruction(new Jump());
		
		BasicBlock loopHeader = new BasicBlock(predecessors);
		loopHeader.stateVector = new StateVector(predecessors.get(0).stateVector);
		
		ArrayList<Phi> fiList = new ArrayList<Phi>();
		
		for(int i=0; i < loopHeader.stateVector.localVars.size(); i++) {
			Phi fi = new Phi(null,loopHeader.stateVector.localVars.get(i).var);
			
			fi.fiPair = new ArrayList<Phi.FiPair>();
			
			markFiLiveness(predecessors.get(0).stateVector.localVars.get(i).instruction);
			fi.fiPair.add(new Phi.FiPair(predecessors.get(0), predecessors.get(0).stateVector.localVars.get(i).instruction));
			
			fi.fiPair.add(null);
			
			//Add all fis to the basic block
			currentFunction().symbolTable.put(fi.varName, fi);
			loopHeader.stateVector.updateLocalVar(fi.varName, fi);
			loopHeader.addInstruction(fi);
			fiList.add(fi);
			
			fiCreated.add(fi);
		}

		currentFunction().currentBasicBlock().setUnconditionalBranch(loopHeader);
		
		currentFunction().addBasicBlock(loopHeader);
		
		relation();
		expect(Token.Type.DO, "whileStatement: miss do"); 

		predecessors = new ArrayList<BasicBlock>();
		predecessors.add(currentFunction().currentBasicBlock());
		
		BasicBlock loopBody = new BasicBlock(predecessors);
		loopBody.stateVector = new StateVector(predecessors.get(0).stateVector);
		
		currentFunction().currentBasicBlock().setUnconditionalBranch(loopBody);
		currentFunction().currentBasicBlock().addInstruction(new Jump());
		currentFunction().addBasicBlock(loopBody);
		
		statSequence();
		
		expect(Token.Type.OD, "whileStatement: miss od"); 
		currentFunction().currentBasicBlock().setUnconditionalBranch(loopHeader);
		
		loopHeader.addPredecessor(currentFunction().currentBasicBlock());
		
		for(int i=0; i < fiList.size(); i++) {
			
			if(loopHeader.predecessors.get(1).stateVector.localVars.get(i).instruction.name.compareTo("phi") != 0) {
				markFiLiveness(loopHeader.predecessors.get(1).stateVector.localVars.get(i).instruction);
				fiList.get(i).fiPair.add(1, new Phi.FiPair(loopHeader.predecessors.get(1), loopHeader.predecessors.get(1).stateVector.localVars.get(i).instruction));
				fiList.get(i).fiPair.remove(2);
			} else {
				//check for unnecessary fis
				Phi temp = (Phi) loopHeader.predecessors.get(1).stateVector.localVars.get(i).instruction;
				if(temp == fiList.get(i)) {
					unnecessaryFis.add(new InstructionPair(fiList.get(i), fiList.get(i).fiPair.get(0).instruction));
					loopHeader.stateVector.localVars.get(i).instruction = fiList.get(i).fiPair.get(0).instruction;
				} else {
					markFiLiveness(loopHeader.predecessors.get(1).stateVector.localVars.get(i).instruction);
					fiList.get(i).fiPair.add(1, new Phi.FiPair(loopHeader.predecessors.get(1), loopHeader.predecessors.get(1).stateVector.localVars.get(i).instruction));
					fiList.get(i).fiPair.remove(2);
				}
			}
		}

		BasicBlock loopExit = new BasicBlock(predecessors);
		loopExit.stateVector = new StateVector(predecessors.get(0).stateVector);
		
		loopHeader.setConditionalBranch(loopExit);
		currentFunction().currentBasicBlock().addInstruction(new Jump());
		currentFunction().addBasicBlock(loopExit);
	}

	public void returnStatement() {
		expect(Token.Type.RETURN, "returnStatement: miss return"); 
		
		Return returnInstruction = new Return(null);
		if(peek(Token.Type.ID) || peek(Token.Type.NUM) || peek(Token.Type.LPAREN) || peek(Token.Type.CALL)) {
			expression();
			
			Instruction temp = popInstructionFromStack();
			if(compareName(temp, "var")) {
				Var tempVar = (Var) temp;
				
				isInitialized(tempVar);

				temp = forwardReferring(temp);
			}
			
			returnInstruction.op = temp;
		}
		currentFunction().currentBasicBlock().addInstruction(returnInstruction);
	}
	
	public void statement() {
		if(peek(Token.Type.LET)) {
			assignment();
		} else if(peek(Token.Type.CALL)) {
			funcCall();
		} else if(peek(Token.Type.IF)) {
			ifStatement();
		} else if(peek(Token.Type.WHILE)) {
			whileStatement();
		} else if(peek(Token.Type.RETURN)) {
			returnStatement();
		} else {
			error("Syntax Error in Statement");
		}
	}
	
	public void statSequence() {
		statement();
		while(peek(Token.Type.SCOLON)) {
			next();
			statement();
		}
	}
	
	public void typeDecl() {
		if(peek(Token.Type.VAR)) {
			next();
			
			Var var = new Var();
			addInstructionToStack(var);
			
			
		} else if (peek(Token.Type.ARRAY)) {
			next();
			
			Array array = new Array();
			ArrayList<Integer> arraySize = new ArrayList<Integer>();
			
			expect(Token.Type.LSB, "typeDecl: miss [");
			expect(Token.Type.NUM, "typeDecl: miss NUM");
			
			arraySize.add(previousToken.intVal);
			
			expect(Token.Type.RSB, "typeDecl: miss ]"); 
			
			while(peek(Token.Type.LSB)) {
				next();
				
				expect(Token.Type.NUM, "typeDecl: miss NUM");
				
				arraySize.add(previousToken.intVal);
				
				expect(Token.Type.RSB, "typeDecl: miss ]"); 
			}
			
			array.arraySize = arraySize;
			addInstructionToStack(array);
		} else {
			error("Syntax Error: miss VAR or ARRAY");
		}	
	}
	
	public void varDecl() {
		typeDecl();
		expect(Token.Type.ID, "varDecl: miss ID"); 
		Var var = null;
		Array array = null;
		if(compareName(instructionStack.peek(), "var")) {
			var = (Var) popInstructionFromStack();
			var.varName = previousToken.idName;
			
			currentFunction().currentBasicBlock().stateVector.addLocalVar(var.varName, var);
			
			currentFunction().symbolTable.put(var.varName, var);
			
			if(currentFunction().functionName.equals("main")) {

				AllocateAddress malloc = new AllocateAddress(var);
				globalMallocs.add(malloc);
				
				globalSymbolTable.put(var.varName, var);
			} 	
		} else {
			array = (Array) popInstructionFromStack();
			array.arrayName = previousToken.idName;
			
			if(currentFunction().functionName.equals("main")) {
				globalSymbolTable.put(array.arrayName, array);
				
				AllocateAddress malloc = new AllocateAddress(array);
				globalMallocs.add(malloc);
			} else {
				currentFunction().symbolTable.put(array.arrayName, array);

				AllocateStack malloc = new AllocateStack(array);
				currentFunction().currentBasicBlock().addInstruction(malloc);
			}
		}
		
		while(peek(Token.Type.COMMA)) {
			next();
			
			if(var != null) {
				var = new Var(var);
			} else {
				array = new Array(array);
			}
			
			expect(Token.Type.ID, "varDecl: miss ID");
			
			if(var != null) {
				var.varName = previousToken.idName;
				currentFunction().currentBasicBlock().stateVector.addLocalVar(var.varName, var);
				
				currentFunction().symbolTable.put(var.varName, var);
				
				if(currentFunction().functionName.equals("main")) {
					AllocateAddress malloc = new AllocateAddress(var);
					globalMallocs.add(malloc);
					
					globalSymbolTable.put(var.varName, var);		
				}
			} else {
				array.arrayName = previousToken.idName;
								
				if(currentFunction().functionName.equals("main")) {
					globalSymbolTable.put(array.arrayName, array);
					
					AllocateAddress malloc = new AllocateAddress(array);
					globalMallocs.add(malloc);
				} else {
					
					currentFunction().symbolTable.put(array.arrayName, array);

					AllocateStack malloc = new AllocateStack(array);
					currentFunction().currentBasicBlock().addInstruction(malloc);
				}
			}
		}
		expect(Token.Type.SCOLON, "varDecl: miss ;");
	}
	
	public void funcDecl() {
		if(peek(Token.Type.FUNCTION) || peek(Token.Type.PROCEDURE)) {
			next();
			
			Function function = new Function();
			
			if(previousToken.type == Token.Type.FUNCTION) {
				function.isReturn = true;
			} else 
				function.isReturn = false;
			
			expect(Token.Type.ID, "funcDecl: miss ID");
			
			function.functionName = previousToken.idName;
			addFunction(function);
			currentFunction().addBasicBlock(new BasicBlock(null));
			
			
			if(peek(Token.Type.LPAREN)) {
				formalParam();
				expect(Token.Type.SCOLON, "funcDecl: miss ;");
			} else if(peek(Token.Type.SCOLON)) {
				next();
			} else {
				error("Syntax Error in funcDecl");
			}
			funcBody();
			
			expect(Token.Type.SCOLON, "funcDecl: miss ;");
		} else {
			error("Syntax Error in funcDecl: miss header");
		}
	}
	
	public void formalParam() {
		expect(Token.Type.LPAREN, "formalParam: miss (");
		
		Function function = currentFunction();
		
		if(peek(Token.Type.ID)) {
			next();
			InputParam inputParam = new InputParam(new Var(previousToken.idName));
			currentFunction().currentBasicBlock().addInstruction(inputParam);
			
			currentFunction().currentBasicBlock().stateVector.addLocalVar(previousToken.idName, inputParam);
			
			currentFunction().symbolTable.put(previousToken.idName, inputParam);
			function.paramsNum++;
			
			while(peek(Token.Type.COMMA)) {
				next();
				expect(Token.Type.ID, "formalParam: miss ID");
				
				inputParam = new InputParam(new Var(previousToken.idName));
				currentFunction().currentBasicBlock().addInstruction(inputParam);
				
				currentFunction().currentBasicBlock().stateVector.addLocalVar(previousToken.idName, inputParam);
				
				currentFunction().symbolTable.put(previousToken.idName, inputParam);
				function.paramsNum++;
			}
			expect(Token.Type.RPAREN, "formalParam: miss )");
		} else {
			expect(Token.Type.RPAREN, "formalParam: miss )");
		}
		
		globalSymbolTable.put(function.functionName, function);
	}
	
	public void funcBody() {
		while(!peek(Token.Type.LCB)) {
			varDecl();
		}
		next();
		
		if(!peek(Token.Type.RCB)) {
			
			ArrayList<BasicBlock> predecessors = new ArrayList<BasicBlock>();
			predecessors.add(currentFunction().currentBasicBlock());
			currentFunction().currentBasicBlock().addInstruction(new Jump());
			
			BasicBlock second = new BasicBlock(predecessors);
			currentFunction().currentBasicBlock().addSuccessor(second);
			
			currentFunction().addBasicBlock(second);
			currentFunction().currentBasicBlock().stateVector = new StateVector(predecessors.get(0).stateVector);
			
			statSequence();
			expect(Token.Type.RCB, "funcBody: miss }"); 
		} else {
			next();
		}
	}
	
	public void computation() {
		expect(Token.Type.MAIN, "computation: miss main"); 
		addFunction(new Function("main"));
		BasicBlock mainBasicBlock = new BasicBlock(null);
		currentFunction().addBasicBlock(mainBasicBlock);
		
		while(peek(Token.Type.VAR) || peek(Token.Type.ARRAY)) {
			varDecl();
		}
		
		
		Function mainFunction = functions.remove(0);
		while(peek(Token.Type.FUNCTION) || peek(Token.Type.PROCEDURE)) {
			funcDecl();
		}
		expect(Token.Type.LCB, "computation: miss {"); 
		
		addFunction(mainFunction);
		
		statSequence();
		expect(Token.Type.RCB, "computation: miss }");
		expect(Token.Type.DOT, "computation: miss .");
	}
	
	//second phase to erase unnecessary fis
	public void secondPhase() {
		for(InstructionPair fi: unnecessaryFis) {
			for(Function function: functions) {
				for(BasicBlock block: function.basicBlocks) {
					for(Instruction instruction: block.instructions) {
						instruction.replaceFi(fi);
					}
					block.instructions.remove(fi.fiInstruction);
				}
			}
		}
		
		for(Function function: functions) {
			for(BasicBlock block: function.basicBlocks) {
				for(Phi phi: fiCreated) {
					if(!phi.liveness)
						block.instructions.remove(phi);
				}
			}
		}
	}
	
	public ArrayList<Function> parsing() {
		computation();
		secondPhase();
		return functions;
	}
	
	public static void main(String args[]) throws IOException {
		Parser p = new Parser("/Users/chenli/git/Compiler/PL241/test.txt");
	
		p.computation();
		p.secondPhase();
		p.printBasicBlock();
	}
}

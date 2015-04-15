package parser;

import java.util.*;

public abstract class Instruction { //abstract class 
	static int instance_id = 0;
	public String name;
	public int instruction_id;
	public int regID;
	
	public Instruction() {
		instance_id++;
		instruction_id = instance_id;
		regID = 0;
	}
	
	public String assemblyCode() {
		return "";
	}
	
	public static boolean isUnaryInstruction(Instruction instruction) {
		return (instruction.name.compareTo("load")==0 || instruction.name.compareTo("neg")==0 || 
				instruction.name.compareTo("write")==0 || instruction.name.compareTo("inputParam")==0 || 
				instruction.name.compareTo("return")==0);
	}
	
	public static boolean isConditionalJump(Instruction instruction) {
		return (instruction.name.compareTo("bne")==0 || instruction.name.compareTo("beq")==0 || 
				instruction.name.compareTo("ble")==0 || instruction.name.compareTo("blt")==0 || 
				instruction.name.compareTo("bge")==0 || instruction.name.compareTo("bgt")==0);
	}
	
	public void replaceFi(Parser.InstructionPair fi) {
		
	}
	
	public static boolean isBinaryInstruction(Instruction instruction) {
		return (instruction.name.compareTo("add")==0 || instruction.name.compareTo("sub")==0 || 
				instruction.name.compareTo("mul")==0 || instruction.name.compareTo("div")==0 || 
				instruction.name.compareTo("cmp")==0 || instruction.name.compareTo("adda")==0 || 
				instruction.name.compareTo("store")==0 || instruction.name.compareTo("move")==0);
	}
	
	public static class BinaryInstruction extends Instruction {
		public Instruction op1, op2;
		public BinaryInstruction(Instruction operand1, Instruction operand2) {
			super();
			op1 = operand1;
			op2 = operand2;	
		}
		
		public void replaceFi(Parser.InstructionPair fi) {
			if(op1 == fi.fiInstruction) {
				op1 = fi.replacementInstruction;
			} 
			if(op2 == fi.fiInstruction) {
				op2 = fi.replacementInstruction;
			} 
		}
		
		public String toString() {
			String result = "";
			
			if(regID != 0) {
				result = result + "r" + regID + " ";
			}
			
			result = result + String.valueOf(instruction_id) + ":" + name + " ";
			
			if(op1.name.equals("constant") || op1.name.equals("register") || op1.name.equals("memory") || op1.name.equals("var")) {
				result = result.concat(op1.toString() + " ,");
			} else {
				result =result.concat(String.valueOf(op1.instruction_id) + " ,");
			}
			

			if(op2.name.equals("constant") || op2.name.equals("register") || op2.name.equals("memory") || op2.name.equals("var")) {
				result =result.concat(op2.toString());
			} else {
				result =result.concat(String.valueOf(op2.instruction_id));
			}
			
			return result;
		}

	}
	
	public static class UnaryInstruction extends Instruction {
		public Instruction op;
		public UnaryInstruction(Instruction operand) {
			super();
			op = operand;	
		}
		
		public void replaceFi(Parser.InstructionPair fi) {
			if(op == fi.fiInstruction) {
				op = fi.replacementInstruction;
			} 
		}
		
		public String toString() {
			String result = "";
			
			if(regID != 0) {
				result = result + "r" + regID + " ";
			}
			
			if(op.name.compareTo("constant") != 0 && !op.name.equals("memory") && !op.name.equals("var") && !op.name.equals("register"))  {
				result = result + instruction_id + ":" + name + " " + String.valueOf(op.instruction_id);
			} else {
				result = result + instruction_id + ":" + name + " " + op.toString();
			}
			
			return result;
		}
	}
	
	public static class Neg extends UnaryInstruction{
		public Neg(Instruction operand) {
			super(operand);
			name = "neg";
	    }
		
		@Override
		public String assemblyCode() {	
			String asm ="";
			if(op.name.equals("constant")) {
				asm += "mov r8, " + ((Constant)op).constantValue + System.getProperty("line.separator");
			} else if(op.regID > 0) {
				asm += "mov r8, r" + op.regID + System.getProperty("line.separator");
			} else if(op.name.equals("var")){
				asm += "mov r8, " + ((Var)op).varName + System.getProperty("line.separator");
				asm += "mov r8, [r8]" + System.getProperty("line.separator");
			} else {
				asm += "mov r8, [rbp-" + op.regID*(-8) + "]" + System.getProperty("line.separator");
			}
			
			asm += "neg r8" + System.getProperty("line.separator");

			if(regID > 0) {
				asm += "mov r" + regID + ", r8";
			} else if (regID < 0) {
				asm += "mov [rbp-" + regID*(-8) + "], r8"; 
			}
			return asm;
		}
	}
	
	public static class Add extends BinaryInstruction{
		public Add(Instruction operand1, Instruction operand2) {
			super(operand1, operand2);
			name = "add";
		}
		
		@Override
		public String assemblyCode() {	
			String asm ="";
			if(op1.name.equals("constant")) {
				asm += "mov r8, " + ((Constant)op1).constantValue + System.getProperty("line.separator");
			} else if(op1.name.equals("var")){
				asm += "mov r8, " + ((Var)op1).varName + System.getProperty("line.separator");
				asm += "mov r8, [r8]" + System.getProperty("line.separator");
			} else {
				if(op1.regID > 0) {
					asm += "mov r8, r" + op1.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "mov r8, [rbp-" + op1.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(op2.name.equals("constant")) {
				asm += "add r8, " + ((Constant)op2).constantValue + System.getProperty("line.separator");
			} else if(op2.name.equals("var")){
				asm += "mov r9, " + ((Var)op2).varName + System.getProperty("line.separator");
				asm += "add r8, [r9]" + System.getProperty("line.separator");
			} else {
				if(op2.regID > 0) {
					asm += "add r8, r" + op2.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "add r8, [rbp-" + op2.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(regID > 0) {
				asm += "mov r" + regID + ", r8";
			} else {
				asm += "mov [rbp-" + regID*(-8) + "], r8"; 
			}
			
			return asm;
		}
	}
	
	public static class Sub extends BinaryInstruction{
		public Sub(Instruction operand1, Instruction operand2) {
			super(operand1, operand2);
			name = "sub";
		}
		
		@Override
		public String assemblyCode() {	
			String asm ="";
			
			if(op1.name.equals("constant")) {
				asm += "mov r8, " + ((Constant)op1).constantValue + System.getProperty("line.separator");
			} else if(op1.name.equals("var")){
				asm += "mov r8, " + ((Var)op1).varName + System.getProperty("line.separator");
				asm += "mov r8, [r8]" + System.getProperty("line.separator");
			} else {
				if(op1.regID > 0) {
					asm += "mov r8, r" + op1.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "mov r8, [rbp-" + op1.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(op2.name.equals("constant")) {
				asm += "sub r8, " + ((Constant)op2).constantValue + System.getProperty("line.separator");
			} else if(op2.name.equals("var")){
				asm += "mov r9, " + ((Var)op2).varName + System.getProperty("line.separator");
				asm += "sub r8, [r9]" + System.getProperty("line.separator");
			} else {
				if(op2.regID > 0) {
					asm += "sub r8, r" + op2.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "sub r8, [rbp-" + op2.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(regID > 0) {
				asm += "mov r" + regID + ", r8";
			} else {
				asm += "mov [rbp-" + regID*(-8) + "], r8"; 
			}
			
			return asm;
		}
	}
	
	public static class Mul extends BinaryInstruction{
		public Mul(Instruction operand1, Instruction operand2) {
			super(operand1, operand2);
			name = "mul";
		}
		
		@Override
		public String assemblyCode() {	
			String asm ="mov rbx, 0" + System.getProperty("line.separator");
			
			if(op1.name.equals("constant")) {
				asm += "mov rax, " + ((Constant)op1).constantValue + System.getProperty("line.separator");
			} else if(op1.name.equals("var")){
				asm += "mov r8, " + ((Var)op1).varName + System.getProperty("line.separator");
				asm += "mov rax, [r8]" + System.getProperty("line.separator");
			} else {
				if(op1.regID > 0) {
					asm += "mov rax, r" + op1.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "mov rax, [rbp-" + op1.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(op2.name.equals("constant")) {
				asm += "mov r8, " + ((Constant)op2).constantValue + System.getProperty("line.separator");
				asm += "mul r8" + System.getProperty("line.separator");
			} else if(op2.name.equals("var")){
				asm += "mov r8, " + ((Var)op2).varName + System.getProperty("line.separator");
				asm += "mov r8, [r8]" + System.getProperty("line.separator");
				asm += "mul r8" + System.getProperty("line.separator");
			} else {
				if(op2.regID > 0) {
					asm += "mul r" + op2.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "mul [rbp-" + op2.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(regID > 0) {
				asm += "mov r" + regID + ", rax";
			} else {
				asm += "mov [rbp-" + regID*(-8) + "], rax"; 
			}
			
			return asm;
		}
	}
	
	public static class Div extends BinaryInstruction{
		public Div(Instruction operand1, Instruction operand2) {
			super(operand1, operand2);
			name = "div";			
		}
		
		@Override
		public String assemblyCode() {	
			String asm ="mov rbx, 0" + System.getProperty("line.separator");
			
			if(op1.name.equals("constant")) {
				asm += "mov rax, " + ((Constant)op1).constantValue + System.getProperty("line.separator");
			} else if(op1.name.equals("var")){
				asm += "mov 8, " + ((Var)op1).varName + System.getProperty("line.separator");
				asm += "mov rax, [r8]" + System.getProperty("line.separator");
			} else {
				if(op1.regID > 0) {
					asm += "mov rax, r" + op1.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "mov rax, [rbp-" + op1.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(op2.name.equals("constant")) {
				asm += "mov r8, " + ((Constant)op2).constantValue + System.getProperty("line.separator");
				asm += "div r8" + System.getProperty("line.separator");
			}  else if(op2.name.equals("var")){
				asm += "mov r8, " + ((Var)op2).varName + System.getProperty("line.separator");
				asm += "mov r8, [r8]" + System.getProperty("line.separator");
				asm += "div r8" + System.getProperty("line.separator");
			} else {
				if(op2.regID > 0) {
					asm += "div r" + op2.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "div [rbp-" + op2.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(regID > 0) {
				asm += "mov r" + regID + ", rax";
			} else {
				asm += "mov [rbp-" + regID*(-8) + "], rax"; 
			}
			
			return asm;
		}
	}
	
	public static class Cmp extends BinaryInstruction{
		public Cmp(Instruction operand1, Instruction operand2) {
			super(operand1, operand2);
			name = "cmp";
		}
		
		@Override
		public String assemblyCode() {	
			String asm ="";
			
			if(op1.name.equals("constant")) {
				asm += "mov rax, " + ((Constant)op1).constantValue + System.getProperty("line.separator");
			}  else if(op1.name.equals("var")){
				asm += "mov r8, " + ((Var)op1).varName + System.getProperty("line.separator");
				asm += "mov rax, [r8]" + System.getProperty("line.separator");
			} else {
				if(op1.regID > 0) {
					asm += "mov rax, r" + op1.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "mov rax, [rbp-" + op1.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(op2.name.equals("constant")) {
				asm += "cmp rax, " + ((Constant)op2).constantValue;
			} else if(op2.name.equals("var")){
				asm += "mov r8, " + ((Var)op2).varName + System.getProperty("line.separator");
				asm += "mov r8, [r8]" + System.getProperty("line.separator");
				asm += "cmp rax, r8" ;
			} else {
				if(op2.regID > 0) {
					asm += "cmp rax, r" + op2.regID;
				} else if (regID < 0) {
					asm += "cmp rax, [rbp-" + op2.regID*(-8) + "]";
				}
			}
			
			return asm;
		}
	}
	
	public static class Call extends Instruction{
		public String functionName;
		public ArrayList<Instruction> formalParams;
		public Call(String inputFunctionName, ArrayList<Instruction> parameters) {
			super();
			formalParams = parameters;
			functionName = inputFunctionName;
			name = "call";
		}
		
		public String toString() {
			String result = "";
			if(regID != 0) {
				result = result + "r" + regID + " ";
			}
			return result + instruction_id + ":" + name + " " + functionName + formalParams.toString();
		}
		
		@Override
		public String assemblyCode() {	
			return "call " + functionName;
		}
		
	}
	
	public static class Adda extends BinaryInstruction{
		//op1 is array
		boolean isGlobal;
		public Adda(Instruction operand1, Instruction operand2, boolean global) {
			super(operand1, operand2);
			name = "adda";
			isGlobal = global;
		}
		
		@Override
		public String assemblyCode() {	
			String asm ="";
			
			if(isGlobal) {
				asm += "mov r8, " + ((Array)op1).arrayName + System.getProperty("line.separator");
				
				if(op2.name.equals("constant")) {
					asm += "add r8, " + ((Constant)op2).constantValue + System.getProperty("line.separator");
				} else if(op1.name.equals("var")){
					asm += "mov r9, " + ((Var)op1).varName + System.getProperty("line.separator");
					asm += "add r8, [r9]" + System.getProperty("line.separator");
				} else {
					if(op2.regID > 0) {
						asm += "add r8, r" + op2.regID + System.getProperty("line.separator");
					} else if (regID < 0) {
						asm += "add r8, [rbp-" + op2.regID*(-8) + "]" + System.getProperty("line.separator");
					}
				}
			} else {
				asm += "mov r8, rbp" + System.getProperty("line.separator");
				asm += "sub r8, " + ((Array)op1).address*(-8) + System.getProperty("line.separator");
				
				if(op2.name.equals("constant")) {
					asm += "sub r8, " + ((Constant)op2).constantValue + System.getProperty("line.separator");
				} else if(op1.name.equals("var")){
					asm += "mov r9, " + ((Var)op1).varName + System.getProperty("line.separator");
					asm += "sub r8, [r9]" + System.getProperty("line.separator");
				} else {
					if(op2.regID > 0) {
						asm += "sub r8, r" + op2.regID + System.getProperty("line.separator");
					} else if (regID < 0) {
						asm += "sub r8, [rbp-" + op2.regID*(-8) + "]" + System.getProperty("line.separator");
					}
				}
			}

			if(regID > 0) {
				asm += "mov r" + regID + ", r8";
			} else if (regID < 0){
				asm += "mov [rbp-" + regID*(-8) + "], r8"; 
			}
			
			return asm;
		}
	}
	
	public static class Load extends UnaryInstruction{

		//Instruction op;
		public Load(Instruction operand) {
			super(operand);
			name = "load";
		}
		
		@Override
		public String assemblyCode() {	
			String asm = "";
			if(op.name.equals("var")) {
				if(regID > 0) {
					asm += "mov r8, " + ((Var)op).varName + System.getProperty("line.separator");
					asm += "mov r" + regID + ", [r8]";
				} else if (regID < 0){
					asm += "mov r8, " + ((Var)op).varName + System.getProperty("line.separator");
					asm += "mov r8, [r8]" + System.getProperty("line.separator");

					asm += "mov [rbp-" + regID*(-8) + "], r8"; 
				}
			} else if(op.regID > 0) {
				if(regID > 0) {
					asm += "mov r" + regID + ", [r" + op.regID + "]";
				} else if (regID < 0) {
					asm += "mov r8, [r" + op.regID + "]" + System.getProperty("line.separator");
					asm += "mov [rbp-" + regID*(-8) + "], r8"; 
				}
			} else if(op.regID < 0){
				if(regID > 0) {
					asm += "mov r8, [rbp-" + op.regID*(-8) + "]"  + System.getProperty("line.separator");
					asm += "mov r" + regID + ", [r8]";
				} else if (regID < 0){
					asm += "mov r8, [rbp-"  + op.regID*(-8) + "]" + System.getProperty("line.separator");
					asm += "mov r8, [r8]" +  System.getProperty("line.separator");
					asm += "mov [rbp-" + regID*(-8) + "], r8"; 
				}
			}
			
			return asm;
		}
	}
	
	public static class Store extends BinaryInstruction{
		//store operand1 to memory address operand2
		public Store(Instruction operand1, Instruction operand2) {
			super(operand1, operand2);
			name = "store";
		}
		
		@Override
		public String assemblyCode() {	
			String asm = "";
			
			if(op1.name.equals("var")) {
				asm += "mov r8, " + ((Var)op1).varName + System.getProperty("line.separator");
				asm += "mov r8, [r8]" + System.getProperty("line.separator");
			} else if(op1.name.equals("constant")) {
				asm += "mov r8, " + ((Constant)op1).constantValue + System.getProperty("line.separator");
			} else {
				if(op1.regID > 0) {
					asm += "mov r8, r" + op1.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "mov r8, [rbp-" + op1.regID*(-8) + "]" + System.getProperty("line.separator"); 
				}
			}
			
			if(op2.name.equals("var")) {
				asm += "mov r9, " + ((Var)op2).varName + System.getProperty("line.separator");
				asm += "mov [r9], r8";
			} else {
				if(op2.regID > 0) {
					asm += "mov r9, r" + op2.regID + System.getProperty("line.separator");
					asm += "mov [r9], r8";
				} else if (op2.regID < 0) {
					asm += "mov r9, [rbp-" + op2.regID*(-8) + "]" + System.getProperty("line.separator");
					asm += "mov [r9], r8";
				}
			}
			return asm;
		}
	}
	
	public static class Move extends BinaryInstruction{
		//assign operand2 = operand1
		public Move(Instruction operand1, Instruction operand2) {
			super(operand1, operand2);
			name = "move";
		}
		
		@Override
		public String assemblyCode() {	
			String asm = "";
			if(op1.name.equals("constant")) {
				asm += "mov r8, " + ((Constant)op1).constantValue + System.getProperty("line.separator");
			} else if(op1.name.equals("var")) {
				asm += "mov r8, " + ((Var)op1).varName + System.getProperty("line.separator");
				asm += "mov r8, [r8]" + System.getProperty("line.separator");
			} else {
				if(op1.regID > 0) {
					asm += "mov r8, r" + op1.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "mov r8, [rbp-" + op1.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			if(op2.name.equals("var")) {
				
				asm += "mov r9, " + ((Var)op2).varName + System.getProperty("line.separator");
				asm += "mov [r9], r8";
			} else {
				asm += "mov r" + op2.regID + ", r8";
			}
			return asm;
		}
	}
	
	public static class Phi extends Instruction{
		public static class FiPair {
			public BasicBlock basicBlock;
			public Instruction instruction;
			
			public FiPair(BasicBlock b, Instruction in) {
				basicBlock = b;
				instruction = in;
			}
			
			public String toString() {
				if(instruction.name.compareTo("constant") == 0) {
					return instruction.toString();
				} else {
					return String.valueOf(instruction.instruction_id);
				}
			}
		}
		
		public ArrayList<FiPair> fiPair;
		public boolean liveness;
		public String varName;
		public Phi(ArrayList<FiPair> inputFiPair, String var) {
			super();
			varName = var;
			liveness = false;
			fiPair = inputFiPair;
			name = "phi";
		}
		
		public Instruction getArgument(int i) {
			return fiPair.get(i).instruction;
		}
		
		public String toString() {
			String result = "";
			
			if(regID != 0) {
				result = result + "r" + regID + " ";
			}
			
			result = result + instruction_id + ":" + name + " " + varName + fiPair.toString();
			return result;
		}
		
		public void replaceFi(Parser.InstructionPair fi) {
			for(int i=0; i<fiPair.size(); i++) {
				if(fiPair.get(i) != null && fiPair.get(i).instruction == fi.fiInstruction) {
					fiPair.get(i).instruction = fi.replacementInstruction;
				}
			}
		}
	}
	
	
	public static class End extends Instruction{
		public End() {
			super();
			name = "end";
		}
		public String toString() {
			return name;
		}
		
		@Override
		public String assemblyCode() {
			String asm = "mov rax, 0x2000001" + System.getProperty("line.separator");
			asm += "mov rdi, 0" + System.getProperty("line.separator");
			asm += "syscall";
			return asm;
		}
	}
	
	public static class Bra extends Instruction{

		public Bra() {
			super();
			name = "bra";
		}
		
		public String toString() {
			return instruction_id + ":" + name;
		}
		
	}
	
	public static class Jump extends Instruction{

		public Jump() {
			super();
			name = "jump";
		}
		
		public String toString() {
			return instruction_id + ":" + name;
		}
	}
	
	public static class Bne extends UnaryInstruction{
		public Bne(Instruction operand) {
			super(operand);
			name = "bne";
		}
		
		@Override
		public String assemblyCode() {	
			return "jne";
		
		}
	}
	
	public static class Beq extends UnaryInstruction{

		public Beq(Instruction operand) {
			super(operand);
			name = "beq";
		}
		
		@Override
		public String assemblyCode() {	
			return "je";
		}
	}
	
	public static class Ble extends UnaryInstruction{

		public Ble(Instruction operand) {
			super(operand);
			name = "ble";
		}
		
		@Override
		public String assemblyCode() {	
			return "jle";
		}
	}
	
	public static class Blt extends UnaryInstruction{

		public Blt(Instruction operand) {
			super(operand);
			name = "blt";
		}
		
		@Override
		public String assemblyCode() {	
			return "jl";
		}
	}
	
	public static class Bge extends UnaryInstruction{

		public Bge(Instruction operand) {
			super(operand);
			name = "bge";
		}
		
		@Override
		public String assemblyCode() {	
			return "jge";
		}
	}
	
	public static class Bgt extends UnaryInstruction{

		public Bgt(Instruction operand) {
			super(operand);
			name = "bgt";
		}
		
		@Override
		public String assemblyCode() {	
			return "jg";
		}
	}
	
	public static class Read extends Instruction{
		public Read() {
			super();
			name = "read";
		}
		
		public String toString() {
			if(regID > 0)
				return "r" + regID + " " + instruction_id + ":" + name;
			else 
				return instruction_id + ":" + name;
		}
		
		@Override
		public String assemblyCode() {
			String asm =  "call InputNum" + System.getProperty("line.separator");
			
			if(regID > 0) {
				asm += "mov r" + regID + ", r8";
			} else if (regID < 0) {
				asm += "mov [rbp-" + regID*(-8) + "], r8";
			}
			
			return asm;
		}
	}
	
	public static class Write extends UnaryInstruction{

		public Write(Instruction operand) {
			super(operand);
			name = "write";
		}
		
		@Override
		public String assemblyCode() {
			String asm =  "";
			
			if(op.name.equals("constant")) {
				asm += "mov r8, " + ((Constant)op).constantValue + System.getProperty("line.separator");
			} else if(op.name.equals("var")) {
				asm += "mov r8, " + ((Var)op).varName + System.getProperty("line.separator");
				asm += "mov r8, [r8]" + System.getProperty("line.separator");
			} else {
				if(op.regID > 0) {
					asm += "mov r8, r" + op.regID + System.getProperty("line.separator");
				} else if (regID < 0) {
					asm += "mov r8, [rbp-" + op.regID*(-8) + "]" + System.getProperty("line.separator");
				}
			}
			
			asm += "call OutputNum";
			
			return asm;
		}
	}
	
	public static class Wln extends Instruction{

		public Wln() {
			super();
			name = "wln";
		}
		
		@Override
		public String assemblyCode() {
			return "call OutputNewLine";
		}
		public String toString() {
			return instruction_id + ":" + name;
		}
	}
	
	public static class Return extends UnaryInstruction{
		public Return(Instruction operand) {
			super(operand);
			name = "return";
		}
	}
	
	//a pseudo code to input parameters for a function
	public static class InputParam extends UnaryInstruction {
		//Instruction param;
		
		public InputParam(Instruction formalParam) {
			super(formalParam);
			name = "inputParam";
		}
		
		public String toString() {
			if(regID != 0) {
				return "r" + regID + " " + instruction_id + ":" + name + "(" + ((Var)op).toString() + ")";
			} else 
				return instruction_id + ":" + name + "(" + ((Var)op).toString() + ")";
		}
	}
	
	public static class Register extends Instruction {
		public Register(int regNum) {
			super();
			regID = regNum;
			name = "register";
			instance_id--;
		}
		
		public String toString() {
			return "r" + String.valueOf(regID);
		}
	}

	public static class Constant extends Instruction {
		public int constantValue;
		public Constant(int value) {
			name = "constant";
			constantValue = value;
			
			instance_id--;
		}
		
		public String toString() {
			return "#" + String.valueOf(constantValue);
		}
	}
	
	public static class Var extends Instruction {
		public String varName;
	
		public Var() {
			name = "var";
			
			instance_id--;
		}
		
		public Var(String var) {
			varName = var;
			name = "var";
			
			instance_id--;
		}
		
		public Var(Var v) {
			name = v.name;
			varName = v.varName;
			
			instance_id--;
		}
		
		public String toString() {
			return varName;
		}
	}
	
	public static class Array extends Instruction {
		public ArrayList<Integer> arraySize;
		public String arrayName;
		public int address;
		
		public Array() {
			name = "array";
			arraySize = new ArrayList<Integer>();
			
			instance_id--;
		}
		
		public Array(String array, ArrayList<Integer> size) {
			arrayName = array;
			
			arraySize = size;
			
			name = "array";
			
			instance_id--;
		}
		
		public Array(Array a) {
			name = a.name;
			arrayName = a.arrayName;
			arraySize = new ArrayList<Integer>();
			
			for(int i=0; i<a.arraySize.size(); i++) {
				arraySize.add(a.arraySize.get(i));
			}
			arraySize = a.arraySize;
			
			address = a.address;
			
			instance_id--;
		}
		
		public int byteSize() {
			int result = 8;
			for(int i=0; i<arraySize.size(); i++) {
				result *= arraySize.get(i);
			}
			
			return result;
		}
		
		public String toString() {
			return arrayName;
		}
	}
	
	public static class AllocateStack extends Instruction {
		public int stackSize;
		public Instruction op;
		public AllocateStack(Instruction instruction) {
			super();
			if(instruction.name.equals("array")) {
				stackSize = ((Array) instruction).byteSize();
			} else {
				stackSize = 1;
			}
			op = instruction;
			instance_id--;
			name = "allocateStack";
		}
		
		@Override
		public String assemblyCode() {
			String asm ="";
			for(int i=0; i<stackSize; i++) {
				asm += "push r8" + System.getProperty("line.separator");
			}
			
			return asm;
		}
		
		public String toString() {
			return "allocate stack " + stackSize + " byte for(" + op.toString() + ")";

		}
	}
	
	public static class FreeStack extends Instruction{
		public int stackSize;
		public Instruction op;
		public FreeStack(Instruction instruction) {
			super();
			if(instruction.name.equals("array")) {
				stackSize = ((Array) instruction).byteSize();
			} else {
				stackSize = 1;
			}
			op = instruction;
			instance_id--;
			name = "freeStack";
		}
		
		@Override
		public String assemblyCode() {
			String asm ="";
			for(int i=0; i<stackSize; i++) {
				asm += "pop r8" + System.getProperty("line.separator");
			}
			
			return asm;
		}
		
		public String toString() {
			return "free stack " + stackSize + " byte for(" + op.toString() + ")";

		}
	}
	
	public static class AllocateAddress extends Instruction {
		Instruction var;
		public AllocateAddress(Instruction variable) {
			super();
			var = variable;
			instance_id--;
			name = "allocate address";
		}
		
		@Override
		public String assemblyCode() {
			String asm = "";
			if(var.name.equals("var")) {
				Var temp = (Var) var;
				asm += temp.varName;
				asm += ":  resb 8";
			} else {
				Array temp = (Array) var;
				asm += temp.arrayName;
				asm += ":  resb " + String.valueOf(temp.byteSize());
			}
			
			return asm;
		}
		
		public String toString() {
			return "allocate address(" + var.toString() + ")";
		}
	}
	
	public static class PassArg extends UnaryInstruction {
		public PassArg(Instruction instruction) {
			super(instruction);
			name = "passArg";
		}
		
		public String toString() {
			if(op != null)
				return instruction_id + " " + name + "(" + op.toString() + ")";
			else 
				return instruction_id + " " + name + " returnSpace";
		}
		
		@Override
		public String assemblyCode() {
			String asm ="";
			if(op == null) {
				asm += "push rax";
			} else {
				if(op.name.equals("constant")) {
					asm += "mov r8, " + ((Constant)op).constantValue + System.getProperty("line.separator");
					asm += "push r8";
				} else if(op.name.equals("var")) {
					asm += "mov r8, " + ((Var)op).varName + System.getProperty("line.separator");
					asm += "mov r8, [r8]" + System.getProperty("line.separator");
					asm += "push r8";
				} else {
					if(op.regID > 0) {
						asm += "push r" + op.regID;
					} else if (regID < 0) {
						asm += "mov r8, [rbp-" + op.regID*(-8) + System.getProperty("line.separator");
						asm += "push r8";
					}
				}
			}
			return asm;
		}
	}
	
	public static class RemoveArg extends UnaryInstruction {
		public RemoveArg(Instruction instruction) {
			super(instruction);
			name = "removeArg";
		}
		
		public String toString() {
			return instruction_id + " " + name + "(" + op.toString() + ")";
		}
		
		@Override 
		public String assemblyCode() {
			return "pop r8";
		}
	}
	
	public static class GetReturn extends Instruction {
		Instruction call;
		public GetReturn(Instruction instruction) {
			super();
			call = instruction;
			name = "getReturn";
		}
		
		@Override
		public String assemblyCode() {
			String asm = "";
			if(call.regID > 0) {
				asm += "pop r" + call.regID;
			} else {
				asm += "pop r8" + System.getProperty("line.separator");
				if(call.regID < 0)
					asm += "mov [rbp-" + call.regID*(-8) +"], " + "r8";
			}
			return asm;
		}
		
		public String toString() {
			return instruction_id + " " + "getReturnValue";
		}
	}
	
	public static class SaveCalleeReg extends Instruction {
		public SaveCalleeReg() {
			super();
			name = "saveCalleeReg";
		}
		
		public String toString() {
			return instruction_id + ":" + name;

		}
		
		@Override
		public String assemblyCode() {
			String asm = "push r10"  + System.getProperty("line.separator");
			asm += "push r11"  + System.getProperty("line.separator");
			asm += "push r12"  + System.getProperty("line.separator");
			asm += "push r13"  + System.getProperty("line.separator");
			asm += "push r14"  + System.getProperty("line.separator");
			asm += "push r15";
			
			return asm;
		}
	}
	
	public static class RestoreCalleeReg extends Instruction {
		public RestoreCalleeReg() {
			super();
			name = "restoreCalleeReg";
		}
		
		public String toString() {
			return instruction_id + ":" + name;

		}
		
		@Override
		public String assemblyCode() {
			String asm = "pop r15"  + System.getProperty("line.separator");
			asm += "pop r14"  + System.getProperty("line.separator");
			asm += "pop r13"  + System.getProperty("line.separator");
			asm += "pop r12"  + System.getProperty("line.separator");
			asm += "pop r11"  + System.getProperty("line.separator");
			asm += "pop r10";
			
			return asm;
		}
	}	
}

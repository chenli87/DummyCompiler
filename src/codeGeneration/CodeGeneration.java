package codeGeneration;

import java.io.*;
import java.util.*;
import parser.BasicBlock;
import parser.Function;
import parser.Instruction;
import parser.Instruction.*;
import registerAllocation.RegisterAllocation;

public class CodeGeneration {
	RegisterAllocation registerAllocation;
	ArrayList<Function> functions;
	ArrayList<Instruction> globalMallocs;
	FileWriter fstream;
	BufferedWriter out;
	String asmFileName;
	
	public CodeGeneration(String fileName) throws IOException {
		registerAllocation = new RegisterAllocation(fileName);
		functions = registerAllocation.allocatedInstructions();
		globalMallocs = registerAllocation.globalMallocs;
		asmFileName = (new StringTokenizer(fileName, ".")).nextToken();
		fstream = new FileWriter( asmFileName + ".asm");
		out = new BufferedWriter(fstream);
	}

	private void allocateMemory() throws IOException {
		out.write("section .bss");
		out.newLine();
		out.write("SYSTEMINPUT:  resb 20");
		out.newLine();
		out.write("SYSTEMOUTPUT:  resb 20");
		out.newLine();
		
		for(Instruction instruction: globalMallocs) {
			out.write(instruction.assemblyCode());
			out.newLine();
		}
		out.newLine();
		out.newLine();
	}
	
	private void addFunctionPrototype() throws IOException {
		for(int i=0; i<functions.size()-1; i++) {
			out.write("global " + functions.get(i).functionName);
			out.newLine();
		}
		
		out.newLine();
		out.newLine();
	}
	
	private void addDefaultFunctions() throws IOException {
		out.write("InputNum: ");
		out.newLine();
		out.write("push r11");
		out.newLine();
		out.write("mov rax, 0x2000003");
		out.newLine();
		out.write("mov rdi, 0");
		out.newLine();
		out.write("mov rsi, SYSTEMINPUT");
		out.newLine();
		out.write("mov rdx, 20");
		out.newLine();
		out.write("syscall");
		out.newLine();

		out.write("mov rdi, rax");
		out.newLine();
		out.write("dec rdi");
		out.newLine();
		out.write("mov rbx, SYSTEMINPUT");
		out.newLine();
		out.write("mov rsi, 0");
		out.newLine();
		out.write("mov r8, 0");
		out.newLine();

		out.write("string2int:");
		out.newLine();
		out.write("dec rdi");
		out.newLine();
		out.write("mov rcx, [rbx + rsi]");
		out.newLine();
		out.write("and rcx, 0xFF");
		out.newLine();
		out.write("inc rsi");
		out.newLine();

		out.write("sub rcx ,48");
		out.newLine();
		out.write("mov rax, r8");
		out.newLine();
		out.write("mov r8, 10");
		out.newLine();
		out.write("mov rdx, 0");
		out.newLine();
		out.write("mul r8");
		out.newLine();
		out.write("mov r8, rax");
		out.newLine();
		out.write("add r8, rcx");
		out.newLine();

		out.write("cmp rdi, 0");
		out.newLine();
		out.write("jg	string2int");
		out.newLine();
		out.write("pop r11");
		out.newLine();

		out.write("ret");
		out.newLine();
		out.newLine();
		
		out.write("OutputNum:");
		out.newLine();
		out.write("push r11");
		out.newLine();
		out.write("mov rdi, 0");
		out.newLine();
		out.write("mov rax, r8");
		out.newLine();
		out.write("mov r8, 10");
		out.newLine();
		out.write("mov rdx, 0");
		out.newLine();

		out.write("int2string:");
		out.newLine();
		out.write("div r8");
		out.newLine();
		out.write("push rdx");
		out.newLine();
		out.write("inc rdi");
		out.newLine();
		out.write("mov rdx, 0");
		out.newLine();
		out.write("cmp rax, 0");
		out.newLine();
		out.write("jg int2string");
		out.newLine();
		out.write("mov rsi, 0");
		out.newLine();
		out.write("mov r8, SYSTEMOUTPUT");		
		out.newLine();

		out.write("write:");
		out.newLine();
		out.write("pop rdx");
		out.newLine();
		out.write("add rdx, 48");
		out.newLine();
		out.write("mov [r8 + rsi], rdx");
		out.newLine();
		out.write("inc rsi");		
		out.newLine();
		out.write("dec rdi");
		out.newLine();
		out.write("cmp rdi, 0");
		out.newLine();
		out.write("jg write");
		out.newLine();

		out.write("mov rax, 0x2000004");
		out.newLine();
		out.write("mov rdi, 1");
		out.newLine();
		out.write("mov rsi, SYSTEMOUTPUT");
		out.newLine();
		out.write("mov rdx, 20");
		out.newLine();
		out.write("syscall");
		out.newLine();
		out.write("pop r11");
		out.newLine();
		out.write("ret");
		out.newLine();
		out.newLine();
		
		out.write("OutputNewLine:");
		out.newLine();
		out.write("push r11");
		out.newLine();
		out.write("mov r8, SYSTEMOUTPUT");
		out.newLine();
		out.write("mov r9, 10");
		out.newLine();
		out.write("mov [r8], r9");
		out.newLine();

		out.write("mov rax, 0x2000004");
		out.newLine();
		out.write("mov rdi, 1");
		out.newLine();
		out.write("mov rsi, SYSTEMOUTPUT");
		out.newLine();
		out.write("mov rdx, 20");
		out.newLine();
		out.write("syscall ");
		out.newLine();
		out.write("pop r11");
		out.newLine();

		out.write("ret");
		out.newLine();
		out.newLine();

	}
	
	private void addFunctions() throws IOException {
		for(int i=0; i<functions.size()-1; i++) {
			addFunction(functions.get(i));
			
			out.write("ret");
			out.newLine();
			out.newLine();
			out.newLine();
		}
	}
	
	private void addFunction(Function function) throws IOException {
		//add function label
		out.write(function.functionName + ":");
		out.newLine();
		//save rbp
		out.write("push rbp");
		out.newLine();
		out.write("mov rbp, rsp");
		out.newLine();
		
		int offset = -1;
		int parameter = 8;
		for(BasicBlock block: function.basicBlocks) {
			//add block label
			out.write("block" + block.blockID + ":");
			out.newLine();
			
			for(Instruction instruction: block.instructions) {
				if(instruction.name.equals("allocateStack")) {
					if(((AllocateStack)instruction).op.name.equals("array")) {
						((Array)((AllocateStack)instruction).op).address = offset;
					}
					out.write(instruction.assemblyCode());
					out.newLine();
					offset = offset - ((AllocateStack)instruction).stackSize;	
				} else if (instruction.name.equals("inputParam")){
					if(instruction.regID > 0) {
						out.write("mov r" + instruction.regID + ", [rbp+" + parameter*8 + "]");
						out.newLine();
					} else if(instruction.regID < 0){
						out.write("mov r8, [rbp+" + parameter*8 + "]");
						out.newLine();
						out.write("mov [rbp-" + instruction.regID*(-8) + "], r8");
						out.newLine();
					}
					parameter++;		
				} else if(instruction.name.equals("return")) {
					if(((Return)instruction).op.name.equals("constant")) {
						out.write("mov r8, " + ((Constant)((Return)instruction).op).constantValue);
						out.newLine();
						out.write("mov [rbp+" + parameter*8 + "], r8");
						out.newLine();
					} else if(((Return)instruction).op.name.equals("var")) {
						out.write("mov r8, " + ((Var)((Return)instruction).op).varName);
						out.newLine();
						out.write("mov r8, [r8]");
						out.newLine();
						out.write("mov [rbp+" + parameter*8 + "], r8");
						out.newLine();
					} else {
						if(((Return)instruction).op.regID > 0) {
							out.write("mov [rbp+" + parameter*8 + "], r" + ((Return)instruction).op.regID);
							out.newLine();
						} else if(((Return)instruction).op.regID < 0){
							out.write("mov r8, [rbp-" + ((Return)instruction).op.regID*(-8) + "]");
							out.newLine();
							out.write("mov [rbp+" + parameter*8 + "], r8");
							out.newLine();
						}
					}
				} else if(Instruction.isConditionalJump(instruction)){
					out.write(instruction.assemblyCode() + " block" + block.getConditionalBranch().blockID);
					out.newLine();
				} else if(instruction.name.equals("jump")) {
					out.write("jmp block" + block.getUnconditionalBranch().blockID);
					out.newLine();
				} else {
					out.write(instruction.assemblyCode());
					out.newLine();
				}
			}
		}
		//save rbp
		out.write("pop rbp");
		out.newLine();		
	}
	
	public void generateAssembly() throws IOException, InterruptedException {
		allocateMemory();
		
		out.write("section .text");
		out.newLine();
		out.write("global start");
		out.newLine();
		out.write("global InputNum");
		out.newLine();
		out.write("global OutputNum");
		out.newLine();
		out.write("global OutputNewLine");
		out.newLine();
		
		addFunctionPrototype();
		
		addDefaultFunctions();
		
		addFunctions();
		
		out.write("start:");
		out.newLine();

		addFunction(functions.get(functions.size()-1));
	
		out.newLine();
		out.newLine();
		
		end();
		
		out.close();
		
		nasm();
		ld();
	}
	
	private void ld() throws IOException, InterruptedException {
		String cmd = "ld " + asmFileName + ".o -o " + asmFileName;
		
		System.out.println(cmd);

		Runtime run = Runtime.getRuntime();
		
		Process pr = run.exec(cmd);
		pr.waitFor();
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line = "";
		while ((line=buf.readLine())!=null) {
			System.out.println(line);
		}
	}

	private void nasm() throws IOException, InterruptedException {
		String cmd = "nasm -f macho64 " + asmFileName + ".asm";
		Runtime run = Runtime.getRuntime();
		
		System.out.println(cmd);
		
		Process pr = run.exec(cmd);
		pr.waitFor();
		BufferedReader buf = new BufferedReader(new InputStreamReader(pr.getInputStream()));
		String line = "";
		while ((line=buf.readLine())!=null) {
			System.out.println(line);
		}
	}

	private void end() throws IOException {
		out.write("mov rax, 0x2000001");
		out.newLine();
		out.write("mov rdi, 0");
		out.newLine();
		out.write("syscall");
		out.newLine();
	}
}

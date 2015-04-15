package scanner;
import java.io.*;
import java.util.EnumSet;

import fileReader.CompilerFileReader;;

public class Scanner {
	public int lineNumber;
	
	private char inputSym;
	private CompilerFileReader reader;
	
	public Scanner(String fileName) throws IOException {
		reader = new CompilerFileReader(fileName);
		
		lineNumber = 1;
		this.next();
	}
	private void next() throws IOException {
		inputSym = reader.getSym();
	}
	
	private void skipWhiteSpace() throws IOException {
		while(inputSym == ' ' || inputSym == 0x09 || inputSym == 0x0c || inputSym == 0x0a || inputSym == 0x0d) {
			if(inputSym == 0x0a)
				lineNumber++;
			this.next();
		}
	}
	public Token getSym() {
		Token result = null;
		try{
			this.skipWhiteSpace();
			String num = "", id = "";
			switch (inputSym) {
				case '(': result = new Token(Token.Type.LPAREN); this.next(); return result;
				case ')': result = new Token(Token.Type.RPAREN); this.next(); return result;
				case '[': result = new Token(Token.Type.LSB); this.next(); return result;
				case ']': result = new Token(Token.Type.RSB); this.next(); return result;
				case '{': result = new Token(Token.Type.LCB); this.next(); return result;
				case '}': result = new Token(Token.Type.RCB); this.next(); return result;
				case '+': result = new Token(Token.Type.PLUS); this.next(); return result;
				case '-': result = new Token(Token.Type.MINUS); this.next(); return result;
				case '*': result = new Token(Token.Type.TIMES); this.next(); return result;
				case '/': result = new Token(Token.Type.DIVIDE); this.next(); return result;
				case ',': result = new Token(Token.Type.COMMA); this.next(); return result;
				case ';': result = new Token(Token.Type.SCOLON); this.next(); return result;
				case '.': result = new Token(Token.Type.DOT); this.next(); return result;
				case '=': this.next();
						  if(inputSym == '=') {
							  result = new Token(Token.Type.EQ);
							  this.next();
							  break;
						  } else {
							  result = new Token(Token.Type.ERROR);
							  break;
						  }
				case '!': this.next();
						  if(inputSym == '=') {
							  result = new Token(Token.Type.NEQ);
							  this.next();
						  } else {
							  result = new Token(Token.Type.ERROR);
						  }
						  break;
				case '<': this.next();
						  if(inputSym == '=') {
							  result = new Token(Token.Type.LE);
							  this.next();
						  } else if(inputSym == '-') {
							  result = new Token(Token.Type.ASSIGN);
							  this.next();
						  } else {
							  result = new Token(Token.Type.LT);
						  }
						  break;
				case '>': this.next();
				  		  if(inputSym == '=') {
				  			  result = new Token(Token.Type.GE);
				  			  this.next();
				  		  } else {
				  			  result = new Token(Token.Type.GT);
				  		  }
				  		  break;
				case '0':
				case '1':
				case '2':
				case '3':
				case '4':
				case '5':
				case '6':
				case '7':
				case '8':
				case '9': num = Character.toString(inputSym);
						  this.next();
						  while(inputSym == '0' || inputSym == '1' || inputSym == '2' || inputSym == '3' || inputSym == '4' || inputSym == '5' || inputSym == '6' || inputSym == '7' || inputSym == '8' || inputSym == '9') {
							  num = num.concat(Character.toString(inputSym));
							  this.next();
						  }
						  result = new Token(Token.Type.NUM, Integer.parseInt(num));
						  break;
				case 'a': 
				case 'b':
				case 'c':
				case 'd':
				case 'e':
				case 'f':
				case 'g':
				case 'h':
				case 'i':
				case 'j':
				case 'k':
				case 'l':
				case 'm':
				case 'n':
				case 'o':
				case 'p':
				case 'q':
				case 'r':
				case 's':
				case 't':
				case 'u':
				case 'v':
				case 'w':
				case 'x':
				case 'y':
				case 'z': id = Character.toString(inputSym);
						  this.next();
						  while(inputSym == '0' || inputSym == '1' || inputSym == '2' || inputSym == '3' || inputSym == '4' || inputSym == '5' || inputSym == '6' || inputSym == '7' || inputSym == '8' || inputSym == '9' || inputSym == 'a' || inputSym == 'b' || inputSym == 'c' || inputSym == 'd' || inputSym == 'e' || inputSym == 'f' || inputSym == 'g' || inputSym == 'h' || inputSym == 'i' || inputSym == 'j' || inputSym == 'k' || inputSym == 'l' || inputSym == 'm' || inputSym == 'n' || inputSym == 'o' || inputSym == 'p' || inputSym == 'q' || inputSym == 'r' || inputSym == 's' || inputSym == 't' || inputSym == 'u' || inputSym == 'v' || inputSym == 'w' || inputSym == 'x' || inputSym == 'y' || inputSym == 'z' ) {
							  id = id.concat(Character.toString(inputSym));
							  this.next();
						  }
						  result = new Token(Token.Type.ID, id);
						  for(Token.Type t : EnumSet.range(Token.Type.LET, Token.Type.MAIN)) {
							  if(t.toString().toLowerCase().compareTo(id) == 0)
								  result = new Token(t);
						  }
						  break;
				case 'I': id = Character.toString(inputSym);
				  		  this.next();
		  		  		  while(inputSym == 'n' || inputSym == 'p' || inputSym == 'u' || inputSym == 't' || inputSym == 'N' || inputSym == 'm') {
				  			  id = id.concat(Character.toString(inputSym));
				  			  this.next();
				  		  }
				  		  if(id.compareTo("InputNum") == 0) {
				  			  result = new Token(Token.Type.ID, id);
				  		  } else {
				  			  result = new Token(Token.Type.ERROR);
				  		  }
				  		  break;
				case 'O': id = Character.toString(inputSym);
		  		  		  this.next();
		  		  		  while(inputSym == 'u' || inputSym == 't' || inputSym == 'p' || inputSym == 'N' || inputSym == 'e' || inputSym == 'w' || inputSym == 'L' || inputSym == 'i' || inputSym == 'n' || inputSym == 'm') {
		  		  			  id = id.concat(Character.toString(inputSym));
		  		  			  this.next();
		  		  		  }
		  		  		  if(id.compareTo("OutputNum") == 0 || id.compareTo("OutputNewLine") == 0) {
		  		  			  result = new Token(Token.Type.ID, id);
		  		  		  } else {
		  		  			  result = new Token(Token.Type.ERROR);
		  		  		  }
		  		  		  break;
				case (char)(-1): result = new Token(Token.Type.EOF);
								 break;
				default: 	this.next();
							result = new Token(Token.Type.ERROR);
			}
		} catch (IOException ioe) {
			System.err.println(ioe);
		}
		return result;
	}
	
	public static void main(String args[]) throws IOException {
		Scanner s = new Scanner("/Users/chenli/compiler/PL241/src/test.txt");
		Token token = s.getSym();
		while (token.type != Token.Type.EOF) {
			System.out.println(token.toString());
			token = s.getSym();
		}
	}
}

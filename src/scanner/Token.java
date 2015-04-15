package scanner;

public class Token {
	public enum Type {
		ID, NUM, EQ, NEQ, LT, LE, GT, GE, LET, CALL, IF, THEN, ELSE, FI, WHILE, DO, OD, 
		RETURN, VAR, ARRAY, FUNCTION, PROCEDURE, MAIN, LPAREN, RPAREN, LSB, RSB, LCB, 
		RCB, TIMES, DIVIDE, PLUS, MINUS, ASSIGN, COMMA, SCOLON, DOT, EOF, ERROR   
	}
	
	public Type type;
	public int intVal;
	public String idName;
	
	public Token(Type inputType) {
		this.type = inputType;
	}
	
	public Token(Type inputType, int val) {
		this.type = inputType;
		this.intVal = val;
	}
	
	public Token(Type inputType, String name) {
		this.type = inputType;
		this.idName = new String(name);
	}
	
	public String toString() {
		if(type == Type.ID) {
			return type.toString().concat("(").concat(idName).concat(")"); 
		} else if(type == Type.NUM) {
			return type.toString().concat("(").concat(String.valueOf(intVal)).concat(")");
		} 
		return type.toString();
	}
}

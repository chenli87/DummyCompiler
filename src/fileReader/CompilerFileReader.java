package fileReader;
import java.io.*;

public class CompilerFileReader {
	private
		BufferedReader input;
	
	public CompilerFileReader(String FileName) throws FileNotFoundException {
		FileReader f = new FileReader(FileName);
		input = new BufferedReader(f);
	}
	
	public char getSym() throws IOException {
		return (char)input.read();
	}
}

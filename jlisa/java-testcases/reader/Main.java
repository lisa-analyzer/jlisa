import java.io.PrintWriter;
import java.io.PushbackReader;

public class Main {
	public static void main(String[] args) {
		PushbackReader r1 = new PushbackReader(null);
		PushbackReader r2 = new PushbackReader(r1, 10);
		Reader r3 = (Reader) new PushbackReader(r2, 15);
		
		int c1 = r1.read();
		int c2 = r2.read();
		int c3 = r3.read();
	}
}
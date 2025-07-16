import java.io.PrintWriter;

public class Main {
	public static void main(String[] args) {
		PrintWriter w1 = new PrintWriter(null);
		Writer w2 = (Writer) new PrintWriter(null);
		Writer w3 = new PrintWriter(w2);
		Writer w4 = new PrintWriter(w2, true);
		
		w1.print(false);
		w1.print(5.2);
		w1.println(false);
		w1.println(5);
		w1.flush();
	}
}
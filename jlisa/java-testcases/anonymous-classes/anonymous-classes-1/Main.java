import java.io.IOException;
import java.io.PrintWriter;

public class Main {

	public static void main(String[] args) throws Exception {
		A a = new A();

		PrintWriter p = a.getWriter();

		p.println("hello");

		assert(a.z == 431);
		assert(a.str.equals("hello"));
	}
}

class A {

	int z;
	String str = "ciao";

	A() {
		z = 42;
	}

	public PrintWriter getWriter() throws IOException {

		return new PrintWriter(System.out) {

			int innerz = 23;
			String innerString = "xyz";

			@Override
			public void println(String x) {
				int y = 0;
				innerz = 50;
				z = 431;
				str = x;
			}
		};
	}

}


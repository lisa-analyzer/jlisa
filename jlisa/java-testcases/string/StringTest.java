public class StringTest {

	public static void main(String[] args) {
		String s1 = new String();
		String s2 = new String("Hello");
		String s3 = new String(s2);
		String s4 = "Hello World";
		int x = 90;
		x = x + s4.length();
		String s5 = "A" + "B";
		String s6 = s5 + s4;
		String s7 = "A" + s6;
		boolean b = s4.contains(s2);
	}
}
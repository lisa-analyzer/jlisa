public class Main {

	public static void main(String[] args) {
		known();
		if (args.length > 0)
			unknown(args[0]);
	}

	// the characters of a known string go into the cells and back
	static void known() {
		char[] c = "abc".toCharArray();
		assert c.length == 3;
		assert c[1] == 'b';
		c[0] = 'x';
		assert new String(c).equals("xbc");
	}

	// a string of known length: the cells are unknown but the one written
	static void unknown(String a) {
		if (a.length() != 5)
			return;
		char[] c = a.toCharArray();
		assert c.length == 5;
		c[0] = 's';
		String s = "HELLO" + new String(c, 0, c.length);
		assert s.charAt(5) == 's';
	}
}

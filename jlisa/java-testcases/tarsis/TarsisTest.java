public class TarsisTest {

	static String any(byte[] bytes) {
		return new String(bytes);
	}

	static String pick(boolean b) {
		return b ? "alpha" : "beta";
	}

	public static void main(String[] args) {
		boolean b = args.length > 0;

		// a set of strings
		String s = pick(b);
		assert !s.contains("<bad/>");
		assert s.length() >= 4;
		int c = s.charAt(0);
		assert c != 120;

		// a known prefix followed by an unknown string
		String t = "id=" + any(new byte[3]);
		assert t.startsWith("id");
		assert !t.equals("xyz");

		// refinement on equals and startsWith
		String u = any(new byte[2]);
		if (u.equals("ok"))
			assert u.length() == 2;
		if (u.startsWith("abc"))
			assert u.length() >= 3;

		// substring and indexOf on a known prefix
		String v = "hello" + any(new byte[1]);
		assert v.substring(0, 2).equals("he");
		assert v.indexOf("l") == 2;

		// case conversion and replace on a set of strings
		String w = pick(b).toUpperCase().replace('A', 'o');
		assert !w.equals("alpha");

		// this one may not hold
		String x = pick(b);
		assert x.equals("alpha");
	}
}

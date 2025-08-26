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
		boolean b0 = s4.contains(s2);
		boolean b1 = s2.equals(s3);
		boolean b2 = s2.equals(s4);

		String s8 = s4.toLowerCase();
		String s9 = s4.toUpperCase();
		String s10 = s5.toString();
		char c1 = s10.charAt(0);
		String s11 = s4.trim();
		
		boolean b3 = s4.startsWith(s2);
		boolean b4 = s4.endsWith("World"); 
		boolean b5 = s5.matches("^AB");
		String s12 = s4.substring(5);
		
		int i1 = s4.compareTo("Hello World");
		String s13 = s4.replaceAll("o", "i");
		long l1 = 123;
		int i2 = s4.indexOf("World");
		int i3 = s4.lastIndexOf('H');
		String s14 = String.valueOf(l1);
		boolean b6 = false;
		String s15 = String.valueOf(b6);
		double d1 = 3.14;
		String s16 = String.valueOf(d1);
		char c2 = s10.charAt(0);
		String s17 = String.valueOf(c2);
		int i4 = s4.indexOf('o');
		Object o1 = new String("Nuovo Oggetto");
		String s18 = String.valueOf(o1);
		//byte[] by1 = s4.getBytes();
		boolean b7 = s4.equalsIgnoreCase("HELLO WORlD");
		String s19 = s4.replace('o', 'a');
		int i5 = s4.indexOf('o', 6);
		int i6 = ("ABCABABCAB").lastIndexOf("AB");
		int i7 = ("ABCABABCAB").lastIndexOf("C", 5);
		String s20 = s4.substring(4, 7);
		
	}
}
public class StringTest {

	public static void main(String[] args) {
		StringBuffer b0 = new StringBuffer();
		String a = new String("a");
		StringBuffer b1 = new StringBuffer(a);
		
		String s = b1.toString();
		
		b1.append('b');
		b1.append("c");
		b1.insert(1, 'x');
	}
}
public class Test1{
	
	String create(String a) {
		String b = new String(a);
		return b;
	}
	
	public static void main(String[] args) {
		
		String a;
		String b;
		String c;
		
		a = create("Ciao");
		b = create("Hello");
		c = create("World");
		
		c = "java";
		
	}
}
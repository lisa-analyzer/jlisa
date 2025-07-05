public class Main {
	
	public static void main(String[] args) {
		
		Object o = new Object();
		int x = 0;
		if (o instanceof Object)
			x = 1;
		else
			x = -1;
	}
}
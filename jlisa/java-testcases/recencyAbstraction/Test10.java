public class A{
	int a;
	
	public A(int a) {
		this.a = a;
	}
}

public class Main{
	
	A create (int n) {
		A obj = new A(n);
		return obj;
	}
	
	public static void main(String[] args) {
		
		A a;
		A b;
				
		int t = System.in.read();
		
		if(t > 1) {
			a = create(5);
		} else {
			b = create(7);
		}
	}
}
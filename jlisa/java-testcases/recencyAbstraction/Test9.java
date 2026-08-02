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
		A c;
		
		a = create(0);
		b = a;
				
		int t = System.in.read();
		
		if(t > 1) {
			a = create(5);
		} else {
			a = create(7);
		}
				
		a.a += 10;
		b.a = 20;
		
		c = b;
		
		b = create(12);
		a = create(21);
		
		c.a += 22;
		
	}
}
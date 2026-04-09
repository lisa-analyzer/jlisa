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
		A d;
		A e;
		
		a = create(1);
		b = create(7);
		//c = create(3);
		
		a.a = 5;
		
		c = create(10);
		
		a.a = 17;
		
		d = create(-3);
		
		e = create(50);
	}
}
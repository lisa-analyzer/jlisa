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
		a = create(2);
		a = create(5);
		b = create(7);
		c = create(-1);
		
		c = create(10);
				
		d = create(-3);
		
		e = create(50);
		
		a = create(100);
		b = create(-12);
		
		int t = args.length * (-args.length);
		
		if(t > 0)
			c = create(10);
		else
			c = create(12);
		
		
	}
}
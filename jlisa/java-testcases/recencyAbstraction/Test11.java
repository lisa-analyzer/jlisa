public class A{
	int a;
	int b;
	
	public A(int a,int b) {
		this.a = a;
		this.b = b;
	}
	
	public A(int a) {
		if(a % 2 == 0)
			this.a = a;
		else
			this.b = a;
	}
}

public class Main{
	
	A create (int n, int m) {
		A obj = new A(n,m);
		return obj;
	}
	
	A create (int n) {
		A obj = new A(n);
		return obj;
	}
	
	public static void main(String[] args) {
		
		A a;
		A b;
		A c;
		A d;
				
		a = create(1,2);
		b = create(2,3);
		c = create(2);
		d = create(3);
		
		int t = args.length * (-args.length);
		
		if(t > 1) {
			a = create(5,6);
		} else {
			c = create(8);
		}
		
		int h = 0;
		
	}
}
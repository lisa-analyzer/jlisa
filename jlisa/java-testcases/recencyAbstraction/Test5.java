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
		b = create(2);
		
		int t = System.in.read();
		int r = t;
		
		if(t > 3) {
			e = create(25);
			if(r < 2) {
				e = create(26);
			} else {
				e = create(27);
			}
		} else {
			d = create(-2);
			if(r < 2) {
				d = create(-3);
			} else {
				d = create(-4);
			}
		}
	}
}
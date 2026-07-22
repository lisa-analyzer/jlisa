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
		
		a = create(0);
		b = create(1);
		
		int i = 0;
		
		while (i < 3) {
			a = create(i);
			b = create(i+1);
			i++;
		}
	}
}
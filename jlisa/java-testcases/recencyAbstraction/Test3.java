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
				
		for(int i = 2; i < 5; i++) {
			for(int j = 1; j < 3; j++) {
				a = create(i*10 + j);
				b = create(i*10 + j + 1);
			}
		}
		
//		while (i < 5) {
//			a = create(i);
//			b = create(i+1);
//			i++;
//		}
	}
}
public class B extends A {
	public int foo() {
		int a = super.foo() + super.foo();
		return a;
	}
}
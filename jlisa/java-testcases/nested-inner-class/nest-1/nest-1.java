public class A {
  class B {

    public String toString(){
      return value;
    }

    protected String value;
  }

  public void foo(){
    B b1 = new B();
	System.out.print(B.toString());
  }
  
  public void main(String[] args) {
	  foo();
  }

}

public class Main {
  public static void main(String[] args) {
	  
	  A a = new A();
	  
	  a.foo();
  }

}
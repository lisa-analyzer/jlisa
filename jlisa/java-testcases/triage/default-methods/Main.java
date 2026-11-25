// the default methods of interfaces are called on the objects of the classes
// implementing them
interface I {
  default int f() {
    return 1;
  }
}

class K implements I {
}

public class Main {
  public static void main(String[] args) {
    I k = new K();
    assert k.f() == 2;
  }
}

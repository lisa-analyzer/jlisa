import org.sosy_lab.sv_benchmarks.Verifier;

interface Shape {}

class Circle implements Shape {}

class Square implements Shape {}

public class Main {
  public static void main(String[] args) {
    int n = Verifier.nondetInt();
    Shape s;
    if (n > 0) {
      s = new Circle();
    } else {
      s = new Square();
    }
    if (s instanceof Circle && n < 5) {
      assert false;
    }
  }
}

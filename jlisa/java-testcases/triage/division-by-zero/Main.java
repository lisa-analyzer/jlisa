import org.sosy_lab.sv_benchmarks.Verifier;

// an integer division by zero raises ArithmeticException
public class Main {
  public static void main(String[] args) {
    int d = Verifier.nondetInt();
    int x = 10 / d;
  }
}

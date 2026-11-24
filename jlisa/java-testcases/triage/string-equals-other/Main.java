import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    String s = Verifier.nondetString();
    // the argument is an Integer: a String never equals it
    if (s.equals(Integer.valueOf(1))) {
      assert false;
    }
  }
}

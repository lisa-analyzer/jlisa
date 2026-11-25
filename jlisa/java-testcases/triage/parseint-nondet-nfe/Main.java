import org.sosy_lab.sv_benchmarks.Verifier;

// a string that is not constant might not be parsable
public class Main {
  public static void main(String[] args) {
    String s = Verifier.nondetString();
    int x = Integer.parseInt(s);
  }
}

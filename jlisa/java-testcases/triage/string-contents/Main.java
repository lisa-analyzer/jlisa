import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    String s = Verifier.nondetString();
    String t = s.concat("x");
    if (t.length() == 3 && t.startsWith("ab")) {
      assert false;
    }
  }
}

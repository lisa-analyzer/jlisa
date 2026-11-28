import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    Integer i = Verifier.nondetInt();
    Short s = Verifier.nondetShort();
    if (i == 42 && s == 7) {
      assert false;
    }
  }
}

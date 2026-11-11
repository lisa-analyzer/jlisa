import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    String a0 = Verifier.nondetString();
    String a1 = Verifier.nondetString();
    String b0 = Verifier.nondetString();
    String b1 = Verifier.nondetString();
    Integer a = a0.compareTo(a1);
    Integer b = b0.compareTo(b1);
    assert !(a == 2 && b == -46);
  }
}

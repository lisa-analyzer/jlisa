import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    Long l = Verifier.nondetLong();
    Double d = Verifier.nondetDouble();
    Float f = Verifier.nondetFloat();
    if (l > 1000000000000L && d <= 0.5 && f >= 1.5f && f < 2.0f) {
      assert false;
    }
  }
}

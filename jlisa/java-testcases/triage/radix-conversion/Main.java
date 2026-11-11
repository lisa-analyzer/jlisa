import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    Integer a = Verifier.nondetInt();
    Integer b = Verifier.nondetInt();
    String x = Integer.toOctalString(a);
    String y = Integer.toHexString(b);
    assert !(x.equals("17") && y.equals("fffffff3"));
  }
}

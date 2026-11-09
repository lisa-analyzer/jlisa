import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int[] values = new int[1];

  public static void main(String[] args) {
    if (Verifier.nondetBoolean()) {
      values[3] = 0;
    } else {
      values[4] = 0;
    }
  }
}

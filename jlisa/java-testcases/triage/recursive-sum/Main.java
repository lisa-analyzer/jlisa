import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static int sum(int m, int n) {
    if (n == 0) {
      return m;
    }
    return sum(m + 1, n - 1);
  }

  public static void main(String[] args) {
    int m = Verifier.nondetInt();
    int n = Verifier.nondetInt();
    if (n < 0 || n > 100) {
      return;
    }
    int result = sum(m, n);
    assert result == m + n;
  }
}

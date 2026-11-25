import org.sosy_lab.sv_benchmarks.Verifier;

// an index out of the bounds of an array raises ArrayIndexOutOfBoundsException
public class Main {
  public static void main(String[] args) {
    int[] a = new int[3];
    int i = Verifier.nondetInt();
    if (i >= 0 && i < 5)
      a[i] = 1;
  }
}

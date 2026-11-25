import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

// the type of the source is not known: the destination might be overwritten
// (with the int[], while the long[] raises ArrayStoreException)
public class Main {
  public static void main(String[] args) {
    boolean c = Verifier.nondetBoolean();
    Object s = c ? new int[]{1, 1} : (Object) new long[2];
    int[] d = new int[2];
    System.arraycopy(s, 0, d, 0, 2);
    assert d[0] == 0;
  }
}

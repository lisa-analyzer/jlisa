import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// the copy writes the destination: the assertion fails
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    int[] a = new int[]{1, 2};
    int[] b = new int[2];
    System.arraycopy(a, 0, b, 0, 2);
    assert b[0] == 0;
  }
}

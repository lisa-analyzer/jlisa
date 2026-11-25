import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

// copies within the same array behave as through a temporary array, both when
// the range moves forward (as when inserting in a list) and backward
public class Main {
  public static void main(String[] args) {
    int[] a = new int[]{1, 2, 3, 4};
    System.arraycopy(a, 0, a, 1, 3);
    assert a[0] == 1 && a[1] == 1 && a[2] == 2 && a[3] == 3;
    int[] b = new int[]{1, 2, 3, 4};
    System.arraycopy(b, 1, b, 0, 3);
    assert b[0] == 2 && b[1] == 3 && b[2] == 4 && b[3] == 4;
  }
}

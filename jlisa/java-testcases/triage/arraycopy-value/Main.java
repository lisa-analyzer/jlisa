import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

// the copied cells are the ones of the source, the others keep their values
public class Main {
  public static void main(String[] args) {
    int[] a = new int[]{1, 2};
    int[] b = new int[3];
    System.arraycopy(a, 0, b, 1, 2);
    assert b[1] == 1 && b[2] == 2 && b[0] == 0;
  }
}

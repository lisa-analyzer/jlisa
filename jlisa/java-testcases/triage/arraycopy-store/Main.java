import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

// an int[] cannot be copied into a long[], even when no cell is copied: the
// copy always raises ArrayStoreException
public class Main {
  public static void main(String[] args) {
    try {
      System.arraycopy(new int[1], 0, new long[1], 0, 0);
      assert false;
    } catch (ArrayStoreException e) {
    }
  }
}

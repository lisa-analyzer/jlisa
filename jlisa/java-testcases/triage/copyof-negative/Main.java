import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// a negative length raises NegativeArraySizeException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    int[] a = new int[2];
    int[] b = Arrays.copyOf(a, -1);
  }
}

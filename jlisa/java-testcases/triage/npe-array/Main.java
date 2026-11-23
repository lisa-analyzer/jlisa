import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// reading a cell of a null array raises NullPointerException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    int[] a = n ? null : new int[1];
    int x = a[0];
  }
}

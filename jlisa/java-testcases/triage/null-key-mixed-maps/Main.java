import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// only a ConcurrentHashMap rejects the null key: after put, n is false
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    Map<Integer, Integer> m = n ? new ConcurrentHashMap<>() : new HashMap<>();
    try {
      m.put(null, 1);
      assert !n;
    } catch (NullPointerException e) {
    }
  }
}

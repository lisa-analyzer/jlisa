import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// a HashMap accepts the null key: the assertion is reached
public class Main {
  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    Map<Integer, Integer> m = n ? new ConcurrentHashMap<>() : new HashMap<>();
    m.put(null, 1);
    assert false;
  }
}

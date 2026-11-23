import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// the second next of a one-element set raises NoSuchElementException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    Set<Integer> s = new HashSet<>();
    s.add(1);
    Iterator<Integer> it = s.iterator();
    it.next();
    it.next();
  }
}

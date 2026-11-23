import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// the cast fails only where o is not a String: the assertion holds
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    Object o = n ? (Object) "s" : (Object) Integer.valueOf(1);
    try {
      String s = (String) o;
    } catch (ClassCastException e) {
      assert !n;
    }
  }
}

import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// parsing "abc" raises NumberFormatException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    int x = Integer.parseInt("abc");
  }
}

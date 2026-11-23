import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// a null Boolean used as a condition raises NullPointerException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    Boolean b = n ? null : Boolean.valueOf(true);
    if (b) { int y = 1; }
  }
}

import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// comparing a null Integer raises NullPointerException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    Integer i = n ? null : Integer.valueOf(1);
    if (i > 0) { int y = 1; }
  }
}

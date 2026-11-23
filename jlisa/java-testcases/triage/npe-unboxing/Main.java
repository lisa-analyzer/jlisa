import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// unboxing null (assigning it to an int) raises NullPointerException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    Integer i = n ? null : Integer.valueOf(1);
    int x = i;
  }
}

import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// inserting beyond the end raises IndexOutOfBoundsException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    StringBuilder sb = new StringBuilder("ab");
    sb.insert(10, "x");
  }
}

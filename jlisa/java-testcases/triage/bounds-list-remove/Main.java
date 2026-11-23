import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// removing from an empty list raises IndexOutOfBoundsException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    List<Integer> l = new ArrayList<>();
    l.remove(0);
  }
}

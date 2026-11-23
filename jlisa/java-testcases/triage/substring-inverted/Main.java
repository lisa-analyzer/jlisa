import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// substring(2, 1) raises IndexOutOfBoundsException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    String s = "abc";
    String t = s.substring(2, 1);
  }
}

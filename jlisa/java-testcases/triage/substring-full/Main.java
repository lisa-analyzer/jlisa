import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// substring(0, length) and substring(length) are within the bounds
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    String s = "abc";
    String t = s.substring(0, s.length());
    String u = s.substring(s.length());
  }
}

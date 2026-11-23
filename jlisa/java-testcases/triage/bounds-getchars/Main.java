import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// copying beyond the end of the string raises IndexOutOfBoundsException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    char[] dst = new char[10];
    "abc".getChars(0, 5, dst, 0);
  }
}

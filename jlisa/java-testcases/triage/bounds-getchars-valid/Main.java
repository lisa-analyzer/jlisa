import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

// the copied characters are within the bounds
public class Main {
  public static void main(String[] args) {
    char[] dst = new char[10];
    "abc".getChars(0, 3, dst, 7);
  }
}

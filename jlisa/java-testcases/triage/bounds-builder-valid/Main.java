import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

// the offsets are within the bounds (delete cuts the end)
public class Main {
  public static void main(String[] args) {
    StringBuilder sb = new StringBuilder("ab");
    sb.insert(2, "x");
    sb.deleteCharAt(0);
    sb.delete(0, 10);
  }
}

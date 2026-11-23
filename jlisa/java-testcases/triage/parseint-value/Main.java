import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;

// parsing "12" yields 12
public class Main {
  public static void main(String[] args) {
    int x = Integer.parseInt("12");
    assert x == 12;
  }
}

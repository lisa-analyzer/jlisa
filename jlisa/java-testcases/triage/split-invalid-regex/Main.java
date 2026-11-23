import org.sosy_lab.sv_benchmarks.Verifier;
import java.util.*;
import java.util.concurrent.*;

// an invalid regular expression raises PatternSyntaxException
public class Main {

  public static void main(String[] args) {
    boolean n = Verifier.nondetBoolean();
    String[] p = "a".split("(");
  }
}

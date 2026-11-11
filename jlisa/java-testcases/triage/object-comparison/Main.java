import java.util.ArrayList;
import java.util.List;
import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    List<Integer> values = new ArrayList<>();
    values.add(Verifier.nondetInt());
    // the element, an Object for the model of the list, is unboxed
    if (values.get(0) == 7) {
      assert false;
    }
  }
}

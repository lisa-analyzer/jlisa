import java.lang.reflect.Field;
import org.sosy_lab.sv_benchmarks.Verifier;

class Box {
  int value;
}

public class Main {
  static void reset(Box box, int n) throws Exception {
    if (n <= 0) return;
    Field f = Box.class.getDeclaredField("value");
    f.setInt(box, n);
    reset(box, n - 1);
  }

  public static void main(String[] args) throws Exception {
    Box box = new Box();
    int v = Verifier.nondetInt();
    int n = Verifier.nondetInt();
    box.value = v;
    if (v == 0 && n > 0 && n < 4) {
      reset(box, n);
      assert box.value == v;
    }
  }
}

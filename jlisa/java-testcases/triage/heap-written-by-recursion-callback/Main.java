import org.sosy_lab.sv_benchmarks.Verifier;

class Item {
  int count;

  @Override
  public String toString() {
    count = count + 1;
    return "item";
  }
}

public class Main {
  static int describe(Item item, int n) {
    if (n <= 0) return 0;
    String s = "" + item;
    return s.length() + describe(item, n - 1);
  }

  public static void main(String[] args) {
    Item item = new Item();
    int v = Verifier.nondetInt();
    int n = Verifier.nondetInt();
    item.count = v;
    if (n > 0 && n < 4) {
      describe(item, n);
      assert item.count == v;
    }
  }
}

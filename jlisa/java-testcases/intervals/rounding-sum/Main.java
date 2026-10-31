public class Main {
  public static void main(String[] args) {
    double x = 0.1 + 0.2; // 0.30000000000000004 in Java
    if (x == 0.30000000000000004) {
      assert false; // A: reached
    }
    double a = args.length > 0 ? 0.1 : 0.1000000000000001;
    double y = a + 0.2; // cp does not know it
    if (y > 0.3) {
      assert false; // B: reached (both values)
    }
  }
}

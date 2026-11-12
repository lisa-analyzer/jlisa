import org.sosy_lab.sv_benchmarks.Verifier;

class Tank {
  boolean pumping;
  int level;

  boolean isHigh() {
    return level > 5;
  }

  void step() {
    if (!pumping && isHigh()) pumping = true;
    else if (pumping && !isHigh()) pumping = false;
  }
}

public class Main {
  public static void main(String[] args) {
    Tank t = new Tank();
    for (int i = 0; i < 3; i++) {
      t.level = Verifier.nondetInt();
      t.step();
      assert !t.pumping || t.level > 5;
    }
  }
}

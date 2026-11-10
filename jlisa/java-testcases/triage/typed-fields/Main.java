import org.sosy_lab.sv_benchmarks.Verifier;

class Sensor {
  int count;
  double level;
  boolean active;
}

public class Main {
  public static void main(String[] args) {
    Sensor s = new Sensor();
    s.count = Verifier.nondetInt();
    s.level = Verifier.nondetDouble();
    s.active = Verifier.nondetBoolean();
    if (s.active && s.count > 5 && s.level > 0.25 && s.level < 0.5) {
      assert false;
    }
  }
}

import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    Character c = Character.valueOf(Verifier.nondetChar());
    // a Character with the same value
    if (c.equals(Character.valueOf('A'))) {
      assert false;
    }
  }
}

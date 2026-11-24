import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  public static void main(String[] args) {
    Character c = Character.valueOf(Verifier.nondetChar());
    // the argument is an Integer: a Character never equals it
    if (c.equals(Integer.valueOf(65))) {
      assert false;
    }
  }
}

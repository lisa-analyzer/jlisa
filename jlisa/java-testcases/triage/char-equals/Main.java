import org.sosy_lab.sv_benchmarks.Verifier;

// a char passed as an Object is boxed into a Character
public class Main {
  public static void main(String[] args) {
    Character c = 'A';
    assert c.equals('A');
    assert !c.equals('B');
  }
}

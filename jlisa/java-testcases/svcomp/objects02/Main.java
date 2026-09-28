import org.sosy_lab.sv_benchmarks.Verifier;
import svcomp.objects.*;

public class Main {

  public static void main(String[] args) {
    Object o = Verifier.nondetObject(Any.class, new Factories.AnyFactory());
    // class-cast exception for types A and D
    B b = (B) o;
  }
}

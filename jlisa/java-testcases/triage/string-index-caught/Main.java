import org.sosy_lab.sv_benchmarks.Verifier;

// string bounds raise StringIndexOutOfBoundsException (also caught as an
// IndexOutOfBoundsException), and getChars checks the bounds of the receiver
// before reading the destination
public class Main {
  public static void main(String[] args) {
    String s = "ab";
    try {
      s.substring(5);
    } catch (StringIndexOutOfBoundsException e) {
    }
    try {
      s.charAt(3);
    } catch (IndexOutOfBoundsException e) {
    }
    try {
      s.getChars(1, 5, null, 0);
    } catch (StringIndexOutOfBoundsException e) {
    }
    StringBuilder b = new StringBuilder("ab");
    try {
      b.deleteCharAt(4);
    } catch (StringIndexOutOfBoundsException e) {
    }
  }
}

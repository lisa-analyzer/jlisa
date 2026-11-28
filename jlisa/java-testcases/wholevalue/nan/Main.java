public class Main {
  public static void main(String[] args) {
    double n = args.length > 5 ? 7.0 : Double.NaN;
    if (!(n <= 5)) {
      // n is 7.0 or NaN here
      if (n > 0) {
        // only 7.0
      } else {
        assert false; // reachable with NaN
      }
    }
  }
}

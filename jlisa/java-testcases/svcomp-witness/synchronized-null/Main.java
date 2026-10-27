public class Main {
  static void lock(Object o) {
    synchronized (o) {
      o.hashCode();
    }
  }

  public static void main(String[] args) {
    lock(null);
  }
}

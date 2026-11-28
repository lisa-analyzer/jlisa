import org.sosy_lab.sv_benchmarks.Verifier;

public class Main {
  static class Node {
    Node next;
  }

  public static void main(String[] args) {
    Node head = new Node();
    if (Verifier.nondetBoolean()) head.next = new Node();
    Node e = head;
    int n = 0;
    while (e != null) {
      e = e.next;
      n++;
    }
    assert n == 1; // fails when the list has two nodes: the exit of the loop is reachable
  }
}

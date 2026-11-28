import org.sosy_lab.sv_benchmarks.Verifier;

class Main {
  public static void main(String[] args) {
    int k = Verifier.nondetInt();
    if (k == 1) successor();
    else if (k == 2) counter();
    else if (k == 3) shiftedWrong();
    else if (k == 4) decrement();
    else if (k == 5) joinWrong();
    else if (k == 6) joinRight();
    else if (k == 7) insertionSort();
    else if (k == 8) copyNext();
  }
  static void successor() { int i = Verifier.nondetInt(); int j = i + 1; assert j > i; } // not proved: i + 1 may overflow
  static void counter() { int i = 0; while (Verifier.nondetBoolean()) i = i + 1; assert i >= 0; } // not proved: i + 1 may overflow
  static void shiftedWrong() {
    int i = Verifier.nondetInt();
    Verifier.assume(i > 0 && i < 1000);
    int j = i + 1;
    i = i + 5;
    assert j > i; // fails
  }
  static void decrement() {
    int x = Verifier.nondetInt();
    int y = Verifier.nondetInt();
    Verifier.assume(y > 0 && y < 1000 && x > 0 && x < 1000);
    Verifier.assume(x <= y + 1);
    y = y - 1;
    assert x <= y + 2; // holds
  }
  static void joinWrong() {
    int x = Verifier.nondetInt();
    int y = Verifier.nondetInt();
    Verifier.assume(y > 0 && y < 1000 && x > 0 && x < 1000);
    if (Verifier.nondetBoolean()) x = y + 1; else x = y + 3;
    assert x <= y + 2; // fails
  }
  static void joinRight() {
    int x = Verifier.nondetInt();
    int y = Verifier.nondetInt();
    Verifier.assume(y > 0 && y < 1000 && x > 0 && x < 1000);
    if (Verifier.nondetBoolean()) x = y + 1; else x = y + 3;
    assert x <= y + 3; // holds
  }
  static void insertionSort() { // no out of bounds accesses
    int n = Verifier.nondetInt();
    Verifier.assume(n > 0 && n < 100);
    int[] a = new int[n];
    for (int i = 0; i < n; i++) a[i] = Verifier.nondetInt();
    for (int i = 1; i < n; i++) {
      int key = a[i];
      int j = i - 1;
      while (j >= 0 && a[j] > key) { a[j + 1] = a[j]; j = j - 1; }
      a[j + 1] = key;
    }
  }
  static void copyNext() { // no out of bounds accesses
    int n = Verifier.nondetInt();
    Verifier.assume(n > 0);
    int[] a = new int[n];
    for (int i = 0; i + 1 < n; i++) a[i + 1] = a[i];
  }
}

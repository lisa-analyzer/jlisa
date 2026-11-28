class Main {
  public static void main(String[] args) {
    char c = 65535;
    char c1 = 2;
    c += c1;
    assert c == 1;
  }
}

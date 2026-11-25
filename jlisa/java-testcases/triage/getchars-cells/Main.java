// getChars writes the chars of the string in the cells of the destination
public class Main {
  public static void main(String[] args) {
    char[] dst = new char[3];
    "abc".getChars(0, 3, dst, 0);
    assert dst[1] == 'b';
  }
}

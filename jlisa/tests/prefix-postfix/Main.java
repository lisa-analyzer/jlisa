public class Main {
    public static void main(String[] args) {
        int a = 0;
        ++a;
        int b = ++a;
        int y = ++a - --b;
        y = -y;
        boolean f = true;
        f = !f;
        a = +a;
        a = ~a;
        a = 10;
        b = a--;
        int x = y;
        x = a++;
    }
}
public class Main {

    private final int value = 20;

    public static void main(String[] args) {
        Outer outer = new Outer();
        outer.foo();
    }
}


class Outer {

    void foo() {

        String s1 = "hello1";
        String s2 = "hello2";

        StringWrapper1 sw1 = new StringWrapper1(s1);
        assert(sw1.getStr().equals("hello1"));

        StringWrapper2 sw2 = new StringWrapper2(s2, 42);
        assert(sw2.getStr().equals("hello2"));
        assert(sw2.getInt() == 42);
    }

    class StringWrapper1 {
        String innerValue;
        StringWrapper1(String s) {
            innerValue = s;
        }

        String getStr() { return innerValue; }
    }

    class StringWrapper2 {
        String innerValue;
        int x;

        StringWrapper2(String s, int y) {
            innerValue = s;
            x = y;
        }

        String getStr() { return innerValue; }
        int getInt() { return x; }
    }

}

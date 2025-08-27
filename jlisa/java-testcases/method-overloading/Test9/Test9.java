public class Test9 {
    public static String foo() {
        return new String("foo");
    }

    public static String foo(String arg) {
        return new String("foo");
    }

    public static void main(String[] args) {
        // OK
    }
}
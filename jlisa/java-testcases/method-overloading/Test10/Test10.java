public class Test10 {
    public static String foo() {
        return new String("foo");
    }

    public static String foo(String arg) {
        return new String("foo");
    }
    public Integer foo() {
        return Integer.valueof(1);
    }
    public static void main(String[] args) {
        // error: method foo() is already defined in class Test10
    }
}
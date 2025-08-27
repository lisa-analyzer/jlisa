public class Test8 {
    public static String foo() {
        return new String("foo");
    }

    public static Integer foo() {
        return Integer.valueOf(1);
    }

    public static void main(String[] args) {
        // error: method foo() is already defined in class Test8
    }
}
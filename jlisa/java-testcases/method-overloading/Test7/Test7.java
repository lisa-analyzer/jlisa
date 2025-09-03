public class Test7 {
    public static void foo(String... args) {
        System.out.println("Varargs");
    }

    public static void foo(String[] args) {
        System.out.println("Array");
    }

    public static void main(String[] args) {
        // error: cannot declare both foo(String[]) and foo(String...) in Test7
    }
}
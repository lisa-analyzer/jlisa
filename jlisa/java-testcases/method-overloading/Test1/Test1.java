public class Test1 {
    public static void foo(long longNumber) {
        System.out.println("long: " + longNumber);
    }

    public static void foo(double doubleNumber) {
        System.out.println("double: " + doubleNumber);
    }

    public static void main(String[] args) {
        foo(123L);      // calls foo(long)
        foo(45.67);     // calls foo(double)
    }
}
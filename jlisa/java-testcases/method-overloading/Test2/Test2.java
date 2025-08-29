public class Test2 {
    public static void foo(String s) {
        System.out.println("String: " + s);
    }

    public static void foo(Object o) {
        System.out.println("Object: " + o);
    }

    public static void main(String[] args) {
        foo("hello");      // calls foo(String)
        foo(new Object()); // calls foo(Object)
    }
}
public class Test6 {
    public static void foo(int i) {
        System.out.println("int: " + i);
    }

    public static void foo(Integer i) {
        System.out.println("Integer: " + i);
    }

    public static void main(String[] args) {
        foo(5);          // calls foo(int)
        foo(Integer.valueOf(5)); // calls foo(Integer)
    }
}
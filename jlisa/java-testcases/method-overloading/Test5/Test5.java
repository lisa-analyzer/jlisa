import java.util.List;

public class Test5 {
    public static void foo(List<Integer> list) {
        System.out.println("List<Integer>");
    }

    public static void foo(List<Integer> anotherList) {
        System.out.println("Duplicate method");
    }

    public static void main(String[] args) {
        // error: method foo(List<Integer>) is already defined in class Test5
    }
}
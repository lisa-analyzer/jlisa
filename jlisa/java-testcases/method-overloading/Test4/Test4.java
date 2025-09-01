import java.util.List;

public class Test4 {
    public static void foo(List<Integer> list) {
        System.out.println("List<Integer>");
    }

    public static void foo(List<Character> list) {
        System.out.println("List<Character>");
    }

    public static void main(String[] args) {
        // error: name clash: foo(List<Character>) and foo(List<Integer>) have the same erasure
    }
}
import java.util.List;
import java.util.ArrayList;

public class Test3 {
    public static void foo(List<String> list) {
        System.out.println("List");
    }

    public static void foo(ArrayList<String> arrayList) {
        System.out.println("ArrayList");
    }

    public static void main(String[] args) {
        ArrayList<String> arr = new ArrayList<>();
        foo(arr);   // valid
    }
}
abstract class MyAbstractClass {
    abstract void abstractMethod();
}

public class MainClass extends MyAbstractClass {

    @Override
    void abstractMethod() {
        System.out.println("Abstract method implemented!");
    }

    public static void main(String[] args) {
        MainClass obj = new MainClass();
        obj.abstractMethod();
    }
}


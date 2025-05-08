public class Main {
    public static void main(String[] args) {
        A a = new A();
        int c = a.getX() + 100;
        a.setX(200);
        /* note: the commented lines are not working
            a.setX(a.getX() + 100);
            a.setX(c);
         */
    }
}
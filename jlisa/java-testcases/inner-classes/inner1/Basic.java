public class Basic {

    private final int value = 10;

    class Inner {
        void display1() {
            int x = value;
            System.out.println("Value from outer class: " + x);
        }
        void display2() {
            int k = 1;
            int y = k + 1;
            int x = Basic.this.value + 1;
            System.out.println("Value from outer class: " + x);
        }
    }

    public static void main(String[] args) {
        Basic outer = new Basic();
        Basic.Inner inner = outer.new Inner();
        inner.display1();
        inner.display2();
    }
}

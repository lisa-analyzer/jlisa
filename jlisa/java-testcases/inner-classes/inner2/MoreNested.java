public class MoreNested {

    private final int value = 20;

    class A {
        void foo() {
            System.out.println(value);
            System.out.println(MoreNested.this.value);
        }

        class B {
            void foo() {
                System.out.println(value);
                System.out.println(MoreNested.this.value);
            }

            class C {
                void foo() {
                    System.out.println(value);
                    System.out.println(MoreNested.this.value);
                }
            }
        }
    }

    public static void main(String[] args) {
        MoreNested outer = new MoreNested();
        MoreNested.A inner1 = outer.new A();
        inner1.foo();
        MoreNested.A.B inner2 = inner1.new B();
        inner2.foo();
        MoreNested.A.B.C inner3 = inner2.new C();
        inner3.foo();
    }

}

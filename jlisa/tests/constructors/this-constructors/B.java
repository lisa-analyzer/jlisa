public class B {
    int i;
    int x;
    C c;
    public B() {
        C c1 = new C();
        this.c = c1;
        this.i = 10;
    }

    public B(int x) {
        this();
        C c1 = new C();
        this.c = c1;
        this.x = x;
    }
}
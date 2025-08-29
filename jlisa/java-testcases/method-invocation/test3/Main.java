class Main {
    public static void main(String[] args) {
        C c;
        if (args.length == 0) {
            c = new A();
        } else {
            c = new B();
        }
        c.foo();
    }
}
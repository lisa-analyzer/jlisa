public class Main {
    public static void main(String[] args) throws Exception {
        int value = SimpleStatic.foo;
	assert (value == 42);
	assert (SimpleStatic.x.equals("hello"));

	SimpleStatic2 tmp = new SimpleStatic2();
    }
}

class SimpleStatic {
    static int foo;
    static String x = "ciao";

    static {
        foo = 42;
    }

    static {
	x = "hello";
    }
}

class SimpleStatic2 {
	static { }
}

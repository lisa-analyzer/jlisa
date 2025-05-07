package it.unive.jlisa.java;

import org.junit.jupiter.api.Test;

public class JavaTest {
    static int x = 3;
    @Test
    void javaTest()  {
        System.out.println(x);
        JavaTest y = new JavaTest();
        System.out.println(y.x);
        y.x = 100;
        System.out.println(y.x);
        System.out.println(this.x);
        int x = 071 + 0xFFf;
        //int y = 00100;
        double f = 7d;
        long h = 001;
        System.out.println(x);
        System.out.println(y);
        boolean t = true;
    }
    @Test
    void javaTest2()  {
        System.out.println(TestClass.x);
        TestClass t = new TestClass();
        TestClass TestClass = new TestClass();
        System.out.println(TestClass.x);
        System.out.println(t.x);
        t.setX(10011);
        System.out.println(TestClass.x);
        System.out.println(t.x);
        TestClass.setXs(999);
        System.out.println(TestClass.x);
        System.out.println(t.x);
        t.setXs(9991);
        System.out.println(TestClass.x);
        System.out.println(t.x);
        TestClass t2 = new TestClass();
        System.out.println(TestClass.x);
        System.out.println(t.x);
    }
}

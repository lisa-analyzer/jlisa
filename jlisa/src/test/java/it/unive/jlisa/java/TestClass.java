package it.unive.jlisa.java;

public class TestClass {
   public static int x;
    public TestClass() {
        x = 100;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public static void setXs(int y) {
        x = y;
    }
}

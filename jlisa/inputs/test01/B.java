class B {
    int x = 0;
    public B() {
        this.x = 10;
    }

    public B(int x) {
        this.x = 0;
        this.x = this.x + x;
    }

    public int increment10(int g) {
        return this.x + g;
    }
}
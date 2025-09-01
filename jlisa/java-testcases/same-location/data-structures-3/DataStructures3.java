public class Datastructures3  {
    public class C {
        private String str;
        private C next;

        public String getData() {
            return this.str;
        }

        public void setData(String str) {
            this.str = str;
        }

        public void setNext(C next) {
            this.next = next;
        }
    }

    public static void main(String[] args) {
        String name = "";
        C c1 = new C();
        c1.setData("anbc");

        C c2 = new C();
        c2.setData(name);
        c1.setNext(c2);

        String str = c1.next.str;
    }
}
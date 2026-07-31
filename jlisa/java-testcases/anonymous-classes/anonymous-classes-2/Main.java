public class Main {

    public static void main(String[] args) throws Exception {
        Outer obj = new Outer();

        assert (obj.x == 10);

        obj.runTest();

        assert (obj.x == 50);
    }
}

class BaseTask {
    public void execute() {
    }
}

class Outer {
    int x = 10;

    public void runTest() {
        BaseTask task = new BaseTask() {
            int y = 5;

            @Override
            public void execute() {
                x = x + y + 35;
            }
        };

        task.execute();
    }
}

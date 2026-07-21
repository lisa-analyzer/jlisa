public class Main {

    public static void main(String[] args) throws Exception {
        Context ctx = new Context();

        assert (ctx.score == 0);
        assert (ctx.finished == false);

        ctx.execute();

        assert (ctx.score == 200);
        assert (ctx.finished == true);
    }
}

class Worker {
    public int baseScore() {
        return 0;
    }

    public int baseScore1() {
        return 100;
    }

}

class Context {
    int score = 0;
    boolean finished = false;

    public void execute() {

        Worker w = new Worker() {

            @Override
            public int baseScore() {
                int b = baseScore2();
                finished = true;

                return b * 2;
            }

            int baseScore2() {
                return baseScore1();
            }
        };

        score = w.baseScore();
    }
}

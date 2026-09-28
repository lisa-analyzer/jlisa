public class Main {
    public static void main(String[] args) throws Exception {
        int x = DerivedConfig.TOTAL;
        int y = DerivedConfig.BASE_VAL;

	assert (x == 75);
	assert (y == 15);

	int z = DerivedConfig.InnerConfig.INNER_VALUE;
	assert (z == 100);
    }
}

class BaseConfig {
    static int BASE_VAL;

    static {
        BASE_VAL = 10;
        BASE_VAL = BASE_VAL + 5;
    }
}

class DerivedConfig extends BaseConfig {
    static int MULTIPLIER = 2;
    static int OFFSET;

    static {
        OFFSET = BASE_VAL * MULTIPLIER;
    }

    static int TOTAL = OFFSET + 5;

    static {
        TOTAL = TOTAL * 2 + 5;
    }

    static class InnerConfig {
	static int INNER_VALUE;

	static {
	    int tmp = TOTAL + 25;
	    INNER_VALUE = tmp;
	}
    }
}


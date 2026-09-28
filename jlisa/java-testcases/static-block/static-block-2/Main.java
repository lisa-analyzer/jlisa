public class Main {
    public static void main(String[] args) throws Exception {
        int finalStatus = LoggerConfig.STATUS_CODE;
        assert (finalStatus == 200);
    }
}

class LoggerConfig {
    static int STATUS_CODE;
    static int BUFFER_SIZE = 1024;

    private static int validateAndScaleBuffer(int size, int factor) {
        int calculated = size * factor;

        assert (calculated == 4096);

        return calculated;
    }

    static {
        int scaleFactor = 4;
        int totalBuffer = validateAndScaleBuffer(BUFFER_SIZE, scaleFactor);

        if (totalBuffer == 4096) {
            STATUS_CODE = 200;
        } else {
            STATUS_CODE = 500;
        }
    }
}

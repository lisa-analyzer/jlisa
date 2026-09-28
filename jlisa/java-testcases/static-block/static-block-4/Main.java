public class Main {
    public static void main(String[] args) throws Exception {
        ServerConfig config = AppManager.DEFAULT_SERVER;

        String host = config.host;
        int port = config.port;
        boolean active = config.active;

	assert (host.equals("localhost"));
        assert (port == 8080);
        assert (active == true);

    }
}

class ServerConfig {
    public String host;
    public int port;
    public boolean active;

    public ServerConfig(String host, int port) {
        this.host = host;
        this.port = port;
        this.active = false;
    }

    public void activate() {
        this.active = true;
    }
}

class AppManager {
    static ServerConfig BACKUP_SERVER = new ServerConfig("127.0.0.1", 9090);

    static ServerConfig DEFAULT_SERVER;

    static {
        ServerConfig server = new ServerConfig("localhost", 8080);

        server.activate();

        DEFAULT_SERVER = server;
    }
}

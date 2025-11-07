package com.example.micro;

public class Runner {
    public static void main(String[] args) {
        System.out.println("Starting User Microservice (mock)...");
        System.out.println("Available endpoint: GET /user/info?name=Alice&age=25");

        UserController controller = new UserController();
        SimpleHttpClient client = new SimpleHttpClient(controller);

        HttpUtils utils = new HttpUtils();
        String url = utils.buildUrlWithQuery("/user/info", "Alice", 25);

        String response = client.sendGet(url);
        System.out.println("Simulated HTTP response:");
        System.out.println(response);

// direct path
        String direct = controller.getInfo("Alice", 25);
        System.out.println(direct);


    }

}

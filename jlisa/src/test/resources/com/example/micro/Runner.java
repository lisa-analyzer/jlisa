package com.example.micro;

// Simulated microservice runner (mock Spring Boot)
public class Runner {
    public static void main(String[] args) {
        System.out.println("Starting User Microservice (mock)...");
        System.out.println("Available endpoint: GET /user/info?name=Alice&age=25");

        UserController controller = new UserController();
        String response = controller.getInfo("Alice", 25);

        System.out.println("Simulated HTTP response:");
        System.out.println(response);
    }
}

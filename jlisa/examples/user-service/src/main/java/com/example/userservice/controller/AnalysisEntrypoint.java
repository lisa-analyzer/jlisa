package com.example.userservice.controller;

public class AnalysisEntrypoint {

    public static void main(String[] args) {
        UserController c = new UserController();

        // Call the endpoints methods so LiSA surely visits them
        c.info("Delara", 25);
        c.create("hello");
        c.update(7L, "newbody");
        c.delete(7L);
    }
}

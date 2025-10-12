package com.example.demo;

public class DemoApplication {
    public static void main(String[] args) {
        // no Spring here – just a harmless entry point for LiSA
        String greeting = hello("World");
        System.out.println(greeting);
    }

    public static String hello(String name) {
        return "Hello " + name + "!";
    }
}

package com.example.demo;

public class DemoApplication {

	// Fake mapping annotation for analysis
	@GetMapping("/hello")
	public static String hello(
			String name) {
		return "Hello " + name;
	}

	public static void main(
			String[] args) {
		System.out.println(hello("World"));
	}
}

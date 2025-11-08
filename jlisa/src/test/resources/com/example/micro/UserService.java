package com.example.micro;

public class UserService {
	public String getUserInfo(
			User user) {
		return "User: " + user.name + ", Age: " + user.age;
	}
}

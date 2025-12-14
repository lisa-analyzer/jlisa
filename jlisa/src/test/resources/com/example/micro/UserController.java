package com.example.micro;

@RestController
public class UserController {

	private UserService service = new UserService();

	@GetMapping("/user/info")
	public String getInfo(
			@RequestParam("name") String name,
			@RequestParam("age") int age) {
		User user = new User(name, age);
		return service.getUserInfo(user);
	}
	//  POST
	@PostMapping("/user/create")
	public String createUser(String name, int age) {
		User user = new User(name, age);
		return getUserInfo(this.service, user);
	}

	// PUT
	@PutMapping("/user/update")
	public String updateUser(String name, int age) {
		User user = new User(name, age);
		return getUserInfo(this.service, user);
	}

	//  DELETE
	@DeleteMapping("/user/delete")
	public String deleteUser(String name, int age) {
		User user = new User(name, age);
		return getUserInfo(this.service, user);
	}
}

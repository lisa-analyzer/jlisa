package com.example.micro;

@RestController
public class UserController {

    private UserService service = new UserService();

    @GetMapping("/user/info")
    public String getInfo(@RequestParam("name") String name, @RequestParam("age") int age) {
        User user = new User(name, age);
        return service.getUserInfo(user);
    }
}

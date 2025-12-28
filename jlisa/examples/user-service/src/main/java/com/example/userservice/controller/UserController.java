package com.example.userservice.controller;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/info")
    public String info(@RequestParam String name, @RequestParam int age) {
        return "name=" + name + ", age=" + age;
    }

    @PostMapping
    public String create(@RequestBody String body) {
        return "created: " + body;
    }

    @PutMapping("/{id}")
    public String update(@PathVariable long id, @RequestBody String body) {
        return "updated " + id + ": " + body;
    }

    @DeleteMapping("/{id}")
    public String delete(@PathVariable long id) {
        return "deleted " + id;
    }
}

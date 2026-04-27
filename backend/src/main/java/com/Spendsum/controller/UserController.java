package com.Spendsum.controller;

import com.Spendsum.model.User;
import com.Spendsum.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;

    @PostMapping
    public ResponseEntity<User> createUser(@RequestBody User user) {
        if (user.getCreatedAt() == null) {
            user.setCreatedAt(LocalDateTime.now());
        }
        return ResponseEntity.ok(userRepository.save(user));
    }

    @GetMapping
    public ResponseEntity<List<User>> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }
}

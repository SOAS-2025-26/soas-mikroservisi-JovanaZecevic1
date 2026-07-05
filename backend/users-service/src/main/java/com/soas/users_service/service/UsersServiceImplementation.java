package com.soas.users_service.service;

import com.soas.users_service.model.User;
import com.soas.users_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import serviceLibrary.dto.usersService.UserDto;
import serviceLibrary.services.usersService.UsersService;

import java.util.List;
import java.util.Optional;

@RestController
public class UsersServiceImplementation implements UsersService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public ResponseEntity<?> getAllUsers() {
        List<UserDto> users = userRepository.findAll().stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(users);
    }

    @Override
    public ResponseEntity<?> getUserByEmail(String email) {
        Optional<User> user = userRepository.findByEmailIgnoreCase(email);
        if (user.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with email " + email + " does not exist");
        }
        return ResponseEntity.ok(toDto(user.get()));
    }

    @Override
    public ResponseEntity<?> createUser(String actorRole, UserDto body) {
        User.Role actor = parseRole(actorRole);
        if (actor == null || actor == User.Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to create users");
        }

        User.Role newRole = parseRole(body.getRole());
        if (newRole == null) {
            return ResponseEntity.badRequest().body("Unknown role: " + body.getRole());
        }

        if (userRepository.existsByEmail(body.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("A user with this email already exists");
        }

        if (newRole == User.Role.OWNER && userRepository.existsByRole(User.Role.OWNER)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("An OWNER already exists in the system");
        }

        if (actor == User.Role.ADMIN && newRole != User.Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ADMIN can only add users with role USER");
        }

        User user = new User();
        user.setEmail(body.getEmail());
        user.setPassword(body.getPassword());
        user.setRole(newRole);
        User saved = userRepository.save(user);

        return ResponseEntity.status(HttpStatus.CREATED).body(toDto(saved));
    }

    @Override
    public ResponseEntity<?> updateUser(String actorRole, UserDto body) {
        User.Role actor = parseRole(actorRole);
        if (actor == null || actor == User.Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to update users");
        }

        Optional<User> existingOpt = userRepository.findByEmail(body.getEmail());
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with email " + body.getEmail() + " does not exist");
        }
        User existing = existingOpt.get();

        if (actor == User.Role.ADMIN && existing.getRole() != User.Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ADMIN can only update users with role USER");
        }

        User.Role newRole = parseRole(body.getRole());
        if (newRole == null) {
            return ResponseEntity.badRequest().body("Unknown role: " + body.getRole());
        }

        if (actor == User.Role.ADMIN && newRole != User.Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ADMIN can only update users to role USER");
        }

        if (newRole == User.Role.OWNER && existing.getRole() != User.Role.OWNER && userRepository.existsByRole(User.Role.OWNER)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("An OWNER already exists in the system");
        }

        existing.setPassword(body.getPassword());
        existing.setRole(newRole);
        User saved = userRepository.save(existing);

        return ResponseEntity.ok(toDto(saved));
    }

    @Override
    public ResponseEntity<?> deleteUser(String actorRole, String email) {
        User.Role actor = parseRole(actorRole);
        if (actor == null || actor == User.Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("You are not authorized to delete users");
        }

        Optional<User> existingOpt = userRepository.findByEmail(email);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User with email " + email + " does not exist");
        }
        User existing = existingOpt.get();

        if (actor == User.Role.ADMIN && existing.getRole() != User.Role.USER) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("ADMIN can only delete users with role USER");
        }

        userRepository.delete(existing);
        return ResponseEntity.noContent().build();
    }

    private UserDto toDto(User user) {
        return new UserDto(user.getEmail(), user.getPassword(), user.getRole().name());
    }

    private User.Role parseRole(String role) {
        if (role == null) {
            return null;
        }
        try {
            return User.Role.valueOf(role.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}

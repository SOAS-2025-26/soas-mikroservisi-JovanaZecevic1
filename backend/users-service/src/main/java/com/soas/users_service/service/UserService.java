package com.soas.users_service.service;

import com.soas.users_service.model.User;
import com.soas.users_service.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Korisnik sa id " + id + " ne postoji"));
    }

    public User createUser(User newUser, User.Role creatorRole) {
        if (userRepository.existsByEmail(newUser.getEmail())) {
            throw new RuntimeException("Korisnik sa ovim email-om vec postoji");
        }

        if (newUser.getRole() == User.Role.OWNER) {
            if (userRepository.existsByRole(User.Role.OWNER)) {
                throw new RuntimeException("Vec postoji OWNER u sistemu");
            }
        }

        if (creatorRole == User.Role.ADMIN && newUser.getRole() != User.Role.USER) {
            throw new RuntimeException("ADMIN moze da dodaje samo korisnike sa ulogom USER");
        }

        return userRepository.save(newUser);
    }

    public User updateUser(Long id, User updatedUser, User.Role updaterRole) {
        User existing = getUserById(id);

        if (updaterRole == User.Role.ADMIN && existing.getRole() != User.Role.USER) {
            throw new RuntimeException("ADMIN moze da azurira samo korisnike sa ulogom USER");
        }

        existing.setEmail(updatedUser.getEmail());
        existing.setPassword(updatedUser.getPassword());
        return userRepository.save(existing);
    }

    public void deleteUser(Long id) {
        User existing = getUserById(id);
        userRepository.delete(existing);
    }
}
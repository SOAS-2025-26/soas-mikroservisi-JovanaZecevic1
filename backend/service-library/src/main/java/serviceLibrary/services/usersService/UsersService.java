package serviceLibrary.services.usersService;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.*;
import serviceLibrary.dto.usersService.UserDto;

@Service
public interface UsersService {

    @GetMapping("/users")
    ResponseEntity<?> getAllUsers();

    @GetMapping("/users/email")
    ResponseEntity<?> getUserByEmail(@RequestParam String email);

    @PostMapping("/users")
    ResponseEntity<?> createUser(@RequestHeader("X-Actor-Role") String actorRole, @RequestBody UserDto body);

    @PutMapping("/users")
    ResponseEntity<?> updateUser(@RequestHeader("X-Actor-Role") String actorRole, @RequestBody UserDto body);

    @DeleteMapping("/users")
    ResponseEntity<?> deleteUser(@RequestHeader("X-Actor-Role") String actorRole, @RequestParam String email);

}
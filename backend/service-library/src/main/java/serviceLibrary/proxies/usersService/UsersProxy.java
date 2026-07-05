package serviceLibrary.proxies.usersService;

import org.springframework.cloud.openfeign.FeignClient;
import serviceLibrary.services.usersService.UsersService;

@FeignClient(name = "users-service")
public interface UsersProxy extends UsersService {
}

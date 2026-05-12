package project.discord.backend.auth;

import java.time.Instant;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import project.discord.backend.auth.dto.AuthResponse;
import project.discord.backend.auth.dto.LoginRequest;
import project.discord.backend.auth.dto.RegisterRequest;
import project.discord.backend.auth.dto.UserResponse;
import project.discord.backend.user.domain.UserAccount;
import project.discord.backend.user.domain.UserRole;
import project.discord.backend.user.domain.UserStatus;
import project.discord.backend.user.repository.UserRepository;

@Service
public class AuthService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern USERNAME_PATTERN = Pattern.compile("^[a-zA-Z0-9_]{3,32}$");

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        String username = normalizeUsername(request.username());
        String password = requireText(request.password(), "Password is required");
        Integer age = request.age();

        validateEmail(email);
        validateUsername(username);
        validatePassword(password);
        validateAge(age);

        if (userRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email is already registered");
        }

        if (userRepository.existsByUsername(username)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username is already taken");
        }

        Instant now = Instant.now();
        UserAccount user = new UserAccount();
        user.setEmail(email);
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setAge(age);
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        UserAccount savedUser = userRepository.save(user);
        return new AuthResponse("Account created", UserResponse.from(savedUser));
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        String password = requireText(request.password(), "Password is required");

        validateEmail(email);

        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "This account is not active");
        }

        if (user.getPasswordHash() == null || !passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        Instant now = Instant.now();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        UserAccount savedUser = userRepository.save(user);

        return new AuthResponse("Login successful", UserResponse.from(savedUser));
    }

    private String normalizeEmail(String email) {
        return requireText(email, "Email is required").toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return requireText(username, "Username is required").trim();
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches() || email.length() > 320) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is invalid");
        }
    }

    private void validateUsername(String username) {
        if (!USERNAME_PATTERN.matcher(username).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Username must be 3-32 characters and use only letters, numbers, or underscores"
            );
        }
    }

    private void validatePassword(String password) {
        if (password.length() < 8 || password.length() > 128) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Password must be between 8 and 128 characters"
            );
        }
    }

    private void validateAge(Integer age) {
        if (age == null || age < 13 || age > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Age must be between 13 and 120");
        }
    }
}

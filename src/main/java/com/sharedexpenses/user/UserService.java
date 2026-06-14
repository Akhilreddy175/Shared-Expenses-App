package com.sharedexpenses.user;

import com.sharedexpenses.AppException;
import com.sharedexpenses.security.JwtTokenProvider;
import com.sharedexpenses.security.UserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtProvider;
    private final AuthenticationManager authManager;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtProvider, AuthenticationManager authManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtProvider = jwtProvider;
        this.authManager = authManager;
    }

    @Transactional
    public Map<String, Object> register(String email, String password, String displayName) {
        String cleanEmail = email.toLowerCase().trim();
        if (userRepository.existsByEmail(cleanEmail)) {
            throw new AppException(HttpStatus.BAD_REQUEST, "Email already registered");
        }
        User user = new User(cleanEmail, passwordEncoder.encode(password), displayName.trim());
        user = userRepository.save(user);
        String token = jwtProvider.generateToken(user.getId(), user.getEmail());
        return Map.of(
                "token", token,
                "userId", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName()
        );
    }

    public Map<String, Object> login(String email, String password) {
        Authentication auth = authManager.authenticate(
                new UsernamePasswordAuthenticationToken(email.toLowerCase().trim(), password));
        UserPrincipal principal = (UserPrincipal) auth.getPrincipal();
        String token = jwtProvider.generateToken(principal.getId(), principal.getEmail());
        return Map.of(
                "token", token,
                "userId", principal.getId(),
                "email", principal.getEmail(),
                "displayName", principal.getUser().getDisplayName()
        );
    }

    public Map<String, Object> getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
        return Map.of(
                "userId", user.getId(),
                "email", user.getEmail(),
                "displayName", user.getDisplayName()
        );
    }
}

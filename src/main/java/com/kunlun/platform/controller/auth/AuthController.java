package com.kunlun.platform.controller.auth;

import com.kunlun.platform.entity.User;
import com.kunlun.platform.repository.UserRepository;
import com.kunlun.platform.util.JwtUtil;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> body) {
        String username = body.get("username");
        String password = body.get("password");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "用户名和密码不能为空"));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }

        User user = userOpt.get();

        // 兼容旧明文密码，同时支持 BCrypt 加密密码
        if (!passwordEncoder.matches(password, user.getPassword())
                && !user.getPassword().equals(password)) {
            return ResponseEntity.status(401).body(Map.of("error", "用户名或密码错误"));
        }

        // 如果旧密码是明文，自动升级为 BCrypt 加密
        if (!user.getPassword().startsWith("$2a$")) {
            user.setPassword(passwordEncoder.encode(password));
            userRepository.save(user);
        }

        String token = JwtUtil.generateToken(user.getUsername(), user.getRole());

        return ResponseEntity.ok(Map.of(
                "token", token,
                "role", user.getRole()
        ));
    }
}

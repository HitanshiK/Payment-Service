package com.paymentSystem.project.controllers;

import com.paymentSystem.project.dto.request.AddUserRequest;
import com.paymentSystem.project.dto.request.LoginRequest;
import com.paymentSystem.project.dto.response.AddUserResponse;
import com.paymentSystem.project.service.AuthService;
import com.paymentSystem.project.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private  final AuthService authService;
    private final UserService userService;

    public AuthController (AuthService authService,UserService userService){
        this.authService = authService;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request){
        System.out.println("login user api");
        String token = authService.login(request);

        return ResponseEntity.ok(Map.of("token",token,"tokenType","Bearer"));
    }

    @PostMapping("/addUser")
    public AddUserResponse addUser(@RequestBody AddUserRequest request){
        System.out.println("in add user api");
        return userService.addUser(request);
    }

    @PostMapping("/logout")
    public void logout(String token) {
        System.out.println("in logout user api");
        authService.logout(token);
        SecurityContextHolder.clearContext();
    }
}

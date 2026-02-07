package com.paymentSystem.project.service;

import com.paymentSystem.project.dto.request.AddUserRequest;
import com.paymentSystem.project.dto.response.AddUserResponse;
import com.paymentSystem.project.entity.User;
import com.paymentSystem.project.exceptions.GlobalExceptionHandler;
import com.paymentSystem.project.repos.UserRepository;
import com.paymentSystem.project.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

/** Future Scope
 security compliance setting user inactive
 profile api's
 update user
 ***/

@Service
public class UserService {

    private final GlobalExceptionHandler globalExceptionHandler;
    private final PinService pinService;
    private final JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    public UserService(GlobalExceptionHandler exceptionHandler,PinService pinService, JwtUtil jwtUtil){
        this.globalExceptionHandler = exceptionHandler;
        this.pinService = pinService;
        this.jwtUtil = jwtUtil;
    }

    public AddUserResponse addUser (AddUserRequest request){
        if(userRepository.existsByEmailOrMobile(request.getEmail(), request.getMobile())){
             globalExceptionHandler.handleUserRegisteredException();
        }else{
            User user = new User();
            user.setName(request.getName());
            user.setEmail(request.getEmail());
            user.setMobile(request.getMobile());
            user.setPassword(pinService.hashPassword(request.getLoginPassword()));
            user.setPin(pinService.hashPassword(request.getPin()));

            userRepository.save(user);

            String token = jwtUtil.generateToken(user.getId());

            AddUserResponse response = new AddUserResponse(user);
            response.setAccessToken(token);
            response.setTokenType("BEARER");

            return  response;
        }
        return new AddUserResponse();
    }
}

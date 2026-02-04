package com.paymentSystem.project.service;

import com.paymentSystem.project.dto.request.LoginRequest;
import com.paymentSystem.project.entity.User;
import com.paymentSystem.project.enums.Status;
import com.paymentSystem.project.exceptions.BusinessException;
import com.paymentSystem.project.exceptions.GlobalExceptionHandler;
import com.paymentSystem.project.exceptions.InvalidPinException;
import com.paymentSystem.project.repos.UserRepository;
import com.paymentSystem.project.utils.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    UserRepository userRepository;
    JwtUtil jwtUtil;
    PinService pinService;

    @Autowired
    GlobalExceptionHandler globalExceptionHandler;

    public AuthService(UserRepository userRepository, JwtUtil jwtUtil, PinService pinService){
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.pinService = pinService;
    }
    String login (LoginRequest request){

        User user = userRepository.findByEmailOrPhone(request.getPhoneOrEmail(), request.getPhoneOrEmail())
                .orElseThrow(()->new RuntimeException("User not found"));

        if(!pinService.match(request.getPin(), user.getPassword())){
            throw new InvalidPinException();
        }

        if(!user.getStatus().equals(Status.ACTIVE)){
         globalExceptionHandler.handleBusinessException(
                 new BusinessException("User is not Active","INACTIVE_USER",403) {
         });
        }

        return jwtUtil.generateToken(user.getId());
    }
}
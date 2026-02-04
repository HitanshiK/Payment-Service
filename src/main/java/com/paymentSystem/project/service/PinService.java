package com.paymentSystem.project.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PinService {

    private final PasswordEncoder passwordEncoder;

    public PinService(PasswordEncoder passwordEncoder){
        this.passwordEncoder = passwordEncoder;
    }
    public Boolean match(String rawPin , String hashPin){
        return passwordEncoder.matches(rawPin,hashPin);
    }
}


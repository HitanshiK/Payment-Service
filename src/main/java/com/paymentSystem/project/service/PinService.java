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

    public String hashPassword (String rawPin){
        return passwordEncoder.encode(rawPin);
    }

    public String hashPin (String rawPin){
        validatePin(rawPin);
        return passwordEncoder.encode(rawPin);
    }

    private void validatePin(String pin) {
        if (!pin.matches("\\d{4,6}")) {
            throw new IllegalArgumentException("PIN must be 4 to 6 digits");
        }
    }

    //reset pin logic with otp verification

}


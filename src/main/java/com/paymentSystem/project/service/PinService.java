package com.paymentSystem.project.service;

import com.paymentSystem.project.entity.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;

@Service
public class PinService {

    private final PasswordEncoder passwordEncoder;
    private static final int MAX_ATTEMPTS = 3;

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

    public boolean verifyPin (String pin, User user){
        if(match(pin, user.getPin())){
            user.setPinAttempts(0);
            return true;
        }else {
            if(user.getPinAttempts() >= MAX_ATTEMPTS){
                user.setPinLockedUntil(new Timestamp(System.currentTimeMillis() + (15 * 60 * 1000)));
                user.setPinAttempts(0);
            }else{
                int attempts = user.getPinAttempts();
                user.setPinAttempts(user.getPinAttempts() + 1);
            }
            return false;
        }
    }

    //reset pin logic with otp verification

}


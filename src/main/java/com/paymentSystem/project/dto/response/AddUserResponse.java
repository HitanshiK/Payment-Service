package com.paymentSystem.project.dto.response;

import com.paymentSystem.project.entity.User;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class AddUserResponse {

    private String accessToken;
    private String tokenType;
    private String name ;
    private String mobile ;
    private String email;
    private boolean pinSet ;

    public AddUserResponse(User user){
        this.name = user.getName();
        this.email = user.getEmail();
        this.mobile = user.getMobile();
        this.pinSet = user.getPin() != null;
    }
}

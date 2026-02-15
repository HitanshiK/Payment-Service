package com.paymentSystem.project.dto.response;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class CachedResponse {
    private int httpStatus;
    private String body;

    public CachedResponse(int httpStatus, String body){
        this.httpStatus = httpStatus;
        this.body = body;
    }
}

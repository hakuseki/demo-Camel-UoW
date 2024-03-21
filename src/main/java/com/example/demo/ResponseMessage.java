package com.example.demo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.Data;

@Data
@JsonSerialize
public class ResponseMessage {

    private int status = 0;

    private String message = "Success";

    public void setError(final int statusCode, final String responseMessage) {
        status = statusCode;
        message = responseMessage;
    }
}

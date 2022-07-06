package com.codedev.demo.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.ToString;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@AllArgsConstructor
@ToString
public class EmailResponse {

    private boolean success;
    private String jobId;
    private String jobGroup;
    private String message;

    // Constructor
    public EmailResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

}

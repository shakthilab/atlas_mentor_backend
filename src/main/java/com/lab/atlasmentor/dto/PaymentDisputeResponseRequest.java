package com.lab.atlasmentor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class PaymentDisputeResponseRequest {
    
    @NotBlank(message = "Response is required")
    @Size(max = 1000, message = "Response must not exceed 1000 characters")
    private String response;
    
    private Boolean acceptDispute;
    
    public PaymentDisputeResponseRequest() {}
    
    public PaymentDisputeResponseRequest(String response, Boolean acceptDispute) {
        this.response = response;
        this.acceptDispute = acceptDispute;
    }
    
    public String getResponse() {
        return response;
    }
    
    public void setResponse(String response) {
        this.response = response;
    }
    
    public Boolean getAcceptDispute() {
        return acceptDispute;
    }
    
    public void setAcceptDispute(Boolean acceptDispute) {
        this.acceptDispute = acceptDispute;
    }
}

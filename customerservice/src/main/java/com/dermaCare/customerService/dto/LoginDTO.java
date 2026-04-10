package com.dermaCare.customerService.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginDTO {
	
	@NotBlank(message = "userName is required")
    private String userName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be 10 digits and start with 6-9")
    private String mobileNumber;

    private String otp;

    @NotBlank(message = "Device ID is required")
    private String deviceId;

}

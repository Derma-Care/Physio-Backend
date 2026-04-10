package com.dermacare.doctorservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChangeDoctorPasswordDTO {
	private String username;
    private String currentPassword;
    private String newPassword;
    private String confirmPassword;

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword != null ? currentPassword.trim() : null;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword != null ? newPassword.trim() : null;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword != null ? confirmPassword.trim() : null;
    }
}


package com.AdminService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ClinicAdminWebAdsDTO {
    private String adId;
    private String mediaUrlOrImage;  // Base64 image or video URL
}

package com.AdminService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorWebAdsDTO {
    private String adId;
    private String mediaUrlOrImage;  // Base64 image or video URL
}

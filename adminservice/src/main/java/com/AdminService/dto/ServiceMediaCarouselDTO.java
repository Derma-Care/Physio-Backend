package com.AdminService.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceMediaCarouselDTO {
	  private String carouselId;
	  private String mediaUrlOrImage;  // Base64 image or video URL
}

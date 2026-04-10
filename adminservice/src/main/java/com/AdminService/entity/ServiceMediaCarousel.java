package com.AdminService.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ServiceMediaCarousel {
	@Id
	 private ObjectId carouselId;
	 private String mediaUrlOrImage; // base64 image or video URL
}

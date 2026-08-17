package com.chiselon.customerservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AccessTokenAndRefreshToken {
	
	private String accessToken;
	private String RefreshToken;
	private String accessTokenExpireTime;

}

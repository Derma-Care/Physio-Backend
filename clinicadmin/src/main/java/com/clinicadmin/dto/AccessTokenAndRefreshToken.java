package com.clinicadmin.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL) 
public class AccessTokenAndRefreshToken {
	
	private String accessToken;
	private String RefreshToken;
	private String accessTokenExpireTime;
	public String hospitalId;
	public String hospitalName;
	public String branchId;

}

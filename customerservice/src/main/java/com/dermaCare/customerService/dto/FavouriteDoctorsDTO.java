package com.dermaCare.customerService.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FavouriteDoctorsDTO {
	
	private String doctorId;
	private String hospitalId;
	private boolean favourite;

}

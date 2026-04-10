package com.dermaCare.customerService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "favouriteDoctors")
public class FavouriteDoctorsEntity {

	@Id
	private String id;
	private String doctorId;
	private String hospitalId;
	private boolean favourite;
}

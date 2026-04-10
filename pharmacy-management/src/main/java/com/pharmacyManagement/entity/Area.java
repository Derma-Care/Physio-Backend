package com.pharmacyManagement.entity;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "areas")
public class Area {
	@Id
	private String id;
	private List<String> areaNames;
	private String cityId;
	private String cityName;
}

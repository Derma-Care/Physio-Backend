package com.dermacare.category_services.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "services")
public class Services{
	@Id
	private ObjectId serviceId;
	private String serviceName;
	private String categoryName;
	private ObjectId categoryId;
	private String description;
	private byte[] serviceImage;
}

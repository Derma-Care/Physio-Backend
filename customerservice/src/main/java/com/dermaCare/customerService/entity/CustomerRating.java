package com.dermaCare.customerService.entity;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "customer_ratings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerRating {
    @Id
    private ObjectId id; // MongoDB ObjectId
    private double doctorRating;
    private double branchRating;
    private String feedback;
    private String hospitalId;
    private String branchId;
    private String doctorId; // Provider's mobile number
    private String customerMobileNumber;
    private String patientId;
    private String patientName;
    private String appointmentId;
    private boolean rated;
    private String dateAndTimeAtRating;
    
    public boolean getRated() {
    	return rated;
    }
}


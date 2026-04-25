package com.clinicadmin.entity;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;
@Data
@Document(collection = "therapy_exercises")
@JsonInclude(JsonInclude.Include.NON_NULL)

public class TherapyExercises {

    @Id
    private String id;

    private String therapyExercisesId; 

    private String clinicId;
    private String branchId;
    private String name;
    private String video;
    private String image;
    private String session;
    private String duration;
    private String frequency;
    private String notes;

    // ✅ Newly Added Fields
    private double pricePerSession;
    private double discountPercentage;
    private double discountAmount;
    private double gst;
    private double otherTax;
    private int sets;
    private int repetitions;
    private int totalPrice;
    
}
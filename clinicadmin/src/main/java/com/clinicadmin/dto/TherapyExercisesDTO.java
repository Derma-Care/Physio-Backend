package com.clinicadmin.dto;

import lombok.Data;

@Data
public class TherapyExercisesDTO {

    private String clinicId;
    private String branchId;
    private String therapyExercisesId;
    private String name;
    private String video;
   private String image;

    private String session;
    private String duration;
    private String frequency;
    private String notes;

    private double pricePerSession;
    private double discountPercentage;
    private double discountAmount;

    private double gst;
    private double otherTax;

    private int sets;
    private int repetitions;
    private int totalPrice;

    
}
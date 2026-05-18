package com.dermaCare.customerService.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.*;

@Document(collection = "TherapyRecord")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TherapyRecord {

    @Id
    private String therapyrecordid;

  
    private String  clincinid;

    
    private String brnchid;

  
    private String patientid;

  
    private String name;

   
    private Integer setsdone;
    private String doctorid;
   
    private boolean repitationdone;

   
    private Boolean sessioncompleted;

   
    private String notes;

    
    private byte[] beforeImage;
    private byte[]  afterImage;
    private byte[]  beforeVideo;
    private byte[] afterVideo;
}

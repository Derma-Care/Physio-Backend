package com.dermaCare.customerService.entity;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.dermaCare.customerService.dto.TherophyRecordListDTO;

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
    private String duration;
  
    private String patientid;

    private String doctorid;
    private String name;
    private String status;
    private String excerciseId;
    private Integer sessioncountremaining;
    private String frequancy;
   
    private List<TherophyRecordList> therapyrecord;
}

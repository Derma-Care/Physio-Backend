package com.dermaCare.customerService.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TherapyRecordDTO {

	private String id;
    private String therapyrecordid;

    @NotNull(message = "Clinic ID is required")
    private String clincinid;
    private String doctorid;

    @NotNull(message = "Branch ID is required")
    private String brnchid;

    @NotNull(message = "Patient ID is required")
    private String patientid;
    private String duration;
    private String name;
    private String excerciseId;
    private String status;
    private Integer sessioncountremaining;
    private String frequancy;
   private List<TherophyRecordListDTO> therapyrecord;
}
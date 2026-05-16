package com.dermaCare.customerService.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TherapyRecordDTO {

    private String therapyrecordid;

    @NotNull(message = "Clinic ID is required")
    private String clincinid;
    private String doctorid;

    @NotNull(message = "Branch ID is required")
    private String brnchid;

    @NotNull(message = "Patient ID is required")
    private String patientid;

    private String name;

    private String setsdone;

    private boolean repitationdone;

    private Boolean sessioncompleted;

    private String notes;

    private String image;

    private String video;
}
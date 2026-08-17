package com.chiselon.clinicadmin.dto;


import java.util.List;

import lombok.Data;

@Data
public class TherapyResponseDTO {
    private String therapyId;
    private String therapyName;

    private List<ExerciseResponseDTO> exercises;


				
	
}

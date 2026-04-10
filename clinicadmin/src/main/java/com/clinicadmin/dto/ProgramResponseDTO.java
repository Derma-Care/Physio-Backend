package com.clinicadmin.dto;

import java.util.List;
import lombok.Data;

@Data
public class ProgramResponseDTO {

//    private String programId;
//    private String programName;
    private List<TherapyResponseDTO> therapyData;
}
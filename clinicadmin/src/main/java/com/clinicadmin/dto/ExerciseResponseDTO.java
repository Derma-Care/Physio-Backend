package com.clinicadmin.dto;


import lombok.Data;
import java.util.List;

@Data
public class ExerciseResponseDTO {
	private String  exerciseId;
    private String exerciseName;
    private String frequency;
    private int sets;
    private int repetitions;
    private int noOfSessions;

    private List<SessionDTO> sessions;
		

}

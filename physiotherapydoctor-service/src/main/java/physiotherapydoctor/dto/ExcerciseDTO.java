package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class ExcerciseDTO {
	 private String exerciseId;
	    private String exerciseName;
	    private Integer sets;
	    private Integer repetitions;
	    private String notes;
	    private String videoUrl;

}

package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class DoctorTherapyExercise {
	private String therapyExercisesId;
	private String name;
	private String session;
	private String frequency;
	private String notes;
	private Integer sets;
	private Integer repetitions;
	private String videoUrl;
	private Double totalPrice;;

}

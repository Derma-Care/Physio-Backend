package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class TherapyExercise {

//	private String id;
	private String therapyExercisesId;

	private String name;
	private String session;
	private String frequency;
	private String notes;

	private int sets;
	private int repetitions;
	private String videoUrl;

	private double totalPrice;
}
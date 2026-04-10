package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class ExercisePlan {

	private List<HomeExercise> homeExercises;
	private String homeAdvice;
}

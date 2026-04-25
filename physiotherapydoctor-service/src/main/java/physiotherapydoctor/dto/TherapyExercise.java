package physiotherapydoctor.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TherapyExercise {

	private String exerciseId;
	private String exerciseName;

	private Double pricePerSession;
	private Integer noOfSessions;
	private Double totalExercisePrice;

	private String paymentStatus;

	private Integer repetitions;
	private String frequency;
	private Integer sets;
	private String youtubeUrl;
	private String notes;

	private List<Session> sessions;
}
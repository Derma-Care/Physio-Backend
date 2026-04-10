package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class DoctorSuggestExercise {
	private String exerciseId;
	private String exerciseName;
	private String frequency;
	private int noOfSession;
	private double pricePerSession;
	private int repetitions;
	private int sets;
	private double totalSessionCost;
	private List<Session> sessions;
	private String youtubeUrl;
}

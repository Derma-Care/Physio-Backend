package physiotherapydoctor.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TherapyExercise {

	private String exerciseId; //
	private String exerciseName; //

	private Double pricePerSession; //
	private Integer noOfSessions;
	private Double totalExercisePrice;

	private String paymentStatus;

	private Integer repetitions;
	private String frequency;
	private Integer sets;
	private String youtubeUrl;
	private String notes;
// ........New fields......
	private String technique;
	private String machine;
	private String intensity;
	private String assistanceLevel;
	private String type;
	private String area;
	private String metric;
	private String value;
	private String unit;
	private String bodyPart;
	// ✅ Activity Fields
	private String activityType;
	private String activityDuration;
	private List<Session> sessions;
}
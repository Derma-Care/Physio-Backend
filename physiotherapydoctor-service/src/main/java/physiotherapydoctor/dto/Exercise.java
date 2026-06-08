package physiotherapydoctor.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Exercise {

	private String exerciseId;
	private String exerciseName;
	private Double totalSessionCost;
	private Integer pricePerSession;
	private Integer noOfSessions;
	private double discountAmount;
	private double gst;
	private double otherTax;
	private double totalPrice;
	private Integer sets;
	private Integer repetitions;
	private String frequency;
	private String notes;
	private String youtubeUrl;
	private double discountPercentage;
	private Double totalExercisePrice;
	private String paymentStatus;
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

}

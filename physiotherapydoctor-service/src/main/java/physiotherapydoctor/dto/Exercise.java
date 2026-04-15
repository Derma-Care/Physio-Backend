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
	private Integer sets;
	private Integer repetitions;
	private Integer frequancy;
	private String notes;
	private String videoUrl;

}

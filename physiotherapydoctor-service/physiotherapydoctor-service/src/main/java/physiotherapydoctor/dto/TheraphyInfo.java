package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class TheraphyInfo {
	 private String therapyId;
	    private String therapyName;
	    private Double totalPrice;
	    private List<Exercise> exercises;

}

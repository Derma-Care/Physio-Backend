package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class TherapyAnswer {
	private String questionKey;
	private String questionId;
	private String question;
	private String answer;
}

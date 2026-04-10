package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class FollowUp {

	private String nextVisitDate;
	private String reviewNotes;
//    private String continueTreatment;
	private String modifications;
}
package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class SubjectiveAssessment {
	private String chiefComplaint;
	private int painScale;
	private String painType;
	private String duration;
	private String onset;
	private String aggravatingFactors;
	private String relievingFactors;
	private String observations;
//	private String posture;
//	private String rangeOfMotion;
//	private String specialTests;
}

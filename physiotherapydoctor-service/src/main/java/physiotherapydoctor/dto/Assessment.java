package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class Assessment {

    private String chiefComplaint;
    private String painScale;
    private String painType;
    private String duration;
    private String onset;
    private String aggravatingFactors;
    private String relievingFactors;
    private String posture;
    private String rangeOfMotion;
    private String specialTests;
    private String observations;
}

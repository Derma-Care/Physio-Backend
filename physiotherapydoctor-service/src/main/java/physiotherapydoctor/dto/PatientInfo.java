package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class PatientInfo {
    private String patientId;
    private String patientName;
    private String mobileNumber;
    private int age;
    private String sex;
}
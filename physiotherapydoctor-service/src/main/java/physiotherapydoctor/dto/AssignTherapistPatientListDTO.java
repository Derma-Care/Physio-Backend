package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class AssignTherapistPatientListDTO {
	private String bookingId;
	private String patientId;
	private String patientName;
	private String mobileNumber;
	private int age;
	private String sex;
	private String doctorId;
	private String doctorName;
	private String therapistId;
	private String therapistName;
	private String therapistRecordId;
	private String programId;;
	private String programName;
	private String serivceType; // program or package
	private String clinicId;
	private String branchId;
	private String overallStatus; // 1 pending  2= active , 3=completed
	
	

}

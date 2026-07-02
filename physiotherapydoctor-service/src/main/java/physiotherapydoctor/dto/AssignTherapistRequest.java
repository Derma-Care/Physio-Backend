package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class AssignTherapistRequest {

    private String clinicId;
    private String branchId;

    private String assignTherapistId;
    private String assignTherapistName;

    private String assignedTherapistId;
    private String assignedTherapistName;

    private String assignedStatus;
    private String therapistRecordId;
}
package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class FirstVisitHistoryRequest {

    private String doctorId;
    private String patientId;
    private String bookingId;
    private String clinicId;
    private String branchId;

}

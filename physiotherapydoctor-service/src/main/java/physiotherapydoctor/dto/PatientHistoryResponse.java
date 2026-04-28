
package physiotherapydoctor.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.Data;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)

public class PatientHistoryResponse {

    private String bookingId;

    private String patientId;
    private String patientName;

    private String doctorId;
    private String doctorName;

    private String therapistId;
    private String therapistName;

    private String bookingDate;
    private String bookingTime;

    private List<TherapistRecordDetails> therapistRecordId;
}
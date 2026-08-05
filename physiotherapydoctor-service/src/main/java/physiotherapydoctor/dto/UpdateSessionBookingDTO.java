package physiotherapydoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UpdateSessionBookingDTO {
    private String clinicId;
    private String branchId;
    private String bookingId;
    private String patientId;
    private String sessionId;
    private String date;
    private String slot;
    private String bookingStatus;
}
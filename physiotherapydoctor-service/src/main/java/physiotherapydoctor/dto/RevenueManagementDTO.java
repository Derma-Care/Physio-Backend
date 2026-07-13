package physiotherapydoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RevenueManagementDTO {

    private String patientName;
    private String bookingId;
    private String serviceDate;
    private String serviceTime;
    private String therapistId;
    private String therapistName;
    private String therapistRecordId;
    private String doctorName;
    private Double consultationFee;
    private Double therapyFee;

    private Double finalAmount;
    private Double dueAmount;
}
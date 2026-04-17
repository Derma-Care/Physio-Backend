package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class Program {

    private String programId;
    private String programName;
//    private Double totalPrice;
    private Double totalProgramPrice;  // ✅ RENAME
    private String paymentStatus;      // ✅ ADD

    private List<TherapyData> therapyData;

	
}

package physiotherapydoctor.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TherapyWithSessions {

    private String packageId;
    private String packageName;
    private Double totalPackagePrice;   // ✅ Added
    private String paymentStatus;       // ✅ Added
    
    private List<Program> programs;
}

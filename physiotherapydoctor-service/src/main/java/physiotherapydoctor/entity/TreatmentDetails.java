package physiotherapydoctor.entity;


import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TreatmentDetails {
    private List<Dates> dates;
    private String reason;
    private String frequency;
    private Integer sittings;        // Remaining sittings
    private String startDate;
    private Integer totalSittings;

    // ✅ Sitting summary fields
    private Integer takenSittings;   // Completed sittings
    private Integer pendingSittings; // Remaining sittings (for convenience)
    private Integer currentSitting;  // Last completed sitting number
}


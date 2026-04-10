package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;
import physiotherapydoctor.entity.PhysiotherapyRecord;

@Data
public class TherapistDashboardResponse {

    private int todayPatientCount;
    private long todayWorkingMinutes;

    private int weeklyPatientCount;
    private long weeklyWorkingMinutes;

    private int monthlyPatientCount;
    private long monthlyWorkingMinutes;

    private List<PhysiotherapyRecord> records;
}

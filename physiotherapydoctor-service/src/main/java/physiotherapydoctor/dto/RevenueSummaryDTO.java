package physiotherapydoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RevenueSummaryDTO {

    private double todayRevenue;

    private double lastWeekRevenue;

    private double lastMonthRevenue;

    private double lastYearRevenue;
}

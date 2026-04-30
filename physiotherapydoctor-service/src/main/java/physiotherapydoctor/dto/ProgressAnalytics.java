package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class ProgressAnalytics {

    private List<Integer> painTrend;
    private List<Integer> mobilityTrend;
    private List<Integer> strengthTrend;
    private List<String> sessionDates;
}
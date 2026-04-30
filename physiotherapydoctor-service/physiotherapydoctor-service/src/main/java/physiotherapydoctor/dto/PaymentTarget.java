package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class PaymentTarget {

    private List<String> packageIds;
    private List<String> programIds;
    private List<String> therapyIds;
    private List<String> exerciseIds;
    private List<String> sessionIds;
}
package physiotherapydoctor.dto;

import java.util.List;

import lombok.Data;

@Data
public class Program {

    private String programId;
    private String programName;
    private Double totalPrice;

    private List<TherapyData> therapyData;
}

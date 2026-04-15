package physiotherapydoctor.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentHistory {

    private Double amount;
    private String paymentMode;
    private String paymentType;
    private String paymentDate;

    private Double discountAmount;
    private String discountIssuedBy;
}

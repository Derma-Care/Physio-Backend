package physiotherapydoctor.dto;

import lombok.Data;

@Data
public class PaymentHistory {

    private String transactionId;

    private double amount;
    private String date;
    private String paymentMode;
    private String paymentType;
    private String discountIssuedBy;
    private double paymentPercent;
}

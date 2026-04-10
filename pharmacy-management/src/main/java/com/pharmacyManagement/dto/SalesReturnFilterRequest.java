package com.pharmacyManagement.dto;

import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import com.pharmacyManagement.enums.ReturnType;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SalesReturnFilterRequest {

    private String patientName;
    private String mobileNo;
    private String billNo;
    private ReturnType returnType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate fromDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate toDate;

    private String clinicId;
    private String branchId;
}

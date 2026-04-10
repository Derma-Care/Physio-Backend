package com.pharmacyManagement.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

import com.pharmacyManagement.entity.Medicine;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OpNoResponse {

    private String opNo;
    private String visitType;   // "existing" or "new"
    private String patientName;
    private String mobile;
    private Integer age;
    private String sex;
    private String doctorName;
    private List<String> medicines;
    private String clinicId;
    private String branchId;
}
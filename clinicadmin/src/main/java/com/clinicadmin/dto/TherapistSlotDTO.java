package com.clinicadmin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TherapistSlotDTO {
    private String therapistId;
    private String clinicId;
    private String branchId;
    private String branchName;
    private String date;
    private int slotInterval;
    private String openingTime;
    private String closingTime;
    private List<DoctorAvailableSlotDTO> availableSlots; // reuse same shape
}
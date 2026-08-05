package com.clinicadmin.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import com.clinicadmin.dto.DoctorAvailableSlotDTO;
import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Document(collection = "therapist_slots")
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TherapistSlot {
    @Id
    private String id;
    private String therapistId;
    private String clinicId;   // matches Therapist.clinicId naming
    private String branchId;
    private String branchName;
    private String date; // yyyy-MM-dd
    private int slotInterval;
    private List<DoctorAvailableSlotDTO> availableSlots; // reuse same slot shape

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Indexed(name = "therapistSlotExpiryIndex", expireAfter = "02d")
    private LocalDateTime createdAt = LocalDateTime.now();
}
package com.dermacare.notification_service.dto;
import java.util.List;
import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Medicines {
    private UUID id = UUID.randomUUID();
    private String name;
    private String dose;
    private String duration;
    private String durationUnit;
    private String food;
    private String medicineType;
    private String note;
    private String remindWhen;
    private String others;
   
    private List<String> times;
}


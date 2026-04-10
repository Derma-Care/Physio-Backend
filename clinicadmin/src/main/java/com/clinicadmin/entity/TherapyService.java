package com.clinicadmin.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Document(collection = "therapyService")
public class TherapyService {

    @Id
    private String id;

    private int consentType;

    private List<String> exerciseIds;

    private String therapyName;

    private String clinicId;

    private String branchId;
    private int noExerciseIdCount;
    
}

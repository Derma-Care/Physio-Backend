package com.dermacare.doctorservice.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "ListOfMedicines")
public class ListOfMedicines {
    @Id
    private String id;
    private String clinicId;
    private List<String> listOfMedicines;
}
package com.dermacare.doctorservice.model;




import lombok.*;

import java.util.List;



@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionDetails {

    private List<Medicines> medicines;
    
  }


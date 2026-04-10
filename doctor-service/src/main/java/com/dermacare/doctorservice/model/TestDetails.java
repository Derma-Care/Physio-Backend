package com.dermacare.doctorservice.model;


import lombok.*;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestDetails {
	
    private List<String> selectedTests;
    private String testReason;
}


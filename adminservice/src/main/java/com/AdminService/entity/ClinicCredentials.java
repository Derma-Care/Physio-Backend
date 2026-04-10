package com.AdminService.entity;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "clinic_credentials")
public class ClinicCredentials {

    @Id
    private String id;  
    private String hospitalName;
    private String userName;
    private String password;
    private String role;
    private Map<String, Map<String, List<String>>> permissions;


    
	}
    

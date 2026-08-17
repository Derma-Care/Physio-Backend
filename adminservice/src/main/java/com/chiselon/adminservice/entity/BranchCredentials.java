package com.chiselon.adminservice.entity;

import java.util.List;
import java.util.Map;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "branchCredentials")
public class BranchCredentials {
    @Id
    private String id;

    private String branchId; 
    private String userName;  
    private String password;  
    private String branchName;
    private List<String> roles;
    private Map<String, Map<String, List<String>>> permissions;

}


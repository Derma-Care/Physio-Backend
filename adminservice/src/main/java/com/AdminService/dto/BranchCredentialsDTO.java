package com.AdminService.dto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BranchCredentialsDTO {
    private String branchId;     // e.g., H_1-B_2
    private String userName;     // same as branchId or custom username
    private String password;     // generated password
    private String branchName;   // ENT Branch
}

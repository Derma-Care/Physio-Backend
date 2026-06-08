package com.clinicadmin.utils;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Component
public class ClinicRelatedInfo {
	
	private List<String> roles;	
	private Map<String, Map<String, List<String>>> permissions;


}

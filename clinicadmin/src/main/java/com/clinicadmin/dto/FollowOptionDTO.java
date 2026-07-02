package com.clinicadmin.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FollowOptionDTO {
	private String id;
	List<FollowUpOptionData> followOptions;
	public Object getClinicId() {
		// TODO Auto-generated method stub
		return null;
	}
	public Object getBranchId() {
		// TODO Auto-generated method stub
		return null;
	}
}

package com.dermaCare.customerService.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DoctorSlotDTO {
	private String doctorId;
	private String date;
	private List<DoctorAvailableSlotDTO> availableSlots;
}

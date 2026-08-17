package com.chiselon.bookingService.dto;

import java.util.List;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TtdAppointments {
	
	private List<List<BookingResponse>> appointments;

}

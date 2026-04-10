package com.dermaCare.customerService.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ResBody<T> {
	private String message;
	private int status;
	private T data;

}

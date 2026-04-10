package com.dermaCare.customerService.util;

import org.springframework.stereotype.Component;

import com.dermaCare.customerService.dto.CustomerDTO;
import com.dermaCare.customerService.entity.Customer;

@Component
public class HelperForConversion {

	public static CustomerDTO convertToDTO(Customer customer) {
        if (customer == null) {
            return null;
        }
        CustomerDTO customerDto = new CustomerDTO();
        customerDto.setFullName(customer.getFullName());
        customerDto.setCustomerId(customer.getCustomerId());
        customerDto.setMobileNumber(customer.getMobileNumber());  // Assuming mobileNumber is long, converting to String
        customerDto.setGender(customer.getGender());
        customerDto.setEmailId(customer.getEmailId());
        customerDto.setDeviceId(customer.getFcm());
        customerDto.setDateOfBirth(customer.getDateOfBirth());
        customerDto.setReferCode(customer.getReferCode());
        customerDto.setDeviceId(customer.getDeviceId());
        return  customerDto;
       
    }
	
	
	public static Customer convertToEntity(CustomerDTO customerDTO) {
        if (customerDTO == null) {
            return null;
        }
        Customer customer = new Customer();
        customer.setFullName(customerDTO.getFullName());
        customer.setCustomerId(customerDTO.getCustomerId());
        customer.setMobileNumber(customerDTO.getMobileNumber());  // Assuming mobileNumber is long, converting to String
        customer.setGender(customerDTO.getGender());
        customer.setEmailId(customerDTO.getEmailId());
        customer.setDateOfBirth(customerDTO.getDateOfBirth());
        customer.setReferCode(customerDTO.getReferCode());
        customer.setFcm(customerDTO.getDeviceId());
        return  customer;
       
    }
	
}



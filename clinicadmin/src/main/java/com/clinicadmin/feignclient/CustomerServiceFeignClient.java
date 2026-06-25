package com.clinicadmin.feignclient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import com.clinicadmin.dto.Response;
import com.clinicadmin.entity.QuestionsByPartEntity;


@FeignClient(name = "customerservice")
public interface CustomerServiceFeignClient {


	@GetMapping("/api/customer/getByKey/{key}")
	   public ResponseEntity<QuestionsByPartEntity> getByKey(@RequestHeader("Authorization") String token,@PathVariable String key);
	
}
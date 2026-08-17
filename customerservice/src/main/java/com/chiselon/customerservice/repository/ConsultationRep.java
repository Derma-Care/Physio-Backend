package com.chiselon.customerservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.customerservice.entity.ConsultationEntity;

public interface ConsultationRep extends MongoRepository<ConsultationEntity, String> {

	ConsultationEntity findByconsultationId(String id);
	ConsultationEntity findByConsultationType(String type);
	
	
	
}

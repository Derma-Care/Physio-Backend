package com.dermaCare.customerService.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dermaCare.customerService.entity.ConsultationEntity;

public interface ConsultationRep extends MongoRepository<ConsultationEntity, String> {

	ConsultationEntity findByconsultationId(String id);
	ConsultationEntity findByConsultationType(String type);
	
	
	
}

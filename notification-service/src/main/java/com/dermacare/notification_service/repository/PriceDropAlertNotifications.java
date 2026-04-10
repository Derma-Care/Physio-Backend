package com.dermacare.notification_service.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.dermacare.notification_service.entity.PriceDropAlertEntity;

public interface PriceDropAlertNotifications extends MongoRepository<PriceDropAlertEntity, String> {

	List<PriceDropAlertEntity> findByClinicIdAndBranchId(String cid,String bid);
	PriceDropAlertEntity findByClinicIdAndBranchIdAndId(String cid,String bid,String id);
	
}

package com.chiselon.notificationservice.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import com.chiselon.notificationservice.entity.PriceDropAlertEntity;

public interface PriceDropAlertNotifications extends MongoRepository<PriceDropAlertEntity, String> {

	List<PriceDropAlertEntity> findByClinicIdAndBranchId(String cid,String bid);
	PriceDropAlertEntity findByClinicIdAndBranchIdAndId(String cid,String bid,String id);
	
}

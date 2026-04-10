package com.AdminService.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import com.AdminService.entity.ClinicAdminWebAds;

public interface ClinicAdminWebAdsRepository extends MongoRepository<ClinicAdminWebAds, ObjectId> {
}

package com.AdminService.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;

import com.AdminService.entity.DoctorWebAds;

public interface DoctorWebAdsRepository extends MongoRepository<DoctorWebAds, ObjectId> {
}

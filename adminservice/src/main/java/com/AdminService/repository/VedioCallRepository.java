package com.AdminService.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.AdminService.entity.VedioCall;

@Repository
public interface VedioCallRepository extends MongoRepository<VedioCall, ObjectId> {

}
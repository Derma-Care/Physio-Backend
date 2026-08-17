package com.chiselon.adminservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.chiselon.adminservice.entity.QuetionsAndAnswerForAddClinic;

@Repository
public interface QuetionsAndAnswerForAddClinicRepository extends MongoRepository<QuetionsAndAnswerForAddClinic, String> {

}
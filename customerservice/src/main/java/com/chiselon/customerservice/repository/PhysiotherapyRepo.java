package com.chiselon.customerservice.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import com.chiselon.customerservice.entity.QuestionsByPartEntity;


public interface PhysiotherapyRepo extends MongoRepository<QuestionsByPartEntity, String> {


}

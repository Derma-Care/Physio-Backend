package com.dermaCare.customerService.repository;


import org.springframework.data.mongodb.repository.MongoRepository;
import com.dermaCare.customerService.entity.QuestionsByPartEntity;


public interface PhysiotherapyRepo extends MongoRepository<QuestionsByPartEntity, String> {


}

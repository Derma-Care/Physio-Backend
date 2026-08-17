package com.chiselon.customerservice.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.chiselon.customerservice.entity.FavouriteDoctorsEntity;

public interface CustomerFavouriteDoctors extends MongoRepository<FavouriteDoctorsEntity,String> {

}

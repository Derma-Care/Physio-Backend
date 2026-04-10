package com.dermaCare.customerService.repository;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.dermaCare.customerService.entity.FavouriteDoctorsEntity;

public interface CustomerFavouriteDoctors extends MongoRepository<FavouriteDoctorsEntity,String> {

}

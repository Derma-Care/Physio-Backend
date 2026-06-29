package com.AdminService.repository;

import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import com.AdminService.entity.RegisterAndLoginEntity;



@Repository
public interface CredentialsRepository extends MongoRepository<RegisterAndLoginEntity, String> {

	@Query("{ 'userName': ?0, 'password': ?1 }")
	Optional<RegisterAndLoginEntity> findByUsernameAndPassword(String userName, String password);
	Optional<RegisterAndLoginEntity> findByUserName(String userName);
	RegisterAndLoginEntity findByMobileNumber(String mnumber);
}
package com.dermaCare.customerService.util;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Component;
import com.dermaCare.customerService.entity.QuestionsByPartEntity;


@Component
public class GetByKey {
	
	@Autowired
	private MongoTemplate mongoTemplate;
	
	public QuestionsByPartEntity getByKey(String key) {
	    try {
	        Query query = new Query(
	                Criteria.where("questionsByPart." + key).exists(true)
	        );

	       QuestionsByPartEntity list =
	                mongoTemplate.findOne(query, QuestionsByPartEntity.class);

	        return list;

	    } catch (Exception e) {
	        return null;
	    }
	}

}

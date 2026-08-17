package com.chiselon.clinicadmin.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import com.chiselon.clinicadmin.entity.Counter;

@Service
public class SequenceGeneratorService {

	@Autowired
	private MongoTemplate mongoTemplate;

	public long getNextSequence(String key) {
		Query query = new Query(Criteria.where("_id").is(key));
		Update update = new Update().inc("seq", 1);
		FindAndModifyOptions options = new FindAndModifyOptions().returnNew(true).upsert(true);

		Counter counter = mongoTemplate.findAndModify(query, update, options, Counter.class);
		return counter.getSeq();
	}
}

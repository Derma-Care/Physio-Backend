package com.dermaCare.customerService.util;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.*;
import org.springframework.data.mongodb.core.query.*;
import org.springframework.stereotype.Service;
import com.dermaCare.customerService.entity.Counter;

@Service
public class SequenceGeneratorService {
	
	 @Autowired
	    private MongoOperations mongoOperations;

	    public long generateSequence(String seqName) {

	        Query query = new Query(Criteria.where("_id").is(seqName));

	        Update update = new Update().inc("seq", 1);

	        FindAndModifyOptions options =
	                new FindAndModifyOptions().returnNew(true).upsert(true);

	        Counter counter = mongoOperations.findAndModify(
	                query, update, options, Counter.class);

	        return counter != null ? counter.getSeq() : 1;
	    }

}

package com.groupon.relevance.ranking.service;

import com.groupon.relevance.ranking.model.Deal;
import com.groupon.relevance.ranking.model.User;
import org.apache.ignite.binary.BinaryObject;

public interface ScoringService {
    double score(Deal deal, User user);
    double score(BinaryObject binaryObject, User user);
}

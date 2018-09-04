package com.groupon.relevance.ranking.service;

import com.groupon.relevance.ranking.model.Deal;
import com.groupon.relevance.ranking.model.User;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.services.Service;
import org.apache.ignite.services.ServiceContext;

public class ScoringServiceImpl implements Service, ScoringService {

    private String svcName;

    @Override
    public double score(final Deal deal, final User user) {
        return deal.getPrice() * user.getAge() * Math.random();
    }

    @Override
    public double score(final BinaryObject binaryObject, final User user) {
        return (double) binaryObject.field("price") * user.getAge() * Math.random();
    }

    @Override
    public void cancel(final ServiceContext serviceContext) {
        System.out.println("Service was cancelled: " + svcName);
    }

    @Override
    public void init(final ServiceContext serviceContext) {
        svcName = serviceContext.name();

        System.out.println("Service was initialized: " + svcName);
    }

    @Override
    public void execute(final ServiceContext serviceContext) {
        System.out.println("Service was executed: " + svcName);
    }
}

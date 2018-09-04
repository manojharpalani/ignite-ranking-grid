package com.groupon.relevance.ranking.grid;

import com.groupon.relevance.ranking.model.Deal;
import com.groupon.relevance.ranking.model.ScoredDeal;
import com.groupon.relevance.ranking.model.User;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class TestDealRankingGrid {

    private static Set<String> dealUUIDs;

    public static void main(String args[]) {

        User user = new User(310);
        dealUUIDs = new HashSet<>();
        DealRankingGrid dealGrid = new DealRankingGrid("deal-grid-cache");
    
        for (int i=0; i<1000; i++) {
            UUID dealUUID = UUID.randomUUID();
            Deal deal = new Deal(dealUUID, Math.random());
            dealGrid.put(deal);
            dealUUIDs.add(dealUUID.toString());
        }

        long start = System.currentTimeMillis();
        dealGrid.getAll(dealUUIDs);
        long stop = System.currentTimeMillis();
        System.out.println("Get All Time elapsed: " + (stop-start) + " ms");

        start = System.currentTimeMillis();
        for (String dealUUID : dealUUIDs) {
            Deal deal = dealGrid.get(dealUUID);
        }
        stop = System.currentTimeMillis();
        System.out.println("Get For Each Time elapsed: " + (stop-start) + " ms");

        start = System.currentTimeMillis();
        dealGrid.broadcastTestCompute();
        stop = System.currentTimeMillis();
        System.out.println("Test Broadcast Time elapsed: " + (stop-start) + " ms");

        start = System.currentTimeMillis();
        List<ScoredDeal> scoredDeals = dealGrid.rankDealsWithAffinityAsync(dealUUIDs, user);
        stop = System.currentTimeMillis();
        System.out.println("Rank All With Async Affinity Call Time elapsed: " + (stop-start) + " ms");
        for(ScoredDeal scoredDeal : scoredDeals) {
            System.out.println(scoredDeal);
        }

        start = System.currentTimeMillis();
        scoredDeals = dealGrid.rankDealsWithCacheEntryProcessor(dealUUIDs, user);
        stop = System.currentTimeMillis();
        System.out.println("Rank All With Cache Entry Processor Time elapsed: " + (stop-start) + " ms");
        for(ScoredDeal scoredDeal : scoredDeals) {
            System.out.println(scoredDeal);
        }

        start = System.currentTimeMillis();
        scoredDeals = dealGrid.rankDealsWithCacheEntryProcessorBinary(dealUUIDs, user);
        stop = System.currentTimeMillis();
        System.out.println("Rank All With Cache Entry Processor Binary Time elapsed: " + (stop-start) + " ms");
        for(ScoredDeal scoredDeal : scoredDeals) {
            System.out.println(scoredDeal);
        }

        for(int i=0; i<500; i++) {
            start = System.currentTimeMillis();
            scoredDeals = dealGrid.rankDealsWithCacheEntryProcessorBinary(dealUUIDs, user);
            stop = System.currentTimeMillis();
            System.out.println("Rank All With Cache Entry Processor Binary Time elapsed: " + (stop-start) + " ms");
        }

    }
}

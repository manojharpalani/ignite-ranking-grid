package com.groupon.relevance.ranking.grid;

import com.groupon.relevance.ranking.model.Deal;
import com.groupon.relevance.ranking.model.ScoredDeal;
import com.groupon.relevance.ranking.model.User;
import com.groupon.relevance.ranking.service.ScoringService;
import com.groupon.relevance.ranking.service.ScoringServiceImpl;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.IgniteException;
import org.apache.ignite.IgniteServices;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteFuture;
import org.apache.ignite.resources.ServiceResource;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.EntryProcessorResult;
import javax.cache.processor.MutableEntry;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DealRankingGrid {

    private final String cacheName;
    private final Ignite ignite;
    private final IgniteCache<String, Deal>  cache;
    private final IgniteCache<String, BinaryObject>  binaryCache;
    private final IgniteCompute compute;

    public DealRankingGrid(final String cacheName) throws IgniteException {
        this.cacheName = cacheName;
        // Set ignite to client mode
        Ignition.setClientMode(true);

        //Start Ignite client
        ignite = Ignition.start("examples/config/example-ignite.xml");

        // Initialize cache
        CacheConfiguration<String, Deal> cfg = new CacheConfiguration<>();
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setOnheapCacheEnabled(true);
        cfg.setStatisticsEnabled(true);
        cfg.setName(cacheName);
        cache = ignite.getOrCreateCache(cfg);
        binaryCache = cache.withKeepBinary();
        System.out.println("Current cache size : " + cache.size(CachePeekMode.ONHEAP));

        // Initialize compute
        compute = ignite.compute();

        // Get an instance of IgniteServices for the cluster group.
        IgniteServices svcs = ignite.services(ignite.cluster().forServers());

        // Deploy per-node singleton. An instance of the service
        // will be deployed on every node within the cluster group.
        svcs.deployNodeSingleton("scoringService", new ScoringServiceImpl());
    }

    private static class ScoringCallable implements IgniteCallable<ScoredDeal> {

        private final IgniteCache<String, Deal> cache;
        private final String dealUUID;
        private final User user;

        @ServiceResource(serviceName = "scoringService")
        private ScoringService scoringService;

        private ScoringCallable(final IgniteCache<String, Deal> cache, final String dealUUID, final User user) {
            this.cache = cache;
            this.dealUUID = dealUUID;
            this.user = user;
        }

        @Override
        public ScoredDeal call() throws Exception {
            Deal deal = cache.localPeek(dealUUID);
            double score = scoringService.score(deal, user);
            return new ScoredDeal(dealUUID, score);
        }
    }

    private static class ScoringCacheEntryProcessor implements CacheEntryProcessor<String, Deal, Double> {

        private final User user;

        public ScoringCacheEntryProcessor(final User user) {
            super();
            this.user = user;
        }

        @ServiceResource(serviceName = "scoringService")
        private ScoringService scoringService;

        @Override
        public Double process(final MutableEntry<String, Deal> mutableEntry, final Object... objects) throws EntryProcessorException {
            return scoringService.score(mutableEntry.getValue(), user);
        }
    }

    private static class ScoringBinaryCacheEntryProcessor implements CacheEntryProcessor<String, BinaryObject, Double> {

        private final User user;

        public ScoringBinaryCacheEntryProcessor(final User user) {
            super();
            this.user = user;
        }

        @ServiceResource(serviceName = "scoringService")
        private ScoringService scoringService;

        @Override
        public Double process(final MutableEntry<String, BinaryObject> mutableEntry, final Object... objects) throws EntryProcessorException {
            return scoringService.score(mutableEntry.getValue(), user);
        }
    }

    public void put(final Deal deal) {
        cache.put(deal.getUUID().toString(), deal);
    }

    public Deal get(final String dealUUID) {
        return cache.get(dealUUID);
    }

    public Map<String, Deal> getAll(final Set<String> dealUUID) {
        return cache.getAll(dealUUID);
    }

    public List<ScoredDeal> rankDealsWithAffinityAsync(final Set<String> dealUUIDs, final User user) {
        List<IgniteFuture<ScoredDeal>> futs = new ArrayList<>();
        for (String dealUUID : dealUUIDs) {
            futs.add(compute.affinityCallAsync(cacheName, dealUUID, new ScoringCallable(cache, dealUUID, user)));
        }
        List<ScoredDeal> scoredDeals = futs.stream().map(future -> future.get()).collect(Collectors.toList());
        Collections.sort(scoredDeals, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));
        return scoredDeals;
    }

    public List<ScoredDeal> rankDealsWithCacheEntryProcessor(final Set<String> dealUUIDs, final User user) {
        Map<String, EntryProcessorResult<Double>> result = cache.invokeAll(dealUUIDs, new ScoringCacheEntryProcessor(user));
        List<ScoredDeal> scoredDeals = result.entrySet().stream().map((res) -> new ScoredDeal(res.getKey(), res.getValue().get())).collect(Collectors.toList());
        Collections.sort(scoredDeals, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));

        return scoredDeals;
    }

    public List<ScoredDeal> rankDealsWithCacheEntryProcessorBinary(final Set<String> dealUUIDs, final User user) {
        Map<String, EntryProcessorResult<Double>> result = binaryCache.invokeAll(dealUUIDs, new ScoringBinaryCacheEntryProcessor(user));
        List<ScoredDeal> scoredDeals = result.entrySet().stream().map((res) -> new ScoredDeal(res.getKey(), res.getValue().get())).collect(Collectors.toList());
        Collections.sort(scoredDeals, (o1, o2) -> Double.compare(o2.getScore(), o1.getScore()));

        return scoredDeals;
    }

    //TODO: See if Ignite Map Reduce could be used for final sorting
    public List<ScoredDeal> rankDealsWithIgniteReducer(final Set<String> dealUUIDs, final User user) {
        List<ScoredDeal> scoredDeals = null;

        return scoredDeals;
    }

    public void broadcastTestCompute() {
        ignite.compute().broadcast(() -> {
            System.out.println(">>> Hello Node! :)");
        });
    }

}

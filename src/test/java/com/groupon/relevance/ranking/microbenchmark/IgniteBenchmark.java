package com.groupon.relevance.ranking.microbenchmark;

import com.groupon.relevance.ranking.grid.DealRankingGrid;
import com.groupon.relevance.ranking.model.Deal;
import com.groupon.relevance.ranking.model.User;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@State(Scope.Benchmark)
public class IgniteBenchmark {

    private DealRankingGrid dealGrid = new DealRankingGrid("deal-grid-cache");
    private User user = new User(21);
    private Set<String> dealUUIDs;

    @Setup(Level.Invocation)
    public void setup() {
        dealUUIDs = new HashSet<>();
        for (int i=0; i<1000; i++) {
            UUID dealUUID = UUID.randomUUID();
            Deal deal = new Deal(dealUUID, Math.random());
            dealGrid.put(deal);
            dealUUIDs.add(dealUUID.toString());
        }
    }

    @Benchmark
    @BenchmarkMode(Mode.All)
    public void benchmarkRank() {
        dealGrid.rankDealsWithCacheEntryProcessorBinary(dealUUIDs, user);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(".*" + IgniteBenchmark.class.getSimpleName() + ".*")
                .warmupIterations(3)
                .measurementIterations(10)
                .threads(8)
                .forks(2)
                .build();

        new Runner(opt).run();
    }
}

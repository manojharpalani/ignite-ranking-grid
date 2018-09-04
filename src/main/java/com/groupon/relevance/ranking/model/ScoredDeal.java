package com.groupon.relevance.ranking.model;

public class ScoredDeal {
    private final double score;
    private final String dealUUID;

    public ScoredDeal(final String dealUUID, final double score) {
        this.score = score;
        this.dealUUID = dealUUID;
    }

    public double getScore() {
        return score;
    }

    public String getDealUUID() {
        return dealUUID;
    }

    @Override
    public String toString() {
        return "ScoredDeal{" +
               "score=" + score +
               ", dealUUID='" + dealUUID + '\'' +
               '}';
    }
}

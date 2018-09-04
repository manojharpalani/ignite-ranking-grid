package com.groupon.relevance.ranking.model;

import java.util.UUID;

public class Deal {
    private UUID id;
    private double price;

    public Deal(UUID id, double price) {
        this.id = id;
        this.price = price;
    }

    public UUID getUUID() {
        return id;
    }

    public double getPrice() {
        return price;
    }

    public String toString() {
        return "Deal - (" + id.toString() + "," + price + ")";
    }
}

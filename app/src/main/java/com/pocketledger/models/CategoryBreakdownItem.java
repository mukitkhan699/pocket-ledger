package com.pocketledger.models;

public class CategoryBreakdownItem {
    public String name;
    public String color;
    public long amount;
    public long total;

    public CategoryBreakdownItem(String name, String color, long amount, long total) {
        this.name = name;
        this.color = color;
        this.amount = amount;
        this.total = total;
    }
}

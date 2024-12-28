package dev.krutz.mc.skyblock;

public class Requirement {

    private String type;
    private String name;
    private int amount;

    public Requirement(String type, String name, int amount) {
        this.type = type;
        this.name = name;
        this.amount = amount;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public int getAmount() {
        return amount;
    }
}
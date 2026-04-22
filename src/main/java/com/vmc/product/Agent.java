package com.vmc.product;

public class Agent {
    Double cliamAount;
    Integer months;


    public Agent(Double cliamAount, Integer months) {
        this.cliamAount = cliamAount;
        this.months = months;
    }

    public Double getCliamAount() {
        return cliamAount;
    }

    public void setCliamAount(Double cliamAount) {
        this.cliamAount = cliamAount;
    }

    public Integer getMonths() {
        return months;
    }

    public void setMonths(Integer months) {
        this.months = months;
    }

    @Override
    public String toString() {
        return "Agent{" +
                "cliamAount=" + cliamAount +
                ", months=" + months +
                '}';
    }
}

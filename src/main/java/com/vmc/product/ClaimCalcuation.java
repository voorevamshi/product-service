package com.vmc.product;

import java.util.*;
import java.util.stream.Collectors;

public class ClaimCalcuation {

    public static void main(String[] args) {
        Agent agent1=new Agent(60000.0,7);
        Agent agent2=new Agent(40000.0,5);
        Agent agent3=new Agent(70000.0,5);
        Agent agent4=new Agent(40000.0,5);
        Agent agent5=new Agent(60000.0,5);
        List<Agent> agetntList=Arrays.asList(agent1,agent2,agent3,agent4,agent5);
       Double avgAmount= agetntList.stream().filter(agent->agent.getMonths()<=6).filter(agent -> agent.getCliamAount()>50000)
                .collect(Collectors.averagingDouble(agent->agent.getCliamAount()));
       System.out.println(avgAmount);
    }

}

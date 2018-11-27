package com.example.admin.project_iot;

public class ParamPot {
    private int id; //potId
    private int type;    //type of tree
    private int humid_max;
    private int humid_min;
    private boolean auto;
    private String MAC;

    public ParamPot(){

    }

    public ParamPot(String id, int type, int humid_max, int humid_min, boolean auto, String MAC){
        this.id = Integer.parseInt(id.substring(id.length()-3));
        this.type = type;
        this.humid_max = humid_max;
        this.humid_min = humid_min;
        this.auto = auto;
        this.MAC = MAC;
    }
}

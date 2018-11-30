package com.example.admin.project_iot;

public class ParamPot {
    private int id; //potId
    private int humid_max;
    private int humid_min;

    public static int potAvailable = 0;

    public ParamPot(){
        id = 0;
        humid_min = -1;
        humid_max = -1;
    }

    public void setId(int id) {
        this.id = id;
    }

    public void setHumid_max(int humid_max) {
        this.humid_max = humid_max;
    }

    public void setHumid_min(int humid_min) {
        this.humid_min = humid_min;
    }

    public int getId(){
        return id;
    }

    public int getHumid_max() {
        return humid_max;
    }

    public int getHumid_min() {
        return humid_min;
    }

    public ParamPot(int id, int humid_max, int humid_min){
        this.id = id;
        this.humid_max = humid_max;
        this.humid_min = humid_min;
    }
}

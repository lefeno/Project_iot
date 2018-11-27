package com.example.admin.project_iot;

public class ParamPlant {
    private String id;
    private String name;
    private String humid_max;
    private String humid_min;

    public ParamPlant(){

    }

    public ParamPlant(String paramID, String paramName, String humid_max, String humid_min) {
        this.id = paramID;
        this.name = paramName;
        this.humid_max = humid_max;
        this.humid_min = humid_min;
    }

    public String getID() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getHumid_max(){
        return humid_max;
    }

    public String getHumid_min() {
        return humid_min;
    }
}

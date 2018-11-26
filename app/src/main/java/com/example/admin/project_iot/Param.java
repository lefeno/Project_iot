package com.example.admin.project_iot;

public class Param {
    private String id;
    private String value;
    private String time;
    public Param(){

    }

    public Param(String paramID, String paramValue, String time) {
        this.id = paramID;
        this.value = paramValue;
        this.time = time;
    }

    public String getID() {
        return id;
    }

    public String getValue() {
        return value;
    }

    public String getCurrentTime(){
        return time;
    }
}

package com.shubham16.aiproject;

public class ContactsModel {
    private String name, mobile;

    public ContactsModel(String name, String mobile) {
        this.name = name;
        this.mobile = mobile;
    }

    public String getName() {
        return name;
    }

    public String getMobile() {
        return mobile;
    }
}
